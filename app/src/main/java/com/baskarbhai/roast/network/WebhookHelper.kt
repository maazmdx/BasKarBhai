package com.baskarbhai.roast.network

import android.os.Handler
import android.os.Looper
import android.util.Log
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Handles Webhook dispatches to Discord or Telegram channels to snitch on doomed scrollers.
 * Architected by maazmdx.
 */
object WebhookHelper {

    private const val TAG = "WebhookHelper"
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    private val mainHandler = Handler(Looper.getMainLooper())

    interface WebhookCallback {
        fun onSuccess(responseMessage: String)
        fun onFailure(errorMessage: String)
    }

    /**
     * Sends a Tier 4 roast alert snitching on the user to Discord / Telegram.
     */
    fun sendTier4SnitchAlert(
        webhookUrl: String,
        nickname: String,
        reelsCount: Int,
        activeDuration: String,
        platformName: String,
        callback: WebhookCallback? = null
    ) {
        if (webhookUrl.isBlank()) {
            callback?.onFailure("Webhook URL is empty.")
            return
        }

        val safeNickname = if (nickname.isBlank()) "Our friend" else nickname
        val message = "🚨 **BAS KAR BHAI - DOOMSCROLL SNITCH ALERT!** 🚨\n\n" +
                "👤 **User:** `$safeNickname`\n" +
                "📱 **Platform:** `$platformName`\n" +
                "📜 **Reels Watched:** `$reelsCount`\n" +
                "⏱️ **Time Wasted:** `$activeDuration`\n" +
                "⚡ **Action Taken:** User hit **Tier 4 Escalation**! Kicked out to Home Screen immediately!\n\n" +
                "_Created by maazmdx • BasKarBhai Sentinel_"

        sendPayload(webhookUrl, safeNickname, message, callback)
    }

    /**
     * Sends a test ping to verify webhook connectivity.
     */
    fun sendTestPing(
        webhookUrl: String,
        nickname: String,
        callback: WebhookCallback
    ) {
        if (webhookUrl.isBlank()) {
            callback.onFailure("Please enter a valid Discord or Telegram Webhook URL.")
            return
        }

        val safeNickname = if (nickname.isBlank()) "maazmdx" else nickname
        val message = "🧪 **BasKarBhai Webhook Test Successful!**\n\n" +
                "Hey **$safeNickname**, your friend-group snitch channel is configured and ready to roast doom-scrollers!\n" +
                "_⚡ Powered by maazmdx_"

        sendPayload(webhookUrl, safeNickname, message, callback)
    }

    private fun sendPayload(
        webhookUrl: String,
        nickname: String,
        messageContent: String,
        callback: WebhookCallback?
    ) {
        try {
            val jsonBody = JSONObject().apply {
                if (webhookUrl.contains("discord.com", ignoreCase = true)) {
                    put("username", "BasKarBhai [maazmdx]")
                    put("content", messageContent)
                } else if (webhookUrl.contains("telegram.org", ignoreCase = true)) {
                    put("text", messageContent)
                    put("parse_mode", "Markdown")
                } else {
                    put("sender", "BasKarBhai")
                    put("user", nickname)
                    put("message", messageContent)
                }
            }

            val requestBody = jsonBody.toString().toRequestBody(JSON_MEDIA_TYPE)
            val request = Request.Builder()
                .url(webhookUrl)
                .post(requestBody)
                .addHeader("User-Agent", "BasKarBhai-Android-maazmdx/1.0")
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e(TAG, "Webhook failed: ${e.message}")
                    mainHandler.post {
                        callback?.onFailure("Network Error: ${e.localizedMessage ?: "Unknown error"}")
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    val isSuccess = response.isSuccessful
                    val code = response.code
                    response.close()

                    mainHandler.post {
                        if (isSuccess || code in 200..204) {
                            callback?.onSuccess("Ping delivered successfully (HTTP $code)!")
                        } else {
                            callback?.onFailure("HTTP error code: $code")
                        }
                    }
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Webhook exception: ${e.message}")
            mainHandler.post {
                callback?.onFailure("Invalid URL or Payload: ${e.message}")
            }
        }
    }
}
