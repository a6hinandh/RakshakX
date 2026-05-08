package com.security.rakshakx.email.warning

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

import androidx.core.app.NotificationCompat

object WarningNotifier {

    fun showHighRiskWarning(

        context: Context,

        title: String,

        reasons: List<String>

    ) {

        val channelId = "fraud_warning_channel"

        // Notification manager
        val manager =

            context.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager

        // Create notification channel
        val channel = NotificationChannel(

            channelId,

            "Fraud Warnings",

            NotificationManager.IMPORTANCE_HIGH
        )

        channel.description =
            "RakshakX phishing alerts"

        manager.createNotificationChannel(channel)

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