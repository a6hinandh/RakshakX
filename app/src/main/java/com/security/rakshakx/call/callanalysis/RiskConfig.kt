package com.security.rakshakx.call.callanalysis

import com.security.rakshakx.R

/**
 * Global configuration for fraud risk scoring and thresholds.
 */
object RiskConfig {
    // Scoring weights for hybrid analysis
    const val ML_WEIGHT = 0.6f
    const val RULES_WEIGHT = 0.4f

    // Risk Score Thresholds (updated per user request)
    const val THRESHOLD_CRITICAL = 0.7f
    const val THRESHOLD_HIGH = 0.5f
    const val THRESHOLD_MEDIUM = 0.3f
    const val THRESHOLD_SAFE_ROUTING = 0.5f

    // UI Color Resources
    val COLOR_CRITICAL = R.color.red_error
    val COLOR_HIGH = R.color.red_error
    val COLOR_MEDIUM = R.color.orange_warning
    val COLOR_SAFE = R.color.green_safe

    // System Color Fallbacks
    val SYS_COLOR_HIGH = android.R.color.holo_red_dark
    val SYS_COLOR_MEDIUM = android.R.color.holo_orange_dark
    val SYS_COLOR_SAFE = android.R.color.holo_green_dark
    val SYS_COLOR_BORDERLINE = android.R.color.holo_orange_light

    /**
     * Helper to get risk label based on score.
     */
    fun getRiskLevel(score: Float): String {
        return when {
            score >= THRESHOLD_CRITICAL -> "CRITICAL RISK"
            score >= THRESHOLD_HIGH -> "HIGH RISK"
            score >= THRESHOLD_MEDIUM -> "MEDIUM RISK"
            else -> "LOW RISK"
        }
    }
}

