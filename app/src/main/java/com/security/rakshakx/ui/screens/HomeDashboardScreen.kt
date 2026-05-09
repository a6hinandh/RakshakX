package com.security.rakshakx.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.security.rakshakx.call.CallMainActivity
import com.security.rakshakx.permissions.PermissionManager
import com.security.rakshakx.sms.SmsMainActivity
import com.security.rakshakx.ui.components.*
import com.security.rakshakx.ui.data.*
import com.security.rakshakx.ui.theme.*
import com.security.rakshakx.web.services.FraudVpnService
import com.security.rakshakx.web.utils.VpnStatusStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun HomeDashboardScreen(
    activity: Activity,
    onNavigateToThreats: () -> Unit,
    onNavigateToCorrelation: () -> Unit,
    onNavigateToLiveThreat: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val colors = LocalRakshakXColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val vpnRunning = VpnStatusStore.isRunning.collectAsState().value
    var readiness by remember { mutableStateOf(PermissionManager.getReadinessState(context)) }

    // Load real threats
    var threats by remember { mutableStateOf<List<ThreatLogEntry>>(emptyList()) }
    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            threats = try { ThreatLogRepository.getAllThreats(context) } catch (_: Exception) { emptyList() }
        }
        readiness = PermissionManager.getReadinessState(context)
    }

    val criticalCount = threats.count { it.severity == Severity.CRITICAL || it.severity == Severity.HIGH }
    val protectionLevel = when {
        criticalCount >= 3 -> ProtectionLevel.THREAT_DETECTED
        criticalCount >= 1 -> ProtectionLevel.ELEVATED
        else -> ProtectionLevel.PROTECTED
    }
    val securityScore = (100 - (criticalCount * 15) - (threats.count { it.severity == Severity.MEDIUM } * 5)).coerceIn(0, 100)

    val channelStatuses = listOf(
        ChannelStatus(Channel.SMS, isActive = readiness.smsReady, threatCount = threats.count { it.channel == Channel.SMS }),
        ChannelStatus(Channel.CALL, isActive = readiness.callReady, threatCount = threats.count { it.channel == Channel.CALL }),
        ChannelStatus(
            Channel.WEB,
            isActive = vpnRunning && readiness.webReady,
            threatCount = threats.count { it.channel == Channel.WEB }
        ),
        ChannelStatus(Channel.EMAIL, isActive = readiness.emailReady, threatCount = threats.count { it.channel == Channel.EMAIL }),
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // ── Header ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("🛡️ RakshakX", style = MaterialTheme.typography.headlineMedium, color = colors.primary, fontWeight = FontWeight.Bold)
                Text("Autonomous Fraud Interception", style = MaterialTheme.typography.bodySmall, color = colors.textMuted)
            }
        }

        // ── Shield Status Hero ──
        ShieldStatusCard(protectionLevel = protectionLevel, securityScore = securityScore)

        // ── Channel Shields Grid ──
        SectionHeader(title = "Protection Modules")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            channelStatuses.take(2).forEach { status ->
                ChannelShieldCard(
                    status = status,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        when (status.channel) {
                            Channel.SMS -> context.startActivity(Intent(context, SmsMainActivity::class.java))
                            Channel.CALL -> context.startActivity(Intent(context, CallMainActivity::class.java))
                            else -> {}
                        }
                    }
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            channelStatuses.drop(2).forEach { status ->
                ChannelShieldCard(
                    status = status,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        if (status.channel == Channel.WEB) onNavigateToSettings()
                    }
                )
            }
        }

        // ── Quick Actions ──
        SectionHeader(title = "Quick Actions")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            QuickActionButton(
                icon = Icons.Filled.Sms,
                label = "Analyze\nSMS",
                color = Cyan400,
                onClick = { context.startActivity(Intent(context, SmsMainActivity::class.java)) }
            )
            QuickActionButton(
                icon = Icons.Filled.Timeline,
                label = "Threat\nTimeline",
                color = OrangeWarn,
                onClick = onNavigateToCorrelation
            )
            QuickActionButton(
                icon = Icons.Filled.History,
                label = "View\nThreats",
                color = RedCritical,
                onClick = onNavigateToThreats
            )
            QuickActionButton(
                icon = Icons.Filled.RadioButtonChecked,
                label = "Live\nMonitor",
                color = GreenSafe,
                onClick = onNavigateToLiveThreat
            )
        }

        // ── Recent Threats ──
        if (threats.isNotEmpty()) {
            SectionHeader(
                title = "Recent Fraud Attempts",
                action = {
                    TextButton(onClick = onNavigateToThreats) {
                        Text("View All", color = colors.primary, style = MaterialTheme.typography.labelMedium)
                    }
                }
            )
            threats.take(3).forEach { entry ->
                ThreatCard(entry = entry)
            }
        } else {
            // Show demo prompt
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = colors.surfaceElevated)
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.Security, contentDescription = null, tint = colors.primary, modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("All Clear", style = MaterialTheme.typography.titleMedium, color = colors.safe)
                    Text("No threats detected. All channels are actively monitoring.", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                }
            }
        }

        // ── Privacy Footer ──
        SectionHeader(title = "Privacy Status")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PrivacyBadge(icon = Icons.Filled.Memory, text = "Local AI")
            PrivacyBadge(icon = Icons.Filled.CloudOff, text = "No Cloud")
            PrivacyBadge(icon = Icons.Filled.Lock, text = "Encrypted")
        }

        Spacer(modifier = Modifier.height(80.dp))  // Bottom nav clearance
    }
}
