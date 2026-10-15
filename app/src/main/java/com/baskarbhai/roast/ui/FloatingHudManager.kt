package com.baskarbhai.roast.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import com.baskarbhai.roast.R
import com.baskarbhai.roast.data.PreferencesManager
import com.baskarbhai.roast.util.TimeUtils
import kotlin.math.abs

/**
 * Draggable floating overlay HUD pill rendered via TYPE_ACCESSIBILITY_OVERLAY.
 * Compact design: shows reel count and timer, draggable to any position.
 * Engineered by maazmdx.
 */
class FloatingHudManager(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var hudView: View? = null
    private var isAttached = false

    private lateinit var tvReels: TextView
    private lateinit var tvTimer: TextView
    private var windowParams: WindowManager.LayoutParams? = null

    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

    @SuppressLint("InflateParams", "ClickableViewAccessibility")
    fun show(initialReels: Int = 0, initialSeconds: Long = 0L) {
        if (isAttached || hudView != null) return

        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.layout_hud_pill, null)
        hudView = view

        tvReels = view.findViewById(R.id.tvReelCount)
        tvTimer = view.findViewById(R.id.tvActiveTimer)

        updateCounts(initialReels, initialSeconds)

        val overlayType = if (context is android.accessibilityservice.AccessibilityService) {
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_APPLICATION
        }

        val displayMetrics = context.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels

        val savedX = PreferencesManager.hudPositionX
        val savedY = PreferencesManager.hudPositionY

        val startX = if (savedX >= 0) savedX else (screenWidth - 380)
        val startY = if (savedY >= 0) savedY else 120

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = startX
            y = startY
        }
        windowParams = params

        view.setOnTouchListener { _, event ->
            val p = windowParams ?: return@setOnTouchListener false
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = p.x
                    initialY = p.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = (event.rawX - initialTouchX).toInt()
                    val deltaY = (event.rawY - initialTouchY).toInt()
                    p.x = initialX + deltaX
                    p.y = initialY + deltaY
                    if (isAttached && hudView != null) {
                        try {
                            windowManager.updateViewLayout(hudView, p)
                        } catch (_: Exception) {
                            // Layout might be transitioning
                        }
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val deltaX = abs(event.rawX - initialTouchX)
                    val deltaY = abs(event.rawY - initialTouchY)
                    if (deltaX < touchSlop && deltaY < touchSlop) {
                        Toast.makeText(context, "BasKarBhai is watching 👁️", Toast.LENGTH_SHORT).show()
                    } else {
                        PreferencesManager.hudPositionX = p.x
                        PreferencesManager.hudPositionY = p.y
                    }
                    true
                }
                else -> false
            }
        }

        try {
            windowManager.addView(view, params)
            isAttached = true
        } catch (e: Exception) {
            isAttached = false
            hudView = null
        }
    }

    fun updateCounts(reelCount: Int, activeSeconds: Long) {
        if (!isAttached || hudView == null) return
        try {
            tvReels.text = "$reelCount"
            tvTimer.text = TimeUtils.formatDurationMmSs(activeSeconds)
        } catch (_: Exception) {
            // View may have unmounted
        }
    }

    fun isShowing(): Boolean = isAttached

    fun hide() {
        if (isAttached && hudView != null) {
            try {
                windowManager.removeView(hudView)
            } catch (_: Exception) {
                // Ignore removal error
            } finally {
                isAttached = false
                hudView = null
            }
        }
    }
}
