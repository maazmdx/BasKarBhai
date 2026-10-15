package com.baskarbhai.roast.ui

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.animation.AlphaAnimation
import android.view.animation.AnimationSet
import android.view.animation.DecelerateInterpolator
import android.view.animation.ScaleAnimation
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.baskarbhai.roast.R
import com.baskarbhai.roast.audio.AudioRoastPlayer
import com.baskarbhai.roast.data.PreferencesManager
import com.baskarbhai.roast.network.WebhookHelper
import com.baskarbhai.roast.util.TimeUtils

/**
 * Full-screen Escalation Modal Overlay via TYPE_ACCESSIBILITY_OVERLAY.
 * Uses local vector graphics, synthetic alert buzzer tones, and TextToSpeech voice roasts.
 * Implements Tier 1 (3s lock), Tier 2 (5 taps challenge), Tier 3 (Auto BACK), Tier 4 (Auto HOME + Webhook).
 * Engineered by maazmdx.
 */
class RoastModalManager(
    private val context: Context,
    private val actionPerformer: ((Int) -> Boolean)? = null
) {

    companion object {
        private const val TAG = "RoastModalManager"
    }

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val audioRoastPlayer = AudioRoastPlayer(context)
    private var modalView: View? = null
    private var isAttached = false

    private var countdownTimer: CountDownTimer? = null
    private var tapRemainingCount = 5
    private val mainHandler = Handler(Looper.getMainLooper())

    interface DismissListener {
        fun onDismissed(tierLevel: Int)
    }

    var dismissListener: DismissListener? = null

    fun isShowing(): Boolean = isAttached && modalView != null

    @SuppressLint("InflateParams", "SetTextI18n")
    fun show(
        tierLevel: Int,
        reelsCount: Int,
        activeSeconds: Long,
        platformName: String = "Instagram Reels"
    ) {
        if (isAttached || modalView != null) {
            hide()
        }

        val tierConfig = PreferencesManager.getTierConfig(tierLevel)

        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.layout_roast_modal, null)
        modalView = view

        val tvTierBadge: TextView = view.findViewById(R.id.tvTierBadge)
        val tvTitle: TextView = view.findViewById(R.id.tvRoastTitle)
        val tvCaption: TextView = view.findViewById(R.id.tvRoastCaption)
        val tvStats: TextView = view.findViewById(R.id.tvModalStats)
        val ivMeme: ImageView = view.findViewById(R.id.ivMemeImage)
        val btnAction: Button = view.findViewById(R.id.btnModalAction)
        val tvHint: TextView = view.findViewById(R.id.tvActionHint)

        // Tier badge with icon
        val tierBadgeText = when (tierLevel) {
            1 -> "⚠️ TIER 1 • REALITY CHECK"
            2 -> "🔥 TIER 2 • DOOMSCROLL DETECTED"
            3 -> "🚨 TIER 3 • EMERGENCY INTERVENTION"
            4 -> "💀 TIER 4 • TOTAL LOCKOUT"
            else -> "⚠️ TIER $tierLevel"
        }
        tvTierBadge.text = tierBadgeText
        tvTitle.text = tierConfig.title
        tvCaption.text = tierConfig.caption
        tvStats.text = "$reelsCount Reels  •  ${TimeUtils.formatDurationMmSs(activeSeconds)} Wasted"

        // Local vector artwork — guaranteed to render crisp without network
        ivMeme.setImageResource(tierConfig.fallbackDrawableRes)

        // Smooth Cyber Entrance Animation
        val cardContainer = view.findViewById<View>(R.id.modalCardContainer)
        val scaleAnim = ScaleAnimation(
            0.88f, 1.0f, 0.88f, 1.0f,
            ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
            ScaleAnimation.RELATIVE_TO_SELF, 0.5f
        )
        val fadeAnim = AlphaAnimation(0f, 1f)
        val animSet = AnimationSet(true).apply {
            addAnimation(scaleAnim)
            addAnimation(fadeAnim)
            duration = 260
            interpolator = DecelerateInterpolator()
        }
        cardContainer.startAnimation(animSet)

        // Play Alert Tone + Vibrate + Speak TextToSpeech Voice Roast
        audioRoastPlayer.playRoastAudio(tierConfig.speechText)
        PreferencesManager.incrementDailyRoasts()

        // Tier mechanics
        when (tierLevel) {
            1 -> setupTier1(btnAction, tvHint)
            2 -> setupTier2(btnAction, tvHint)
            3 -> setupTier3(btnAction, tvHint)
            4 -> setupTier4(btnAction, tvHint, reelsCount, activeSeconds, platformName)
            else -> setupTier1(btnAction, tvHint)
        }

        // Support both AccessibilityService overlay and Activity preview overlay
        val overlayType = if (context is AccessibilityService) {
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_APPLICATION
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_FULLSCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        try {
            windowManager.addView(view, params)
            isAttached = true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add overlay: ${e.message}")
            isAttached = false
            modalView = null
            audioRoastPlayer.release()
        }
    }

    /**
     * Tier 1: 3-Second Locked Countdown Button
     */
    private fun setupTier1(btnAction: Button, tvHint: TextView) {
        btnAction.isEnabled = false
        btnAction.alpha = 0.45f
        tvHint.text = "Locked for 3 seconds. Take a breath and look away."

        countdownTimer?.cancel()
        countdownTimer = object : CountDownTimer(3000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = (millisUntilFinished / 1000) + 1
                btnAction.text = "Take a breath ($secondsLeft s)"
            }

            override fun onFinish() {
                btnAction.isEnabled = true
                btnAction.alpha = 1.0f
                btnAction.text = "I'm Putting Phone Down"
                tvHint.text = "Tap to dismiss and return to reality."
            }
        }.start()

        btnAction.setOnClickListener {
            dismiss(1)
        }
    }

    /**
     * Tier 2: 5 Rapid Taps Challenge
     */
    @SuppressLint("SetTextI18n")
    private fun setupTier2(btnAction: Button, tvHint: TextView) {
        tapRemainingCount = 5
        btnAction.text = "Tap 5 times to prove you're awake (5)"
        tvHint.text = "Willpower test: Tap repeatedly to dismiss."

        btnAction.setOnClickListener {
            tapRemainingCount--
            if (tapRemainingCount > 0) {
                btnAction.text = "Keep tapping! ($tapRemainingCount left)"
            } else {
                btnAction.text = "Dismissing..."
                dismiss(2)
            }
        }
    }

    /**
     * Tier 3: Emergency Brake — Auto Back Navigation
     */
    private fun setupTier3(btnAction: Button, tvHint: TextView) {
        btnAction.isEnabled = false
        btnAction.text = "Emergency Exit in 3s..."
        tvHint.text = "System is auto-exiting Reels player."

        countdownTimer?.cancel()
        countdownTimer = object : CountDownTimer(3000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = (millisUntilFinished / 1000) + 1
                btnAction.text = "Emergency Exit in ${secondsLeft}s..."
            }

            override fun onFinish() {
                executeAutoBack()
            }
        }.start()

        btnAction.setOnClickListener {
            executeAutoBack()
        }
    }

    private fun executeAutoBack() {
        actionPerformer?.invoke(AccessibilityService.GLOBAL_ACTION_BACK)
        dismiss(3)
    }

    /**
     * Tier 4: Total Lockout — Ejection to Home Screen + Webhook Alert
     */
    private fun setupTier4(
        btnAction: Button,
        tvHint: TextView,
        reelsCount: Int,
        activeSeconds: Long,
        platformName: String
    ) {
        btnAction.isEnabled = false
        btnAction.text = "Ejecting to Home Screen..."
        tvHint.text = "Alerting friend-group webhook & exiting app."

        val webhookUrl = PreferencesManager.webhookUrl
        val nickname = PreferencesManager.nickname
        if (webhookUrl.isNotBlank()) {
            WebhookHelper.sendDoomscrollAlert(
                webhookUrl = webhookUrl,
                nickname = nickname,
                platform = platformName,
                tier = 4,
                reelsCount = reelsCount,
                durationSeconds = activeSeconds
            )
        }

        mainHandler.postDelayed({
            actionPerformer?.invoke(AccessibilityService.GLOBAL_ACTION_HOME)
            dismiss(4)
        }, 3200L)
    }

    private fun dismiss(tierLevel: Int) {
        hide()
        dismissListener?.onDismissed(tierLevel)
    }

    fun hide() {
        countdownTimer?.cancel()
        countdownTimer = null
        mainHandler.removeCallbacksAndMessages(null)
        audioRoastPlayer.release()

        if (isAttached && modalView != null) {
            try {
                windowManager.removeView(modalView)
            } catch (e: Exception) {
                Log.w(TAG, "Error removing modal overlay: ${e.message}")
            }
            isAttached = false
            modalView = null
        }
    }
}
