package com.rakshakx.core.storage

import com.rakshakx.ai.inference.FraudInferenceEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class RiskScoreRepository(
    private val riskScoreDao: RiskScoreDao,
    private val fraudInferenceEngine: FraudInferenceEngine? = null
) {
    private val scamKeywords = listOf("otp", "urgent", "verify", "bank", "lottery", "prize", "kyc")
    private val modelWeight = 0.25f

    suspend fun onSmsEvent(phoneNumber: String, message: String?) {
        val existing = riskScoreDao.getByPhoneNumber(phoneNumber)
        val baseIncrement = 0.08f
        val keywordIncrement = if (containsScamKeyword(message)) 0.22f else 0f
        val modelScore = if (!message.isNullOrBlank() && fraudInferenceEngine != null) {
            fraudInferenceEngine.classifySms(message)
        } else {
            0.0f
        }
        val modelIncrement = modelScore * modelWeight
        val updatedScore = (
            (existing?.riskScore ?: 0f) +
                baseIncrement +
                keywordIncrement +
                modelIncrement
            ).coerceAtMost(1.0f)
        val snippet = message?.take(80)

        riskScoreDao.upsert(
            RiskScoreEntity(
                phoneNumber = phoneNumber,
                lastEventType = "SMS",
                lastMessageSnippet = snippet,
                riskScore = updatedScore,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun onCallEvent(phoneNumber: String) {
        val existing = riskScoreDao.getByPhoneNumber(phoneNumber)
        val updatedScore = ((existing?.riskScore ?: 0f) + 0.05f).coerceAtMost(1.0f)

        riskScoreDao.upsert(
            RiskScoreEntity(
                phoneNumber = phoneNumber,
                lastEventType = "CALL",
                lastMessageSnippet = existing?.lastMessageSnippet,
                riskScore = updatedScore,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    fun observeTopRiskyContacts(limit: Int = 10): Flow<List<RiskScoreEntity>> {
        return riskScoreDao.observeTopRiskContacts(limit)
    }

    fun observeRecentModelScores(): Flow<List<Float>> {
        return fraudInferenceEngine?.observeRecentScores() ?: flowOf(emptyList())
    }

    fun observeModelLoaded(): Flow<Boolean> {
        return fraudInferenceEngine?.observeModelLoaded() ?: flowOf(false)
    }

    suspend fun getTopRiskyContacts(limit: Int = 10): List<RiskScoreEntity> {
        return riskScoreDao.getTopRiskContacts(limit)
    }

    private fun containsScamKeyword(message: String?): Boolean {
        if (message.isNullOrBlank()) return false
        val lowered = message.lowercase()
        return scamKeywords.any { lowered.contains(it) }
    }
}
