package com.security.rakshakx.email.listener

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

import com.security.rakshakx.email.analyzer.AttachmentAnalyzer
import com.security.rakshakx.email.analyzer.IntentAnalyzer
import com.security.rakshakx.email.analyzer.ObfuscationAnalyzer
import com.security.rakshakx.email.analyzer.UrlAnalyzer
import com.security.rakshakx.email.analyzer.UrlReputationAnalyzer

import com.security.rakshakx.email.database.ThreatDatabase
import com.security.rakshakx.email.database.ThreatEntity

import com.security.rakshakx.email.model.EmailFeatures

import com.security.rakshakx.email.scoring.RiskEngine
import com.security.rakshakx.email.scoring.ThreatCorrelationEngine

import com.security.rakshakx.email.utils.TextNormalizer

import com.security.rakshakx.email.warning.WarningNotifier

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class EmailNotificationListener :
    NotificationListenerService() {

    override fun onNotificationPosted(
        sbn: StatusBarNotification
    ) {

        val packageName = sbn.packageName

        val extras = sbn.notification.extras

        val title =
            extras.getString("android.title") ?: ""

        val text =
            extras.getCharSequence("android.text")
                ?.toString() ?: ""

        // Ignore RakshakX warning notifications
        if (

            packageName == "com.security.rakshakx"

            &&

            title.contains(
                "HIGH RISK EMAIL DETECTED",
                true
            )
        ) {
            return
        }

        // Supported email apps
        val supportedEmailApps = listOf(

            "com.google.android.gm",

            "com.microsoft.office.outlook",

            "com.yahoo.mobile.client.android.mail",

            "com.samsung.android.email.provider",

            "ch.protonmail.android",

            "com.easilydo.mail",

            "com.readdle.spark",

            // Internal testing
            "com.security.rakshakx"
        )

        if (!supportedEmailApps.contains(packageName)) {
            return
        }

        val fullText = "$title $text"

        // STEP 1 — Normalize
        val normalizedText =
            TextNormalizer.normalize(fullText)

        // STEP 2 — URL Analysis
        val urls =
            UrlAnalyzer.extractUrls(normalizedText)

        val hasLink =
            urls.isNotEmpty()

        val multipleLinks =
            UrlAnalyzer.hasMultipleLinks(urls)

        // URL Reputation Analysis
        val urlReputationReasons =
            mutableListOf<String>()

        urls.forEach { url ->

            val reputationIssues =

                UrlReputationAnalyzer
                    .analyzeUrl(url)

            reputationIssues.forEach {

                Log.d(
                    "RakshakX",
                    "URL Reputation Issue: $it"
                )
            }

            urlReputationReasons
                .addAll(reputationIssues)
        }

        // Threat correlation analysis
        val correlationReasons =

            ThreatCorrelationEngine
                .processUrls(urls)

        correlationReasons.forEach {

            Log.d(
                "RakshakX",
                "Threat Correlation: $it"
            )
        }

        // STEP 3 — Intent Analysis
        val suspiciousIntent =
            IntentAnalyzer.detectSuspiciousIntent(
                normalizedText
            )

        val suspiciousPhraseCount =
            IntentAnalyzer.countSuspiciousPhrases(
                normalizedText
            )

        // STEP 4 — Obfuscation Analysis
        val excessiveCaps =
            ObfuscationAnalyzer.hasExcessiveCaps(
                fullText
            )

        val symbolReplacement =
            ObfuscationAnalyzer.hasSymbolReplacement(
                fullText
            )

        val repeatedSymbols =
            ObfuscationAnalyzer.hasRepeatedSymbols(
                fullText
            )

        // STEP 5 — Financial/Urgency Checks
        val hasUrgency = listOf(

            "urgent",
            "immediately",
            "act now",
            "warning"

        ).any {

            normalizedText.contains(it, true)
        }

        val hasFinancialWords = listOf(

            "bank",
            "account",
            "payment",
            "verify"

        ).any {

            normalizedText.contains(it, true)
        }

        // STEP 6 — Attachment Analysis
        val dangerousAttachment =

            AttachmentAnalyzer
                .hasDangerousAttachment(
                    normalizedText
                )

        // STEP 7 — Build Feature Object
        val features = EmailFeatures(

            originalText = fullText,

            normalizedText = normalizedText,

            hasLink = hasLink,

            hasUrgency = hasUrgency,

            hasFinancialWords =
                hasFinancialWords,

            suspiciousIntent =
                suspiciousIntent,

            suspiciousPhraseCount =
                suspiciousPhraseCount,

            excessiveCaps =
                excessiveCaps,

            symbolReplacement =
                symbolReplacement,

            repeatedSymbols =
                repeatedSymbols,

            multipleLinks =
                multipleLinks,

            dangerousAttachment =
                dangerousAttachment
        )

        // STEP 8 — Risk Scoring
        val result =
            RiskEngine.calculateRisk(features)

        // STEP 9 — LOG EVERYTHING
        Log.d("RakshakX", "==============")
        Log.d("RakshakX", "EMAIL DETECTED")
        Log.d("RakshakX", "==============")

        Log.d(
            "RakshakX",
            "Original Text: $fullText"
        )

        Log.d(
            "RakshakX",
            "Normalized Text: $normalizedText"
        )

        Log.d(
            "RakshakX",
            "URLs: $urls"
        )

        Log.d(
            "RakshakX",
            "Has Link: $hasLink"
        )

        Log.d(
            "RakshakX",
            "Multiple Links: $multipleLinks"
        )

        Log.d(
            "RakshakX",
            "Suspicious Intent: $suspiciousIntent"
        )

        Log.d(
            "RakshakX",
            "Phrase Count: $suspiciousPhraseCount"
        )

        Log.d(
            "RakshakX",
            "Excessive Caps: $excessiveCaps"
        )

        Log.d(
            "RakshakX",
            "Symbol Replacement: $symbolReplacement"
        )

        Log.d(
            "RakshakX",
            "Repeated Symbols: $repeatedSymbols"
        )

        Log.d(
            "RakshakX",
            "Dangerous Attachment: $dangerousAttachment"
        )

        Log.d(
            "RakshakX",
            "Risk Score: ${result.score}"
        )

        Log.d(
            "RakshakX",
            "Risk Level: ${result.riskLevel}"
        )

        Log.d("RakshakX", "Reasons:")

        result.reasons.forEach {

            Log.d(
                "RakshakX",
                "- $it"
            )
        }

        // URL reputation reasons
        urlReputationReasons.forEach {

            Log.d(
                "RakshakX",
                "- URL Threat: $it"
            )
        }

        // Correlation reasons
        correlationReasons.forEach {

            Log.d(
                "RakshakX",
                "- Correlation Threat: $it"
            )
        }

        // Show warning notification
        if (result.riskLevel == "HIGH RISK") {

            WarningNotifier.showHighRiskWarning(

                context = this,

                title = fullText,

                reasons =

                    result.reasons +
                            urlReputationReasons +
                            correlationReasons
            )

            // Save threat into database
            CoroutineScope(Dispatchers.IO)
                .launch {

                    val database =

                        ThreatDatabase.getDatabase(
                            this@EmailNotificationListener
                        )

                    val threat = ThreatEntity(

                        title = title,

                        message = fullText,

                        riskScore = result.score,

                        riskLevel = result.riskLevel,

                        timestamp =
                            System.currentTimeMillis()
                    )

                    database
                        .threatDao()
                        .insertThreat(threat)

                    Log.d(
                        "RakshakX",
                        "Threat saved into database"
                    )
                }
        }
    }
}