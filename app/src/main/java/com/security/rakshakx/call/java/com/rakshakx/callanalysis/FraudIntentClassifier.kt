package com.rakshakx.callanalysis

import android.util.Log
import java.util.LinkedList

/**
 * Data class representing fraud analysis explanation.
 *
 * @param score Normalized fraud risk score in range [0.0f, 1.0f]. Higher = more suspicious.
 * @param reasons Human-readable list of detected fraud indicators (deduplicated).
 * @param matchedKeywords Exact keywords/phrases that triggered scoring rules (deduplicated).
 */
data class FraudExplanation(
    val score: Float,
    val reasons: List<String>,
    val matchedKeywords: List<String>
)

/**
 * Tier-0 fraud detection engine using rule-based keyword matching and weighted scoring.
 *
 * This is a deterministic, on-device analyzer that:
 * - Normalizes call transcripts (lowercase, remove punctuation, collapse spaces)
 * - Matches keywords from predefined fraud categories
 * - Applies weighted scoring rules (individual + co-occurrence bonuses)
 * - Applies negative signals (normal conversation dampening, short transcripts)
 * - Returns a fraud risk score between 0.0 and 1.0 with explanations
 *
 * No networking, no reflection, no external dependencies beyond Kotlin stdlib.
 * Fast enough for on-device use even on low-end phones.
 */
class FraudIntentClassifier {

    companion object {
        private const val TAG = "FraudIntentClassifier"

        // Keyword groups representing different fraud tactics
        private val BANK_TERMS = listOf(
            "bank", "account", "kyc", "credit card", "debit card",
            "savings", "current account", "banking", "bank account"
        )

        private val OTP_TERMS = listOf(
            "otp", "one time password", "verification code", "confirm code",
            "authenticate", "2fa", "two factor", "otp code"
        )

        private val SENSITIVE_AUTH_TERMS = listOf(
            "cvv", "pin", "password", "login details", "login credentials",
            "security code", "expiry date", "card number", "account password"
        )

        private val THREAT_TERMS = listOf(
            "block your account", "suspend your account", "freeze your account",
            "legal case", "police complaint", "fine", "penalty",
            "block account", "suspend account", "freeze account",
            "arrested", "arrest you", "police action", "legal action"
        )

        private val URGENCY_TERMS = listOf(
            "immediately", "right now", "within 5 minutes", "urgent",
            "last warning", "final notice", "asap", "hurry", "quickly",
            "don't delay", "no time", "now now", "before closing"
        )

        private val LOTTERY_TERMS = listOf(
            "lottery", "jackpot", "prize", "bumper draw", "lucky winner",
            "congratulations", "win", "won", "prize money", "claim prize"
        )

        private val PAYMENT_TERMS = listOf(
            "send money", "transfer", "pay now", "scan the qr", "upi",
            "google pay", "phonepe", "paytm", "payment", "remittance",
            "wire transfer", "bank transfer", "send funds"
        )

        private val NORMAL_CONVERSATION_HINTS = listOf(
            "lunch", "dinner", "movie", "college", "class",
            "assignment", "office meeting", "birthday", "vacation",
            "weather", "friend", "family", "how are you", "goodbye",
            "see you", "thanks", "thank you", "bye", "hello"
        )

        // Scoring weights (sum can exceed 1.0 before clamping)
        private const val WEIGHT_BANK = 0.15f
        private const val WEIGHT_OTP = 0.20f
        private const val WEIGHT_AUTH = 0.20f
        private const val WEIGHT_THREAT = 0.20f
        private const val WEIGHT_URGENCY = 0.15f
        private const val WEIGHT_LOTTERY = 0.15f
        private const val WEIGHT_PAYMENT = 0.15f
        private const val WEIGHT_NORMAL_DAMPENING = -0.15f

        // Co-occurrence bonuses
        private const val BONUS_BANK_OTP = 0.20f
        private const val BONUS_THREAT_URGENCY = 0.20f
        private const val BONUS_PAYMENT_LOTTERY = 0.15f

        // Short transcript dampening factor
        private const val SHORT_TRANSCRIPT_THRESHOLD = 5
        private const val SHORT_TRANSCRIPT_FACTOR = 0.5f

        // Padding for exact word boundary matching
        private const val WORD_BOUNDARY = " "
    }

