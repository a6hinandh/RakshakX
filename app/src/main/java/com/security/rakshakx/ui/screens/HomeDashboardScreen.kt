package com.security.rakshakx.ui.screens

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.security.rakshakx.core.SettingsStore
import com.security.rakshakx.permissions.PermissionManager
import com.security.rakshakx.ui.anim.StaggeredEntry
import com.security.rakshakx.ui.anim.animateIntCounter
import com.security.rakshakx.ui.anim.breathingScale
import com.security.rakshakx.ui.anim.rememberHaptics
import com.security.rakshakx.ui.components.*
import com.security.rakshakx.ui.data.*
import com.security.rakshakx.ui.theme.*
import com.security.rakshakx.web.utils.VpnStatusStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun HomeDashboardScreen(
    activity: Activity,
    onNavigateToThreats: () -> Unit,
    onNavigateToCorrelation: () -> Unit,
    onNavigateToLiveThreat: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToShieldsControl: () -> Unit,
    onNavigateToScanning: () -> Unit,
    onNavigateToReport: () -> Unit = {},
    onNavigateToThreatIntel: () -> Unit = {},
    onNavigateToDeviceHealth: () -> Unit = {},
    onNavigateToAttackMatrix: () -> Unit = {}
) {
    val colors = LocalRakshakXColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptics = rememberHaptics()
    val settingsStore = remember { SettingsStore.getInstance(context) }

    val smsEnabled by settingsStore.smsEnabled.collectAsState()
    val callEnabled by settingsStore.callEnabled.collectAsState()
    val emailEnabled by settingsStore.emailEnabled.collectAsState()
    val vpnRunning by VpnStatusStore.isRunning.collectAsState()

    var readiness by remember { mutableStateOf(PermissionManager.getReadinessState(context)) }
    var threats by remember { mutableStateOf<List<ThreatLogEntry>>(emptyList()) }

    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            val autoDeleteDaysVal = settingsStore.autoDeleteDays.value
            ThreatLogRepository.cleanOldLogs(context, autoDeleteDaysVal)
            threats = try { ThreatLogRepository.getAllThreats(context) } catch (_: Exception) { emptyList() }
        }
        readiness = PermissionManager.getReadinessState(context)
    }

    val criticalCount = threats.count { it.severity == Severity.CRITICAL || it.severity == Severity.HIGH }
    val protectionLevel = when {
        criticalCount >= 3 -> ProtectionLevel.THREAT_DETECTED
        criticalCount >= 1 -> ProtectionLevel.ELEVATED
        else               -> ProtectionLevel.PROTECTED
    }

    val activeModulesCount = listOf(smsEnabled, callEnabled, emailEnabled, vpnRunning).count { it }
    val baseScore = 60 + (activeModulesCount * 10)
    val securityScore = (baseScore - (criticalCount * 15) - (threats.count { it.severity == Severity.MEDIUM } * 5)).coerceIn(0, 100)

    val channelStatuses = listOf(
        ChannelStatus(Channel.SMS,   isActive = smsEnabled && readiness.smsReady,   threatCount = threats.count { it.channel == Channel.SMS }),
        ChannelStatus(Channel.CALL,  isActive = callEnabled && readiness.callReady,  threatCount = threats.count { it.channel == Channel.CALL }),
        ChannelStatus(Channel.WEB,   isActive = vpnRunning && readiness.webReady,   threatCount = threats.count { it.channel == Channel.WEB }),
        ChannelStatus(Channel.EMAIL, isActive = emailEnabled && readiness.emailReady, threatCount = threats.count { it.channel == Channel.EMAIL }),
    )

    PremiumBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .statusBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // ── Header (index 0) ──
            StaggeredEntry(index = 0) {
                PageHeader(
                    title = "RakshakX",
                    infoText = "Your home dashboard showing protection status, active signal monitors, security tools, and recent threat activity across all channels.",
                    trailing = {
                        HeaderIconButton(Icons.Outlined.GridOn, "ATT&CK", onNavigateToAttackMatrix)
                        HeaderIconButton(Icons.Outlined.Assessment, "Report", onNavigateToReport)
                        HeaderIconButton(Icons.Outlined.Settings, "Settings", onNavigateToSettings)
                    }
                )
            }

            // ── Protection Status (index 1) ──
            StaggeredEntry(index = 1, baseDelayMs = 80) {
                AnimatedShieldStatusCard(
                    protectionLevel = protectionLevel,
                    securityScore = securityScore
                )
            }

            // ── Signal Summary (index 2) ──
            StaggeredEntry(index = 2, baseDelayMs = 80) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionHeader(title = "Active Signals")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        channelStatuses.forEach { status ->
                            SignalPill(
                                status = status,
                                onClick = onNavigateToShieldsControl,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // ── Tools (index 3) ──
            StaggeredEntry(index = 3, baseDelayMs = 80) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionHeader(title = "Security Tools")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ToolCard(
                            title = "Scan Link",
                            subtitle = "URL & QR threat analysis",
                            icon = Icons.Filled.QrCodeScanner,
                            accentColor = colors.primary,
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToScanning
                        )
                        ToolCard(
                            title = "Live Monitor",
                            subtitle = "Real-time threat feed",
                            icon = Icons.Filled.Radar,
                            accentColor = colors.primaryVariant,
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToLiveThreat
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ToolCard(
                            title = "Device Health",
                            subtitle = "Integrity & posture check",
                            icon = Icons.Filled.PhoneAndroid,
                            accentColor = Emerald,
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToDeviceHealth
                        )
                        ToolCard(
                            title = "ATT&CK Matrix",
                            subtitle = "MITRE technique coverage",
                            icon = Icons.Filled.GridOn,
                            accentColor = Amber,
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToAttackMatrix
                        )
                    }
                }
            }

            // ── Recent Activity (index 4) ──
            StaggeredEntry(index = 4, baseDelayMs = 80) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionHeader(
                        title = "Recent Activity",
                        action = {
                            if (threats.isNotEmpty()) {
                                TextButton(onClick = onNavigateToThreats) {
                                    Text("View all", color = colors.primary, style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    )
                    if (threats.isNotEmpty()) {
                        threats.take(3).forEachIndexed { i, entry ->
                            StaggeredEntry(index = 5 + i, baseDelayMs = 80) {
                                ThreatCard(entry = entry)
                            }
                        }
                    } else {
                        EmptyState(
                            icon = Icons.Filled.CheckCircle,
                            title = "All clear",
                            description = "No threats detected across all channels"
                        )
                    }
                }
            }

            RakshakXFooter()
        }
    }
}

