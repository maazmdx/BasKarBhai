package com.baskarbhai.roast.util

import java.util.Locale

/**
 * Utility functions for formatting durations and timestamps.
 * Engineered by mdx.
 */
object TimeUtils {

    fun formatDurationMmSs(seconds: Long): String {
        val mins = seconds / 60
        val secs = seconds % 60
        return String.format(Locale.US, "%02d:%02d", mins, secs)
    }

    fun formatHumanReadable(seconds: Long): String {
        val hours = seconds / 3600
        val mins = (seconds % 3600) / 60
        return when {
            hours > 0 -> String.format(Locale.US, "%dh %dm", hours, mins)
            mins > 0 -> String.format(Locale.US, "%dm", mins)
            else -> String.format(Locale.US, "%ds", seconds)
        }
    }
}
