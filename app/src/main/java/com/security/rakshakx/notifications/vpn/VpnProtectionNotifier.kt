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
            .setContentTitle("Threat blocked")
            .setContentText(message)
            .setAutoCancel(true)
            .build()
    }

    fun notifyThreat(message: String) {
        if (!PermissionManager.hasNotificationPermission(context)) {
            return
        }
        manager.notify(THREAT_NOTIFICATION_ID, buildThreatAlert(message))
    }

    companion object {
        private const val THREAT_NOTIFICATION_ID = 2001
    }
}
