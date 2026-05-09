package com.security.rakshakx.email.pipeline

import android.content.Context
import android.util.Log
import com.security.rakshakx.email.analyzer.AttachmentAnalyzer
import com.security.rakshakx.email.analyzer.IntentAnalyzer
import com.security.rakshakx.email.analyzer.ObfuscationAnalyzer
import com.security.rakshakx.email.analyzer.UrlAnalyzer
import com.security.rakshakx.email.analyzer.UrlReputationAnalyzer
import com.security.rakshakx.email.database.ThreatDatabase
import com.security.rakshakx.email.database.ThreatEntity
import com.security.rakshakx.email.model.EmailFeatures
import com.security.rakshakx.email.model.RiskResult
import com.security.rakshakx.email.scoring.RiskEngine
import com.security.rakshakx.email.scoring.ThreatCorrelationEngine
import com.security.rakshakx.email.utils.TextNormalizer
import com.security.rakshakx.email.warning.WarningNotifier
import com.security.rakshakx.permissions.PermissionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Same email analysis path used by NotificationListenerService and manual in-app scans.
 */
object EmailThreatPipeline {

    private const val TAG = "EmailThreatPipeline"

    fun presetPhishingSampleTitle(): String = "Security Alert — Action Required"

    fun presetPhishingSampleBody(): String =
        """Dear user, URGENT action required — verify your account immediately or login will be blocked.
Your bank account payment failed — confirm your identity here: https://phish.example.com/secure-login
Click here before unauthorized login locks you out.""".trimIndent()

    /**
     * @param persistenceScope if non-null and result is HIGH RISK, persists on [Dispatchers.IO] via this scope
     */
    fun process(
        context: Context,
        title: String,
        body: String,
        persistenceScope: CoroutineScope?,
        logPrefix: String? = null,
    ): RiskResult {
        logPrefix?.let { Log.d(TAG, "$it") }

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
            hasUrgency = listOf("urgent", "immediately", "act now", "warning").any { normalizedText.contains(it, ignoreCase = true) },
            hasFinancialWords = listOf("bank", "account", "payment", "verify").any { normalizedText.contains(it, ignoreCase = true) },
            suspiciousIntent = suspiciousIntent,
            suspiciousPhraseCount = suspiciousPhraseCount,
            excessiveCaps = ObfuscationAnalyzer.hasExcessiveCaps(fullText),
            symbolReplacement = ObfuscationAnalyzer.hasSymbolReplacement(fullText),
            repeatedSymbols = ObfuscationAnalyzer.hasRepeatedSymbols(fullText),
            multipleLinks = UrlAnalyzer.hasMultipleLinks(urls),
            dangerousAttachment = AttachmentAnalyzer.hasDangerousAttachment(normalizedText)
        )

        val result = RiskEngine.calculateRisk(features)
        Log.d(TAG, "Email scan result: ${result.riskLevel} score=${result.score}")

        if (
            result.riskLevel == "HIGH RISK"
            ||
            result.riskLevel == "MEDIUM RISK"
        ){
            val combinedReasons = result.reasons + urlReputationReasons + correlationReasons
            if (PermissionManager.hasNotificationPermission(context)) {
                WarningNotifier.showRiskWarning(

                    context = context,

                    notificationTitle = if (
                        result.riskLevel == "HIGH RISK"
                    ) {
                        "🚨 HIGH RISK EMAIL DETECTED"
                    } else {
                        "⚠ Suspicious Email Detected"
                    },

                    emailTitle = fullText,

                    reasons = combinedReasons
                )
            } else {
                Log.w(
                    TAG,
                    "${result.riskLevel} but POST_NOTIFICATIONS not granted; skipping alert notification"
                )}

            val appCtx = context.applicationContext
            persistenceScope?.launch(Dispatchers.IO) {
                try {
                    val database = ThreatDatabase.getDatabase(appCtx)
                    database.threatDao().insertThreat(
                        ThreatEntity(
                            title = title,
                            message = fullText,
                            riskScore = result.score,
                            riskLevel = result.riskLevel,
                            timestamp = System.currentTimeMillis()
                        )
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to save email threat", e)
                }
            }
        }

        return result
    }
}
