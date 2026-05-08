package com.security.rakshakx.web.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.security.rakshakx.R

class VpnProtectionNotifier(private val context: Context) {
    private val channelId = "rakshakx_vpn"
    private val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun createChannel() {
        val channel = NotificationChannel(
            channelId,
            "RakshakX Protection",
            NotificationManager.IMPORTANCE_LOW
        )
        manager.createNotificationChannel(channel)
    }

    fun buildForegroundNotification(): Notification {
        return NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("RakshakX Protection")
            .setContentText("Monitoring browser traffic locally")
            .setOngoing(true)
            .build()
    }

    fun buildThreatAlert(message: String): Notification {
        return NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Threat blocked")
            .setContentText(message)
            .setAutoCancel(true)
            .build()
    }

    fun notifyThreat(message: String) {
        manager.notify(THREAT_NOTIFICATION_ID, buildThreatAlert(message))
    }

    companion object {
        private const val THREAT_NOTIFICATION_ID = 2001
    }
}
