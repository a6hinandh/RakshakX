package com.security.rakshakx.email.scoring

import com.security.rakshakx.email.model.EmailFeatures
import com.security.rakshakx.email.model.RiskResult

object RiskEngine {

    fun calculateRisk(features: EmailFeatures): RiskResult {

        var score = 0

        val reasons = mutableListOf<String>()

        if (features.hasLink) {
            score += 3
            reasons.add("Contains link")
        }

        if (features.multipleLinks) {
            score += 2
            reasons.add("Contains multiple links")
        }

        if (features.hasUrgency) {
            score += 2
            reasons.add("Urgency language detected")
        }

        if (features.hasFinancialWords) {
            score += 2
            reasons.add("Financial keywords detected")
        }

        if (features.suspiciousIntent) {
            score += 3
            reasons.add("Suspicious phishing intent")
        }

        if (features.excessiveCaps) {
            score += 1
            reasons.add("Excessive capitalization")
        }

        if (features.symbolReplacement) {
            score += 2
            reasons.add("Obfuscated text detected")
        }

        if (features.repeatedSymbols) {
            score += 1
            reasons.add("Repeated symbols detected")
        }

        if (features.dangerousAttachment) {
            score += 5
            reasons.add("Dangerous attachment detected")
        }

        val riskLevel = when {

            score >= 10 -> "HIGH RISK"

            score >= 5 -> "MEDIUM RISK"

            else -> "SAFE"
        }

        return RiskResult(
            score,
            riskLevel,
            reasons
        )
    }
}