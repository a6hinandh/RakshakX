package com.security.rakshakx.web.models

enum class ThreatLevel {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

enum class ThreatAction {
    ALLOW,
    BLOCK
}

data class ThreatAssessment(
    val timestamp: Long,
    val domain: String,
    val level: ThreatLevel,
    val action: ThreatAction,
    val reasons: List<String>
)
