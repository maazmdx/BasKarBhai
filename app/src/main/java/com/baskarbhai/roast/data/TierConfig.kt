package com.baskarbhai.roast.data

import androidx.annotation.DrawableRes
import com.baskarbhai.roast.R

/**
 * Definition of an Escalation Tier for BasKarBhai.
 * Engineered by mdx.
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
                title = "Bhai kya kar raha hai tu?",
                caption = "10 reels dekh chuka hai! Dimag ka dahi mat bana, abhi band kar!",
                defaultReelThreshold = 10,
                defaultMinuteThreshold = 5,
                memeUrl = "https://media.giphy.com/media/AGGcmjQuwPHvvE6v08/giphy.gif", // Ashneer Grover meme
                fallbackDrawableRes = R.drawable.meme_tier1_ashneer
            ),
            TierConfig(
                tierLevel = 2,
                title = "Bas kar bhai! Padhai likhai me dhyan lagao!",
                caption = "IAS YAS bano, desh ko sambhalo! Lekin nahi, hume to reels dekhna hai!",
                defaultReelThreshold = 25,
                defaultMinuteThreshold = 12,
                memeUrl = "https://media.giphy.com/media/26ufcVAp3AiJJsrIs/giphy.gif", // Padhai likhai meme
                fallbackDrawableRes = R.drawable.meme_tier2_ias
            ),
            TierConfig(
                tierLevel = 3,
                title = "Ruko Jara, Sabar Karo!",
                caption = "Bas bahut hua! Exiting Reels player right now to save your future!",
                defaultReelThreshold = 40,
                defaultMinuteThreshold = 20,
                memeUrl = "https://media.giphy.com/media/3o7bu12GHm4G5MI7UW/giphy.gif", // Hindustani Bhau / Ruko Jara
                fallbackDrawableRes = R.drawable.meme_tier3_bhau
            ),
            TierConfig(
                tierLevel = 4,
                title = "Khatam! Tata! Bye-Bye!",
                caption = "60 Reels / 30 Minutes! Kicking you out to phone Home Screen and notifying your group chat!",
                defaultReelThreshold = 60,
                defaultMinuteThreshold = 30,
                memeUrl = "https://media.giphy.com/media/ISOckXUybVfQ4/giphy.gif", // Rahul Gandhi Khatam Tata
                fallbackDrawableRes = R.drawable.meme_tier4_tata
            )
        )
    }
}
