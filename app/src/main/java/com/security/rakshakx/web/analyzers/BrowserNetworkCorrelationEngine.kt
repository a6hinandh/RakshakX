package com.security.rakshakx.web.analyzers

import com.security.rakshakx.web.models.BrowserSessionData
import com.security.rakshakx.web.models.ThreatAssessment
import com.security.rakshakx.web.models.ThreatLevel

class BrowserNetworkCorrelationEngine {
    fun correlate(session: BrowserSessionData?, vpnDomain: String): ThreatAssessment? {
        if (session == null || vpnDomain.isBlank()) {
            return null
        }

        val visibleDomain = session.url.substringAfter("//").substringBefore("/").lowercase()
        val normalizedVpn = vpnDomain.lowercase()

        if (visibleDomain.isBlank()) {
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
}
