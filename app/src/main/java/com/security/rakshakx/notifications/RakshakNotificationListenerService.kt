package com.security.rakshakx.notifications

import android.app.Notification
import android.content.ComponentName
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.security.rakshakx.call.services.foreground.FraudMonitoringForegroundService
import com.security.rakshakx.email.pipeline.EmailThreatPipeline
import com.security.rakshakx.sms.RiskEngine
import com.security.rakshakx.sms.SmsDeduplicationGuard
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Single [NotificationListenerService] for SMS + email notification ingress (Android allows one active listener per app).
 */
class RakshakNotificationListenerService : NotificationListenerService() {

    companion object {
        private const val TAG = "RakshakX_NLS"

        val SMS_PACKAGES = setOf(
            "com.google.android.apps.messaging",
            "com.samsung.android.messaging",
            "com.android.mms",
            "com.android.messaging",
            "com.textra",
            "com.moez.QKSMS",
            "com.handcent.nextsms",
            "com.klinker.android.evolve_sms",
            "com.verizon.messaging.vzmsgs",
            "com.att.messages",
            "com.t_mobile.messages",
            "org.thoughtcrime.securesms",
            "com.xiaomi.mipush.sdk",
            "com.android.mms.ui",
            "com.microsoft.android.smsorganizer",
        )

        val EMAIL_PACKAGES = setOf(
            "com.google.android.gm",
            "com.microsoft.office.outlook",
            "com.yahoo.mobile.client.android.mail",
            "com.samsung.android.email.provider",
            "ch.protonmail.android",
            "com.easilydo.mail",
            "com.readdle.spark"
        )

        private const val OWN_PACKAGE = "com.security.rakshakx"
    }

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "RakshakNotificationListenerService created")
        RakshakNotificationChannels.bootstrap(applicationContext)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.w(TAG, "RakshakNotificationListenerService destroyed")
    }

    override fun onBind(intent: android.content.Intent?): IBinder? {
        Log.d(TAG, "onBind called")
        return super.onBind(intent)
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "onListenerConnected")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.w(TAG, "onListenerDisconnected")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isOurNotificationListenerAuthorized()) {
            try {
                requestRebind(ComponentName(this, RakshakNotificationListenerService::class.java))
            } catch (e: Exception) {
                Log.w(TAG, "requestRebind ignored", e)
            }
        }
    }

    private fun isOurNotificationListenerAuthorized(): Boolean {
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
            ?: return false
        val mine = ComponentName(this, RakshakNotificationListenerService::class.java)
        return flat.split(':').mapNotNull { runCatching { ComponentName.unflattenFromString(it) }.getOrNull() }
            .any { it == mine }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val pkg = sbn.packageName ?: return
        if (pkg == OWN_PACKAGE) return

        val extras = sbn.notification.extras ?: return
        val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""
        val body = bigText.ifBlank { text }

        if (body.isBlank()) return

        when {
            isSmsAppPackage(pkg) -> runCatching { handleSmsNotification(pkg, title, body) }
                .onFailure { Log.e(TAG, "SMS pipeline failed", it) }
            isEmailAppPackage(pkg) -> runCatching { handleEmailNotification(pkg, title, body) }
                .onFailure { Log.e(TAG, "Email pipeline failed", it) }
        }
    }

    private fun handleSmsNotification(pkg: String, title: String, body: String) {
        Log.d(TAG, "Processing SMS from: $pkg")
        if (!SmsDeduplicationGuard.shouldProcess(this, title, body)) {
            Log.d(TAG, "Skipping duplicate SMS event from notification path")
            return
        }
        val risk = RiskEngine.calculate(body, this)
        if (risk >= RiskEngine.ALERT_THRESHOLD) {
            SmsFraudNotifications.showFraudAlert(
                context = this,
                sender = title,
                message = body,
                riskScore = risk,
                source = "SMS"
            )
        }

        if (title.isNotBlank()) {
            coroutineScope.launch {
                try {
                    val orchestrator = FraudMonitoringForegroundService.getOrchestrator(applicationContext)
                    orchestrator.handleSmsEvent(phoneNumber = title, message = body)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to route to call orchestrator", e)
                }
            }
        }
    }

    private fun handleEmailNotification(pkg: String, title: String, body: String) {
        EmailThreatPipeline.process(
            context = this,
            title = title,
            body = body,
            persistenceScope = coroutineScope,
            logPrefix = "Processing Email from: $pkg"
        )
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) { /* no-op */ }

    private fun isSmsAppPackage(packageName: String): Boolean =
        packageName in SMS_PACKAGES || packageName.contains("messag")

    private fun isEmailAppPackage(packageName: String): Boolean =
        packageName in EMAIL_PACKAGES || packageName.contains("email") || packageName.contains("mail")
}
