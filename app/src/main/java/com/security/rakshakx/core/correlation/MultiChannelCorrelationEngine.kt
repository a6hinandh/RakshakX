package com.security.rakshakx.core.correlation

import android.content.Context
import android.util.Log
import com.security.rakshakx.call.core.storage.DatabaseFactory
import com.security.rakshakx.data.dao.FraudDao
import com.security.rakshakx.data.entities.*

class MultiChannelCorrelationEngine(private val context: Context) {

    private val TAG = "RakshakX_Correlation"
    private val db = DatabaseFactory.getInstance(context)
    private val fraudDao: FraudDao = db.fraudDao()

    companion object {
        private const val WINDOW_24H = 24 * 60 * 60 * 1000L
        private const val WINDOW_1H = 60 * 60 * 1000L
        private const val WINDOW_15M = 15 * 60 * 1000L
    }

    // ── SMS ↔ Web: URL / domain match ──────────────────────────────

    suspend fun correlateUrlWithRecentSms(url: String): CorrelationResult? {
        val domain = extractDomain(url)
        val since = System.currentTimeMillis() - WINDOW_24H

        val exactMatches = fraudDao.findRecentSmsWithUrl(url, since)
        if (exactMatches.isNotEmpty()) {
            val sms = exactMatches.first()
            return CorrelationResult(
                type = CorrelationType.SMS_WEB_URL,
                sourceSms = sms,
                reason = "Exact URL match: $url found in SMS from ${sms.sender}",
                riskEscalation = 0.5f
            )
        }

        val allRecentSms = fraudDao.getAllSmsList(200)
        val domainMatch = allRecentSms
            .filter { it.timestamp > since }
            .firstOrNull { sms ->
                sms.detectedUrls.split(",")
                    .filter { it.isNotBlank() }
                    .any { smsUrl ->
                        val smsDomain = extractDomain(smsUrl)
                        smsDomain.equals(domain, ignoreCase = true) && domain.length > 3
                    }
            }

        if (domainMatch != null) {
            return CorrelationResult(
                type = CorrelationType.SMS_WEB_URL,
                sourceSms = domainMatch,
                reason = "Domain match: SMS from ${domainMatch.sender} linked to $domain",
                riskEscalation = 0.4f
            )
        }

        return null
    }

    // ── Email ↔ Web: phishing email links opened in browser ────────

    suspend fun correlateWebWithRecentEmail(url: String): CorrelationResult? {
        val domain = extractDomain(url)
        val since = System.currentTimeMillis() - WINDOW_24H

        val recentEmails = fraudDao.getAllEmailsList(200)
        val match = recentEmails
            .filter { it.timestamp > since && it.fraudRiskScore > 0.3f }
            .firstOrNull { email ->
                val text = "${email.subject} ${email.previewText} ${email.phishingIndicators}"
                text.contains(domain, ignoreCase = true) ||
                    extractDomainsFromText(text).any { it.equals(domain, ignoreCase = true) }
            }

        if (match != null) {
            return CorrelationResult(
                type = CorrelationType.EMAIL_WEB_URL,
                sourceEmail = match,
                reason = "Phishing email from ${match.senderEmail} contained link to $domain",
                riskEscalation = 0.45f
            )
        }
        return null
    }

    // ── Call ↔ SMS: same phone number across channels ──────────────

    suspend fun correlateCallWithSms(phoneNumber: String): CorrelationResult? {
        val normalized = normalizePhone(phoneNumber)
        val since = System.currentTimeMillis() - WINDOW_24H

        val smsMatches = fraudDao.findRecentSmsByPhone(normalized, since)
        val suspiciousSms = smsMatches.filter { it.fraudRiskScore > 0.3f }

        if (suspiciousSms.isNotEmpty()) {
            val sms = suspiciousSms.first()
            return CorrelationResult(
                type = CorrelationType.CALL_SMS_PHONE,
                sourceSms = sms,
                reason = "Caller $phoneNumber also sent suspicious SMS (risk: ${"%.0f".format(sms.fraudRiskScore * 100)}%)",
                riskEscalation = 0.4f
            )
        }

        if (smsMatches.size >= 3) {
            return CorrelationResult(
                type = CorrelationType.CALL_SMS_PHONE,
                sourceSms = smsMatches.first(),
                reason = "Caller $phoneNumber sent ${smsMatches.size} SMS messages in 24h — high-volume pattern",
                riskEscalation = 0.25f
            )
        }

        return null
    }

