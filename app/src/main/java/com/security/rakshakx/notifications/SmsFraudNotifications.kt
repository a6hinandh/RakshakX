package com.security.rakshakx.notifications

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.security.rakshakx.permissions.PermissionManager
import com.security.rakshakx.sms.RiskEngine

object SmsFraudNotifications {

    private const val GROUP_KEY_THREATS = "com.security.rakshakx.THREAT_GROUP"
    private const val SUMMARY_ID = 9000

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
        val isCritical = severity == "CRITICAL" || severity == "HIGH"

        val channelId = if (isCritical) {
            RakshakNotificationChannels.ALERTS_CRITICAL
        } else {
            RakshakNotificationChannels.ALERTS_LOW
        }

        val title = when (severity) {
            "CRITICAL" -> "CRITICAL Fraud Alert"
            "HIGH" -> "High Risk SMS Detected"
            "MEDIUM" -> "Suspicious SMS"
            else -> "Low Risk Activity"
        }

        val body = "Risk: $riskScore/100 [$severity]\nFrom: $sender\n\n" +
            message.take(300).let { if (message.length > 300) "$it…" else it }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("$title — $source")
            .setContentText("From $sender — Risk: $riskScore/100")
            .setStyle(NotificationCompat.BigTextStyle().bigText(body).setBigContentTitle(title))
            .setPriority(if (isCritical) NotificationCompat.PRIORITY_MAX else NotificationCompat.PRIORITY_LOW)
            .setCategory(if (isCritical) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_STATUS)
            .setAutoCancel(true)
            .setGroup(GROUP_KEY_THREATS)
            .apply {
                if (isCritical) setVibrate(longArrayOf(0, 300, 200, 300))
            }
            .build()

        manager.notify(System.currentTimeMillis().toInt(), notification)

        val summaryNotification = NotificationCompat.Builder(context, RakshakNotificationChannels.ALERTS)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("RakshakX Threat Summary")
            .setContentText("Multiple threats detected")
            .setStyle(NotificationCompat.InboxStyle()
                .setSummaryText("Threat Activity"))
            .setGroup(GROUP_KEY_THREATS)
            .setGroupSummary(true)
            .setAutoCancel(true)
            .build()

        manager.notify(SUMMARY_ID, summaryNotification)
    }
}