    /**
     * Main entry point for fraud analysis.
     *
     * @param transcript Raw call transcript (may be null or empty).
     * @return FraudExplanation with score, reasons, and matched keywords.
     */
    fun compute(transcript: String?): FraudExplanation {
        if (transcript.isNullOrBlank()) {
            return FraudExplanation(score = 0.0f, reasons = emptyList(), matchedKeywords = emptyList())
        }

        // Normalize transcript
        val normalized = normalizeTranscript(transcript)
        val tokens = tokenize(normalized)

        Log.d(TAG, "Analyzing ${tokens.size}-word transcript")

        var score = 0.0f
        val reasons = LinkedList<String>()
        val matchedKeywords = LinkedList<String>()

        // Check each rule group
        val hasBankTerms = checkAndAddMatches(normalized, tokens, BANK_TERMS, matchedKeywords)
        val hasOtpTerms = checkAndAddMatches(normalized, tokens, OTP_TERMS, matchedKeywords)
        val hasAuthTerms = checkAndAddMatches(normalized, tokens, SENSITIVE_AUTH_TERMS, matchedKeywords)
        val hasThreatTerms = checkAndAddMatches(normalized, tokens, THREAT_TERMS, matchedKeywords)
        val hasUrgencyTerms = checkAndAddMatches(normalized, tokens, URGENCY_TERMS, matchedKeywords)
        val hasLotteryTerms = checkAndAddMatches(normalized, tokens, LOTTERY_TERMS, matchedKeywords)
        val hasPaymentTerms = checkAndAddMatches(normalized, tokens, PAYMENT_TERMS, matchedKeywords)
        val hasNormalTerms = checkAndAddMatches(normalized, tokens, NORMAL_CONVERSATION_HINTS, matchedKeywords)

        // Apply individual rule weights
        if (hasBankTerms) {
            score += WEIGHT_BANK
            reasons.add("Detected banking-related language")
        }

        if (hasOtpTerms) {
            score += WEIGHT_OTP
            reasons.add("Detected OTP / verification code request")
        }

        if (hasAuthTerms) {
            score += WEIGHT_AUTH
            reasons.add("Detected request for sensitive authentication details (PIN/CVV/password)")
        }

        if (hasThreatTerms) {
            score += WEIGHT_THREAT
            reasons.add("Detected threat of account blocking / legal action")
        }

        if (hasUrgencyTerms) {
            score += WEIGHT_URGENCY
            reasons.add("Detected strong urgency / time pressure")
        }

        if (hasLotteryTerms) {
            score += WEIGHT_LOTTERY
            reasons.add("Detected lottery / prize related language")
        }

        if (hasPaymentTerms) {
            score += WEIGHT_PAYMENT
            reasons.add("Detected payment / money transfer request")
        }

        // Apply co-occurrence bonuses (high-risk combinations)
        if (hasBankTerms && hasOtpTerms) {
            score += BONUS_BANK_OTP
            reasons.add("Banking + OTP combination is highly suspicious")
        }

        if (hasThreatTerms && hasUrgencyTerms) {
            score += BONUS_THREAT_URGENCY
            reasons.add("Threat + urgency pattern detected")
        }

        if (hasPaymentTerms && hasLotteryTerms) {
            score += BONUS_PAYMENT_LOTTERY
            reasons.add("Lottery + payment request is suspicious")
        }

        // Apply negative signals (dampening)
        if (hasNormalTerms) {
            score += WEIGHT_NORMAL_DAMPENING
            reasons.add("Detected normal conversation topics (may be benign call)")
        }

        // Dampen score for very short transcripts to avoid false positives
        if (tokens.size < SHORT_TRANSCRIPT_THRESHOLD) {
            score *= SHORT_TRANSCRIPT_FACTOR
            if (score > 0.0f) {
                reasons.add("Short transcript: reduced confidence")
            }
        }

        // Clamp score to valid range
        score = score.coerceIn(0.0f, 1.0f)

        // Remove duplicates and prepare result
        val uniqueReasons = reasons.distinct()
        val uniqueKeywords = matchedKeywords.distinct()

        Log.d(TAG, "Fraud score: $score | Reasons: ${uniqueReasons.size} | Keywords matched: ${uniqueKeywords.size}")
        for (keyword in uniqueKeywords.take(5)) {
            Log.d(TAG, "  Matched: $keyword")
        }

        return FraudExplanation(
            score = score,
            reasons = uniqueReasons,
            matchedKeywords = uniqueKeywords
        )
    }

