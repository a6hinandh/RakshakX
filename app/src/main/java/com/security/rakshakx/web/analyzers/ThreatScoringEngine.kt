package com.security.rakshakx.web.analyzers

import com.security.rakshakx.web.models.FraudAction
import com.security.rakshakx.web.models.FraudCategory
import com.security.rakshakx.web.models.FraudRiskResult
import com.security.rakshakx.web.models.ThreatLevel

class ThreatScoringEngine(private val intelRepository: ThreatIntelRepository) {
    data class FraudSignalInput(
        val domain: String,
        val domainScore: Int,
        val languageScore: Int,
        val fieldScore: Int,
        val redirectScore: Int,
        val mismatchScore: Int,
        val dnsScore: Int,
        val tlsScore: Int,
        val paymentScore: Int,
        val aiScore: Int,
        val reasons: List<String>,
        val visibleSignals: List<String>,
        val correlationData: String,
        val categoryHints: Set<FraudCategory>
    )

    fun score(input: FraudSignalInput): FraudRiskResult {
        val total = (input.domainScore + input.languageScore + input.fieldScore + input.redirectScore +
            input.mismatchScore + input.dnsScore + input.tlsScore + input.paymentScore + input.aiScore)
            .coerceIn(0, 100)

        val severity = pickSeverity(total)
        val category = pickCategory(input, total)
        val action = pickAction(input.domain, total)
        val blockReason = if (action == FraudAction.BLOCK) {
            input.reasons.firstOrNull().orEmpty()
        } else {
            ""
        }

        return FraudRiskResult(
            timestamp = System.currentTimeMillis(),
            domain = input.domain,
            score = total,
            severity = severity,
            category = category,
            action = action,
            reasons = input.reasons,
            visibleSignals = input.visibleSignals,
            correlationData = input.correlationData,
            blockReason = blockReason
        )
    }

    private fun pickAction(domain: String, score: Int): FraudAction {
        if (intelRepository.isAllowListed(domain)) {
            return FraudAction.ALLOW
        }
        return when {
            score >= 80 -> FraudAction.BLOCK
            score >= 55 -> FraudAction.WARN
            else -> FraudAction.ALLOW
        }
    }

    private fun pickCategory(input: FraudSignalInput, score: Int): FraudCategory {
        val impersonationAllowed = input.mismatchScore >= 10 &&
            (input.languageScore >= 6 || input.fieldScore >= 10 || input.domainScore >= 25)
        return when {
            input.categoryHints.contains(FraudCategory.BANKING_FRAUD) -> FraudCategory.BANKING_FRAUD
            input.categoryHints.contains(FraudCategory.PAYMENT_SCAM) -> FraudCategory.PAYMENT_SCAM
            input.categoryHints.contains(FraudCategory.CREDENTIAL_THEFT) -> FraudCategory.CREDENTIAL_THEFT
            impersonationAllowed && input.categoryHints.contains(FraudCategory.IMPERSONATION) -> FraudCategory.IMPERSONATION
            score >= 60 -> FraudCategory.PHISHING
            else -> FraudCategory.SUSPICIOUS
        }
    }

    private fun pickSeverity(score: Int): ThreatLevel {
        return when {
            score >= 80 -> ThreatLevel.CRITICAL
            score >= 60 -> ThreatLevel.HIGH
            score >= 35 -> ThreatLevel.MEDIUM
            else -> ThreatLevel.LOW
        }
    }
}
