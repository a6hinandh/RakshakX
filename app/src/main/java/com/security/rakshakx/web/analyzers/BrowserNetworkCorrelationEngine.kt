package com.security.rakshakx.web.analyzers

import com.security.rakshakx.web.models.BrowserSessionData
import com.security.rakshakx.web.models.ThreatAssessment
import com.security.rakshakx.web.models.ThreatLevel

class BrowserNetworkCorrelationEngine(
    private val intelRepository: ThreatIntelRepository
) {
    fun correlate(session: BrowserSessionData?, vpnDomain: String): ThreatAssessment? {
        if (session == null || vpnDomain.isBlank()) {
            return null
        }

        val visibleDomain = session.url.substringAfter("//").substringBefore("/").lowercase()
        val normalizedVpn = vpnDomain.lowercase()

        if (visibleDomain.isBlank()) {
            return null
        }

        if (intelRepository.isSafeSuffix(normalizedVpn)) {
            return null
        }

        if (sameBaseDomain(visibleDomain, normalizedVpn)) {
            return null
        }

        if (visibleDomain != normalizedVpn && !normalizedVpn.endsWith(visibleDomain)) {
            return ThreatAssessment(
                timestamp = System.currentTimeMillis(),
                domain = normalizedVpn,
                level = ThreatLevel.CRITICAL,
                action = com.security.rakshakx.web.models.ThreatAction.BLOCK,
                reasons = listOf("Visible URL domain mismatch with network destination")
            )
        }

        return null
    }

    private fun sameBaseDomain(left: String, right: String): Boolean {
        val leftParts = left.split('.').filter { it.isNotBlank() }
        val rightParts = right.split('.').filter { it.isNotBlank() }
        if (leftParts.size < 2 || rightParts.size < 2) return false
        val leftBase = leftParts.takeLast(2).joinToString(".")
        val rightBase = rightParts.takeLast(2).joinToString(".")
        return leftBase == rightBase
    }
}
