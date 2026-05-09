package com.security.rakshakx.integration

data class ModelResult(
    val isScam: Boolean,
    val confidence: Float,
    val label: String,           // "SAFE" | "SCAM" | "SUSPICIOUS"
    val modelUsed: String,       // "distilbert" | "indicbert" | "fallback"
    val channel: String,         // "sms" | "email" | "call" | "web" | "generic"
    val ruleScore: Int = 0,      // 0-100 from RiskEngine
    val finalScore: Float = 0f   // Combined 0-1.0 score
) {
    val riskLevel: RiskLevel
        get() = when {
            finalScore < 0.4f        -> RiskLevel.LOW
            finalScore < 0.7f        -> RiskLevel.MEDIUM
            else                     -> RiskLevel.HIGH
        }

    val isIndicBert: Boolean
        get() = modelUsed == "indicbert"

    override fun toString(): String =
        "ModelResult(label=$label, finalScore=${"%.1f".format(finalScore * 100)}%, " +
                "mlConf=${"%.1f".format(confidence * 100)}%, rules=$ruleScore, " +
                "model=$modelUsed, channel=$channel, risk=$riskLevel)"
}

enum class RiskLevel {
    LOW, MEDIUM, HIGH
}