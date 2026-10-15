package com.baskarbhai.roast.data

import androidx.annotation.DrawableRes
import com.baskarbhai.roast.R

/**
 * Definition of an Escalation Tier for BasKarBhai.
 * Features vector graphics and synchronized TextToSpeech voice roasts.
 * Engineered by maazmdx.
 */
data class TierConfig(
    val tierLevel: Int,
    val title: String,
    val caption: String,
    val speechText: String,
    val defaultReelThreshold: Int,
    val defaultMinuteThreshold: Int,
    @DrawableRes val fallbackDrawableRes: Int
) {
    companion object {
        fun getDefaultTiers(): List<TierConfig> = listOf(
            TierConfig(
                tierLevel = 1,
                title = "REALITY CHECK",
                caption = "You've scrolled through 10 reels back-to-back.\nThat’s pure dopamine fog. Look up, take a breath, and put the phone down.",
                speechText = "Reality check! You have watched 10 reels. Put the phone down right now.",
                defaultReelThreshold = 10,
                defaultMinuteThreshold = 5,
                fallbackDrawableRes = R.drawable.art_tier1_reality_check
            ),
            TierConfig(
                tierLevel = 2,
                title = "DOOMSCROLL DETECTED",
                caption = "25 reels deep into the algorithm.\nYou could've finished a workout, read a chapter, or gone outside.\nSnap out of it.",
                speechText = "Doomscroll alert! 25 reels wasted. Close the app and touch some grass.",
                defaultReelThreshold = 25,
                defaultMinuteThreshold = 12,
                fallbackDrawableRes = R.drawable.art_tier2_doomscroll
            ),
            TierConfig(
                tierLevel = 3,
                title = "SYSTEM INTERVENTION",
                caption = "40 reels burned! Emergency brake engaged.\nWe are ejecting you back to your feed right now.\nStop negotiating with the algorithm.",
                speechText = "System intervention! 40 reels in. Pulling the emergency brake now.",
                defaultReelThreshold = 40,
                defaultMinuteThreshold = 20,
                fallbackDrawableRes = R.drawable.art_tier3_intervention
            ),
            TierConfig(
                tierLevel = 4,
                title = "TOTAL LOCKOUT",
                caption = "60 reels and 30 minutes lost to the void.\nEjecting to Home Screen immediately.\nAlerting your Discord or Telegram webhook. It's over.",
                speechText = "Total lockout! 60 reels burned. Ejecting to home screen.",
                defaultReelThreshold = 60,
                defaultMinuteThreshold = 30,
                fallbackDrawableRes = R.drawable.art_tier4_lockdown
            )
        )
    }
}
