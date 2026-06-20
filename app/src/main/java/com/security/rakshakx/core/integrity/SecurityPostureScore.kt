package com.security.rakshakx.core.integrity

// ─────────────────────────────────────────────────────────────────
// SecurityPostureScore
// Weighted composite of device, network, and threat sub-scores.
// ─────────────────────────────────────────────────────────────────

data class SecurityPostureScore(
    val deviceScore: Int,
    val networkScore: Int,
    val threatScore: Int,
    val overallScore: Int,
    val grade: String,
    val label: String
) {
    companion object {

        /**
         * Compute a composite posture score.
         *
         * @param deviceScore  0-100 from DeviceIntegrityScanner
         * @param networkScore 0-100 (caller supplied; e.g. VPN active, no rogue AP)
         * @param threatCount  Raw number of active / recent threats
         */
        fun compute(
            deviceScore: Int,
            networkScore: Int,
            threatCount: Int
        ): SecurityPostureScore {
            // Invert threat count: 0 threats → 100, 10+ threats → 0
            val threatScore = (100 - (threatCount * 10)).coerceIn(0, 100)

            // Weighted average: 40% device, 30% network, 30% threat
            val overall = (
                deviceScore  * 0.40 +
                networkScore * 0.30 +
                threatScore  * 0.30
            ).toInt().coerceIn(0, 100)

            val (grade, label) = gradeFromScore(overall)

            return SecurityPostureScore(
                deviceScore  = deviceScore.coerceIn(0, 100),
                networkScore = networkScore.coerceIn(0, 100),
                threatScore  = threatScore,
                overallScore = overall,
                grade        = grade,
                label        = label
            )
        }

        /**
         * Map a 0-100 score to a letter grade and human label.
         */
        fun gradeFromScore(score: Int): Pair<String, String> = when {
            score >= 90 -> "A" to "Excellent"
            score >= 75 -> "B" to "Good"
            score >= 55 -> "C" to "Fair"
            score >= 35 -> "D" to "Poor"
            else        -> "F" to "Critical"
        }
    }
}
