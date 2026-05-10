package com.security.rakshakx.ui.screens

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.security.rakshakx.call.callanalysis.ui.RakshakXActivity
import com.security.rakshakx.permissions.PermissionManager
import com.security.rakshakx.sms.SmsMainActivity
import com.security.rakshakx.ui.components.*
import com.security.rakshakx.ui.data.*
import com.security.rakshakx.ui.theme.*
import com.security.rakshakx.web.services.FraudVpnService
import com.security.rakshakx.web.utils.VpnStatusStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.security.rakshakx.core.SettingsStore
import androidx.compose.ui.graphics.Brush

@Composable
fun HomeDashboardScreen(
    activity: Activity,
    onNavigateToThreats: () -> Unit,
    onNavigateToCorrelation: () -> Unit,
    onNavigateToLiveThreat: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToScanning: () -> Unit
) {
    val colors = LocalRakshakXColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsStore = remember { SettingsStore.getInstance(context) }
    
    val smsEnabled by settingsStore.smsEnabled.collectAsState()
    val callEnabled by settingsStore.callEnabled.collectAsState()
    val emailEnabled by settingsStore.emailEnabled.collectAsState()
    val vpnRunning by VpnStatusStore.isRunning.collectAsState()
    
    var readiness by remember { mutableStateOf(PermissionManager.getReadinessState(context)) }

    // Load real threats and perform cleanup
    var threats by remember { mutableStateOf<List<ThreatLogEntry>>(emptyList()) }
    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            val autoDeleteDaysVal = settingsStore.autoDeleteDays.value
            ThreatLogRepository.cleanOldLogs(context, autoDeleteDaysVal)
            threats = try { ThreatLogRepository.getAllThreats(context) } catch (_: Exception) { emptyList() }
        }
        readiness = PermissionManager.getReadinessState(context)
    }

    // Dynamic security score calculation
    val criticalCount = threats.count { it.severity == Severity.CRITICAL || it.severity == Severity.HIGH }
    val protectionLevel = when {
        criticalCount >= 3 -> ProtectionLevel.THREAT_DETECTED
        criticalCount >= 1 -> ProtectionLevel.ELEVATED
        else -> ProtectionLevel.PROTECTED
    }
    
    // Score based on active modules and threats
    val activeModulesCount = listOf(smsEnabled, callEnabled, emailEnabled, vpnRunning).count { it }
    val baseScore = 60 + (activeModulesCount * 10) // Max 100
    val securityScore = (baseScore - (criticalCount * 15) - (threats.count { it.severity == Severity.MEDIUM } * 5)).coerceIn(0, 100)

    val channelStatuses = listOf(
        ChannelStatus(Channel.SMS, isActive = smsEnabled && readiness.smsReady, threatCount = threats.count { it.channel == Channel.SMS }),
        ChannelStatus(Channel.CALL, isActive = callEnabled && readiness.callReady, threatCount = threats.count { it.channel == Channel.CALL }),
        ChannelStatus(
            Channel.WEB,
            isActive = vpnRunning && readiness.webReady,
            threatCount = threats.count { it.channel == Channel.WEB }
        ),
        ChannelStatus(Channel.EMAIL, isActive = emailEnabled && readiness.emailReady, threatCount = threats.count { it.channel == Channel.EMAIL }),
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0F172A), Color(0xFF1E293B))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // ── Premium Header ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "RakshakX",
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color.White
                    )
                    Text(
                        text = "Autonomous Fraud Interception",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
                IconButton(
                    onClick = onNavigateToSettings,
                    modifier = Modifier.background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                ) {
                    Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = Color.White)
                }
            }

            // ── Premium Hero: Shield Status ──
            PremiumShieldStatusCard(
                protectionLevel = protectionLevel,
                securityScore = securityScore,
                threatCount = threats.size
            )

            // ── Protection Modules Row ──
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    channelStatuses.forEach { status ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { onNavigateToSettings() }
                        ) {
                            val accentColor = when (status.channel) {
                                Channel.SMS -> Color(0xFF4776E6)
                                Channel.CALL -> Color(0xFF8E54E9)
                                Channel.WEB -> Color(0xFF10B981)
                                Channel.EMAIL -> Color(0xFFEF4444)
                            }
                            Box(contentAlignment = Alignment.Center) {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .background(
                                            if (status.isActive) accentColor.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f),
                                            shape = RoundedCornerShape(14.dp)
                                        )
                                )
                                Icon(
                                    imageVector = when (status.channel) {
                                        Channel.SMS -> Icons.Filled.Sms
                                        Channel.CALL -> Icons.Filled.Call
                                        Channel.WEB -> Icons.Filled.Language
                                        Channel.EMAIL -> Icons.Filled.Email
                                    },
                                    contentDescription = status.channel.name,
                                    tint = if (status.isActive) accentColor else Color.White.copy(alpha = 0.3f),
                                    modifier = Modifier.size(24.dp)
                                )
                                // Active indicator dot
                                if (status.isActive) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .align(Alignment.TopEnd)
                                            .offset(x = 2.dp, y = (-2).dp)
                                            .background(Color(0xFF10B981), CircleShape)
                                            .border(2.dp, Color(0xFF0F172A), CircleShape)
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                status.channel.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (status.isActive) Color.White else Color.White.copy(alpha = 0.4f)
                            )
                        }
                    }
                }
            }

            // ── Threat Intelligence ──
            SectionHeader(title = "THREAT INTELLIGENCE")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Scan Link
                Card(
                    onClick = onNavigateToScanning,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4776E6).copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFF4776E6).copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.QrCodeScanner, null, tint = Color(0xFF4776E6), modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Text("Scan Link", style = MaterialTheme.typography.titleSmall, color = Color.White)
                    }
                }

                // Live Feed
                Card(
                    onClick = onNavigateToLiveThreat,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF8E54E9).copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFF8E54E9).copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Radar, null, tint = Color(0xFF8E54E9), modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Text("Live Feed", style = MaterialTheme.typography.titleSmall, color = Color.White)
                    }
                }
            }

            // ── Recent Threats ──
            if (threats.isNotEmpty()) {
                SectionHeader(
                    title = "RECENT INTERCEPTIONS",
                    action = {
                        TextButton(onClick = onNavigateToThreats) {
                            Text("DETAILS", color = Color(0xFF4776E6), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                )
                threats.take(3).forEach { entry ->
                    ThreatCard(entry = entry)
                }
            } else {
                PremiumEmptyState(colors = colors)
            }

            RakshakXFooter()
        }
    }
}

