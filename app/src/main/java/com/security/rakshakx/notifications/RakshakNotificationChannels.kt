package com.security.rakshakx.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

/**
 * Single source of truth for notification channel IDs used across SMS, call, email, and web.
 * Channel string values are stable for end-user OS settings.
 */
object RakshakNotificationChannels {

    const val ALERTS = "rakshak_alerts"
    const val STATUS = "rakshak_fg"
    /** Canonical email phishing / HIGH RISK (aligned with [com.security.rakshakx.email.warning.WarningNotifier]). */
    const val EMAIL_PHISHING = "fraud_warning_channel"
    const val TEST_PHISHING = "test_phishing_channel"
    const val RECORDING = "rakshakx_recording"
    const val FRAUD_RESULTS = "rakshakx_fraud_alerts"
    const val VPN = "rakshakx_vpn"

    fun bootstrap(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (manager.getNotificationChannel(EMAIL_PHISHING) == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    EMAIL_PHISHING,
                    "Email Phishing Alerts",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Alerts when phishing emails are detected"
                }
            )
        }
        if (manager.getNotificationChannel(TEST_PHISHING) == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    TEST_PHISHING,
                    "Test Phishing Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Test notifications for phishing detection"
                }
            )
        }
        if (manager.getNotificationChannel(ALERTS) == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    ALERTS,
                    "Rakshak Fraud Alerts",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Alerts for suspicious SMS and call fraud"
                    enableVibration(true)
                }
            )
        }
        if (manager.getNotificationChannel(STATUS) == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    STATUS,
                    "RakshakX Status",
                    NotificationManager.IMPORTANCE_MIN
                ).apply {
                    description = "Background monitoring status"
                    setShowBadge(false)
                }
            )
        }
        if (manager.getNotificationChannel(RECORDING) == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    RECORDING,
                    "RakshakX Recording",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Notification while analyzing calls"
                }
            )
        }
        if (manager.getNotificationChannel(FRAUD_RESULTS) == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    FRAUD_RESULTS,
                    "Fraud Alerts",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notifications about potential scam calls"
                }
            )
        }
        if (manager.getNotificationChannel(VPN) == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    VPN,
                    "RakshakX Protection",
                    NotificationManager.IMPORTANCE_LOW
                )
            )
        }
    }
}
