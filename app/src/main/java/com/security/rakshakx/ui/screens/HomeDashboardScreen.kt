package com.security.rakshakx.ui.screens

import android.app.Activity
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(Navy900, Color(0xFF0A0F1D))
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
                        text = "RAKSHAKX",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = androidx.compose.ui.unit.TextUnit.Unspecified
                        ),
                        color = colors.primary
                    )
                    Text(
                        text = "NEURAL FRAUD INTERCEPTION",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textMuted,
                        letterSpacing = 2.sp
                    )
                }
                IconButton(
                    onClick = onNavigateToSettings,
                    modifier = Modifier.background(GlassBg, MaterialTheme.shapes.small)
                ) {
                    Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = colors.textSecondary)
                }
            }

            // ── Premium Hero: Shield Status ──
            PremiumShieldStatusCard(
                protectionLevel = protectionLevel,
                securityScore = securityScore,
                threatCount = threats.size
            )

            // ── Protection Modules Grid ──
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                SectionHeader(title = "CYBER DEFENSE MODULES")
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    channelStatuses.take(2).forEach { status ->
                        PremiumChannelCard(
                            status = status,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                when (status.channel) {
                                    Channel.SMS -> context.startActivity(Intent(context, SmsMainActivity::class.java))
                                    Channel.CALL -> context.startActivity(Intent(context, RakshakXActivity::class.java))
                                    else -> {}
                                }
                            }
                        )
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    channelStatuses.drop(2).forEach { status ->
                        PremiumChannelCard(
                            status = status,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                if (status.channel == Channel.WEB) onNavigateToSettings()
                            }
                        )
                    }
                }
            }

            // ── Quick Intelligence ──
            SectionHeader(title = "THREAT INTELLIGENCE")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf(
                    Triple(Icons.Filled.Security, "Scan Link", onNavigateToSettings),
                    Triple(Icons.Filled.History, "Timeline", onNavigateToCorrelation),
                    Triple(Icons.Filled.Radar, "Live Feed", onNavigateToLiveThreat),
                    Triple(Icons.Filled.Shield, "Verify", onNavigateToThreats)
                ).forEach { (icon, label, action) ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(70.dp)
                    ) {
                        IconButton(
                            onClick = action,
                            modifier = Modifier
                                .size(56.dp)
                                .background(GlassBg, MaterialTheme.shapes.medium)
                        ) {
                            Icon(icon, contentDescription = label, tint = colors.primary, modifier = Modifier.size(24.dp))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(label, style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                    }
                }
            }

            // ── Web Scanner (Modernized) ──
            PremiumWebScannerCard(activity = activity, colors = colors)

            // ── Recent Threats ──
            if (threats.isNotEmpty()) {
                SectionHeader(
                    title = "RECENT INTERCEPTIONS",
                    action = {
                        TextButton(onClick = onNavigateToThreats) {
                            Text("DETAILS", color = colors.primary, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                )
                threats.take(3).forEach { entry ->
                    ThreatCard(entry = entry)
                }
            } else {
                PremiumEmptyState(colors = colors)
            }

            Spacer(modifier = Modifier.height(100.dp))
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
    
    val tint = if (status.isActive) colors.primary else colors.textMuted

    Card(
        onClick = onClick,
        modifier = modifier.height(110.dp),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = Navy800),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (status.isActive) colors.primary.copy(alpha = 0.2f) else Color(0x1AFFFFFF)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
                if (status.threatCount > 0) {
                    Box(
                        modifier = Modifier
                            .background(colors.critical.copy(alpha = 0.2f), androidx.compose.foundation.shape.CircleShape)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${status.threatCount}",
                            color = colors.critical,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
            
            Column {
                Text(
                    text = status.channel.name,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
                Text(
                    text = if (status.isActive) "ACTIVE" else "INACTIVE",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = if (status.isActive) colors.safe else colors.textMuted
                )
            }
        }
    }
}

@Composable
fun PremiumWebScannerCard(activity: Activity, colors: RakshakXColors) {
    var manualUrl by remember { mutableStateOf("") }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = Navy800),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x1AFFFFFF))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "MANUAL INTERCEPTION",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            OutlinedTextField(
                value = manualUrl,
                onValueChange = { manualUrl = it },
                placeholder = { Text("Paste suspicious URL here", color = colors.textMuted) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.primary,
                    unfocusedBorderColor = Color(0x1AFFFFFF),
                    focusedContainerColor = Navy900,
                    unfocusedContainerColor = Navy900,
                )
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = {
                        if (manualUrl.isNotBlank()) {
                            val intent = Intent(activity, com.security.rakshakx.web.ui.UrlScanActivity::class.java).apply {
                                putExtra("EXTRA_URL", manualUrl)
                            }
                            activity.startActivity(intent)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text("SCAN URL", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Color.Black))
                }
                IconButton(
                    onClick = {
                        activity.startActivity(Intent(activity, com.security.rakshakx.web.ui.QrScannerActivity::class.java))
                    },
                    modifier = Modifier
                        .background(GlassBg, MaterialTheme.shapes.medium)
                        .size(48.dp)
                ) {
                    Icon(Icons.Filled.QrCodeScanner, contentDescription = null, tint = colors.primary)
                }
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

