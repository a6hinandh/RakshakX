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

        // ── Call/SMS orchestrator risk scores ──
        try {
            val rakshakDb = DatabaseFactory.getInstance(context)
            val riskScores = rakshakDb.riskScoreDao().getTopRiskContacts(50)
            riskScores.filter { it.riskScore >= 0.3f }.forEach { r ->
                val channel = if (r.lastEventType == "SMS") Channel.SMS else Channel.CALL
                results.add(
                    ThreatLogEntry(
                        id = "risk_${r.phoneNumber}_${r.updatedAt}",
                        channel = channel,
                        severity = when {
                            r.riskScore >= 0.7f -> Severity.CRITICAL
                            r.riskScore >= 0.5f -> Severity.HIGH
                            r.riskScore >= 0.3f -> Severity.MEDIUM
                            else -> Severity.LOW
                        },
                        title = "Risk Event (${r.lastEventType})",
                        description = r.lastMessageSnippet ?: "No details available",
                        source = r.phoneNumber,
                        riskScore = r.riskScore,
                        timestamp = r.updatedAt,
                        indicators = buildList {
                            if (r.lastMessageSnippet?.lowercase()?.contains("otp") == true) add("OTP request")
                            if (r.lastMessageSnippet?.lowercase()?.contains("bank") == true) add("Bank impersonation")
                        }
                    )
                )
            }
        } catch (_: Exception) { /* DB not initialized yet */ }

        return results.sortedByDescending { it.timestamp }
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
