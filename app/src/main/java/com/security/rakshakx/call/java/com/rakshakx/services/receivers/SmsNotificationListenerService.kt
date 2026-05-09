package com.rakshakx.services.receivers

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.rakshakx.services.foreground.FraudMonitoringForegroundService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsNotificationListenerService : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return
        if (!isLikelySmsNotification(sbn.packageName)) return

        val extras = sbn.notification.extras ?: return
        val sender = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim()
        val message = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim()

        if (sender.isNullOrBlank() || message.isNullOrBlank()) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val orchestrator = FraudMonitoringForegroundService.getOrchestrator(applicationContext)
                orchestrator.handleSmsEvent(
                    phoneNumber = sender,
                    message = message
                )
            } catch (exception: Exception) {
                Log.e("SmsNotificationListener", "Failed to process SMS notification", exception)
            }
        }
    }

    private fun isLikelySmsNotification(packageName: String?): Boolean {
        if (packageName.isNullOrBlank()) return false
        val knownSmsPackages = setOf(
            "com.google.android.apps.messaging",
            "com.android.messaging",
            "com.samsung.android.messaging",
            "com.microsoft.android.smsorganizer"
        )
        return packageName in knownSmsPackages || packageName.contains("messag")
    }
}
