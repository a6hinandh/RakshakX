package com.security.rakshakx.call.services.agents

import com.security.rakshakx.call.core.orchestrator.SecurityEvent

interface FraudDetectionAgent {
    suspend fun initialize()
    suspend fun evaluate(event: SecurityEvent): AgentRiskResult
    suspend fun shutdown()
}

data class AgentRiskResult(
    val agentId: String,
    val riskScore: Float,
    val reasonTag: String
)


