package com.baskarbhai.roast.data

import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Manages local persistence for stats, user credentials, and escalation thresholds.
 * Automatically handles daily statistics roll-over.
 * Engineered by mdx.
 */
object PreferencesManager {

    private const val PREFS_NAME = "baskarbhai_preferences"

    private const val KEY_NICKNAME = "user_nickname"
    private const val KEY_WEBHOOK_URL = "webhook_url"
    private const val KEY_LAST_DATE = "stats_last_date"
    private const val KEY_DAILY_REELS = "stats_daily_reels"
    private const val KEY_DAILY_SECONDS = "stats_daily_seconds"
    private const val KEY_DAILY_ROASTS = "stats_daily_roasts"

    private const val KEY_HUD_X = "hud_pos_x"
    private const val KEY_HUD_Y = "hud_pos_y"
    private const val KEY_HUD_ENABLED = "hud_enabled"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        checkAndResetDailyStats()
    }

    private fun getTodayDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    private fun checkAndResetDailyStats() {
        val today = getTodayDateString()
        val savedDate = prefs.getString(KEY_LAST_DATE, "")
        if (savedDate != today) {
            prefs.edit()
                .putString(KEY_LAST_DATE, today)
                .putInt(KEY_DAILY_REELS, 0)
                .putLong(KEY_DAILY_SECONDS, 0L)
                .putInt(KEY_DAILY_ROASTS, 0)
                .apply()
        }
    }

    var nickname: String
        get() = prefs.getString(KEY_NICKNAME, "Bhai") ?: "Bhai"
        set(value) = prefs.edit().putString(KEY_NICKNAME, value.trim()).apply()

    var webhookUrl: String
        get() = prefs.getString(KEY_WEBHOOK_URL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_WEBHOOK_URL, value.trim()).apply()

    var dailyReels: Int
        get() {
            checkAndResetDailyStats()
            return prefs.getInt(KEY_DAILY_REELS, 0)
        }
        set(value) {
            checkAndResetDailyStats()
            prefs.edit().putInt(KEY_DAILY_REELS, value).apply()
        }

    fun incrementDailyReels() {
        dailyReels += 1
    }

    var dailyActiveSeconds: Long
        get() {
            checkAndResetDailyStats()
            return prefs.getLong(KEY_DAILY_SECONDS, 0L)
        }
        set(value) {
            checkAndResetDailyStats()
            prefs.edit().putLong(KEY_DAILY_SECONDS, value).apply()
        }

    fun addDailySeconds(seconds: Long) {
        dailyActiveSeconds += seconds
    }

    var dailyRoastsTriggered: Int
        get() {
            checkAndResetDailyStats()
            return prefs.getInt(KEY_DAILY_ROASTS, 0)
        }
        set(value) {
            checkAndResetDailyStats()
            prefs.edit().putInt(KEY_DAILY_ROASTS, value).apply()
        }

    fun incrementDailyRoasts() {
        dailyRoastsTriggered += 1
    }

    fun resetDailyStats() {
        prefs.edit()
            .putString(KEY_LAST_DATE, getTodayDateString())
            .putInt(KEY_DAILY_REELS, 0)
            .putLong(KEY_DAILY_SECONDS, 0L)
            .putInt(KEY_DAILY_ROASTS, 0)
            .apply()
    }

    var hudPositionX: Int
        get() = prefs.getInt(KEY_HUD_X, -1)
        set(value) = prefs.edit().putInt(KEY_HUD_X, value).apply()

    var hudPositionY: Int
        get() = prefs.getInt(KEY_HUD_Y, -1)
        set(value) = prefs.edit().putInt(KEY_HUD_Y, value).apply()

    var isHudEnabled: Boolean
        get() = prefs.getBoolean(KEY_HUD_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_HUD_ENABLED, value).apply()

    fun getTierConfig(level: Int): TierConfig {
        val defaults = TierConfig.getDefaultTiers().first { it.tierLevel == level }
        val reelsKey = "tier_${level}_reels"
        val minsKey = "tier_${level}_mins"
        val captionKey = "tier_${level}_caption"
        val memeKey = "tier_${level}_meme"

        return TierConfig(
            tierLevel = level,
            title = defaults.title,
            caption = prefs.getString(captionKey, defaults.caption) ?: defaults.caption,
            defaultReelThreshold = prefs.getInt(reelsKey, defaults.defaultReelThreshold),
            defaultMinuteThreshold = prefs.getInt(minsKey, defaults.defaultMinuteThreshold),
            memeUrl = prefs.getString(memeKey, defaults.memeUrl) ?: defaults.memeUrl,
            fallbackDrawableRes = defaults.fallbackDrawableRes
        )
    }

    fun saveTierConfig(config: TierConfig) {
        val level = config.tierLevel
        prefs.edit()
            .putInt("tier_${level}_reels", config.defaultReelThreshold)
            .putInt("tier_${level}_mins", config.defaultMinuteThreshold)
            .putString("tier_${level}_caption", config.caption)
            .putString("tier_${level}_meme", config.memeUrl)
            .apply()
    }
}
