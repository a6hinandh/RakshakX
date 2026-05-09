package com.security.rakshakx.notifications

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.security.rakshakx.permissions.PermissionManager
import com.security.rakshakx.sms.RiskEngine

object SmsFraudNotifications {

    fun showFraudAlert(
        context: Context,
        sender: String,
        message: String,
        riskScore: Int,
        source: String
    ) {
        if (!PermissionManager.hasNotificationPermission(context)) {
            return
        }
        RakshakNotificationChannels.bootstrap(context.applicationContext)
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val severity = RiskEngine.severity(riskScore)
        val title = when (severity) {
            "CRITICAL" -> "🚨 CRITICAL Fraud Alert"
            else -> "⚠️ Suspicious SMS Detected"
        }

        val body = "Risk: $riskScore/100 [$severity]\nFrom: $sender\n\n" +
            message.take(300).let { if (message.length > 300) "$it…" else it }

        val notification = NotificationCompat.Builder(context, RakshakNotificationChannels.ALERTS)
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
}
