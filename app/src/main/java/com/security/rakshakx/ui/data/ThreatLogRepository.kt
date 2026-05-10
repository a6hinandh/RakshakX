package com.security.rakshakx.ui.data

import android.content.Context
import com.security.rakshakx.call.callanalysis.data.CallDatabase
import com.security.rakshakx.call.core.storage.DatabaseFactory
import com.security.rakshakx.email.database.ThreatDatabase as EmailThreatDb

/**
 * ThreatLogRepository — reads from all four channel databases and
 * returns a unified list of [ThreatLogEntry] items sorted by timestamp.
 *
 * Also provides a demo scenario for cross-channel correlation visualization.
 */
object ThreatLogRepository {

    /**
     * Fetch all threats from all channels, unified into a single list.
     */
    suspend fun getAllThreats(context: Context): List<ThreatLogEntry> {
        val results = mutableListOf<ThreatLogEntry>()

        // ── Email threats ──
        try {
            val emailDb = EmailThreatDb.getDatabase(context)
            val emailThreats = emailDb.threatDao().getAllThreats()
            emailThreats.forEach { e ->
                results.add(
                    ThreatLogEntry(
                        id = "email_${e.id}",
                        channel = Channel.EMAIL,
                        severity = when {
                            e.riskScore >= 80 -> Severity.CRITICAL
                            e.riskScore >= 60 -> Severity.HIGH
                            e.riskScore >= 40 -> Severity.MEDIUM
                            else -> Severity.LOW
                        },
                        title = e.riskLevel,
                        description = e.message.take(120),
                        source = e.title,
                        riskScore = e.riskScore / 100f,
                        timestamp = e.timestamp,
                        indicators = listOf("Phishing email"),
                        reason = "Email risk analysis: ${e.riskLevel}"
                    )
                )
            }
        } catch (_: Exception) { /* DB not initialized yet */ }

        // ── Call threats ──
        try {
            val callDb = CallDatabase.getInstance(context)
            val callRecords = callDb.callDao().getRecentCallsSync()
            callRecords.filter { it.riskScore >= 0.3f }.forEach { c ->
                results.add(
                    ThreatLogEntry(
                        id = "call_${c.id}",
                        channel = Channel.CALL,
                        severity = when {
                            c.riskScore >= 0.7f -> Severity.CRITICAL
                            c.riskScore >= 0.5f -> Severity.HIGH
                            c.riskScore >= 0.3f -> Severity.MEDIUM
                            else -> Severity.LOW
                        },
                        title = "Suspicious Call",
                        description = c.transcript.take(120),
                        source = c.phoneNumber,
                        riskScore = c.riskScore,
                        timestamp = c.timestamp,
                        action = c.action,
                        reason = c.reason,
                        indicators = buildList {
                            if (c.transcript.lowercase().contains("otp")) add("OTP request")
                            if (c.transcript.lowercase().contains("bank")) add("Bank impersonation")
                            if (c.transcript.lowercase().contains("urgent")) add("Urgency language")
                        }
                    )
                )
            }
        } catch (_: Exception) { /* DB not initialized yet */ }

        // ── SMS threats (Individual Events) ──
        try {
            val rakshakDb = DatabaseFactory.getInstance(context)
            val smsEvents = rakshakDb.fraudDao().getAllSmsList(50)
            smsEvents.forEach { s ->
                results.add(
                    ThreatLogEntry(
                        id = "sms_${s.id}",
                        channel = Channel.SMS,
                        severity = when {
                            s.fraudRiskScore >= 0.7f -> Severity.CRITICAL
                            s.fraudRiskScore >= 0.5f -> Severity.HIGH
                            s.fraudRiskScore >= 0.3f -> Severity.MEDIUM
                            else -> Severity.LOW
                        },
                        title = if (s.fraudRiskScore >= 0.5f) "Scam SMS Detected" else "Suspicious SMS",
                        description = s.messageBody,
                        source = s.sender,
                        riskScore = s.fraudRiskScore,
                        timestamp = s.timestamp,
                        indicators = buildList {
                            if (s.containsOtp) add("OTP Request")
                            if (s.detectedKeywords.isNotBlank()) add(s.detectedKeywords)
                        }
                    )
                )
            }
        } catch (_: Exception) { }

        // ── Web threats (Web Module Database) ──
        try {
            val webModuleDb = com.security.rakshakx.web.storage.ThreatDatabase.getInstance(context)
            val webThreats = webModuleDb.threatDao().recent(50)
            webThreats.forEach { w ->
                results.add(
                    ThreatLogEntry(
                        id = "web_mod_${w.id}",
                        channel = Channel.WEB,
                        severity = when (w.level) {
                            "CRITICAL" -> Severity.CRITICAL
                            "HIGH" -> Severity.HIGH
                            "MEDIUM" -> Severity.MEDIUM
                            else -> Severity.LOW
                        },
                        title = w.fraudCategory.ifBlank { "Phishing URL Intercepted" },
                        description = w.url,
                        source = w.domain,
                        riskScore = w.fraudScore / 100f,
                        timestamp = w.timestamp,
                        indicators = w.reasons.split(",").filter { it.isNotBlank() },
                        reason = w.blockReason
                    )
                )
            }
        } catch (_: Exception) { }

        return results.sortedByDescending { it.timestamp }
    }

