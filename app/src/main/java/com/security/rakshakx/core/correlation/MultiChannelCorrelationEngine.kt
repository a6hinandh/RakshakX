package com.security.rakshakx.core.correlation

import android.content.Context
import android.util.Log
import com.security.rakshakx.call.core.storage.DatabaseFactory
import com.security.rakshakx.data.entities.SmsEventEntity
import com.security.rakshakx.data.entities.ThreatSessionEntity
import com.security.rakshakx.data.entities.WebEventEntity

/**
 * MultiChannelCorrelationEngine
 *
 * The brain of RakshakX's coordinated attack detection.
 * Links signals across SMS, Web, Call, and Email.
 */
class MultiChannelCorrelationEngine(private val context: Context) {

    private val TAG = "RakshakX_Correlation"
    private val db = DatabaseFactory.getInstance(context)
    private val fraudDao = db.fraudDao()

    /**
     * Checks if a scanned URL matches any suspicious URLs received via SMS
     * in the last 24 hours.
     */
    suspend fun correlateUrlWithRecentSms(url: String): CorrelationResult? {
        val domain = getDomain(url)
        val since = System.currentTimeMillis() - (24 * 60 * 60 * 1000) // 24 hours
        
        Log.d(TAG, "Correlating URL: $url (domain: $domain) since $since")
        
        // 1. Search for exact URL match in SMS
        val exactMatches = fraudDao.findRecentSmsWithUrl(url, since)
        if (exactMatches.isNotEmpty()) {
            val sms = exactMatches.first()
            return CorrelationResult(
                sourceSms = sms,
                reason = "Exact URL match: ${url} was found in a suspicious SMS from ${sms.sender}",
                riskEscalation = 0.5f
            )
        }

        // 2. Search for domain-level match
        // This is more robust as scammers often use different paths or protocols
        val allRecentSms = fraudDao.getAllSmsList(100)
        val domainMatch = allRecentSms.filter { it.timestamp > since }.firstOrNull { sms ->
            val smsUrls = sms.detectedUrls.split(",").filter { it.isNotBlank() }
            smsUrls.any { smsUrl -> 
                val smsDomain = getDomain(smsUrl)
                smsDomain.equals(domain, ignoreCase = true) && domain.isNotBlank() && domain.length > 3
            }
        }

        if (domainMatch != null) {
            return CorrelationResult(
                sourceSms = domainMatch,
                reason = "Domain match: Recent suspicious SMS from ${domainMatch.sender} contained a link to ${domain}",
                riskEscalation = 0.4f
            )
        }

        return null
    }

    /**
     * Persists a correlated threat session and escalates the risk.
     */
    suspend fun createCorrelatedSession(
        webEvent: WebEventEntity,
        correlation: CorrelationResult
    ): Long {
        val session = ThreatSessionEntity(
            linkedSmsId = correlation.sourceSms.id,
            linkedWebId = webEvent.id,
            overallThreatScore = (webEvent.fraudRiskScore + correlation.riskEscalation).coerceAtMost(1.0f),
            threatCategory = "COORDINATED_SMISHING",
            correlationReason = correlation.reason,
            recommendedAction = "IMMEDIATE_BLOCK"
        )
        
        Log.i(TAG, "Coordinated attack detected! Session created: ${correlation.reason}")
        return fraudDao.insertThreatSession(session)
    }

    private fun getDomain(urlStr: String): String {
        return try {
            val formattedUrl = if (!urlStr.startsWith("http")) "https://$urlStr" else urlStr
            val uri = android.net.Uri.parse(formattedUrl)
            uri.host?.removePrefix("www.") ?: urlStr
        } catch (e: Exception) {
            urlStr
        }
    }
}

data class CorrelationResult(
    val sourceSms: SmsEventEntity,
    val reason: String,
    val riskEscalation: Float
)
