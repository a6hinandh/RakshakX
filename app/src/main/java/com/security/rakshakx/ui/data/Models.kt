package com.security.rakshakx.ui.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Sms
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.security.rakshakx.ui.theme.PremiumBlue
import com.security.rakshakx.ui.theme.PremiumGreen
import com.security.rakshakx.ui.theme.PremiumOrange
import com.security.rakshakx.ui.theme.PremiumPurple
import com.security.rakshakx.ui.theme.PremiumRed
import com.security.rakshakx.ui.theme.GreenSafe
import com.security.rakshakx.ui.theme.OrangeWarn
import com.security.rakshakx.ui.theme.RedCritical

/** Represents a single channel in RakshakX */
enum class Channel(
    val label: String,
    val icon: ImageVector,
    val color: Color
) {
    SMS("SMS", Icons.Filled.Sms, PremiumBlue),
    CALL("Call", Icons.Filled.Call, PremiumPurple),
    WEB("Web", Icons.Filled.Language, PremiumGreen),
    EMAIL("Email", Icons.Filled.Email, PremiumRed);
}

/** Severity level for threat events */
enum class Severity(val label: String, val color: Color) {
    CRITICAL("Critical", RedCritical),
    HIGH("High", Color(0xFFFF5252)),
    MEDIUM("Medium", OrangeWarn),
    LOW("Low", GreenSafe);
}

/** Unified threat log entry — aggregated from all channel databases */
data class ThreatLogEntry(
    val id: String,
    val channel: Channel,
    val severity: Severity,
    val title: String,
    val description: String,
    val source: String,          // phone number, domain, email sender
    val riskScore: Float,        // 0.0 - 1.0
    val timestamp: Long,
    val indicators: List<String> = emptyList(),  // e.g. "OTP request", "Bank impersonation"
    val action: String = "",     // BLOCK, WARN, ALLOW
    val reason: String = "",     // Why it was flagged
)

/** Timeline event for cross-channel correlation */
data class CorrelationEvent(
    val id: String,
    val channel: Channel,
    val severity: Severity,
    val title: String,
    val description: String,
    val timestamp: Long,
    val riskScore: Float,
    val escalationDelta: Float = 0f,  // How much risk increased from previous event
)

/** Protection status for a single channel */
data class ChannelStatus(
    val channel: Channel,
    val isActive: Boolean,
    val threatCount: Int = 0,
    val lastThreatTime: Long? = null,
)

/** Overall protection state */
enum class ProtectionLevel(val label: String, val color: Color) {
    PROTECTED("Protected", GreenSafe),
    ELEVATED("Elevated Risk", OrangeWarn),
    THREAT_DETECTED("Threat Detected", RedCritical);
}