    // ── Phone number correlation: same number flagged in multiple channels ──

    suspend fun correlatePhoneNumber(phoneNumber: String): List<CorrelationResult> {
        val normalized = normalizePhone(phoneNumber)
        val since = System.currentTimeMillis() - WINDOW_24H
        val results = mutableListOf<CorrelationResult>()

        val smsList = fraudDao.findRecentSmsByPhone(normalized, since)
        val callList = fraudDao.findRecentCallsByPhone(normalized, since)

        val suspiciousSms = smsList.filter { it.fraudRiskScore > 0.4f }
        val suspiciousCalls = callList.filter { it.fraudRiskScore > 0.4f }

        if (suspiciousSms.isNotEmpty() && suspiciousCalls.isNotEmpty()) {
            results.add(
                CorrelationResult(
                    type = CorrelationType.PHONE_MULTI_CHANNEL,
                    sourceSms = suspiciousSms.first(),
                    sourceCall = suspiciousCalls.first(),
                    reason = "Phone $phoneNumber flagged in both SMS (${suspiciousSms.size}) and calls (${suspiciousCalls.size})",
                    riskEscalation = 0.55f
                )
            )
        }

        if (smsList.size + callList.size >= 5) {
            results.add(
                CorrelationResult(
                    type = CorrelationType.PHONE_MULTI_CHANNEL,
                    reason = "Phone $phoneNumber has ${smsList.size + callList.size} events across channels — potential coordinated attack",
                    riskEscalation = 0.3f
                )
            )
        }

        return results
    }

    // ── Temporal proximity: burst of events across channels ────────

    suspend fun correlateTemporalBurst(anchorTimestamp: Long): CorrelationResult? {
        val windowStart = anchorTimestamp - WINDOW_15M
        val windowEnd = anchorTimestamp + WINDOW_15M

        val smsInWindow = fraudDao.findSmsInTimeRange(windowStart, windowEnd)
        val callsInWindow = fraudDao.findCallsInTimeRange(windowStart, windowEnd)
        val emailsInWindow = fraudDao.findEmailsInTimeRange(windowStart, windowEnd)

        val suspiciousSms = smsInWindow.filter { it.fraudRiskScore > 0.3f }
        val suspiciousCalls = callsInWindow.filter { it.fraudRiskScore > 0.3f }
        val suspiciousEmails = emailsInWindow.filter { it.fraudRiskScore > 0.3f }

        val channelCount = listOf(
            suspiciousSms.isNotEmpty(),
            suspiciousCalls.isNotEmpty(),
            suspiciousEmails.isNotEmpty()
        ).count { it }

        if (channelCount >= 2) {
            val totalEvents = suspiciousSms.size + suspiciousCalls.size + suspiciousEmails.size
            val channels = buildList {
                if (suspiciousSms.isNotEmpty()) add("SMS(${suspiciousSms.size})")
                if (suspiciousCalls.isNotEmpty()) add("Call(${suspiciousCalls.size})")
                if (suspiciousEmails.isNotEmpty()) add("Email(${suspiciousEmails.size})")
            }

            return CorrelationResult(
                type = CorrelationType.TEMPORAL_BURST,
                sourceSms = suspiciousSms.firstOrNull(),
                sourceCall = suspiciousCalls.firstOrNull(),
                sourceEmail = suspiciousEmails.firstOrNull(),
                reason = "Coordinated burst: $totalEvents threats across ${channels.joinToString(", ")} within 15 minutes",
                riskEscalation = 0.15f * channelCount
            )
        }

        return null
    }

    // ── Full scan: run all correlations for a web event ─────────────

    suspend fun correlateWebEvent(webEvent: WebEventEntity): List<CorrelationResult> {
        val results = mutableListOf<CorrelationResult>()

        correlateUrlWithRecentSms(webEvent.url)?.let { results.add(it) }
        correlateWebWithRecentEmail(webEvent.url)?.let { results.add(it) }
        correlateTemporalBurst(webEvent.timestamp)?.let { results.add(it) }

        return results
    }

    // ── Full scan: run all correlations for a call event ────────────

