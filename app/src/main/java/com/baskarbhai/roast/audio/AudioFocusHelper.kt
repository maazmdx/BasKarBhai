package com.baskarbhai.roast.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.os.Build
import android.util.Log

/**
 * Handles Audio Focus hijacking (AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
 * to instantly mute/pause background Reels and Shorts during roast interventions.
 * Engineered by mdx.
 */
class AudioFocusHelper(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var focusRequest: AudioFocusRequest? = null
    private var toneGenerator: ToneGenerator? = null

    /**
     * Hijacks audio focus exclusively so Instagram/YouTube pauses audio immediately.
     */
    fun hijackAudioFocus() {
        try {
            val playbackAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
                    .setAudioAttributes(playbackAttributes)
                    .setAcceptsDelayedFocusGain(false)
                    .setOnAudioFocusChangeListener { focusChange ->
                        Log.d("AudioFocusHelper", "Focus change: $focusChange")
                    }
                    .build()
                focusRequest = request
                audioManager.requestAudioFocus(request)
            } else {
                @Suppress("DEPRECATION")
                audioManager.requestAudioFocus(
                    null,
                    AudioManager.STREAM_NOTIFICATION,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE
                )
            }
            playAlertSound()
        } catch (e: Exception) {
            Log.e("AudioFocusHelper", "Failed to hijack audio focus: ${e.message}")
        }
    }

    /**
     * Plays alert audio tone or notification sound to shock the user out of the trance.
     */
    private fun playAlertSound() {
        try {
            // Play notification tone
            val notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(context, notificationUri)
            ringtone?.play()

            // Play high-priority tone buzzer
            toneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, 100)
            toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ALERT_NETWORK_LITE, 600)
        } catch (e: Exception) {
            Log.w("AudioFocusHelper", "Could not play alert sound: ${e.message}")
        }
    }

    /**
     * Releases audio focus back to the system.
     */
    fun releaseAudioFocus() {
        try {
            toneGenerator?.release()
            toneGenerator = null

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
                focusRequest = null
            } else {
                @Suppress("DEPRECATION")
                audioManager.abandonAudioFocus(null)
            }
        } catch (e: Exception) {
            Log.e("AudioFocusHelper", "Failed to abandon audio focus: ${e.message}")
        }
    }
}
