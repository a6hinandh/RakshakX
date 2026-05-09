package com.security.rakshakx.email.warning

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.security.rakshakx.notifications.RakshakNotificationChannels

object WarningNotifier {

    fun showHighRiskWarning(

        context: Context,

        title: String,

        reasons: List<String>

    ) {

        RakshakNotificationChannels.bootstrap(context.applicationContext)

        val channelId = RakshakNotificationChannels.EMAIL_PHISHING

        val manager =

            context.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            manager.getNotificationChannel(channelId) == null
        ) {
            val channel = NotificationChannel(
                channelId,
                "Fraud Warnings",
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.description = "RakshakX phishing alerts"
            manager.createNotificationChannel(channel)
        }

        // Convert reasons list into readable text
        val reasonText =
            reasons.joinToString("\n• ", "• ")

        // Build warning notification
        val notification = NotificationCompat.Builder(
            context,
            channelId
        )

            .setSmallIcon(
                android.R.drawable.ic_dialog_alert
            )

            .setContentTitle(
                "⚠️ HIGH RISK EMAIL DETECTED"
            )

            .setContentText(title)

            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(
                        "Possible phishing attempt detected.\n\n$reasonText"
                    )
            )

            .setPriority(
                NotificationCompat.PRIORITY_HIGH
            )

            .setAutoCancel(true)

            .build()

        // Show notification
        manager.notify(
            9999,
            notification
        )
    }
}