package com.security.rakshakx.integration

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.security.rakshakx.MainActivity
import com.security.rakshakx.R

class ScamAlertManager(private val context: Context) {

    private val TAG = "ScamAlertManager"

    companion object {
        const val CHANNEL_ID_HIGH   = "rakshakx_scam_high"
        const val CHANNEL_ID_MEDIUM = "rakshakx_scam_medium"
        const val CHANNEL_ID_LOW    = "rakshakx_safe"
        private var notificationId  = 1000
    }

    init {
        createNotificationChannels()
    }

    // ─── Main Entry Point ─────────────────────────────────────────────────────

    fun handleResult(result: ModelResult) {
        Log.d(TAG, "Handling result: $result")

        when (result.riskLevel) {
            RiskLevel.HIGH   -> sendHighAlert(result)
            RiskLevel.MEDIUM -> sendMediumAlert(result)
            RiskLevel.LOW    -> Log.d(TAG, "Safe content on ${result.channel}, no alert needed")
        }
    }

    // ─── Alert Builders ───────────────────────────────────────────────────────

    private fun sendHighAlert(result: ModelResult) {
        val channelIcon = channelIcon(result.channel)
        val title       = "⚠️ Scam Detected — ${result.channel.uppercase()}"
        val body        = "$channelIcon ${describeResult(result)}\n" +
                "Confidence: ${"%.0f".format(result.confidence * 100)}% • " +
                "Detected by: ${modelLabel(result.modelUsed)}"

        sendNotification(
            channelId  = CHANNEL_ID_HIGH,
            title      = title,
            body       = body,
            priority   = NotificationCompat.PRIORITY_HIGH
        )
    }

    private fun sendMediumAlert(result: ModelResult) {
        val title = "⚠ Suspicious Content — ${result.channel.uppercase()}"
        val body  = "${describeResult(result)} " +
                "(${" %.0f".format(result.confidence * 100)}% confidence)"

        sendNotification(
            channelId  = CHANNEL_ID_MEDIUM,
            title      = title,
            body       = body,
            priority   = NotificationCompat.PRIORITY_DEFAULT
        )
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private fun sendNotification(
        channelId: String,
        title: String,
        body: String,
        priority: Int
    ) {
        val notifManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_shield_warning)   // Updated from ic_shield
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(priority)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notifManager.notify(notificationId++, notification)
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID_HIGH,
                    "Scam Alerts (High Risk)",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply { description = "Critical scam detections across all channels" }
            )
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID_MEDIUM,
                    "Suspicious Content",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply { description = "Medium-risk suspicious content alerts" }
            )
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID_LOW,
                    "Safe Content Info",
                    NotificationManager.IMPORTANCE_LOW
                ).apply { description = "Informational: content cleared as safe" }
            )
        }
    }

    private fun describeResult(result: ModelResult): String = when (result.channel) {
        "sms"   -> "This SMS message appears to be a scam."
        "email" -> "This email has been flagged as potentially fraudulent."
        "call"  -> "Call transcript detected suspicious content."
        "web"   -> "This website may be a phishing or scam site."
        else    -> "Suspicious content detected."
    }

    private fun channelIcon(channel: String): String = when (channel) {
        "sms"   -> "💬"
        "email" -> "📧"
        "call"  -> "📞"
        "web"   -> "🌐"
        else    -> "🔍"
    }

    private fun modelLabel(modelUsed: String): String = when {
        modelUsed.contains("indicbert") -> "IndicBERT (Indic Language)"
        modelUsed.contains("distilbert") -> "DistilBERT"
        else -> "Fallback"
    }
}