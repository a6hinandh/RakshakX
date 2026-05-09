package com.security.rakshakx.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sms_events")
data class SmsEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sender: String,
    val messageBody: String,
    val timestamp: Long = System.currentTimeMillis(),
    val containsOtp: Boolean = false,
    val otpCode: String? = null,
    val fraudRiskScore: Float = 0f,
    val detectedKeywords: String = "",
    val sourceType: String = "SMS"
)

@Entity(tableName = "call_events")
data class CallEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val phoneNumber: String,
    val transcript: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val callDuration: Long = 0,
    val analyzed: Boolean = false,
    val fraudRiskScore: Float = 0f,
    val detectedIntent: String = "",
    val sourceType: String = "CALL"
)

@Entity(tableName = "email_events")
data class EmailEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val senderEmail: String,
    val subject: String,
    val previewText: String,
    val timestamp: Long = System.currentTimeMillis(),
    val fraudRiskScore: Float = 0f,
    val phishingIndicators: String = "",
    val sourceType: String = "EMAIL"
)

@Entity(tableName = "web_events")
data class WebEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val url: String,
    val domain: String,
    val pageTitle: String,
    val timestamp: Long = System.currentTimeMillis(),
    val vpnFlagged: Boolean = false,
    val accessibilityFlagged: Boolean = false,
    val fraudRiskScore: Float = 0f,
    val phishingIndicators: String = "",
    val sourceType: String = "WEB"
)

@Entity(tableName = "threat_sessions")
data class ThreatSessionEntity(
    @PrimaryKey(autoGenerate = true) val sessionId: Long = 0,
    val linkedSmsId: Long? = null,
    val linkedCallId: Long? = null,
    val linkedEmailId: Long? = null,
    val linkedWebId: Long? = null,
    val overallThreatScore: Float = 0f,
    val threatCategory: String,
    val createdAt: Long = System.currentTimeMillis(),
    val resolved: Boolean = false,
    val recommendedAction: String
)
