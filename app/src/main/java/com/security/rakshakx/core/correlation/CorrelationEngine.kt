package com.security.rakshakx.core.correlation

import com.security.rakshakx.data.entities.*
import com.security.rakshakx.data.repository.FraudRepository

class CorrelationEngine(private val repository: FraudRepository) {

    suspend fun processNewSms(sms: SmsEventEntity) {
        if (sms.containsOtp) {
            // Check for recent suspicious calls from same sender or general high risk
            // Logic to create or update ThreatSession
        }
    }

    suspend fun correlateWebWithSms(web: WebEventEntity, sms: SmsEventEntity) {
        if (web.domain.contains(sms.sender, ignoreCase = true) || 
            sms.messageBody.contains(web.domain, ignoreCase = true)) {
            
            val session = ThreatSessionEntity(
                linkedSmsId = sms.id,
                linkedWebId = web.id,
                overallThreatScore = (sms.fraudRiskScore + web.fraudRiskScore) / 2 + 0.2f,
                threatCategory = "SMISHING_TO_PHISHING",
                recommendedAction = "BLOCK_AND_REPORT"
            )
            repository.logThreatSession(session)
        }
    }
}
