package com.security.rakshakx.notifications

import android.app.Notification
import android.content.ComponentName
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.security.rakshakx.call.services.foreground.FraudMonitoringForegroundService
import com.security.rakshakx.email.EmailScamDetector
import com.security.rakshakx.email.pipeline.EmailThreatPipeline
import com.security.rakshakx.sms.RiskEngine
import com.security.rakshakx.sms.SmsDeduplicationGuard
import com.security.rakshakx.sms.SmsScamDetector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Single [NotificationListenerService] for SMS + email ingress (Android allows one active listener per app).
 *
 * Routes by posting package:
 * - **SMS** — hybrid ML ([SmsScamDetector]), rule score ([RiskEngine]), fraud alerts,
 *   and [FraudMonitoringForegroundService] orchestrator for cross-channel correlation.
 * - **Email** — hybrid ML ([EmailScamDetector]) plus shared [EmailThreatPipeline] (URL/reputation/rules, DB, warnings).
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

        private val EMAIL_IN_TEXT = Regex("\\S+@\\S+\\.\\S+")
    }

    /** Sender / subject / snippet inferred from [Notification.extras] (varies by OEM and mail app). */
    private data class EmailNotificationIngress(
        val sender: String,
        val subject: String,
        val bodySnippet: String,
    )

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
        Log.d("RAKSHAK_LISTENER", "CONNECTED SUCCESSFULLY")
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
            isSmsAppPackage(pkg) -> {
                if (title.contains("new messages", ignoreCase = true)) return
                handleSmsNotification(pkg, title, body)
            }
            isEmailAppPackage(pkg) -> {
                val ingress = extractEmailIngress(extras, pkg, title, body)
                handleEmailNotification(pkg, ingress)
            }
            else -> Log.d(TAG, "Ignoring notification from non-SMS/non-email package: $pkg")
        }
    }



    private fun handleSmsNotification(pkg: String, title: String, body: String) {
        Log.d(TAG, "Processing SMS notification from: $pkg")
        if (!SmsDeduplicationGuard.shouldProcess(this, title, body)) {
            Log.d(TAG, "Skipping duplicate SMS event from notification path")
            return
        }

        coroutineScope.launch {
            try {
                SmsScamDetector(this@RakshakNotificationListenerService).analyze(sender = title, body = body)

                val risk = RiskEngine.calculate(body, this@RakshakNotificationListenerService)
                if (risk >= RiskEngine.ALERT_THRESHOLD) {
                    SmsFraudNotifications.showFraudAlert(
                        context = this@RakshakNotificationListenerService,
                        sender = title,
                        message = body,
                        riskScore = risk,
                        source = "SMS"
                    )
                }

                if (title.isNotBlank()) {
                    val orchestrator = FraudMonitoringForegroundService.getOrchestrator(applicationContext)
                    orchestrator.handleSmsEvent(phoneNumber = title, message = body)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to process SMS notification pipeline", e)
            }
        }
    }

    private fun handleEmailNotification(pkg: String, ingress: EmailNotificationIngress) {
        Log.d(TAG, "Processing Email notification from: $pkg (sender=${ingress.sender.take(48)}…)")

        val pipelineTitle = ingress.subject.ifBlank { ingress.bodySnippet.take(120).trim() }
        val pipelineBody = buildString {
            val s = ingress.sender
            if (s.isNotBlank() && !s.equals(pkg, ignoreCase = true)) {
                append("From: ").appendLine(s)
            }
            if (ingress.subject.isNotBlank()) {
                append("Subject: ").appendLine(ingress.subject)
            }
            append(ingress.bodySnippet)
        }.trim()

        coroutineScope.launch {
            try {
                EmailScamDetector(this@RakshakNotificationListenerService).analyze(
                    sender = ingress.sender.ifBlank { pkg },
                    subject = ingress.subject,
                    body = ingress.bodySnippet.ifBlank { pipelineBody }
                )
                val result = EmailThreatPipeline.process(
                    context = this@RakshakNotificationListenerService,
                    title = pipelineTitle.ifBlank { ingress.sender },
                    body = pipelineBody.ifBlank { ingress.bodySnippet },
                    persistenceScope = coroutineScope,
                    logPrefix = "Processing Email from: $pkg"
                )
                Log.d(TAG, "Email ingress result: ${result.riskLevel} score=${result.score}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to process email notification pipeline", e)
            }
        }
    }

    /**
     * Mail apps differ: many use title=sender / text=subject / bigText=snippet;
     * others put subject in title and sender in [Notification.EXTRA_SUB_TEXT].
     */
    private fun extractEmailIngress(
        extras: Bundle,
        pkg: String,
        title: String,
        combinedBody: String,
    ): EmailNotificationIngress {
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim().orEmpty()
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.trim().orEmpty()
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()?.trim().orEmpty()
        val summaryText =
            extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT)?.toString()?.trim().orEmpty()

        val snippet = bigText.ifBlank { combinedBody }.ifBlank { text }
        val titleHasEmail = EMAIL_IN_TEXT.containsMatchIn(title)
        val subHasEmail = EMAIL_IN_TEXT.containsMatchIn(subText)
        val subProbablySender = subText.isNotBlank() &&
            subText != title &&
            (subHasEmail || subText.length <= 72)

        return when {
            titleHasEmail -> EmailNotificationIngress(
                sender = title,
                subject = text.ifBlank { summaryText },
                bodySnippet = snippet,
            )
            subProbablySender && title.isNotBlank() -> EmailNotificationIngress(
                sender = subText,
                subject = title,
                bodySnippet = bigText.ifBlank { text },
            )
            else -> EmailNotificationIngress(
                sender = title.ifBlank { pkg },
                subject = text.ifBlank { summaryText },
                bodySnippet = snippet,
            )
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) { /* no-op */ }

    private fun isSmsAppPackage(packageName: String): Boolean =
        packageName in SMS_PACKAGES || packageName.contains("messag")

    private fun isEmailAppPackage(packageName: String): Boolean =
        packageName in EMAIL_PACKAGES || packageName.contains("email") || packageName.contains("mail")
}
