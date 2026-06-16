package com.security.rakshakx.ui.screens

import android.app.Activity
import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.security.rakshakx.core.SettingsStore
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import com.security.rakshakx.ui.anim.rememberHaptics
import com.security.rakshakx.ui.components.*
import com.security.rakshakx.ui.theme.*
import com.security.rakshakx.web.services.FraudVpnService
import com.security.rakshakx.web.utils.VpnStatusStore

@Composable
fun SettingsScreen(activity: Activity, onBack: () -> Unit) {
    val context = LocalContext.current
    val colors = LocalRakshakXColors.current
    val haptics = rememberHaptics()
    val settingsStore = remember { SettingsStore.getInstance(context) }

    val smsEnabled by settingsStore.smsEnabled.collectAsState()
    val callEnabled by settingsStore.callEnabled.collectAsState()
    val emailEnabled by settingsStore.emailEnabled.collectAsState()
    val sensitivity by settingsStore.sensitivity.collectAsState()
    val autoDeleteDays by settingsStore.autoDeleteDays.collectAsState()
    val vpnRunning by VpnStatusStore.isRunning.collectAsState()

    val vpnLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { haptics.tick(); onBack() },
                    modifier = Modifier.size(40.dp).background(colors.surfaceElevated, RoundedCornerShape(12.dp))
                ) {
                    Icon(Icons.Filled.ArrowBack, null, tint = colors.textPrimary, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text("Settings", style = MaterialTheme.typography.headlineSmall, color = colors.textPrimary, fontWeight = FontWeight.Bold)
                    Text("Configure protection preferences", style = MaterialTheme.typography.bodySmall, color = colors.textMuted)
                }
            }

            // ── Detection Sensitivity ──
            SectionHeader(title = "Detection Sensitivity")
            GlassCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Threshold", style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                    val sensitivityLabel = when { sensitivity >= 0.7f -> "Aggressive"; sensitivity >= 0.4f -> "Balanced"; else -> "Permissive" }
                    val sensitivityColor = when { sensitivity >= 0.7f -> colors.critical; sensitivity >= 0.4f -> colors.warning; else -> colors.safe }
                    AnimatedContent(
                        targetState = sensitivityLabel,
                        transitionSpec = {
                            (fadeIn(tween(200)) + slideInVertically(tween(200)) { -it / 2 }) togetherWith
                                (fadeOut(tween(150)) + slideOutVertically(tween(150)) { it / 2 })
                        },
                        label = "sensitivityLabel"
                    ) { label ->
                        Text(label, style = MaterialTheme.typography.labelLarge, color = sensitivityColor)
                    }
                }
                Slider(
                    value = sensitivity,
                    onValueChange = { settingsStore.setSensitivity(it) },
                    colors = SliderDefaults.colors(
                        thumbColor = colors.primary,
                        activeTrackColor = colors.primary,
                        inactiveTrackColor = colors.border.copy(alpha = 0.3f)
                    )
                )
                Text("Higher sensitivity catches more threats but may produce more alerts.",
                    style = MaterialTheme.typography.bodySmall, color = colors.textMuted)
            }

            // ── Channel Toggles ──
            SectionHeader(title = "Channel Monitoring")
            SettingsToggleItem(Icons.Filled.Sms, "SMS Shield", "Real-time SMS fraud interception", smsEnabled, { settingsStore.setSmsEnabled(it) }, colors.channelSms)
            SettingsToggleItem(Icons.Filled.Call, "Call Shield", "Live call voice pattern analysis", callEnabled, { settingsStore.setCallEnabled(it) }, colors.channelCall)
            SettingsToggleItem(Icons.Filled.Language, "Web Shield", "VPN-based phishing URL filter", vpnRunning, { enable ->
                if (enable) {
                    val intent = VpnService.prepare(activity)
                    if (intent != null) vpnLauncher.launch(intent) else { FraudVpnService.start(activity.applicationContext); settingsStore.setWebEnabled(true) }
                } else { FraudVpnService.stop(activity.applicationContext); settingsStore.setWebEnabled(false) }
            }, colors.channelWeb)
            SettingsToggleItem(Icons.Filled.Email, "Email Shield", "Notification-level email analysis", emailEnabled, { settingsStore.setEmailEnabled(it) }, colors.channelEmail)

            // ── Data Retention ──
            SectionHeader(title = "Data Retention")
            GlassCard {
                Text("Auto-delete threat logs after:", style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(7, 14, 30, 90).forEach { days ->
                        FilterChip(
                            selected = autoDeleteDays == days,
                            onClick = { haptics.tick(); settingsStore.setAutoDeleteDays(days) },
                            label = { Text("${days}d") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colors.primaryMuted,
                                selectedLabelColor = colors.primary,
                                containerColor = colors.surfaceElevated,
                                labelColor = colors.textMuted
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }

            // ── About ──
            SectionHeader(title = "About")
            GlassCard(borderColor = colors.gold.copy(alpha = 0.12f)) {
                Text("RakshakX", style = MaterialTheme.typography.headlineSmall, color = colors.gold, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    "Privacy-first AI guardian that intercepts scams across calls, SMS, email, and web. " +
                    "All analysis runs on-device — your data never leaves your phone.",
                    style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary
                )
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = colors.border.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(10.dp))
                Text("Version 2.0.0 • On-Device Neural Engine", style = MaterialTheme.typography.labelSmall, color = colors.textMuted)
                Text("Developed by InnovateX", style = MaterialTheme.typography.labelLarge, color = colors.primaryVariant)
            }

            RakshakXFooter()
        }
    }
}

@Composable
private fun SettingsToggleItem(
    icon: ImageVector,
    title: String,
    description: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    iconColor: Color
) {
    val colors = LocalRakshakXColors.current
    val haptics = rememberHaptics()
    GlassSurface {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier.size(40.dp).background(iconColor.copy(alpha = 0.08f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = iconColor, modifier = Modifier.size(20.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                Text(description, style = MaterialTheme.typography.bodySmall, color = colors.textMuted)
            }
            Switch(
                checked = isChecked,
                onCheckedChange = { enabled ->
                    if (enabled) haptics.toggleOn() else haptics.toggleOff()
                    onCheckedChange(enabled)
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = TextWhite,
                    checkedTrackColor = iconColor,
                    uncheckedThumbColor = TextMuted,
                    uncheckedTrackColor = colors.border
                )
            )
        }
    }
}
