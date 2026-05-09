package com.rakshakx.core.orchestrator

import com.rakshakx.core.storage.RiskScoreEntity
import kotlinx.coroutines.flow.Flow

interface Orchestrator {
    suspend fun start()
    suspend fun stop()
    suspend fun onEvent(event: SecurityEvent)
    suspend fun handleSmsEvent(phoneNumber: String, message: String?)
    suspend fun handleCallEvent(phoneNumber: String)
    fun observeTopRiskyContacts(): Flow<List<RiskScoreEntity>>
    fun observeRecentModelScores(): Flow<List<Float>>
    fun observeModelLoaded(): Flow<Boolean>
    suspend fun scanSmsDemo(message: String): DemoScanResult
    suspend fun scanCallDemo(phoneNumber: String): DemoScanResult
    suspend fun scanLinkDemo(url: String): DemoScanResult
}

data class DemoScanResult(
    val score: Float,
    val explanation: String
)

data class SecurityEvent(
    val type: EventType,
    val source: String,
    val timestamp: Long,
    val metadata: Map<String, String> = emptyMap()
)

enum class EventType {
    SMS,
    CALL,
    APP_SIGNAL
}
