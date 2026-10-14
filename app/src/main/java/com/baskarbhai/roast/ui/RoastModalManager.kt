package com.baskarbhai.roast.ui

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
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import coil.load
import com.baskarbhai.roast.R
import com.baskarbhai.roast.audio.AudioFocusHelper
import com.baskarbhai.roast.data.PreferencesManager
import com.baskarbhai.roast.data.TierConfig
import com.baskarbhai.roast.network.WebhookHelper
import com.baskarbhai.roast.util.TimeUtils

/**
 * Full-screen Escalation Modal Overlay via TYPE_ACCESSIBILITY_OVERLAY.
 * Implements Tier 1 (3s lock), Tier 2 (5 taps), Tier 3 (Auto BACK), and Tier 4 (Auto HOME + Webhook).
 * Hijacks and releases Audio Focus seamlessly.
 * Engineered by mdx.
 */
class RoastModalManager(
    private val context: Context,
    private val actionPerformer: ((Int) -> Boolean)? = null
) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val audioFocusHelper = AudioFocusHelper(context)
    private var modalView: View? = null
    private var isAttached = false

    private var countdownTimer: CountDownTimer? = null
    private var tapRemainingCount = 5
    private val mainHandler = Handler(Looper.getMainLooper())

    interface DismissListener {
        fun onDismissed(tierLevel: Int)
    }

    var dismissListener: DismissListener? = null

    @SuppressLint("InflateParams", "SetTextI18n")
    fun show(
        tierLevel: Int,
        reelsCount: Int,
        activeSeconds: Long,
        platformName: String = "Instagram Reels"
    ) {
        if (isAttached || modalView != null) {
            // Dismiss existing before showing higher escalation
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
        val pbLoading: ProgressBar = view.findViewById(R.id.pbMemeLoading)
        val btnAction: Button = view.findViewById(R.id.btnModalAction)
        val tvHint: TextView = view.findViewById(R.id.tvActionHint)

        tvTierBadge.text = "🚨 TIER $tierLevel ESCALATION • mdx shield"
        tvTitle.text = tierConfig.title
        tvCaption.text = tierConfig.caption
        tvStats.text = "📜 $reelsCount Reels  |  ⏱️ ${TimeUtils.formatDurationMmSs(activeSeconds)} Lost"

        // Load Meme GIF with Coil and fallback to custom vector artwork
        pbLoading.visibility = View.VISIBLE
        ivMeme.load(tierConfig.memeUrl) {
            crossfade(true)
            placeholder(tierConfig.fallbackDrawableRes)
            error(tierConfig.fallbackDrawableRes)
            listener(
                onSuccess = { _, _ -> pbLoading.visibility = View.GONE },
                onError = { _, _ -> pbLoading.visibility = View.GONE }
            )
        }

        // Hijack audio focus immediately to mute Reels/Shorts
        audioFocusHelper.hijackAudioFocus()
        PreferencesManager.incrementDailyRoasts()

        // Configure Tier-Specific Escalation Mechanics
        when (tierLevel) {
            1 -> setupTier1(btnAction, tvHint)
            2 -> setupTier2(btnAction, tvHint)
            3 -> setupTier3(btnAction, tvHint)
            4 -> setupTier4(btnAction, tvHint, reelsCount, activeSeconds, platformName)
            else -> setupTier1(btnAction, tvHint)
        }

        val overlayType = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY

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
            Log.e("RoastModalManager", "Failed to add overlay window: ${e.message}")
            isAttached = false
            modalView = null
            audioFocusHelper.releaseAudioFocus()
        }
    }

    /**
     * Tier 1: 3-Second Locked Button
     */
    private fun setupTier1(btnAction: Button, tvHint: TextView) {
        btnAction.isEnabled = false
        btnAction.alpha = 0.5f
        tvHint.text = "Button locked for 3 seconds. Reflect on your life choices."

        countdownTimer?.cancel()
        countdownTimer = object : CountDownTimer(3000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = (millisUntilFinished / 1000) + 1
                btnAction.text = "Patience… ($secondsLeft)"
            }

            override fun onFinish() {
                btnAction.isEnabled = true
                btnAction.alpha = 1.0f
                btnAction.text = "Theek Hai, Sudhar Jaunga"
                tvHint.text = "Tap to dismiss and go do something productive."
            }
        }.start()

        btnAction.setOnClickListener {
            dismiss(1)
        }
    }

    /**
     * Tier 2: 5 Taps Required to Dismiss
     */
    @SuppressLint("SetTextI18n")
    private fun setupTier2(btnAction: Button, tvHint: TextView) {
        tapRemainingCount = 5
        btnAction.isEnabled = true
        btnAction.text = "Tap 5 times to dismiss (5 remaining)"
        tvHint.text = "Work hard! Tap rapidly to earn your escape!"

        btnAction.setOnClickListener {
            tapRemainingCount--
            if (tapRemainingCount > 0) {
                btnAction.text = "Tap $tapRemainingCount more times!"
            } else {
                btnAction.text = "Unlocked! Sudhar jao!"
                dismiss(2)
            }
        }
    }

    /**
     * Tier 3: Auto-execute BACK to exit Reels Player
     */
    private fun setupTier3(btnAction: Button, tvHint: TextView) {
        btnAction.text = "Exiting Reels Player Now…"
        tvHint.text = "Auto-executing BACK gesture to protect your attention!"

        countdownTimer?.cancel()
        countdownTimer = object : CountDownTimer(2500, 500) {
            override fun onTick(millisUntilFinished: Long) {
                val secs = (millisUntilFinished / 1000) + 1
                btnAction.text = "Auto-exiting Reels in $secs s…"
            }

            override fun onFinish() {
                // Execute GLOBAL_ACTION_BACK (value 1)
                actionPerformer?.invoke(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK)
                dismiss(3)
            }
        }.start()

        btnAction.setOnClickListener {
            countdownTimer?.cancel()
            actionPerformer?.invoke(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK)
            dismiss(3)
        }
    }

    /**
     * Tier 4: Auto-execute HOME + Webhook Snitch Alert
     */
    private fun setupTier4(
        btnAction: Button,
        tvHint: TextView,
        reelsCount: Int,
        activeSeconds: Long,
        platformName: String
    ) {
        btnAction.text = "Kicking to Home Screen & Snitching…"
        tvHint.text = "Tier 4 triggered! Alert sent to friend-group Discord/Telegram!"

        // Fire optional webhook alert
        val webhookUrl = PreferencesManager.webhookUrl
        val nickname = PreferencesManager.nickname
        if (webhookUrl.isNotBlank()) {
            WebhookHelper.sendTier4SnitchAlert(
                webhookUrl = webhookUrl,
                nickname = nickname,
                reelsCount = reelsCount,
                activeDuration = TimeUtils.formatHumanReadable(activeSeconds),
                platformName = platformName
            )
        }

        countdownTimer?.cancel()
        countdownTimer = object : CountDownTimer(2000, 500) {
            override fun onTick(millisUntilFinished: Long) {
                val secs = (millisUntilFinished / 1000) + 1
                btnAction.text = "Closing to Home Screen in $secs s…"
            }

            override fun onFinish() {
                // Execute GLOBAL_ACTION_HOME (value 2)
                actionPerformer?.invoke(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME)
                dismiss(4)
            }
        }.start()

        btnAction.setOnClickListener {
            countdownTimer?.cancel()
            actionPerformer?.invoke(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME)
            dismiss(4)
        }
    }

    private fun dismiss(tierLevel: Int) {
        hide()
        dismissListener?.onDismissed(tierLevel)
    }

    fun isShowing(): Boolean = isAttached

    fun hide() {
        countdownTimer?.cancel()
        countdownTimer = null

        audioFocusHelper.releaseAudioFocus()

        if (isAttached && modalView != null) {
            try {
                windowManager.removeView(modalView)
            } catch (e: Exception) {
                // View might already be detached
            } finally {
                isAttached = false
                modalView = null
            }
        }
    }
}
