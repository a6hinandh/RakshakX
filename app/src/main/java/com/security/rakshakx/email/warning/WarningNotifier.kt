package com.security.rakshakx.email.warning

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.security.rakshakx.notifications.RakshakNotificationChannels

object WarningNotifier {

    fun showRiskWarning(
        context: Context,
        notificationTitle: String,
        emailTitle: String,
        reasons: List<String>
    ) {

        RakshakNotificationChannels.bootstrap(
            context.applicationContext
        )

        val channelId =
            RakshakNotificationChannels.EMAIL_PHISHING

        val manager =
            context.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager

        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            manager.getNotificationChannel(channelId) == null
        ) {

            val channel = NotificationChannel(
                channelId,
                "Fraud Warnings",
                NotificationManager.IMPORTANCE_HIGH
            )

            channel.description =
                "RakshakX phishing alerts"

            manager.createNotificationChannel(channel)
        }

        val reasonText =
            reasons.joinToString(
                separator = "\n• ",
                prefix = "• "
            )

        val notification =
            NotificationCompat.Builder(
                context,
                channelId
            )

                .setSmallIcon(
                    android.R.drawable.ic_dialog_alert
                )

                .setContentTitle(
                    notificationTitle
                )

                .setContentText(
                    emailTitle.take(120)
                )

                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(
                            "$notificationTitle\n\n$reasonText"
                        )
                )

                .setPriority(
                    NotificationCompat.PRIORITY_HIGH
                )

                .setCategory(
                    NotificationCompat.CATEGORY_MESSAGE
                )

                .setAutoCancel(true)

                .build()

        manager.notify(
            System.currentTimeMillis().toInt(),
            notification
        )
    }
}