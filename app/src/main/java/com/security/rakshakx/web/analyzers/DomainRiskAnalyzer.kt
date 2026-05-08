package com.security.rakshakx.web.analyzers

import com.security.rakshakx.web.models.ThreatAction
import com.security.rakshakx.web.models.ThreatAssessment
import com.security.rakshakx.web.models.ThreatLevel
import kotlin.math.ln

class DomainRiskAnalyzer {
    private val riskyTlds = setOf("zip", "xyz", "top", "gq", "work", "click", "help")
    private val brandKeywords = listOf("bank", "secure", "login", "verify", "account", "pay")

    fun assess(domain: String, redirectCount: Int, tlsMismatch: Boolean, dnsFlags: List<String>): ThreatAssessment {
        val reasons = mutableListOf<String>()
        val normalized = domain.lowercase()
        val tld = normalized.substringAfterLast('.', "")

        var score = 0
        if (tld in riskyTlds) {
            score += 25
            reasons.add("High-risk TLD: .$tld")
        }

        if (normalized.count { it == '.' } >= 4) {
            score += 10
            reasons.add("Excessive subdomains")
        }

        if (brandKeywords.any { normalized.contains(it) }) {
            score += 12
            reasons.add("Brand keyword present")
        }

        if (entropy(normalized) > 4.2) {
            score += 15
            reasons.add("High entropy domain")
        }

        if (redirectCount >= 3) {
            score += 15
            reasons.add("Multiple redirects")
        }

        if (tlsMismatch) {
            score += 20
            reasons.add("TLS hostname mismatch")
        }

        if (dnsFlags.isNotEmpty()) {
            score += 8
            reasons.addAll(dnsFlags)
        }

        val level = when {
            score >= 70 -> ThreatLevel.CRITICAL
            score >= 50 -> ThreatLevel.HIGH
            score >= 25 -> ThreatLevel.MEDIUM
            else -> ThreatLevel.LOW
        }

        val action = if (level >= ThreatLevel.HIGH) ThreatAction.BLOCK else ThreatAction.ALLOW

        return ThreatAssessment(
            timestamp = System.currentTimeMillis(),
            domain = normalized,
            level = level,
            action = action,
            reasons = reasons
        )
    }

    private fun entropy(input: String): Double {
        if (input.isBlank()) {
            return 0.0
        }

        val counts = mutableMapOf<Char, Int>()
        input.forEach { counts[it] = (counts[it] ?: 0) + 1 }
        val length = input.length.toDouble()

        return counts.values.fold(0.0) { acc, count ->
            val p = count / length
            acc - p * ln(p)
        }
    }
}