    /**
     * Compute hybrid fraud score combining ML and rules
     */
    fun computeHybridScore(transcript: String, mlResult: MLResult): Pair<Float, String> {
        // Get rule-based score using existing compute() method
        val rulesExplanation = compute(transcript)
        val rulesScore = rulesExplanation.score
        val mlScore = mlResult.score

        // Hybrid combination: Use weights from RiskConfig
        val hybridScore = (RiskConfig.ML_WEIGHT * mlScore) + (RiskConfig.RULES_WEIGHT * rulesScore)

        val mlScorePct = (mlScore * 100).toInt()
        val rulesScorePct = (rulesScore * 100).toInt()
        val finalScorePct = (hybridScore * 100).toInt()

        val riskLevel = RiskConfig.getRiskLevel(hybridScore)

        // Generate explanation
        val explanation = buildString {
            appendLine("🤖 Hybrid AI Analysis:")
            appendLine()
            appendLine("ML Score: $mlScorePct% (label: ${mlResult.label})")
            appendLine("Rules Score: $rulesScorePct%")
            appendLine("Final Hybrid Score: $finalScorePct%")
            appendLine()
            appendLine("ML Reasons:")
            mlResult.reasons.forEach { reason ->
                appendLine("• $reason")
            }
            appendLine()
            appendLine("Rules-based Detected Patterns:")
            if (rulesExplanation.reasons.isEmpty()) {
                appendLine("• (none)")
            } else {
                rulesExplanation.reasons.forEach { reason ->
                    appendLine("• $reason")
                }
            }
            appendLine()
            appendLine("Matched Keywords:")
            if (rulesExplanation.matchedKeywords.isEmpty()) {
                appendLine("• (none)")
            } else {
                appendLine("• ${rulesExplanation.matchedKeywords.joinToString(", ")}")
            }
            appendLine()
            appendLine("Risk Assessment: $riskLevel")
        }

        Log.d(TAG, "Hybrid: ML=$mlScore, Rules=$rulesScore, Final=$hybridScore")

        return Pair(hybridScore, explanation)
    }

    /**
     * Normalize transcript for consistent matching.
     * - Convert to lowercase
     * - Remove common punctuation
     * - Collapse multiple spaces to single space
     * - Strip leading/trailing whitespace
     */
    private fun normalizeTranscript(transcript: String): String {
        return transcript
            .lowercase()
            .replace(Regex("[.,!?;:'\"()\\[\\]{}]"), "")
            .replace(Regex(" +"), " ")
            .trim()
    }

    /**
     * Split normalized transcript into tokens (words).
     */
    private fun tokenize(normalized: String): List<String> {
        return normalized.split(Regex("\\s+")).filter { it.isNotEmpty() }
    }

    /**
     * Check if any phrase from the given list appears in the normalized transcript.
     * Adds matched phrases to the matchedKeywords list.
     * Returns true if at least one match found.
     */
    private fun checkAndAddMatches(
        normalized: String,
        tokens: List<String>,
        phrases: List<String>,
        matchedKeywords: MutableList<String>
    ): Boolean {
        var found = false
        for (phrase in phrases) {
            if (matchesPhrase(normalized, tokens, phrase)) {
                matchedKeywords.add(phrase)
                found = true
            }
        }
        return found
    }

    /**
     * Check if a phrase matches in the transcript.
     * For single-word phrases: check token membership
     * For multi-word phrases: use contains check with word boundaries
     */
    private fun matchesPhrase(normalized: String, tokens: List<String>, phrase: String): Boolean {
        return if (phrase.contains(WORD_BOUNDARY)) {
            // Multi-word phrase: use contains with padding for word boundaries
            normalized.contains(WORD_BOUNDARY + phrase + WORD_BOUNDARY) ||
                    normalized.startsWith(phrase + WORD_BOUNDARY) ||
                    normalized.endsWith(WORD_BOUNDARY + phrase)
        } else {
            // Single word: check token membership
            tokens.contains(phrase)
        }
    }

    /**
     * Legacy method for backward compatibility with existing code.
     * Maps to the new compute() method.
     */
    fun computeRiskScore(transcript: String?): Float {
        return compute(transcript).score
    }
}