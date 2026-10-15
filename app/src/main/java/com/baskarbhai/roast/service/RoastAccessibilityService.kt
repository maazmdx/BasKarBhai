package com.baskarbhai.roast.service

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.baskarbhai.roast.data.PreferencesManager
import com.baskarbhai.roast.ui.FloatingHudManager
import com.baskarbhai.roast.ui.RoastModalManager
import com.baskarbhai.roast.util.FilterUtils

/**
 * Core AccessibilityService for BasKarBhai.
 * Features strict scroll filtering, screen off dismissal, and rock-solid HUD lifecycle.
 * Engineered by maazmdx.
 */
class RoastAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "RoastAccessibility"
        private const val SCROLL_DEBOUNCE_MS = 2000L
        private const val SURFACE_ENTER_COOLDOWN_MS = 1200L
        private const val SESSION_TIMEOUT_MS = 10 * 60 * 1000L // 10 minutes

        const val PACKAGE_INSTAGRAM = "com.instagram.android"
        const val PACKAGE_YOUTUBE = "com.google.android.youtube"

        @Volatile
        var instance: RoastAccessibilityService? = null
            private set

        fun isServiceRunning(): Boolean = instance != null
    }

    private lateinit var hudManager: FloatingHudManager
    private lateinit var modalManager: RoastModalManager

    private val mainHandler = Handler(Looper.getMainLooper())
    private var isTimerRunning = false
    private var lastScrollTimestamp = 0L
    private var lastLeaveTimestamp = 0L
    private var enteredReelsTimestamp = 0L
    private var isCurrentlyInReelsSurface = false

    var sessionReelCount = 0
        private set
    var sessionActiveSeconds = 0L
        private set
    private var currentForegroundPackage: String? = null
    private var highestTierTriggered = 0

    // Screen off receiver to immediately hide HUD when device is locked/screen turned off
    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                Log.d(TAG, "Screen off detected — dismissing HUD immediately.")
                handleLeftTargetApp()
            }
        }
    }

    private val timerRunnable = object : Runnable {
        override fun run() {
            if (!isInTargetApp()) {
                handleLeftTargetApp()
                return
            }

            // Periodic sanity check: confirm active window package
            val activePkg = try {
                rootInActiveWindow?.packageName?.toString()
            } catch (_: Exception) {
                null
            }

            if (activePkg != null && !isInTargetApp(activePkg)) {
                currentForegroundPackage = activePkg
                handleLeftTargetApp()
                return
            }

            sessionActiveSeconds++
            PreferencesManager.addDailySeconds(1L)
            hudManager.updateCounts(sessionReelCount, sessionActiveSeconds)
            checkMilestones()
            mainHandler.postDelayed(this, 1000L)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.i(TAG, "BasKarBhai RoastAccessibilityService connected. Created by maazmdx.")

        hudManager = FloatingHudManager(this)
        modalManager = RoastModalManager(this) { actionId ->
            performGlobalAction(actionId)
        }

        try {
            val filter = IntentFilter(Intent.ACTION_SCREEN_OFF)
            registerReceiver(screenReceiver, filter)
        } catch (e: Exception) {
            Log.w(TAG, "Could not register screen off receiver: ${e.message}")
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val pkgName = event.packageName?.toString() ?: return

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                handleWindowStateChanged(pkgName)
            }
            AccessibilityEvent.TYPE_VIEW_SCROLLED -> {
                if (isInTargetApp(pkgName)) {
                    handleViewScrolled(pkgName, event)
                }
            }
        }
    }

    private fun handleWindowStateChanged(pkgName: String) {
        val wasInTarget = isInTargetApp()
        currentForegroundPackage = pkgName

        if (isInTargetApp(pkgName)) {
            val now = System.currentTimeMillis()
            if (lastLeaveTimestamp > 0 && (now - lastLeaveTimestamp > SESSION_TIMEOUT_MS)) {
                Log.d(TAG, "Session timed out (>10m). Resetting session counters.")
                resetSession()
            }

            if (PreferencesManager.isHudEnabled && !hudManager.isShowing()) {
                hudManager.show(sessionReelCount, sessionActiveSeconds)
            }

            if (!isTimerRunning) {
                isTimerRunning = true
                mainHandler.post(timerRunnable)
            }
        } else {
            if (wasInTarget) {
                lastLeaveTimestamp = System.currentTimeMillis()
            }
            handleLeftTargetApp()
        }
    }

    private fun handleLeftTargetApp() {
        isCurrentlyInReelsSurface = false
        isTimerRunning = false
        mainHandler.removeCallbacks(timerRunnable)

        if (hudManager.isShowing()) {
            hudManager.hide()
        }
        if (modalManager.isShowing()) {
            modalManager.hide()
        }
    }

    private fun handleViewScrolled(pkgName: String, event: AccessibilityEvent) {
        val now = System.currentTimeMillis()

        // 1. Debounce check
        if (now - lastScrollTimestamp < SCROLL_DEBOUNCE_MS) {
            return
        }

        // 2. Class check: must be a pager/recycler view
        val sourceClassName = event.className?.toString().orEmpty()
        val isLikelyPager = sourceClassName.contains("RecyclerView") ||
                sourceClassName.contains("ViewPager")

        if (!isLikelyPager) return

        // 3. Inspect source view physical bounds
        val sourceNode = try { event.source } catch (_: Exception) { null }
        if (sourceNode != null) {
            val bounds = Rect()
            sourceNode.getBoundsInScreen(bounds)
            val displayMetrics = resources.displayMetrics
            val screenHeight = displayMetrics.heightPixels
            val screenWidth = displayMetrics.widthPixels

            // The main Reels/Shorts pager MUST occupy at least 65% of screen height and 70% width
            // This discards comments sheets, horizontal tag carousels, and caption popups
            if (bounds.height() < screenHeight * 0.65f || bounds.width() < screenWidth * 0.70f) {
                return
            }

            // On Android 28+ (Android 10+), check vertical movement
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                if (event.scrollDeltaY == 0 && event.fromIndex == -1 && event.scrollY == 0) {
                    // Static layout pass, no real scroll
                    return
                }
            }
        }

        // 4. Verify full window hierarchy is genuinely Reels/Shorts
        val rootNode = try { rootInActiveWindow } catch (_: Exception) { null }
        val isConfirmedReels = FilterUtils.isReelsOrShortsSurface(rootNode, pkgName)

        if (!isConfirmedReels) {
            isCurrentlyInReelsSurface = false
            return
        }

        // First transition onto Reels surface: give 1200ms grace period for initial viewpager layout
        if (!isCurrentlyInReelsSurface) {
            isCurrentlyInReelsSurface = true
            enteredReelsTimestamp = now
            lastScrollTimestamp = now // prevent counting the initial mount
            Log.d(TAG, "Entered Reels surface. Starting grace cooldown.")
            return
        }

        // In grace cooldown period: discard layout pre-caching
        if (now - enteredReelsTimestamp < SURFACE_ENTER_COOLDOWN_MS) {
            return
        }

        // Confirmed genuine user swipe!
        lastScrollTimestamp = now
        sessionReelCount++
        PreferencesManager.incrementDailyReels()

        Log.d(TAG, "Reel counted: #$sessionReelCount for $pkgName")
        hudManager.updateCounts(sessionReelCount, sessionActiveSeconds)
        checkMilestones()
    }

    private fun checkMilestones() {
        if (modalManager.isShowing()) return

        val t4 = PreferencesManager.getTierConfig(4)
        val t3 = PreferencesManager.getTierConfig(3)
        val t2 = PreferencesManager.getTierConfig(2)
        val t1 = PreferencesManager.getTierConfig(1)

        val minutes = sessionActiveSeconds / 60

        when {
            highestTierTriggered < 4 && (sessionReelCount >= t4.defaultReelThreshold || minutes >= t4.defaultMinuteThreshold) -> {
                triggerEscalation(4)
            }
            highestTierTriggered < 3 && (sessionReelCount >= t3.defaultReelThreshold || minutes >= t3.defaultMinuteThreshold) -> {
                triggerEscalation(3)
            }
            highestTierTriggered < 2 && (sessionReelCount >= t2.defaultReelThreshold || minutes >= t2.defaultMinuteThreshold) -> {
                triggerEscalation(2)
            }
            highestTierTriggered < 1 && (sessionReelCount >= t1.defaultReelThreshold || minutes >= t1.defaultMinuteThreshold) -> {
                triggerEscalation(1)
            }
        }
    }

    fun triggerEscalation(tierLevel: Int) {
        highestTierTriggered = tierLevel
        val platform = if (currentForegroundPackage == PACKAGE_YOUTUBE) "YouTube Shorts" else "Instagram Reels"
        modalManager.show(
            tierLevel = tierLevel,
            reelsCount = sessionReelCount,
            activeSeconds = sessionActiveSeconds,
            platformName = platform
        )
    }

    private fun isInTargetApp(pkg: String? = currentForegroundPackage): Boolean {
        return pkg == PACKAGE_INSTAGRAM || pkg == PACKAGE_YOUTUBE
    }

    private fun resetSession() {
        sessionReelCount = 0
        sessionActiveSeconds = 0L
        highestTierTriggered = 0
        isCurrentlyInReelsSurface = false
        hudManager.updateCounts(0, 0L)
    }

    override fun onInterrupt() {
        Log.w(TAG, "RoastAccessibilityService interrupted.")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        isTimerRunning = false
        mainHandler.removeCallbacks(timerRunnable)
        try {
            unregisterReceiver(screenReceiver)
        } catch (_: Exception) {}
        hudManager.hide()
        modalManager.hide()
        Log.i(TAG, "BasKarBhai RoastAccessibilityService destroyed.")
    }
}
