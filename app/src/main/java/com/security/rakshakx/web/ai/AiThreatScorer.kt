package com.security.rakshakx.web.ai

import android.util.Log
import com.security.rakshakx.web.models.BrowserSessionData
import com.security.rakshakx.web.models.ThreatAssessment
import com.security.rakshakx.web.models.VpnTrafficData

class AiThreatScorer(
    private val preprocessor: FraudTextPreprocessor,
    private val model: OnDeviceFraudModel
) {
    fun score(
        session: BrowserSessionData,
        traffic: VpnTrafficData,
        assessment: ThreatAssessment
    ): Float? {
        return try {
            val fieldHints = buildFieldHints(session)
            val text = preprocessor.buildText(
                title = session.pageTitle,
                visibleText = session.visibleText,
                url = session.url,
                dnsDomain = traffic.dnsQuery,
                dnsReasons = assessment.reasons,
                fieldHints = fieldHints
            )
            val input = preprocessor.toModelInput(text)
            model.predict(input)
        } catch (e: Exception) {
            Log.w(TAG, "AI scoring failed", e)
            null
        }
    }

    private fun buildFieldHints(session: BrowserSessionData): List<String> {
        val hints = mutableListOf<String>()
        if (session.passwordFieldDetected) hints.add("password")
        if (session.otpFieldDetected) hints.add("otp")
        if (session.emailFieldDetected) hints.add("email")
        if (session.paymentFieldDetected) hints.add("payment")
        return hints
    }

    companion object {
        private const val TAG = "RakshakX-AI"
    }
}
