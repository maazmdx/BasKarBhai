package com.baskarbhai.roast.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale
import kotlin.concurrent.thread
import kotlin.math.sin

/**
 * Handles Audio Focus hijacking, alert siren synthesis via AudioTrack,
 * haptic vibration, and dynamic TextToSpeech voice roasts.
 * Engineered by maazmdx.
 */
class AudioRoastPlayer(private val context: Context) : TextToSpeech.OnInitListener {

    companion object {
        private const val TAG = "AudioRoastPlayer"
        private const val SAMPLE_RATE = 44100
    }

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var focusRequest: AudioFocusRequest? = null
    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private var pendingSpeechText: String? = null

    init {
        try {
            tts = TextToSpeech(context.applicationContext, this)
        } catch (e: Exception) {
            Log.w(TAG, "TextToSpeech init failed: ${e.message}")
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                isTtsReady = true
                tts?.setSpeechRate(1.05f)
                tts?.setPitch(1.0f)
                pendingSpeechText?.let {
                    speak(it)
                    pendingSpeechText = null
                }
            } else {
                Log.w(TAG, "US English not available on TTS engine")
            }
        } else {
            Log.w(TAG, "TTS initialization reported status $status")
        }
    }

    /**
     * Hijacks audio focus on STREAM_MUSIC so background Reels/Shorts are paused instantly.
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
                        Log.d(TAG, "Audio focus changed: $focusChange")
                    }
                    .build()
                focusRequest = request
                audioManager.requestAudioFocus(request)
            } else {
                @Suppress("DEPRECATION")
                audioManager.requestAudioFocus(
                    null,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to hijack audio focus: ${e.message}")
        }
    }

    /**
     * Plays alert buzzer tone and speaks out the voice roast message loudly.
     */
    fun playRoastAudio(speechText: String) {
        hijackAudioFocus()
        playAlertTone()
        triggerHaptic()
        speak(speechText)
    }

    /**
     * Generates a high-frequency dual-beep alert tone directly on STREAM_MUSIC via AudioTrack.
     * Guaranteed to work on all devices without external media files.
     */
    private fun playAlertTone() {
        thread(name = "AlertToneThread") {
            try {
                val durationMs = 350
                val numSamples = (durationMs * SAMPLE_RATE) / 1000
                val buffer = ShortArray(numSamples)
                val freq1 = 880.0 // A5 high beep
                val freq2 = 1200.0 // High alert chime

                for (i in 0 until numSamples) {
                    val time = i.toDouble() / SAMPLE_RATE
                    val freq = if (i < numSamples / 2) freq1 else freq2
                    val angle = 2.0 * Math.PI * freq * time
                    val sample = (sin(angle) * Short.MAX_VALUE * 0.85).toInt().toShort()
                    buffer[i] = sample
                }

                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()

                val audioFormat = AudioFormat.Builder()
                    .setSampleRate(SAMPLE_RATE)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()

                val track = AudioTrack.Builder()
                    .setAudioAttributes(audioAttributes)
                    .setAudioFormat(audioFormat)
                    .setBufferSizeInBytes(buffer.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                track.write(buffer, 0, buffer.size)
                track.play()
                Thread.sleep((durationMs + 50).toLong())
                track.stop()
                track.release()
            } catch (e: Exception) {
                Log.w(TAG, "AudioTrack tone generation failed: ${e.message}")
            }
        }
    }

    /**
     * Sharp physical haptic vibration to break the physical trance.
     */
    private fun triggerHaptic() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                val vibrator = vibratorManager?.defaultVibrator
                val effect = VibrationEffect.createWaveform(longArrayOf(0, 150, 80, 200), -1)
                vibrator?.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val effect = VibrationEffect.createWaveform(longArrayOf(0, 150, 80, 200), -1)
                    vibrator?.vibrate(effect)
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(longArrayOf(0, 150, 80, 200), -1)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Haptic trigger failed: ${e.message}")
        }
    }

    /**
     * Speaks the roast out loud via TextToSpeech.
     */
    fun speak(text: String) {
        if (isTtsReady && tts != null) {
            try {
                tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "RoastUtteranceId")
            } catch (e: Exception) {
                Log.w(TAG, "TTS speak failed: ${e.message}")
            }
        } else {
            pendingSpeechText = text
        }
    }

    /**
     * Releases audio focus and shuts down TextToSpeech.
     */
    fun release() {
        try {
            tts?.stop()
            tts?.shutdown()
            tts = null
            isTtsReady = false

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
                focusRequest = null
            } else {
                @Suppress("DEPRECATION")
                audioManager.abandonAudioFocus(null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to release AudioRoastPlayer: ${e.message}")
        }
    }
}