// Shield card with animated counter for the score
@Composable
private fun AnimatedShieldStatusCard(
    protectionLevel: ProtectionLevel,
    securityScore: Int
) {
    val animatedScore = animateIntCounter(target = securityScore, durationMs = 900)
    ShieldStatusCard(
        protectionLevel = protectionLevel,
        securityScore = animatedScore
    )
}

@Composable
private fun HeaderIconButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    val colors = LocalRakshakXColors.current
    val haptics = rememberHaptics()
    IconButton(
        onClick = { haptics.tick(); onClick() },
        modifier = Modifier.size(36.dp)
    ) {
        Icon(icon, label, tint = colors.textMuted, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun SignalPill(
    status: ChannelStatus,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalRakshakXColors.current
    val channelColor = if (status.isActive) status.channel.color else colors.textMuted.copy(alpha = 0.5f)
    val dotScale = if (status.isActive) breathingScale() else 1f

    val haptics = rememberHaptics()
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(colors.cardBackground)
            .clickable { haptics.tick(); onClick() }
            .padding(vertical = 12.dp, horizontal = 8.dp)
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            Icon(
                imageVector = status.channel.icon,
                contentDescription = status.channel.label,
                tint = channelColor,
                modifier = Modifier.size(22.dp)
            )
            if (status.isActive) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .scale(dotScale)
                        .clip(CircleShape)
                        .background(colors.safe)
                        .offset(x = 2.dp, y = (-2).dp)
                )
            }
        }
        Text(
            text = status.channel.name,
            style = MaterialTheme.typography.labelSmall,
            color = if (status.isActive) colors.textSecondary else colors.textMuted.copy(alpha = 0.5f)
        )
    }
}

@Composable
private fun ToolCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colors = LocalRakshakXColors.current
    GlassSurface(
        modifier = modifier.height(110.dp),
        onClick = onClick,
        borderColor = colors.border
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(accentColor.copy(alpha = 0.10f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = accentColor, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(title, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = colors.textMuted, maxLines = 1)
        }
    }
}

@Composable
fun PremiumShieldStatusCard(
    protectionLevel: ProtectionLevel,
    securityScore: Int,
    threatCount: Int
) {
    ShieldStatusCard(protectionLevel = protectionLevel, securityScore = securityScore)
}

@Composable
fun PremiumChannelCard(
    status: ChannelStatus,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    ChannelShieldCard(status = status, modifier = modifier, onClick = onClick)
}

@Composable
fun PremiumEmptyState(colors: RakshakXColors) {
    EmptyState(
        icon = Icons.Filled.CheckCircle,
        title = "All clear",
        description = "No threats detected across all channels"
    )
}
