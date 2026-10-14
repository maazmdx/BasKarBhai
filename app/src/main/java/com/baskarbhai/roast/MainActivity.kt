package com.baskarbhai.roast

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.accessibility.AccessibilityManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.baskarbhai.roast.data.PreferencesManager
import com.baskarbhai.roast.data.TierConfig
import com.baskarbhai.roast.databinding.ActivityMainBinding
import com.baskarbhai.roast.network.WebhookHelper
import com.baskarbhai.roast.service.RoastAccessibilityService
import com.baskarbhai.roast.ui.FloatingHudManager
import com.baskarbhai.roast.ui.RestrictedSettingsDialog
import com.baskarbhai.roast.ui.RoastModalManager
import com.baskarbhai.roast.util.TimeUtils

/**
 * Main Dashboard for BasKarBhai.
 * Designed for Android 10 through Android 14+ with 1-tap activation and fast presets.
 * Architected by maazmdx.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var previewHudManager: FloatingHudManager? = null
    private var previewModalManager: RoastModalManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        updateServiceStatus()
        updateDailyStats()
    }

    private fun setupUI() {
        binding.etNickname.setText(PreferencesManager.nickname)
        binding.etWebhookUrl.setText(PreferencesManager.webhookUrl)

        // On Android 10, 11, 12, Google Restricted Settings do not exist!
        // Hide the button to keep setup completely clean and instant.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            binding.btnRestrictedSettings.visibility = View.GONE
        } else {
            binding.btnRestrictedSettings.visibility = View.VISIBLE
        }

        updateDailyStats()
        updateServiceStatus()
    }

    private fun setupListeners() {
        // 1-Tap Turn On Protection Button
        binding.btnToggleService.setOnClickListener {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
        }

        // Android 13+ Restricted Settings Guide Dialog
        binding.btnRestrictedSettings.setOnClickListener {
            RestrictedSettingsDialog(this).show()
        }

        // Fast 1-Tap Presets
        binding.btnPresetStrict.setOnClickListener { applyPreset("strict") }
        binding.btnPresetBalanced.setOnClickListener { applyPreset("balanced") }
        binding.btnPresetRelaxed.setOnClickListener { applyPreset("relaxed") }

        // Reset Daily Stats
        binding.btnResetStats.setOnClickListener {
            PreferencesManager.resetDailyStats()
            updateDailyStats()
            Toast.makeText(this, "Daily stats reset to zero!", Toast.LENGTH_SHORT).show()
        }

        // Save Webhook and Nickname
        binding.btnSaveWebhook.setOnClickListener {
            val nick = binding.etNickname.text?.toString().orEmpty()
            val webhook = binding.etWebhookUrl.text?.toString().orEmpty()
            PreferencesManager.nickname = nick
            PreferencesManager.webhookUrl = webhook
            Toast.makeText(this, "Settings saved successfully!", Toast.LENGTH_SHORT).show()
        }

        // Test Webhook Ping
        binding.btnTestWebhook.setOnClickListener {
            val webhook = binding.etWebhookUrl.text?.toString().orEmpty()
            val nick = binding.etNickname.text?.toString().orEmpty()

            if (webhook.isBlank()) {
                Toast.makeText(this, "Please enter a Discord or Telegram Webhook URL first.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            binding.btnTestWebhook.isEnabled = false
            binding.btnTestWebhook.text = "Pinging…"

            WebhookHelper.sendTestPing(webhook, nick, object : WebhookHelper.WebhookCallback {
                override fun onSuccess(responseMessage: String) {
                    binding.btnTestWebhook.isEnabled = true
                    binding.btnTestWebhook.text = "Test Ping"
                    Toast.makeText(this@MainActivity, "🎉 $responseMessage", Toast.LENGTH_LONG).show()
                }

                override fun onFailure(errorMessage: String) {
                    binding.btnTestWebhook.isEnabled = true
                    binding.btnTestWebhook.text = "Test Ping"
                    Toast.makeText(this@MainActivity, "⚠️ Webhook Ping Failed: $errorMessage", Toast.LENGTH_LONG).show()
                }
            })
        }

        // Tier Previews (1-4)
        binding.btnPreviewTier1.setOnClickListener { previewRoastModal(1) }
        binding.btnPreviewTier2.setOnClickListener { previewRoastModal(2) }
        binding.btnPreviewTier3.setOnClickListener { previewRoastModal(3) }
        binding.btnPreviewTier4.setOnClickListener { previewRoastModal(4) }

        // Test Floating HUD
        binding.btnTestHud.setOnClickListener {
            toggleHudPreview()
        }

        // Author Badge & Footer
        val authorClick: (View) -> Unit = {
            showAuthorManifesto()
        }
        binding.tvAuthorBadge.setOnClickListener(authorClick)
        binding.tvFooterCredits.setOnClickListener(authorClick)
    }

    private data class PresetThresholds(
        val t1Reels: Int, val t1Mins: Int,
        val t2Reels: Int, val t2Mins: Int,
        val t3Reels: Int, val t3Mins: Int,
        val t4Reels: Int, val t4Mins: Int
    )

    private fun applyPreset(presetType: String) {
        val preset = when (presetType) {
            "strict" -> PresetThresholds(5, 3, 10, 6, 15, 10, 20, 15)
            "relaxed" -> PresetThresholds(20, 10, 40, 20, 60, 30, 80, 45)
            else -> PresetThresholds(10, 5, 25, 12, 40, 20, 60, 30) // balanced
        }

        val defaults = TierConfig.getDefaultTiers()
        PreferencesManager.saveTierConfig(defaults[0].copy(defaultReelThreshold = preset.t1Reels, defaultMinuteThreshold = preset.t1Mins))
        PreferencesManager.saveTierConfig(defaults[1].copy(defaultReelThreshold = preset.t2Reels, defaultMinuteThreshold = preset.t2Mins))
        PreferencesManager.saveTierConfig(defaults[2].copy(defaultReelThreshold = preset.t3Reels, defaultMinuteThreshold = preset.t3Mins))
        PreferencesManager.saveTierConfig(defaults[3].copy(defaultReelThreshold = preset.t4Reels, defaultMinuteThreshold = preset.t4Mins))

        val label = when (presetType) {
            "strict" -> "Strict (5 Reels / 3m)"
            "relaxed" -> "Relaxed (20 Reels / 10m)"
            else -> "Balanced (10 Reels / 5m)"
        }
        Toast.makeText(this, "Preset applied: $label", Toast.LENGTH_SHORT).show()
    }

    private fun updateServiceStatus() {
        val isEnabled = isAccessibilityServiceEnabled(this, RoastAccessibilityService::class.java) ||
                RoastAccessibilityService.isServiceRunning()

        if (isEnabled) {
            binding.tvStatusTitle.text = getString(R.string.service_status_active)
            binding.tvStatusTitle.setTextColor(ContextCompat.getColor(this, R.color.accent_green))
            binding.viewStatusDot.setBackgroundColor(ContextCompat.getColor(this, R.color.accent_green))
            binding.btnToggleService.text = "Protection Active (Tap to Configure)"
        } else {
            binding.tvStatusTitle.text = getString(R.string.service_status_inactive)
            binding.tvStatusTitle.setTextColor(ContextCompat.getColor(this, R.color.flame_orange))
            binding.viewStatusDot.setBackgroundColor(ContextCompat.getColor(this, R.color.flame_orange))
            binding.btnToggleService.text = getString(R.string.enable_service_btn)
        }
    }

    private fun updateDailyStats() {
        val reels = PreferencesManager.dailyReels
        val seconds = PreferencesManager.dailyActiveSeconds
        val roasts = PreferencesManager.dailyRoastsTriggered

        binding.tvDailyReels.text = reels.toString()
        binding.tvDailyTime.text = TimeUtils.formatHumanReadable(seconds)
        binding.tvDailyRoasts.text = roasts.toString()
    }

    private fun previewRoastModal(tierLevel: Int) {
        val activeService = RoastAccessibilityService.instance
        if (activeService != null) {
            activeService.triggerEscalation(tierLevel)
        } else {
            if (previewModalManager == null) {
                previewModalManager = RoastModalManager(this) { actionId ->
                    if (actionId == android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME) {
                        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                            addCategory(Intent.CATEGORY_HOME)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        startActivity(homeIntent)
                        true
                    } else {
                        onBackPressedDispatcher.onBackPressed()
                        true
                    }
                }
            }
            previewModalManager?.show(
                tierLevel = tierLevel,
                reelsCount = when (tierLevel) {
                    1 -> 10
                    2 -> 25
                    3 -> 40
                    else -> 60
                },
                activeSeconds = when (tierLevel) {
                    1 -> 300L
                    2 -> 720L
                    3 -> 1200L
                    else -> 1800L
                },
                platformName = "Instagram Reels"
            )
        }
    }

    private fun toggleHudPreview() {
        val activeService = RoastAccessibilityService.instance
        if (activeService != null) {
            Toast.makeText(this, "HUD is handled automatically by active Accessibility Service!", Toast.LENGTH_SHORT).show()
            return
        }

        if (previewHudManager == null) {
            previewHudManager = FloatingHudManager(this)
        }

        if (previewHudManager?.isShowing() == true) {
            previewHudManager?.hide()
            binding.btnTestHud.text = getString(R.string.test_hud_btn)
            Toast.makeText(this, "HUD Preview Hidden", Toast.LENGTH_SHORT).show()
        } else {
            previewHudManager?.show(12, 255L)
            binding.btnTestHud.text = "Hide Floating HUD Preview"
            Toast.makeText(this, "HUD Preview Shown (Draggable!)", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showAuthorManifesto() {
        AlertDialog.Builder(this, R.style.Theme_BasKarBhai_Dialog)
            .setTitle("⚡ BasKarBhai by maazmdx")
            .setMessage("Architected by maazmdx.\n\n" +
                    "BasKarBhai is an aggressive anti-doomscroll shield designed to snap users out of infinite loops on Instagram Reels and YouTube Shorts.\n\n" +
                    "• Usable from Android 10 through 14+\n" +
                    "• Zero-permission TYPE_ACCESSIBILITY_OVERLAY\n" +
                    "• Smart Reels & Shorts vertical scroll filter\n" +
                    "• Audio focus hijacking (instantly mutes background audio)\n" +
                    "• 4 Escalation tiers\n" +
                    "• Friend-group Discord / Telegram Snitch Webhook")
            .setPositiveButton("Got It!", null)
            .show()
    }

    private fun isAccessibilityServiceEnabled(context: Context, serviceClass: Class<*>): Boolean {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC)
        for (enabled in enabledServices) {
            val resolveInfo = enabled.resolveInfo.serviceInfo
            if (resolveInfo.packageName == context.packageName && resolveInfo.name == serviceClass.name) {
                return true
            }
        }
        return false
    }

    override fun onDestroy() {
        super.onDestroy()
        previewHudManager?.hide()
        previewModalManager?.hide()
    }
}
