package com.security.rakshakx.call.callanalysis.ml

class DummyFraudTextModel : FraudTextModel {

    private val fraudVocabulary = setOf(
        "otp", "password", "pin", "cvv", "card", "number", "account",
        "verify", "confirm", "update", "suspended", "blocked", "expire"
    )

    private val urgencyVocabulary = setOf(
        "urgent", "immediately", "now", "hurry", "quick", "deadline",
        "expire", "today", "asap", "emergency"
    )

    private val financialVocabulary = setOf(
        "bank", "credit", "debit", "payment", "transfer", "refund",
        "money", "rupees", "dollars", "transaction", "balance"
    )

    private val legitimateVocabulary = setOf(
        "hello", "hi", "thanks", "please", "help", "question",
        "meeting", "lunch", "friend", "family", "tomorrow", "later"
    )

    override fun predictFraud(text: String): MlFraudResult {
        if (text.isBlank()) {
            return MlFraudResult(
                probabilityFraud = 0.5f,
                label = "unknown",
                reasons = listOf("Empty transcript")
            )
        }

        val lower = text.lowercase()
        val tokens = tokenize(lower)

        val embeddings = generateEmbeddings(tokens)
        val baseScore = inferFraudScore(embeddings, lower)
        val reasons = buildHeuristicReasons(lower)

        return MlFraudResult(
            probabilityFraud = baseScore.coerceIn(0f, 1f),
            label = "unknown",
            reasons = reasons.ifEmpty { listOf("No strong ML-based fraud indicators detected.") }
        )
    }

    private fun tokenize(text: String): List<String> {
        return text
            .replace(Regex("[^a-z0-9\\s]"), "")
            .split(Regex("\\s+"))
            .filter { it.isNotEmpty() }
            .take(MAX_SEQUENCE_LENGTH)
    }

    private fun generateEmbeddings(tokens: List<String>): FloatArray {
        val embeddings = FloatArray(EMBEDDING_DIM) { 0f }

        tokens.forEachIndexed { index, token ->
            val weight = 1.0f / (index + 1)

            when {
                token in fraudVocabulary -> embeddings[0] += weight * 2.0f
                token in urgencyVocabulary -> embeddings[1] += weight * 1.5f
                token in financialVocabulary -> embeddings[2] += weight * 1.8f
                token in legitimateVocabulary -> embeddings[3] -= weight * 1.2f
            }
        }

        val norm = kotlin.math.sqrt(embeddings.sumOf { (it * it).toDouble() }.toFloat())
        if (norm > 0) {
            for (i in embeddings.indices) {
                embeddings[i] /= norm
            }
        }

        return embeddings
    }

    private fun inferFraudScore(embeddings: FloatArray, transcript: String): Float {
        var score = 0.5f

        score += embeddings[0].coerceIn(-1f, 1f) * 0.3f
        score += embeddings[1].coerceIn(-1f, 1f) * 0.2f
        score += embeddings[2].coerceIn(-1f, 1f) * 0.25f
        score += embeddings[3].coerceIn(-1f, 1f) * 0.15f

        score += detectPhishingPatterns(transcript)
        score += detectSocialEngineering(transcript)

        return score
    }

    private fun buildHeuristicReasons(text: String): List<String> {
        val reasons = mutableListOf<String>()

        val phishingScore = detectPhishingPatterns(text)
        if (phishingScore > 0.3f) {
            reasons.add("Detected phishing-like patterns (links or payment handles).")
        }

        val socialEngScore = detectSocialEngineering(text)
        if (socialEngScore > 0.3f) {
            reasons.add("Detected social engineering cues (urgency, fear, authority).")
        }

        if (fraudVocabulary.any { text.contains(it) }) {
            reasons.add("Contains known fraud-associated keywords.")
        }
        if (urgencyVocabulary.any { text.contains(it) }) {
            reasons.add("High-pressure or urgent language detected.")
        }
        if (financialVocabulary.any { text.contains(it) }) {
            reasons.add("Discusses sensitive financial or banking topics.")
        }

        return reasons.distinct()
    }

    private fun detectPhishingPatterns(text: String): Float {
        var boost = 0f

        if (text.contains(Regex("(password|pin|otp|cvv).*?(share|give|tell|send)"))) {
            boost += 0.15f
        }
        if (text.contains(Regex("(bank|police|government).*?(urgent|immediately|now|suspended)"))) {
            boost += 0.12f
        }
        if (text.contains(Regex("(won|prize|lottery).*?(claim|pay|fee|tax)"))) {
            boost += 0.10f
        }
        if (text.contains(Regex("(arrest|legal action|suspended).*?(unless|pay|send)"))) {
            boost += 0.13f
        }

        return boost
    }

    private fun detectSocialEngineering(text: String): Float {
        var boost = 0f

        val timeWords = listOf("immediately", "urgent", "now", "today", "deadline", "expire")
        val timeCount = timeWords.count { text.contains(it) }
        boost += (timeCount * 0.03f).coerceAtMost(0.09f)

        val authorityWords = listOf("officer", "manager", "director", "official", "department")
        val authCount = authorityWords.count { text.contains(it) }
        boost += (authCount * 0.04f).coerceAtMost(0.08f)

        val fearWords = listOf("arrest", "legal", "lawsuit", "blocked", "suspended", "terminated")
        val fearCount = fearWords.count { text.contains(it) }
        boost += (fearCount * 0.04f).coerceAtMost(0.10f)

        return boost
    }

    companion object {
        private const val MAX_SEQUENCE_LENGTH = 128
        private const val EMBEDDING_DIM = 64
    }
}