    /**
     * Delete logs older than [days] from all underlying databases.
     */
    suspend fun cleanOldLogs(context: Context, days: Int) {
        val threshold = System.currentTimeMillis() - (days.toLong() * 24 * 60 * 60 * 1000)
        
        // Email
        try {
            EmailThreatDb.getDatabase(context).threatDao().deleteOldThreats(threshold)
        } catch (_: Exception) {}

        // Calls (CallDatabase)
        try {
            CallDatabase.getInstance(context).callDao().deleteOldCalls(threshold)
        } catch (_: Exception) {}

        // Risk Scores (DatabaseFactory)
        try {
            DatabaseFactory.getInstance(context).riskScoreDao().deleteOldScores(threshold)
        } catch (_: Exception) {}

        // Orchestrator Logs (FraudDao)
        try {
            val fraudDao = DatabaseFactory.getInstance(context).fraudDao()
            fraudDao.pruneOldSms(threshold)
            fraudDao.pruneOldCalls(threshold)
            fraudDao.pruneOldWebEvents(threshold)
            fraudDao.pruneOldEmails(threshold)
            fraudDao.pruneOldSessions(threshold)
        } catch (_: Exception) {}
    }

    /**
     * Pre-built demo scenario showing a multi-stage scam attack
     * across all four channels for hackathon demonstration.
     */
    fun getDemoCorrelationTimeline(): List<CorrelationEvent> {
        val now = System.currentTimeMillis()
        return listOf(
            CorrelationEvent(
                id = "demo_1",
                channel = Channel.SMS,
                severity = Severity.MEDIUM,
                title = "OTP SMS Received",
                description = "\"Your SBI OTP is 482917. Do NOT share with anyone. Ref: TXN892731\"",
                timestamp = now - 3600_000 * 4,    // 4 hours ago
                riskScore = 0.35f,
                escalationDelta = 0.35f
            ),
            CorrelationEvent(
                id = "demo_2",
                channel = Channel.CALL,
                severity = Severity.HIGH,
                title = "Scam Call — Bank Impersonation",
                description = "Caller claiming to be SBI officer requesting OTP verification. Urgency language detected.",
                timestamp = now - 3600_000 * 3,    // 3 hours ago
                riskScore = 0.72f,
                escalationDelta = 0.37f
            ),
            CorrelationEvent(
                id = "demo_3",
                channel = Channel.EMAIL,
                severity = Severity.HIGH,
                title = "Phishing Email Intercepted",
                description = "Email from 'sbi-alerts@secure-banking.xyz' requesting KYC update with suspicious link.",
                timestamp = now - 3600_000 * 2,    // 2 hours ago
                riskScore = 0.81f,
                escalationDelta = 0.09f
            ),
            CorrelationEvent(
                id = "demo_4",
                channel = Channel.WEB,
                severity = Severity.CRITICAL,
                title = "Phishing URL Blocked",
                description = "Blocked access to https://sbi-kyc-update.xyz — IP-based redirect, fake login page detected.",
                timestamp = now - 3600_000,         // 1 hour ago
                riskScore = 0.95f,
                escalationDelta = 0.14f
            ),
            CorrelationEvent(
                id = "demo_5",
                channel = Channel.SMS,
                severity = Severity.CRITICAL,
                title = "Fraud Escalation — Second OTP Attempt",
                description = "Another OTP request from unknown number. Correlated with ongoing scam attack — AUTO-BLOCKED.",
                timestamp = now - 1800_000,         // 30 min ago
                riskScore = 1.0f,
                escalationDelta = 0.05f
            ),
        )
    }
}
