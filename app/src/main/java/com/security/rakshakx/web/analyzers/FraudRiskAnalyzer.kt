package com.security.rakshakx.web.analyzers

import com.security.rakshakx.web.ai.AiThreatScorer
import com.security.rakshakx.web.models.BrowserSessionData
import com.security.rakshakx.web.models.FraudCategory
import com.security.rakshakx.web.models.FraudRiskResult
import com.security.rakshakx.web.models.ThreatAssessment
import com.security.rakshakx.web.models.ThreatLevel
import com.security.rakshakx.web.models.VpnTrafficData
import java.util.Locale

class FraudRiskAnalyzer(
    private val intelRepository: ThreatIntelRepository,
    private val scamLanguageAnalyzer: ScamLanguageAnalyzer,
    private val scoringEngine: ThreatScoringEngine,
    private val aiThreatScorer: AiThreatScorer?
) {
    fun analyze(
        session: BrowserSessionData,
        traffic: VpnTrafficData,
        domainAssessment: ThreatAssessment
    ): FraudRiskResult {
        val domain = traffic.dnsQuery.ifBlank { extractHost(session.url) }
        val reasons = domainAssessment.reasons.toMutableList()
        val visibleSignals = mutableListOf<String>()
        val categoryHints = mutableSetOf<FraudCategory>()

        val domainScore = when (domainAssessment.level) {
            ThreatLevel.CRITICAL -> 35
            ThreatLevel.HIGH -> 25
            ThreatLevel.MEDIUM -> 15
            ThreatLevel.LOW -> 5
        }

        val combinedText = buildString {
            append(session.pageTitle).append(' ').append(session.visibleText)
        }
        val languageResult = scamLanguageAnalyzer.analyze(combinedText)
        val languageScore = languageResult.score
        if (languageResult.matches.isNotEmpty()) {
            val matchLabel = languageResult.matches.joinToString(", ")
            reasons.add("Scam language: $matchLabel")
            visibleSignals.add("Language signals: $matchLabel")
        }

        var fieldScore = 0
        if (session.passwordFieldDetected) {
            fieldScore += 12
            reasons.add("Password field detected")
            categoryHints.add(FraudCategory.CREDENTIAL_THEFT)
        }
        if (session.otpFieldDetected) {
            fieldScore += 10
            reasons.add("OTP field detected")
            categoryHints.add(FraudCategory.CREDENTIAL_THEFT)
        }
        if (session.emailFieldDetected) {
            fieldScore += 8
            reasons.add("Email field detected")
            categoryHints.add(FraudCategory.CREDENTIAL_THEFT)
        }
        if (session.paymentFieldDetected) {
            fieldScore += 15
            reasons.add("Payment field detected")
            categoryHints.add(FraudCategory.PAYMENT_SCAM)
        }

        val redirectScore = when {
            traffic.redirectChain.size >= 5 -> 12
            traffic.redirectChain.size >= 3 -> 8
            else -> 0
        }
        if (redirectScore > 0) {
            reasons.add("Multiple redirects observed")
        }

        val visibleDomain = extractHost(session.url)
        val strongContext = languageScore >= 6 || fieldScore >= 10 || redirectScore >= 8 || domainScore >= 25
        val mismatchScore = if (visibleDomain.isNotBlank() && domain.isNotBlank() &&
            visibleDomain != domain && !domain.endsWith(visibleDomain) &&
            !intelRepository.isSafeSuffix(domain) && strongContext
        ) {
            reasons.add("Browser/network domain mismatch")
            categoryHints.add(FraudCategory.IMPERSONATION)
            12
        } else {
            0
        }

        val dnsScore = domainAssessment.reasons.count { it.contains("TLD") || it.contains("entropy") } * 4
        val tlsScore = if (traffic.sniHost.isNotBlank() &&
            traffic.sniHost.lowercase(Locale.US) != domain.lowercase(Locale.US)
        ) {
            reasons.add("TLS SNI mismatch")
            10
        } else {
            0
        }

        val paymentScore = if (session.paymentFieldDetected ||
            containsPaymentTerms(session.visibleText)
        ) {
            categoryHints.add(FraudCategory.PAYMENT_SCAM)
            8
        } else {
            0
        }

        val aiProbability = aiThreatScorer?.score(session, traffic, domainAssessment)
        val aiScore = ((aiProbability ?: 0f) * 40f).toInt().coerceIn(0, 40)
        if (aiProbability != null && aiProbability >= 0.6f) {
            reasons.add("AI fraud probability ${String.format(Locale.US, "%.2f", aiProbability)}")
            visibleSignals.add("AI risk score ${String.format(Locale.US, "%.0f", aiProbability * 100)}")
        }

        if (intelRepository.bankBrandKeywords().any { combinedText.lowercase().contains(it) }) {
            categoryHints.add(FraudCategory.BANKING_FRAUD)
        }

        val correlationData = buildString {
            append("visibleDomain=").append(visibleDomain)
            append("; vpnDomain=").append(domain)
        }

        val input = ThreatScoringEngine.FraudSignalInput(
            domain = domain.ifBlank { visibleDomain },
            domainScore = domainScore,
            languageScore = languageScore,
            fieldScore = fieldScore,
            redirectScore = redirectScore,
            mismatchScore = mismatchScore,
            dnsScore = dnsScore,
            tlsScore = tlsScore,
            paymentScore = paymentScore,
            aiScore = aiScore,
            reasons = reasons.distinct(),
            visibleSignals = visibleSignals.distinct(),
            correlationData = correlationData,
            categoryHints = categoryHints
        )

        return scoringEngine.score(input)
    }

    private fun extractHost(url: String): String {
        val trimmed = url.trim()
        val noScheme = trimmed.substringAfter("//", trimmed)
        return noScheme.substringBefore("/")
            .substringBefore(":")
            .lowercase(Locale.US)
    }

    private fun containsPaymentTerms(text: String): Boolean {
        val normalized = text.lowercase()
        return listOf("upi", "card", "cvv", "expiry", "debit", "credit", "payment").any {
            normalized.contains(it)
        }
    }
}
