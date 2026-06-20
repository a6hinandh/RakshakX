package com.security.rakshakx.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.security.rakshakx.ui.anim.StaggeredEntry
import com.security.rakshakx.ui.anim.breathingScale
import com.security.rakshakx.ui.anim.rememberHaptics
import androidx.compose.ui.platform.LocalContext
import com.security.rakshakx.call.core.storage.DatabaseFactory
import com.security.rakshakx.call.callanalysis.data.BlockedNumbersRepository
import com.security.rakshakx.data.entities.*
import com.security.rakshakx.web.utils.VpnStatusStore
import com.security.rakshakx.ui.data.ThreatLogRepository
import com.security.rakshakx.ui.data.ThreatLogEntry
import com.security.rakshakx.ui.data.Channel
import com.security.rakshakx.ui.data.Severity
import kotlinx.coroutines.launch
import android.provider.Settings
import android.content.pm.PackageManager
import android.widget.Toast
import android.Manifest
import androidx.core.content.ContextCompat
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.security.rakshakx.ui.components.*
import com.security.rakshakx.ui.theme.*

@Composable
fun LiveThreatScreen(onBack: () -> Unit) {
    val colors = LocalRakshakXColors.current
    val haptics = rememberHaptics()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val db = remember { DatabaseFactory.getInstance(context) }
    val fraudDao = remember { db.fraudDao() }
    val blockedRepo = remember { BlockedNumbersRepository(context) }

    // ── Collectors for active sessions and recent events ──
    val activeSessions by fraudDao.getActiveThreatSessions().collectAsState(initial = emptyList())
    val activeSession = activeSessions.firstOrNull()

    // ── Unified threat logs for activity feed ──
    var recentThreats by remember { mutableStateOf<List<ThreatLogEntry>>(emptyList()) }
    LaunchedEffect(activeSessions) {
        recentThreats = ThreatLogRepository.getAllThreats(context).take(5)
    }

    // ── Check permissions & VPN state for Channel Status ──
    val vpnRunning by VpnStatusStore.isRunning.collectAsState()
    
    val nlsEnabled = remember(activeSessions) {
        val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        flat?.contains(context.packageName) == true
    }

    val callPermission = remember(activeSessions) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
    }

    // ── Fetch linked event details if there's an active threat session ──
    var detectedOtp by remember { mutableStateOf(false) }
    var detectedBank by remember { mutableStateOf(false) }
    var detectedUrgency by remember { mutableStateOf(false) }
    var detectedUrl by remember { mutableStateOf(false) }
    var threatPhoneSource by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(activeSession) {
        if (activeSession != null) {
            detectedOtp = false
            detectedBank = false
            detectedUrgency = false
            detectedUrl = activeSession.linkedWebId != null

            // Query SMS details if linked
            activeSession.linkedSmsId?.let { smsId ->
                try {
                    val smsList = fraudDao.getAllSmsList(200)
                    smsList.firstOrNull { it.id == smsId }?.let { sms ->
                        threatPhoneSource = sms.sender
                        if (sms.containsOtp) detectedOtp = true
                        if (sms.detectedKeywords.lowercase().contains("bank")) detectedBank = true
                        if (sms.detectedKeywords.lowercase().contains("urgent") || sms.detectedKeywords.lowercase().contains("verify")) detectedUrgency = true
                        if (sms.detectedUrls.isNotBlank()) detectedUrl = true
                    }
                } catch (_: Exception) {}
            }

            // Query Call details if linked
            activeSession.linkedCallId?.let { callId ->
                try {
                    val callList = fraudDao.getAllCallsList(200)
                    callList.firstOrNull { it.id == callId }?.let { call ->
                        threatPhoneSource = call.phoneNumber
                        val transcript = call.transcript?.lowercase() ?: ""
                        if (transcript.contains("otp")) detectedOtp = true
                        if (transcript.contains("bank") || call.detectedIntent.lowercase().contains("bank")) detectedBank = true
                        if (transcript.contains("urgent") || transcript.contains("verify")) detectedUrgency = true
                    }
                } catch (_: Exception) {}
            }
            
            // Query Email details if linked
            activeSession.linkedEmailId?.let { emailId ->
                try {
                    val emailList = fraudDao.getAllEmailsList(200)
                    emailList.firstOrNull { it.id == emailId }?.let { email ->
                        val text = (email.subject + " " + email.previewText).lowercase()
                        if (text.contains("otp")) detectedOtp = true
                        if (text.contains("bank")) detectedBank = true
                        if (text.contains("urgent") || text.contains("verify") || email.phishingIndicators.lowercase().contains("urgency")) detectedUrgency = true
                        if (email.phishingIndicators.lowercase().contains("link") || email.phishingIndicators.lowercase().contains("url")) detectedUrl = true
                    }
                } catch (_: Exception) {}
            }
        } else {
            detectedOtp = false
            detectedBank = false
            detectedUrgency = false
            detectedUrl = false
            threatPhoneSource = null
        }
    }

    val currentRiskScore = activeSession?.overallThreatScore ?: 0.00f

    val pulseAnim = rememberInfiniteTransition(label = "livePulse")
    val pulse by pulseAnim.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200, easing = EaseInOutCubic), RepeatMode.Reverse),
        label = "liveAlpha"
    )

    val indicators = listOf(
        Triple("OTP Request", Icons.Filled.Sms, detectedOtp),
        Triple("Bank Impersonation", Icons.Filled.AccountBalance, detectedBank),
        Triple("Urgency Language", Icons.Filled.Warning, detectedUrgency),
        Triple("Suspicious URL", Icons.Filled.Link, detectedUrl),
    )

    PremiumBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .statusBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            PageHeader(
                title = "Live Intelligence",
                infoText = "Monitor real-time threat intelligence and active telemetry from all detection channels.",
                onBack = { haptics.tick(); onBack() },
                trailing = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.background(colors.safeBg, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(colors.safe.copy(alpha = pulse)))
                        Text("LIVE", style = MaterialTheme.typography.labelSmall, color = colors.safe, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    }
                }
            )

            val gaugeBreathing = breathingScale(durationMs = 3000)
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                SecurityScoreGauge(
                    score = ((1f - currentRiskScore) * 100).toInt(),
                    size = 160.dp,
                    strokeWidth = (11f + gaugeBreathing).dp
                )
            }

            val riskLevel = when { currentRiskScore >= 0.7f -> "CRITICAL RISK"; currentRiskScore >= 0.5f -> "HIGH RISK"; currentRiskScore >= 0.3f -> "ELEVATED"; else -> "SYSTEM SECURE" }
            val riskColor = when { currentRiskScore >= 0.7f -> colors.critical; currentRiskScore >= 0.5f -> colors.criticalLight; currentRiskScore >= 0.3f -> colors.warning; else -> colors.safe }
            AnimatedContent(
                targetState = riskLevel,
                transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
                label = "riskText",
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) { label ->
                Text(label, style = MaterialTheme.typography.titleLarge, color = riskColor, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            }

            SectionHeader(title = "Threat Indicators")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                indicators.chunked(2).forEach { row ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { (name, icon, detected) ->
                            GlassSurface(
                                modifier = Modifier.weight(1f),
                                borderColor = if (detected) colors.critical.copy(alpha = 0.2f) else colors.border.copy(alpha = 0.3f)
                            ) {
                                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Box(
                                        modifier = Modifier.size(34.dp).background(
                                            if (detected) colors.criticalBg else colors.surfaceElevated, RoundedCornerShape(10.dp)
                                        ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(icon, null, tint = if (detected) colors.critical else colors.textMuted, modifier = Modifier.size(18.dp))
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(name, style = MaterialTheme.typography.labelSmall, color = if (detected) colors.critical else colors.textSecondary, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                        Text(if (detected) "DETECTED" else "Clear", style = MaterialTheme.typography.labelSmall,
                                            color = if (detected) colors.critical.copy(alpha = 0.7f) else colors.safe, fontSize = 9.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (activeSession != null) {
                SectionHeader(title = "Active Session Details")
                GlassCard(borderColor = colors.critical.copy(alpha = 0.2f)) {
                    Text(
                        text = activeSession.threatCategory,
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.critical,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = activeSession.correlationReason ?: "No details provided.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Recommended Action:",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textMuted
                        )
                        Text(
                            text = activeSession.recommendedAction,
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.warning,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                SectionHeader(title = "Active Session Details")
                GlassCard(borderColor = colors.safe.copy(alpha = 0.1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Security, null, tint = colors.safe, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "System Fully Secured",
                                style = MaterialTheme.typography.titleSmall,
                                color = colors.textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "No active threat sessions detected. All channels reporting clear.",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textMuted
                            )
                        }
                    }
                }
            }

            SectionHeader(title = "Channel Status")
            GlassCard {
                ChannelStatusRow("SMS Channel", if (nlsEnabled) "Monitoring notifications" else "Permissions required", nlsEnabled, colors.channelSms)
                Spacer(Modifier.height(12.dp))
                ChannelStatusRow("Call Channel", if (callPermission) "Listening for incoming calls" else "Microphone/Phone permissions required", callPermission, colors.channelCall)
                Spacer(Modifier.height(12.dp))
                ChannelStatusRow("Web Channel", if (vpnRunning) "VPN traffic analysis active" else "VPN service not running", vpnRunning, colors.channelWeb)
                Spacer(Modifier.height(12.dp))
                ChannelStatusRow("Email Channel", if (nlsEnabled) "Monitoring email notifications" else "Notification permission required", nlsEnabled, colors.channelEmail)
            }

            SectionHeader(title = "Actions")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        scope.launch {
                            haptics.heavyClick()
                            if (activeSession != null) {
                                fraudDao.insertThreatSession(activeSession.copy(resolved = true, recommendedAction = "BLOCKED"))
                                threatPhoneSource?.let { phone ->
                                    blockedRepo.addBlockedNumber(phone, "Correlated scam threat actor: ${activeSession.threatCategory}")
                                    Toast.makeText(context, "Blocked $phone and resolved session.", Toast.LENGTH_LONG).show()
                                } ?: run {
                                    Toast.makeText(context, "Resolved session (no phone number to block).", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }, 
                    modifier = Modifier.weight(1f),
                    enabled = activeSession != null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.critical,
                        disabledContainerColor = colors.critical.copy(alpha = 0.2f),
                        disabledContentColor = colors.textMuted
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Block, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(4.dp))
                    Text("Block", fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = {
                        scope.launch {
                            haptics.warning()
                            if (activeSession != null) {
                                val notifier = com.security.rakshakx.notifications.vpn.VpnProtectionNotifier(context)
                                notifier.createChannel()
                                notifier.notifyThreat(
                                    "Critical Threat Escalated",
                                    "Action requested for active threat session: ${activeSession.correlationReason}"
                                )
                                Toast.makeText(context, "Escalation warning triggered.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }, 
                    modifier = Modifier.weight(1f),
                    enabled = activeSession != null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.warning,
                        disabledContainerColor = colors.warning.copy(alpha = 0.2f),
                        disabledContentColor = colors.textMuted
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Warning, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(4.dp))
                    Text("Warn", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            haptics.tick()
                            if (activeSession != null) {
                                fraudDao.insertThreatSession(activeSession.copy(resolved = true, recommendedAction = "IGNORED"))
                                Toast.makeText(context, "Session dismissed.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }, 
                    modifier = Modifier.weight(1f),
                    enabled = activeSession != null,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = colors.textSecondary,
                        disabledContentColor = colors.textMuted
                    ),
                    border = ButtonDefaults.outlinedButtonBorder(activeSession != null).copy(
                        brush = androidx.compose.ui.graphics.SolidColor(
                            if (activeSession != null) colors.border else colors.border.copy(alpha = 0.2f)
                        )
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Ignore", fontWeight = FontWeight.Bold) }
            }

            SectionHeader(title = "Recent Threat Activity")
            if (recentThreats.isEmpty()) {
                GlassCard {
                    Text(
                        text = "No recent threats logged. Keep up the good work!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textMuted,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    recentThreats.forEach { entry ->
                        GlassCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(entry.channel.color.copy(alpha = 0.1f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = entry.channel.icon,
                                        contentDescription = null,
                                        tint = entry.channel.color,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = entry.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = colors.textPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = entry.severity.label,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = entry.severity.color,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = entry.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.textSecondary,
                                        maxLines = 2
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Source: ${entry.source}",
                                            modifier = Modifier.weight(1f),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = colors.textMuted,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        val timeStr = remember(entry.timestamp) {
                                            android.text.format.DateUtils.getRelativeTimeSpanString(
                                                entry.timestamp,
                                                System.currentTimeMillis(),
                                                android.text.format.DateUtils.MINUTE_IN_MILLIS
                                            ).toString()
                                        }
                                        Text(
                                            text = timeStr,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = colors.textMuted,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            RakshakXFooter()
        }
    }
}

@Composable
private fun ChannelStatusRow(
    name: String, 
    status: String, 
    isActive: Boolean,
    activeColor: androidx.compose.ui.graphics.Color
) {
    val colors = LocalRakshakXColors.current
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column {
            Text(name, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
            Text(status, style = MaterialTheme.typography.bodySmall, color = colors.textMuted)
        }
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(if (isActive) activeColor else colors.critical))
    }
}