@Composable
fun PremiumShieldStatusCard(
    protectionLevel: ProtectionLevel,
    securityScore: Int,
    threatCount: Int
) {
    val colors = LocalRakshakXColors.current
    val statusColor = when (protectionLevel) {
        ProtectionLevel.PROTECTED -> colors.safe
        ProtectionLevel.ELEVATED -> colors.warning
        ProtectionLevel.THREAT_DETECTED -> colors.critical
    }
    
    val statusText = when (protectionLevel) {
        ProtectionLevel.PROTECTED -> "SYSTEMS SECURE"
        ProtectionLevel.ELEVATED -> "ELEVATED RISK"
        ProtectionLevel.THREAT_DETECTED -> "BREACH DETECTED"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = Navy800),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFFFFF))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Abstract background glow
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .align(Alignment.CenterEnd)
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.radialGradient(
                            colors = listOf(statusColor.copy(alpha = 0.15f), Color.Transparent)
                        )
                    )
            )

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Large Score Ring
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(120.dp)
                ) {
                    CircularProgressIndicator(
                        progress = 1f,
                        modifier = Modifier.fillMaxSize(),
                        color = Color(0x1AFFFFFF),
                        strokeWidth = 8.dp,
                        trackColor = Color.Transparent
                    )
                    CircularProgressIndicator(
                        progress = securityScore / 100f,
                        modifier = Modifier.fillMaxSize(),
                        color = statusColor,
                        strokeWidth = 8.dp,
                        trackColor = Color.Transparent,
                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$securityScore",
                            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Black),
                            color = TextPrimary
                        )
                        Text(
                            text = "SCORE",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.width(24.dp))

                Column {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = statusColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (threatCount == 0) "No active threats detected" else "$threatCount threats neutralized",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(statusColor.copy(alpha = 0.1f), MaterialTheme.shapes.extraSmall)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(statusColor, androidx.compose.foundation.shape.CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "LIVE MONITOR ACTIVE",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = statusColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PremiumChannelCard(
    status: ChannelStatus,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colors = LocalRakshakXColors.current
    val icon = when (status.channel) {
        Channel.SMS -> Icons.Filled.Sms
        Channel.CALL -> Icons.Filled.Call
        Channel.WEB -> Icons.Filled.Language
        Channel.EMAIL -> Icons.Filled.Email
    }
    
    val description = when (status.channel) {
        Channel.SMS -> "AI-driven SMS intent analysis"
        Channel.CALL -> "Live call voice pattern monitor"
        Channel.WEB -> "Real-time phishing URL filter"
        Channel.EMAIL -> "Notification-level email guard"
    }
    
    val accentColor = when (status.channel) {
        Channel.SMS -> Color(0xFF4776E6)
        Channel.CALL -> Color(0xFF8E54E9)
        Channel.WEB -> Color(0xFF10B981)
        Channel.EMAIL -> Color(0xFFEF4444)
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (status.isActive) Color(0xFF1E293B) else Color(0xFF334155).copy(alpha = 0.3f),
        border = if (status.isActive) androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.3f)) else null,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        if (status.isActive) accentColor.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (status.isActive) accentColor else Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "${status.channel.name} Shield",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }

            if (status.isActive) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Active",
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Icon(
                    Icons.Default.Cancel,
                    contentDescription = "Inactive",
                    tint = Color.White.copy(alpha = 0.3f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}


@Composable
fun PremiumEmptyState(colors: RakshakXColors) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = colors.safe.copy(alpha = 0.2f),
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            "NO THREATS DETECTED",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = colors.safe
        )
        Text(
            "Your digital space is clean.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary
        )
    }
}

