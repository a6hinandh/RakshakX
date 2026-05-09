package com.rakshakx.callanalysis

/**
 * FraudDecision
 *
 * Result of fraud analysis containing recommended action and human-readable reason.
 */
data class FraudDecision(
    val action: FraudAction,
    val reason: String
)

/**
 * FraudAction
 *
 * Recommended action based on fraud risk score.
 */
enum class FraudAction {
    WARN,   // Show warning to user
    BLOCK,  // Recommend blocking/hanging up
    ALLOW   // Call appears safe
}

/**
 * PreActionDecisionEngine
 *
 * Maps risk scores to recommended actions and generates decision reasons.
 * This is a simple threshold-based engine that can be evolved with user settings
 * and cross-channel correlation later.
 */
class PreActionDecisionEngine {

    /**
     * Decide action based on risk score.
     *
     * @param riskScore Normalized fraud risk score (0.0 to 1.0)
     * @return FraudDecision containing action and reason
     */
    fun decideAction(riskScore: Float): FraudDecision {
        return when {
            riskScore >= RiskConfig.THRESHOLD_CRITICAL -> {
                FraudDecision(
                    action = FraudAction.BLOCK,
                    reason = "Critical fraud detected: OTP phishing, account suspension threat, urgent language"
                )
            }
            riskScore >= RiskConfig.THRESHOLD_SAFE_ROUTING -> {
                FraudDecision(
                    action = FraudAction.WARN,
                    reason = "Suspicious activity detected: Possible phishing attempt"
                )
            }
            riskScore >= RiskConfig.THRESHOLD_MEDIUM -> {
                FraudDecision(
                    action = FraudAction.WARN,
                    reason = "Low-level risk detected: Monitor this call"
                )
            }
            else -> {
                FraudDecision(
                    action = FraudAction.ALLOW,
                    reason = "No threats detected - appears to be a legitimate call"
                )
            }
        }
    }
}