    suspend fun correlateCallEvent(callEvent: CallEventEntity): List<CorrelationResult> {
        val results = mutableListOf<CorrelationResult>()

        correlateCallWithSms(callEvent.phoneNumber)?.let { results.add(it) }
        results.addAll(correlatePhoneNumber(callEvent.phoneNumber))
        correlateTemporalBurst(callEvent.timestamp)?.let { results.add(it) }

        return results
    }

    // ── Full scan: run all correlations for an SMS event ────────────

    suspend fun correlateSmsEvent(smsEvent: SmsEventEntity): List<CorrelationResult> {
        val results = mutableListOf<CorrelationResult>()

        results.addAll(correlatePhoneNumber(smsEvent.sender))
        correlateTemporalBurst(smsEvent.timestamp)?.let { results.add(it) }

        val urls = smsEvent.detectedUrls.split(",").filter { it.isNotBlank() }
        for (url in urls) {
            correlateWebWithRecentEmail(url)?.let { results.add(it) }
        }

        return results
    }

    // ── Persist a correlated threat session ─────────────────────────

    suspend fun createCorrelatedSession(
        webEvent: WebEventEntity,
        correlation: CorrelationResult
    ): Long {
        val session = ThreatSessionEntity(
            linkedSmsId = correlation.sourceSms?.id,
            linkedCallId = correlation.sourceCall?.id,
            linkedEmailId = correlation.sourceEmail?.id,
            linkedWebId = webEvent.id,
            overallThreatScore = (webEvent.fraudRiskScore + correlation.riskEscalation).coerceAtMost(1.0f),
            threatCategory = correlation.type.category,
            correlationReason = correlation.reason,
            recommendedAction = if (correlation.riskEscalation >= 0.4f) "IMMEDIATE_BLOCK" else "WARN_USER"
        )

        Log.i(TAG, "Correlated attack: ${correlation.reason}")
        return fraudDao.insertThreatSession(session)
    }

    suspend fun createCorrelatedSessionFromCall(
        callEvent: CallEventEntity,
        correlation: CorrelationResult
    ): Long {
        val session = ThreatSessionEntity(
            linkedSmsId = correlation.sourceSms?.id,
            linkedCallId = callEvent.id,
            linkedEmailId = correlation.sourceEmail?.id,
            overallThreatScore = (callEvent.fraudRiskScore + correlation.riskEscalation).coerceAtMost(1.0f),
            threatCategory = correlation.type.category,
            correlationReason = correlation.reason,
            recommendedAction = if (correlation.riskEscalation >= 0.4f) "IMMEDIATE_BLOCK" else "WARN_USER"
        )

        Log.i(TAG, "Correlated call attack: ${correlation.reason}")
        return fraudDao.insertThreatSession(session)
    }

    // ── Utility ─────────────────────────────────────────────────────

    private fun extractDomain(urlStr: String): String {
        return try {
            val formatted = if (!urlStr.startsWith("http")) "https://$urlStr" else urlStr
            android.net.Uri.parse(formatted).host?.removePrefix("www.") ?: urlStr
        } catch (_: Exception) {
            urlStr
        }
    }

    private fun extractDomainsFromText(text: String): List<String> {
        val urlPattern = Regex("""https?://[^\s<>"{}|\\^`\[\]]+|(?:[a-zA-Z0-9-]+\.)+[a-zA-Z]{2,}(?:/[^\s]*)?""")
        return urlPattern.findAll(text).map { extractDomain(it.value) }.toList()
    }

    private fun normalizePhone(phone: String): String {
        val digits = phone.replace(Regex("[^0-9]"), "")
        return when {
            digits.length > 10 -> digits.takeLast(10)
            else -> digits
        }
    }
}

enum class CorrelationType(val category: String) {
    SMS_WEB_URL("COORDINATED_SMISHING"),
    EMAIL_WEB_URL("PHISHING_CHAIN"),
    CALL_SMS_PHONE("VISHING_SMISHING"),
    PHONE_MULTI_CHANNEL("MULTI_CHANNEL_ATTACK"),
    TEMPORAL_BURST("COORDINATED_BURST")
}

data class CorrelationResult(
    val type: CorrelationType,
    val sourceSms: SmsEventEntity? = null,
    val sourceCall: CallEventEntity? = null,
    val sourceEmail: EmailEventEntity? = null,
    val reason: String,
    val riskEscalation: Float
)
