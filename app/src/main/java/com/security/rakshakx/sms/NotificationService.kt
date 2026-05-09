package com.security.rakshakx.sms

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.os.IBinder
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import com.security.rakshakx.call.services.foreground.FraudMonitoringForegroundService
import com.security.rakshakx.email.analyzer.*
import com.security.rakshakx.email.database.ThreatDatabase
import com.security.rakshakx.email.database.ThreatEntity
import com.security.rakshakx.email.model.EmailFeatures
import com.security.rakshakx.email.scoring.ThreatCorrelationEngine
import com.security.rakshakx.email.utils.TextNormalizer
import com.security.rakshakx.email.warning.WarningNotifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * NotificationService — UNIFIED NotificationListenerService for all channels.
 *
 * Android only allows ONE active NotificationListenerService per app.
 * This service handles notification interception for:
 *   1. SMS channel — rule-based fraud scoring via RiskEngine
 *   2. Call channel — orchestrator-based risk scoring via RakshakOrchestrator
 *   3. Email channel — URL reputation and intent analysis
 */
class NotificationService : NotificationListenerService() {

    companion object {
        private const val TAG = "RakshakX_NLS"
        private const val FG_CHANNEL_ID    = "rakshak_fg"
        private const val FG_NOTIF_ID      = 1
        private const val ALERT_CHANNEL_ID = "rakshak_alerts"

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

    // -----------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "NotificationService created")
        createChannels()
        startForeground(FG_NOTIF_ID, buildPersistentNotification())
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.w(TAG, "NotificationService destroyed")
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            requestRebind(
                android.content.ComponentName(this, NotificationService::class.java)
            )
        }
    }

    // -----------------------------------------------------------------------
    // Core logic — routes to SMS, Email and Call analysis pipelines
    // -----------------------------------------------------------------------

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
            isSmsAppPackage(pkg) -> handleSmsNotification(pkg, title, body)
            isEmailAppPackage(pkg) -> handleEmailNotification(pkg, title, body)
        }
    }

    private fun handleSmsNotification(pkg: String, title: String, body: String) {
        Log.d(TAG, "Processing SMS from: $pkg")
        val risk = RiskEngine.calculate(body, this)
        if (risk >= RiskEngine.ALERT_THRESHOLD) {
            NotificationHelper.showFraudAlert(
                context   = this,
                sender    = title,
                message   = body,
                riskScore = risk,
                source    = "SMS"
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
        Log.d(TAG, "Processing Email from: $pkg")
        val fullText = "$title $body"
        val normalizedText = TextNormalizer.normalize(fullText)
        val urls = UrlAnalyzer.extractUrls(normalizedText)
        
        val urlReputationReasons = mutableListOf<String>()
        urls.forEach { url ->
            urlReputationReasons.addAll(UrlReputationAnalyzer.analyzeUrl(url))
        }
        
        val correlationReasons = ThreatCorrelationEngine.processUrls(urls)
        val suspiciousIntent = IntentAnalyzer.detectSuspiciousIntent(normalizedText)
        val suspiciousPhraseCount = IntentAnalyzer.countSuspiciousPhrases(normalizedText)
        
        val features = EmailFeatures(
            originalText = fullText,
            normalizedText = normalizedText,
            hasLink = urls.isNotEmpty(),
            hasUrgency = listOf("urgent", "immediately", "act now", "warning").any { normalizedText.contains(it, true) },
            hasFinancialWords = listOf("bank", "account", "payment", "verify").any { normalizedText.contains(it, true) },
            suspiciousIntent = suspiciousIntent,
            suspiciousPhraseCount = suspiciousPhraseCount,
            excessiveCaps = ObfuscationAnalyzer.hasExcessiveCaps(fullText),
            symbolReplacement = ObfuscationAnalyzer.hasSymbolReplacement(fullText),
            repeatedSymbols = ObfuscationAnalyzer.hasRepeatedSymbols(fullText),
            multipleLinks = UrlAnalyzer.hasMultipleLinks(urls),
            dangerousAttachment = AttachmentAnalyzer.hasDangerousAttachment(normalizedText)
        )

        val result = com.security.rakshakx.email.scoring.RiskEngine.calculateRisk(features)
        
        if (result.riskLevel == "HIGH RISK") {
            val combinedReasons = result.reasons + urlReputationReasons + correlationReasons
            WarningNotifier.showHighRiskWarning(this, fullText, combinedReasons)

            coroutineScope.launch {
                try {
                    val database = ThreatDatabase.getDatabase(this@NotificationService)
                    database.threatDao().insertThreat(ThreatEntity(
                        title = title,
                        message = fullText,
                        riskScore = result.score,
                        riskLevel = result.riskLevel,
                        timestamp = System.currentTimeMillis()
                    ))
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to save email threat", e)
                }
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) { /* no-op */ }

    private fun isSmsAppPackage(packageName: String): Boolean = 
        packageName in SMS_PACKAGES || packageName.contains("messag")

    private fun isEmailAppPackage(packageName: String): Boolean = 
        packageName in EMAIL_PACKAGES || packageName.contains("email") || packageName.contains("mail")

    private fun buildPersistentNotification(): Notification {
        return NotificationCompat.Builder(this, FG_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("RakshakX Protection Active")
            .setContentText("Monitoring SMS and Email for fraud")
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(FG_CHANNEL_ID) == null) {
            manager.createNotificationChannel(NotificationChannel(FG_CHANNEL_ID, "RakshakX Status", NotificationManager.IMPORTANCE_MIN).apply { setShowBadge(false) })
        }
        if (manager.getNotificationChannel(ALERT_CHANNEL_ID) == null) {
            manager.createNotificationChannel(NotificationChannel(ALERT_CHANNEL_ID, "Rakshak Fraud Alerts", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Fires when a suspicious message is detected"
                enableVibration(true)
            })
        }
    }
}
