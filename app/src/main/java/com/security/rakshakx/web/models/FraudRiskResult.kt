package com.security.rakshakx.web.models

enum class FraudCategory {
    PHISHING,
    BANKING_FRAUD,
    CREDENTIAL_THEFT,
    PAYMENT_SCAM,
    IMPERSONATION,
    SUSPICIOUS
}

enum class FraudAction {
    ALLOW,
    WARN,
    BLOCK
}

data class FraudRiskResult(
    val timestamp: Long,
    val domain: String,
    val score: Int,
    val severity: ThreatLevel,
    val category: FraudCategory,
    val action: FraudAction,
    val reasons: List<String>,
    val visibleSignals: List<String>,
    val correlationData: String,
    val blockReason: String
)
