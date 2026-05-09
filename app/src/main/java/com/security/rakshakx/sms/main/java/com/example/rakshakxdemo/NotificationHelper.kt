package com.example.rakshakxdemo

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat

object NotificationHelper {

    private const val CHANNEL_ID = "rakshak_alerts"

    fun showFraudAlert(
        context: Context,
        sender: String,
        message: String,
        riskScore: Int,
        source: String
    ) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        ensureChannel(manager)

        val severity = RiskEngine.severity(riskScore)
        val title = when (severity) {
            "CRITICAL" -> "🚨 CRITICAL Fraud Alert"
            else       -> "⚠️ Suspicious SMS Detected"
        }

        val body = "Risk: $riskScore/100 [$severity]\nFrom: $sender\n\n" +
                message.take(300).let { if (message.length > 300) "$it…" else it }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText("From $sender — Risk: $riskScore/100")
            .setStyle(NotificationCompat.BigTextStyle().bigText(body).setBigContentTitle(title))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 300, 200, 300))
            .build()

        manager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun ensureChannel(manager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Rakshak Fraud Alerts",
                    NotificationManager.IMPORTANCE_HIGH).apply {
                    enableVibration(true)
                }
            )
        }
    }
}