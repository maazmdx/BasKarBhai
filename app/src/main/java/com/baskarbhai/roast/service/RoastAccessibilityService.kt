package com.baskarbhai.roast.service

import android.accessibilityservice.AccessibilityService
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
 * Features:
 *  - Zero-permission floating HUD and full-screen modal overlays via TYPE_ACCESSIBILITY_OVERLAY.
 *  - Smart Reels/Shorts filter detecting vertical pagers while filtering out Instagram DMs.
 *  - 1200ms scroll debounce.
 *  - Active session timer running every 1000ms while target app is active.
 *  - 10-minute session timeout reset.
 *  - 4 Escalation Tiers triggered by EITHER Reel Count OR Time.
 * Engineered by mdx.
 */
class RoastAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "RoastAccessibility"
        private const val SCROLL_DEBOUNCE_MS = 1200L
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

    // Active session counters
    var sessionReelCount = 0
        private set
    var sessionActiveSeconds = 0L
        private set
    private var currentForegroundPackage: String? = null
    private var highestTierTriggered = 0

    // 1-second interval session timer runnable
    private val timerRunnable = object : Runnable {
        override fun run() {
            if (isInTargetApp()) {
                sessionActiveSeconds++
                PreferencesManager.addDailySeconds(1L)
                hudManager.updateCounts(sessionReelCount, sessionActiveSeconds)
                checkMilestones()
                mainHandler.postDelayed(this, 1000L)
            } else {
                isTimerRunning = false
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.i(TAG, "BasKarBhai RoastAccessibilityService connected. Powered by mdx.")

        hudManager = FloatingHudManager(this)
        modalManager = RoastModalManager(this) { actionId ->
            performGlobalAction(actionId)
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
                handleViewScrolled(pkgName)
            }
        }
    }

    private fun handleWindowStateChanged(pkgName: String) {
        val wasInTarget = isInTargetApp()
        currentForegroundPackage = pkgName

        if (isInTargetApp()) {
            val now = System.currentTimeMillis()
            // Check 10-minute session timeout
            if (lastLeaveTimestamp > 0 && (now - lastLeaveTimestamp > SESSION_TIMEOUT_MS)) {
                Log.d(TAG, "Session timed out (>10m). Resetting session counters.")
                resetSession()
            }

            // Start HUD & Timer if not already active
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
            // Hide HUD when outside target apps unless modal is showing
            if (hudManager.isShowing() && !modalManager.isShowing()) {
                hudManager.hide()
            }
            isTimerRunning = false
            mainHandler.removeCallbacks(timerRunnable)
        }
    }

    private fun handleViewScrolled(pkgName: String) {
        if (!isInTargetApp(pkgName)) return

        val now = System.currentTimeMillis()
        if (now - lastScrollTimestamp < SCROLL_DEBOUNCE_MS) {
            // Debounced scroll event
            return
        }

        // Deep inspect view hierarchy: ensure it's a Reels/Shorts vertical pager and NOT an Instagram DM
        val rootNode = try {
            rootInActiveWindow
        } catch (e: Exception) {
            null
        }

        if (FilterUtils.isReelsOrShortsSurface(rootNode, pkgName)) {
            lastScrollTimestamp = now
            sessionReelCount++
            PreferencesManager.incrementDailyReels()

            hudManager.updateCounts(sessionReelCount, sessionActiveSeconds)
            checkMilestones()
        }
    }

    /**
     * Checks if either Reel Count OR Active Time has crossed an escalation threshold.
     */
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
        hudManager.hide()
        modalManager.hide()
        Log.i(TAG, "BasKarBhai RoastAccessibilityService destroyed.")
    }
}
