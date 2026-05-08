package com.security.rakshakx.web.utils

import android.content.Context
import com.security.rakshakx.web.models.BrowserSessionData
import com.security.rakshakx.web.models.FraudRiskResult
import com.security.rakshakx.web.models.ThreatAssessment
import com.security.rakshakx.web.models.VpnTrafficData
import com.security.rakshakx.web.storage.BrowserSessionEntity
import com.security.rakshakx.web.storage.ThreatDatabase
import com.security.rakshakx.web.storage.ThreatEntity
import com.security.rakshakx.web.storage.VpnTrafficEntity
import com.security.rakshakx.web.storage.EncryptedStorageManager

class VpnThreatLogger(context: Context) {
    private val database = ThreatDatabase.getInstance(context)
    private val legacyCleaner = EncryptedStorageManager(context)

    init {
        legacyCleaner.deleteLegacyLog("browser_sessions.jsonl")
    }

    suspend fun logThreat(
        assessment: ThreatAssessment,
        traffic: VpnTrafficData,
        session: BrowserSessionData?,
        fraudResult: FraudRiskResult? = null
    ) {
        val entity = ThreatEntity(
            timestamp = assessment.timestamp,
            domain = assessment.domain,
            level = assessment.level.name,
            action = assessment.action.name,
            reasons = assessment.reasons.joinToString(" | "),
            fraudScore = fraudResult?.score ?: 0,
            fraudCategory = fraudResult?.category?.name.orEmpty(),
            recommendedAction = fraudResult?.action?.name.orEmpty(),
            blockReason = fraudResult?.blockReason.orEmpty(),
            visibleSignals = fraudResult?.visibleSignals?.joinToString(" | ").orEmpty(),
            correlationData = fraudResult?.correlationData.orEmpty(),
            browserPackage = session?.browserPackage.orEmpty(),
            url = session?.url.orEmpty(),
            destinationIp = traffic.destinationIp,
            sniHost = traffic.sniHost
        )

        database.threatDao().insert(entity)
    }

    suspend fun logTraffic(traffic: VpnTrafficData) {
        val entity = VpnTrafficEntity(
            timestamp = traffic.timestamp,
            protocol = traffic.protocol,
            sourceIp = traffic.sourceIp,
            destinationIp = traffic.destinationIp,
            sourcePort = traffic.sourcePort,
            destinationPort = traffic.destinationPort,
            dnsQuery = traffic.dnsQuery,
            sniHost = traffic.sniHost,
            redirectChain = traffic.redirectChain.joinToString(" -> ")
        )

        database.vpnTrafficDao().insert(entity)
    }

    suspend fun logBrowserSession(session: BrowserSessionData) {
        val entity = BrowserSessionEntity(
            timestamp = session.timestamp,
            browserPackage = session.browserPackage,
            url = session.url,
            pageTitle = session.pageTitle,
            visibleText = session.visibleText,
            passwordFieldDetected = session.passwordFieldDetected,
            otpFieldDetected = session.otpFieldDetected,
            emailFieldDetected = session.emailFieldDetected,
            paymentFieldDetected = session.paymentFieldDetected
        )

        database.browserSessionDao().insert(entity)
    }
}
