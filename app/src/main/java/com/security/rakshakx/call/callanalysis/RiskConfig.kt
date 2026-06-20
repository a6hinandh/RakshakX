package com.security.rakshakx.call.callanalysis

import com.security.rakshakx.R
import com.security.rakshakx.RakshakXApplication
import com.security.rakshakx.core.SettingsStore

/**
 * Global configuration for fraud risk scoring and thresholds.
 */
object RiskConfig {
    // Scoring weights for hybrid analysis
    const val ML_WEIGHT = 0.6f
    const val RULES_WEIGHT = 0.4f

    // Risk Score Thresholds (updated dynamically based on Settings sensitivity)
    val THRESHOLD_CRITICAL: Float
        get() {
            val sensitivity = SettingsStore.getInstance(RakshakXApplication.instance).sensitivity.value
            return (0.90f - (sensitivity * 0.40f)).coerceIn(0.50f, 0.95f)
        }

    val THRESHOLD_HIGH: Float
        get() {
            val sensitivity = SettingsStore.getInstance(RakshakXApplication.instance).sensitivity.value
            return (0.75f - (sensitivity * 0.50f)).coerceIn(0.30f, 0.85f)
        }

    val THRESHOLD_MEDIUM: Float
        get() {
            val sensitivity = SettingsStore.getInstance(RakshakXApplication.instance).sensitivity.value
            return (0.50f - (sensitivity * 0.40f)).coerceIn(0.10f, 0.60f)
        }

    val THRESHOLD_SAFE_ROUTING: Float
        get() = THRESHOLD_HIGH

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

