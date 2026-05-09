package com.security.rakshakx.notifications.vpn

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.security.rakshakx.R
import com.security.rakshakx.notifications.RakshakNotificationChannels
import com.security.rakshakx.permissions.PermissionManager

class VpnProtectionNotifier(private val context: Context) {

    private val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun createChannel() {
        RakshakNotificationChannels.bootstrap(context.applicationContext)
    }

    fun buildForegroundNotification(): Notification {
        return NotificationCompat.Builder(context, RakshakNotificationChannels.VPN)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("RakshakX Protection")
            .setContentText("Monitoring browser traffic locally")
            .setOngoing(true)
            .build()
    }

    fun buildThreatAlert(message: String): Notification {
        return NotificationCompat.Builder(context, RakshakNotificationChannels.VPN)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("RakshakX Threat Blocked")
            .setContentText(message.substringBefore(" | "))
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
    }

    fun notifyThreat(title: String, message: String) {
        if (!PermissionManager.hasNotificationPermission(context)) {
            return
        }
        val notification = NotificationCompat.Builder(context, RakshakNotificationChannels.VPN)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message.substringBefore("\n"))
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
            
        manager.notify(THREAT_NOTIFICATION_ID, notification)
    }

    companion object {
        private const val THREAT_NOTIFICATION_ID = 2001
    }
}
