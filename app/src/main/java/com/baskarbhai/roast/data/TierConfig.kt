package com.baskarbhai.roast.data

import androidx.annotation.DrawableRes
import com.baskarbhai.roast.R

/**
 * Definition of an Escalation Tier for BasKarBhai.
 * Architected by maazmdx.
 */
data class TierConfig(
    val tierLevel: Int,
    val title: String,
    val caption: String,
    val defaultReelThreshold: Int,
    val defaultMinuteThreshold: Int,
    val memeUrl: String,
    @DrawableRes val fallbackDrawableRes: Int
) {
    companion object {
        fun getDefaultTiers(): List<TierConfig> = listOf(
            TierConfig(
                tierLevel = 1,
                title = "What Are You Doing?",
                caption = "10 reels watched already! Stop scrolling and do something productive!",
                defaultReelThreshold = 10,
                defaultMinuteThreshold = 5,
                memeUrl = "https://media.giphy.com/media/AGGcmjQuwPHvvE6v08/giphy.gif",
                fallbackDrawableRes = R.drawable.meme_tier1_ashneer
            ),
            TierConfig(
                tierLevel = 2,
                title = "Focus On What Matters!",
                caption = "25 reels in! Put down the phone, build your future and reclaim your focus!",
                defaultReelThreshold = 25,
                defaultMinuteThreshold = 12,
                memeUrl = "https://media.giphy.com/media/26ufcVAp3AiJJsrIs/giphy.gif",
                fallbackDrawableRes = R.drawable.meme_tier2_ias
            ),
            TierConfig(
                tierLevel = 3,
                title = "Wait! Hold Up!",
                caption = "40 reels watched! Exiting Reels player right now to save your attention!",
                defaultReelThreshold = 40,
                defaultMinuteThreshold = 20,
                memeUrl = "https://media.giphy.com/media/3o7bu12GHm4G5MI7UW/giphy.gif",
                fallbackDrawableRes = R.drawable.meme_tier3_bhau
            ),
            TierConfig(
                tierLevel = 4,
                title = "Game Over! Bye-Bye!",
                caption = "60 Reels / 30 Minutes reached! Closing to Home Screen and notifying your group chat!",
                defaultReelThreshold = 60,
                defaultMinuteThreshold = 30,
                memeUrl = "https://media.giphy.com/media/ISOckXUybVfQ4/giphy.gif",
                fallbackDrawableRes = R.drawable.meme_tier4_tata
            )
        )
    }
}
