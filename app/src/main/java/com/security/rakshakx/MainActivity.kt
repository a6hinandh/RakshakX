package com.security.rakshakx

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

import com.security.rakshakx.ui.navigation.RakshakXNavHost
import com.security.rakshakx.ui.theme.RakshakXTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Ensure notification channels exist
        createNotificationChannels()

        setContent {
            RakshakXTheme {
                RakshakXNavHost(activity = this)
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // Notification channels
    // ═══════════════════════════════════════════════════════════════

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Email phishing alerts
            val emailChannel = NotificationChannel(
                "phishing_warning_channel",
                "Email Phishing Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when phishing emails are detected"
            }
            manager.createNotificationChannel(emailChannel)

            // Test notifications
            val testChannel = NotificationChannel(
                "test_phishing_channel",
                "Test Phishing Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Test notifications for phishing detection"
            }
            manager.createNotificationChannel(testChannel)

            // SMS fraud alerts
            val smsChannel = NotificationChannel(
                "rakshak_alerts",
                "SMS Fraud Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts for suspicious SMS messages"
                enableVibration(true)
            }
            manager.createNotificationChannel(smsChannel)

            // Monitoring status
            val monitoringChannel = NotificationChannel(
                "rakshak_fg",
                "RakshakX Status",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "Background monitoring status"
                setShowBadge(false)
            }
            manager.createNotificationChannel(monitoringChannel)
        }
    }
}