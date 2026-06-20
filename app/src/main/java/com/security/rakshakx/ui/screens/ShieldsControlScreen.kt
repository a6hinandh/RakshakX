package com.security.rakshakx.ui.screens

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.security.rakshakx.core.SettingsStore
import com.security.rakshakx.ui.anim.StaggeredEntry
import com.security.rakshakx.ui.anim.rememberHaptics
import com.security.rakshakx.ui.components.*
import com.security.rakshakx.ui.data.Channel
import com.security.rakshakx.ui.data.ThreatLogRepository
import com.security.rakshakx.ui.theme.*
import com.security.rakshakx.permissions.PermissionManager
import com.security.rakshakx.web.services.FraudVpnService
import com.security.rakshakx.web.utils.VpnStatusStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ShieldsControlScreen(
    activity: Activity,
    onNavigateToPrivacyDashboard: () -> Unit,
    onBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val colors = LocalRakshakXColors.current
    val haptics = rememberHaptics()
    val scope = rememberCoroutineScope()
    val settingsStore = remember { SettingsStore.getInstance(context) }

    val smsEnabled by settingsStore.smsEnabled.collectAsState()
    val callEnabled by settingsStore.callEnabled.collectAsState()
    val emailEnabled by settingsStore.emailEnabled.collectAsState()
    val vpnRunning by VpnStatusStore.isRunning.collectAsState()

    var threatCounts by remember { mutableStateOf(mapOf<Channel, Int>()) }

    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            val threats = try {
                ThreatLogRepository.getAllThreats(context)
            } catch (_: Exception) {
                emptyList()
            }
            val counts = Channel.entries.associateWith { channel ->
                threats.count { it.channel == channel }
            }
            threatCounts = counts
        }
    }

    val activeCount = listOf(smsEnabled, callEnabled, emailEnabled, vpnRunning).count { it }
    var showAccessibilityDialog by remember { mutableStateOf(false) }

    val vpnLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            FraudVpnService.start(activity.applicationContext)
            settingsStore.setWebEnabled(true)
        }
    }

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

            // Header Row
            PageHeader(
                title = "Shield Center",
                infoText = "Manage your core defense modules. Toggle SMS, Call, Web, and Email shields on or off. Tap the glowing Pulse Core button on any card to activate or pause a shield.",
                onBack = if (onBack != null) { { haptics.tick(); onBack() } } else null
            )

            // Summary Dashboard Card
            StaggeredEntry(index = 0) {
                GlassCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .background(
                                    if (activeCount == 4) colors.safeBg else colors.warningBg,
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Shield,
                                contentDescription = null,
                                tint = if (activeCount == 4) colors.safe else colors.warning,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "System Defense Status",
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = when (activeCount) {
                                    4 -> "Maximum Protection Enabled"
                                    0 -> "All Shields Disabled — Device Vulnerable"
                                    else -> "Partial Protection — $activeCount of 4 active"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (activeCount == 4) colors.safe else colors.textMuted
                            )
                        }

                        // Status Badge
                        Box(
                            modifier = Modifier
                                .background(
                                    if (activeCount == 4) colors.safe.copy(alpha = 0.12f) else colors.warning.copy(alpha = 0.12f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "$activeCount/4 ON",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (activeCount == 4) colors.safe else colors.warning,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Section Info Banner
            StaggeredEntry(index = 1, baseDelayMs = 60) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surfaceElevated.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Tap the glowing Pulse Core button on any card to activate or pause the shield.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary
                        )
                    }
                }
            }

            // Tracker Audit Quick Action (Visually Prominent)
            StaggeredEntry(index = 2, baseDelayMs = 70) {
                GlassCard(borderColor = colors.warning.copy(alpha = 0.3f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .background(colors.warning.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.TrackChanges,
                                contentDescription = null,
                                tint = colors.warning,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Tracker Audit",
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Audit installed applications for trackers, adware, and privacy telemetry.",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textMuted
                            )
                        }

                        Button(
                            onClick = {
                                haptics.click()
                                onNavigateToPrivacyDashboard()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.warning.copy(alpha = 0.15f),
                                contentColor = colors.warning
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Audit", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }

            // Shields list header
            StaggeredEntry(index = 3, baseDelayMs = 80) {
                SectionHeader(title = "Core Defense Tunnels")
            }

            // Shields cards
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // SMS Shield
                StaggeredEntry(index = 4, baseDelayMs = 90) {
                    ShieldMitigationCard(
                        title = "SMS Shield",
                        description = "NLP classification for phishing links, OTP fraud, and financial scams.",
                        mitreLabel = "MITRE T1660 — Phishing",
                        icon = Icons.Filled.Sms,
                        color = colors.channelSms,
                        isActive = smsEnabled,
                        threatCount = threatCounts[Channel.SMS] ?: 0,
                        onClickCore = {
                            settingsStore.setSmsEnabled(!smsEnabled)
                        }
                    )
                }

                // Call Shield
                StaggeredEntry(index = 5, baseDelayMs = 100) {
                    ShieldMitigationCard(
                        title = "Call Shield",
                        description = "On-device transcription and Voice Threat NLP checks to detect scam patterns.",
                        mitreLabel = "MITRE T1660.001 — Spearphishing Voice",
                        icon = Icons.Filled.Call,
                        color = colors.channelCall,
                        isActive = callEnabled,
                        threatCount = threatCounts[Channel.CALL] ?: 0,
                        onClickCore = {
                            settingsStore.setCallEnabled(!callEnabled)
                        }
                    )
                }

                // Web Shield (VPN Service based)
                StaggeredEntry(index = 6, baseDelayMs = 110) {
                    ShieldMitigationCard(
                        title = "Web Shield",
                        description = "DNS-level phishing URL mitigation, sandbox domain risk lookup, and tracker audits.",
                        mitreLabel = "MITRE T1659 — Content Injection",
                        icon = Icons.Filled.Language,
                        color = colors.channelWeb,
                        isActive = vpnRunning,
                        threatCount = threatCounts[Channel.WEB] ?: 0,
                        onClickCore = {
                            if (!vpnRunning) {
                                if (!PermissionManager.isAccessibilityEnabled(context)) {
                                    showAccessibilityDialog = true
                                } else {
                                    val intent = VpnService.prepare(activity)
                                    if (intent != null) {
                                        vpnLauncher.launch(intent)
                                    } else {
                                        FraudVpnService.start(activity.applicationContext)
                                        settingsStore.setWebEnabled(true)
                                    }
                                }
                            } else {
                                FraudVpnService.stop(activity.applicationContext)
                                settingsStore.setWebEnabled(false)
                            }
                        }
                    )
                }

                // Email Shield
                StaggeredEntry(index = 7, baseDelayMs = 120) {
                    ShieldMitigationCard(
                        title = "Email Shield",
                        description = "Interception of notification headers to identify spam and phishing sender profiles.",
                        mitreLabel = "MITRE T1566 — Phishing",
                        icon = Icons.Filled.Email,
                        color = colors.channelEmail,
                        isActive = emailEnabled,
                        threatCount = threatCounts[Channel.EMAIL] ?: 0,
                        onClickCore = {
                            settingsStore.setEmailEnabled(!emailEnabled)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        if (showAccessibilityDialog) {
            AlertDialog(
                onDismissRequest = { showAccessibilityDialog = false },
                icon = { Icon(Icons.Filled.Accessibility, null, tint = Emerald) },
                title = {
                    Text(
                        "Accessibility Required",
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                },
                text = {
                    Text(
                        "Web Shield requires the Accessibility Service to monitor browser URLs and block phishing sites. Please enable it to continue.",
                        color = colors.textSecondary
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        showAccessibilityDialog = false
                        try {
                            context.startActivity(Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS))
                        } catch (_: Exception) {}
                    }) {
                        Text("Enable", color = colors.primary, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAccessibilityDialog = false }) {
                        Text("Cancel", color = colors.textMuted)
                    }
                },
                containerColor = colors.cardBackground,
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}

@Composable
private fun ShieldMitigationCard(
    title: String,
    description: String,
    mitreLabel: String,
    icon: ImageVector,
    color: Color,
    isActive: Boolean,
    threatCount: Int,
    onClickCore: () -> Unit
) {
    val colors = LocalRakshakXColors.current

    GlassCard(
        borderColor = if (isActive) color.copy(alpha = 0.35f) else colors.border.copy(alpha = 0.2f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Premium Pulse Core Indicator
            ShieldPulseCore(
                isActive = isActive,
                color = color,
                icon = icon,
                onClick = onClickCore
            )

            // Info Content
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    
                    // Small Active Badge
                    Box(
                        modifier = Modifier
                            .background(
                                if (isActive) colors.safe.copy(alpha = 0.12f) else colors.critical.copy(alpha = 0.1f),
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isActive) "MONITORING" else "PAUSED",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isActive) colors.safe else colors.critical,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.scale(0.85f)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textMuted,
                    maxLines = 3
                )
                
                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = mitreLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.primary.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (threatCount > 0) {
                        Box(
                            modifier = Modifier
                                .background(colors.critical.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "$threatCount Intercepted",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.critical,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    } else {
                        Text(
                            text = "No threats detected",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.safe,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ShieldPulseCore(
    isActive: Boolean,
    color: Color,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = rememberHaptics()
    
    // Breathing scale and alpha animations for active core
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isActive) 1.08f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "coreScale"
    )
    
    val ringScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isActive) 1.45f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = EaseOutQuad),
            repeatMode = RepeatMode.Restart
        ),
        label = "ringScale"
    )
    
    val ringAlpha by infiniteTransition.animateFloat(
        initialValue = if (isActive) 0.4f else 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = EaseOutQuad),
            repeatMode = RepeatMode.Restart
        ),
        label = "ringAlpha"
    )

    Box(
        modifier = modifier
            .size(72.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                haptics.tick()
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        // Pulsing background ring
        if (isActive) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .scale(ringScale)
                    .background(color.copy(alpha = ringAlpha), CircleShape)
                    .border(1.dp, color.copy(alpha = ringAlpha), CircleShape)
            )
        }

        // Inner glowing core
        Box(
            modifier = Modifier
                .size(54.dp)
                .scale(scale)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            if (isActive) color else color.copy(alpha = 0.15f),
                            if (isActive) color.copy(alpha = 0.4f) else color.copy(alpha = 0.05f)
                        )
                    ),
                    shape = CircleShape
                )
                .border(
                    width = 1.5.dp,
                    color = if (isActive) color else color.copy(alpha = 0.3f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isActive) Color.White else color.copy(alpha = 0.6f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
