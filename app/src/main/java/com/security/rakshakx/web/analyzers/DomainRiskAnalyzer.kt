package com.security.rakshakx.web.analyzers

import com.security.rakshakx.web.models.ThreatAction
import com.security.rakshakx.web.models.ThreatAssessment
import com.security.rakshakx.web.models.ThreatLevel
import java.util.Locale
import kotlin.math.ln

class DomainRiskAnalyzer(private val intelRepository: ThreatIntelRepository) {
    private val urgencyKeywords = listOf(
        "verify", "immediately", "urgent", "suspended", "security alert", "action required",
        "account locked", "payment failed", "claim refund"
    )
    private val supportScamKeywords = listOf("support", "call", "helpline", "toll free", "customer service")
    private val paymentKeywords = listOf("upi", "card", "cvv", "expiry", "debit", "credit", "payment")

    fun assess(
        domain: String,
        redirectCount: Int,
        tlsMismatch: Boolean,
        dnsFlags: List<String>,
        url: String? = null,
        visibleText: String? = null,
        passwordField: Boolean = false,
        otpField: Boolean = false,
        emailField: Boolean = false,
        paymentField: Boolean = false,
        domainAgeDays: Int? = null
    ): ThreatAssessment {
        val reasons = mutableListOf<String>()
        val normalized = domain.lowercase(Locale.US)
        val tld = normalized.substringAfterLast('.', "")

        var score = 0
        if (tld in intelRepository.riskyTlds()) {
            score += 25
            reasons.add("High-risk TLD: .$tld")
        }

        if (normalized.count { it == '.' } >= 4) {
            score += 10
            reasons.add("Excessive subdomains")
        }

        if (intelRepository.brandKeywords().any { normalized.contains(it) }) {
            score += 12
            reasons.add("Brand keyword present")
        }

        if (intelRepository.bankBrandKeywords().any { normalized.contains(it) } &&
            !intelRepository.isAllowListed(normalized)
        ) {
            score += 15
            reasons.add("Possible brand impersonation")
        }

        if (normalized.contains("xn--")) {
            score += 12
            reasons.add("Punycode encoded domain")
        }

        if (containsUnicodeHomograph(normalized)) {
            score += 18
            reasons.add("Unicode homograph risk")
        }

        if (intelRepository.isShortener(normalized)) {
            score += 10
            reasons.add("URL shortener detected")
        }

        if (entropy(normalized) > 4.2) {
            score += 15
            reasons.add("High entropy domain")
        }

        if (domainAgeDays != null && domainAgeDays <= 30) {
            score += 12
            reasons.add("Newly registered domain")
        }

        if (redirectCount >= 3) {
            score += 15
            reasons.add("Multiple redirects")
        }

        if (tlsMismatch) {
            score += 20
            reasons.add("TLS hostname mismatch")
        }

        if (url?.startsWith("http://") == true &&
            intelRepository.brandKeywords().any { url.contains(it, ignoreCase = true) }
        ) {
            score += 12
            reasons.add("Suspicious HTTPS downgrade")
        }

        val formSignals = listOf(passwordField, otpField, emailField, paymentField).count { it }
        if (formSignals >= 2) {
            score += 12
            reasons.add("Excessive form harvesting")
        }

        val text = visibleText?.lowercase(Locale.US).orEmpty()
        if (text.isNotBlank()) {
            val urgencyHits = urgencyKeywords.count { text.contains(it) }
            if (urgencyHits > 0) {
                score += 6 + (urgencyHits * 2)
                reasons.add("Scam urgency language")
            }

            if (supportScamKeywords.any { text.contains(it) }) {
                score += 10
                reasons.add("Customer support scam pattern")
            }

            if (paymentField || paymentKeywords.any { text.contains(it) }) {
                score += 10
                reasons.add("Payment phishing signals")
            }
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

    private fun containsUnicodeHomograph(domain: String): Boolean {
        val scripts = domain.filter { it.code > 127 }.map { Character.UnicodeScript.of(it.code) }.toSet()
        if (scripts.isEmpty()) return false
        val asciiLetters = domain.any { it in 'a'..'z' || it in 'A'..'Z' }
        return asciiLetters && scripts.size >= 1
    }
}
