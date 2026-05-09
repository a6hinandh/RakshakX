package com.security.rakshakx.call.core.orchestrator

import com.security.rakshakx.call.ai.inference.FraudInferenceEngine
import com.security.rakshakx.call.core.storage.RiskScoreEntity
import com.security.rakshakx.call.core.storage.RiskScoreRepository
import com.security.rakshakx.call.callanalysis.RiskConfig
import kotlinx.coroutines.flow.Flow

class RakshakOrchestrator(
    private val repository: RiskScoreRepository,
    private val fraudInferenceEngine: FraudInferenceEngine? = null
) : Orchestrator {
    private val scamKeywords = listOf("otp", "urgent", "verify", "bank", "lottery", "prize", "kyc")
    private val modelWeight = 0.25f

    override suspend fun start() {
        // AI handle is lazily loaded at first classification request.
        fraudInferenceEngine?.isModelLoaded()
    }

    override suspend fun stop() {
        // No-op for minimal local pipeline.
    }

    override suspend fun onEvent(event: SecurityEvent) {
        when (event.type) {
            EventType.SMS -> handleSmsEvent(
                phoneNumber = event.source,
                message = event.metadata["message"]
            )
            EventType.CALL -> handleCallEvent(phoneNumber = event.source)
            EventType.APP_SIGNAL -> Unit
        }
    }

    override suspend fun handleSmsEvent(phoneNumber: String, message: String?) {
        repository.onSmsEvent(phoneNumber, message)
    }

    override suspend fun handleCallEvent(phoneNumber: String) {
        repository.onCallEvent(phoneNumber)
    }

    override fun observeTopRiskyContacts(): Flow<List<RiskScoreEntity>> {
        return repository.observeTopRiskyContacts()
    }

    override fun observeRecentModelScores(): Flow<List<Float>> {
        return repository.observeRecentModelScores()
    }

    override fun observeModelLoaded(): Flow<Boolean> {
        return repository.observeModelLoaded()
    }

    override suspend fun scanSmsDemo(message: String): DemoScanResult {
        val normalized = message.trim()
        if (normalized.isBlank()) {
            return DemoScanResult(0f, "Empty SMS text.")
        }

        val keywordHits = scamKeywords.count { normalized.lowercase().contains(it) }
        val base = 0.08f
        val keywordIncrement = (keywordHits * 0.11f).coerceAtMost(0.33f)
        val modelScore = fraudInferenceEngine?.classifySms(normalized) ?: 0f
        val score = (base + keywordIncrement + (modelScore * modelWeight)).coerceIn(0f, 1f)
        val label = riskLabel(score)
        val explanation = buildString {
            append("Risk: ${"%.2f".format(score)} ($label). ")
            if (keywordHits > 0) append("Keyword matches: $keywordHits. ") else append("No high-risk keywords. ")
            append("AI score: ${"%.2f".format(modelScore)}.")
        }
        return DemoScanResult(score, explanation)
    }

    override suspend fun scanCallDemo(phoneNumber: String): DemoScanResult {
        val normalized = phoneNumber.filter { !it.isWhitespace() }
        if (normalized.isBlank()) {
            return DemoScanResult(0f, "Empty phone number.")
        }

        var score = 0.05f
        val reasons = mutableListOf<String>()
        if (normalized.startsWith("+") || normalized.length >= 12) {
            score += 0.15f
            reasons += "unusual/long caller format"
        }
        if (normalized.any { !it.isDigit() && it != '+' }) {
            score += 0.15f
            reasons += "non-standard characters"
        }
        if (normalized.takeLast(6).all { it == normalized.lastOrNull() }) {
            score += 0.2f
            reasons += "repeating digits pattern"
        }
        val finalScore = score.coerceIn(0f, 1f)
        val label = riskLabel(finalScore)
        val reasonText = if (reasons.isEmpty()) "No suspicious call patterns detected." else reasons.joinToString()
        return DemoScanResult(finalScore, "Risk: ${"%.2f".format(finalScore)} ($label). $reasonText.")
    }

    override suspend fun scanLinkDemo(url: String): DemoScanResult {
        val normalized = url.trim().lowercase()
        if (normalized.isBlank()) {
            return DemoScanResult(0f, "Empty URL.")
        }

        var score = 0.05f
        val reasons = mutableListOf<String>()
        val suspiciousTerms = listOf("verify", "gift", "reward", "bank", "kyc", "otp", "login")
        if (Regex("""https?://\d{1,3}(\.\d{1,3}){3}""").containsMatchIn(normalized)) {
            score += 0.35f
            reasons += "IP-address based URL"
        }
        if (suspiciousTerms.any { normalized.contains(it) }) {
            score += 0.25f
            reasons += "phishing-like keywords"
        }
        if (normalized.contains("@") || normalized.count { it == '-' } >= 3) {
            score += 0.15f
            reasons += "obfuscation pattern"
        }
        if (listOf(".xyz", ".top", ".click", ".shop").any { normalized.contains(it) }) {
            score += 0.2f
            reasons += "high-risk TLD"
        }
        val finalScore = score.coerceIn(0f, 1f)
        val label = riskLabel(finalScore)
        val reasonText = if (reasons.isEmpty()) "No strong suspicious link markers detected." else reasons.joinToString()
        return DemoScanResult(finalScore, "Risk: ${"%.2f".format(finalScore)} ($label). $reasonText.")
    }

    private fun riskLabel(score: Float): String {
        return RiskConfig.getRiskLevel(score)
    }
}


