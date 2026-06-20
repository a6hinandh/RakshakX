package com.security.rakshakx.ui.screens

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.net.VpnService
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.security.rakshakx.ui.data.ThreatLogRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.security.rakshakx.core.modelupdate.ModelUpdateManager
import com.security.rakshakx.core.correlation.MitreAttackMapper

@Composable
fun SettingsScreen(activity: Activity, onBack: () -> Unit) {
    val context = LocalContext.current
    val colors = LocalRakshakXColors.current
    val haptics = rememberHaptics()
    val settingsStore = remember { SettingsStore.getInstance(context) }
    val scope = rememberCoroutineScope()

    val sensitivity by settingsStore.sensitivity.collectAsState()
    val autoDeleteDays by settingsStore.autoDeleteDays.collectAsState()

    val modelUpdateManager = remember { ModelUpdateManager.getInstance(context) }
    val modelVersion by modelUpdateManager.currentModelVersion.collectAsState()
    val ruleVersion by modelUpdateManager.currentRuleVersion.collectAsState()

    val packageInfo = remember {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, android.content.pm.PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
        } catch (_: Exception) {
            null
        }
    }
    val appVersionName = packageInfo?.versionName ?: "1.0"
    val appVersionCode = packageInfo?.let {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            it.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            it.versionCode.toLong()
        }
    } ?: 1L

    val nlsEnabled = remember {
        val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        flat?.contains(context.packageName) == true
    }
    val accessibilityEnabled = remember {
        try {
            val comp = ComponentName(context, "com.security.rakshakx.web.services.AccessibilityMonitorService")
            val enabled = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            enabled?.contains(comp.flattenToString()) == true
        } catch (_: Exception) { false }
    }
    val vpnRunning by VpnStatusStore.isRunning.collectAsState()

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
                title = "Settings",
                infoText = "Configure detection sensitivity, data retention, system access permissions, and manage your app data.",
                onBack = { haptics.tick(); onBack() }
            )

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

            // ── Data Retention ──
            SectionHeader(title = "Data Retention")
            GlassCard {
                Text("Auto-delete threat logs after:", style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(7, 14, 30, 90).forEach { days ->
                        FilterChip(
                            selected = autoDeleteDays == days,
                            onClick = {
                                haptics.tick()
                                settingsStore.setAutoDeleteDays(days)
                                scope.launch(Dispatchers.IO) {
                                    ThreatLogRepository.cleanOldLogs(context, days)
                                }
                            },
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

            // ── System Access ──
            SectionHeader(title = "System Access")
            GlassCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ClickablePermissionRow(
                        icon = Icons.Filled.Notifications,
                        title = "Notification Listener",
                        desc = "Required for SMS and email interception",
                        active = nlsEnabled,
                        onClick = {
                            try {
                                context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                            } catch (_: Exception) {}
                        }
                    )
                    
                    HorizontalDivider(color = colors.border.copy(alpha = 0.2f))
                    
                    ClickablePermissionRow(
                        icon = Icons.Filled.Accessibility,
                        title = "Accessibility Service",
                        desc = "Browser session monitoring for web protection",
                        active = accessibilityEnabled,
                        onClick = {
                            try {
                                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                            } catch (_: Exception) {}
                        }
                    )
                    
                    HorizontalDivider(color = colors.border.copy(alpha = 0.2f))
                    
                    ClickablePermissionRow(
                        icon = Icons.Filled.VpnLock,
                        title = "VPN Service",
                        desc = "DNS-level threat blocking layer",
                        active = vpnRunning,
                        onClick = {
                            try {
                                context.startActivity(Intent("android.settings.VPN_SETTINGS"))
                            } catch (_: Exception) {}
                        }
                    )
                }
            }

            // ── Database Security ──
            SectionHeader(title = "Database Security")
            GlassCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Encrypted Threat Database",
                            style = MaterialTheme.typography.titleSmall,
                            color = colors.textPrimary
                        )
                        Text(
                            text = "All threat events stored with SQLCipher AES-256. No cloud sync.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textMuted
                        )
                    }
                    StatusChip("Secure", colors.safe)
                }
            }

            // ── Storage & Reset ──
            SectionHeader(title = "Storage & Reset")
            GlassCard(borderColor = colors.critical.copy(alpha = 0.12f)) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Delete Application Data",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.critical,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "This will permanently delete all threat logs, call transcripts, saved scan results, and reset all app preferences to default.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textMuted
                    )
                    
                    var showConfirmDialog by remember { mutableStateOf(false) }
                    
                    Button(
                        onClick = { haptics.tick(); showConfirmDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.critical.copy(alpha = 0.15f), contentColor = colors.critical),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Delete, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Delete All Local Data", fontWeight = FontWeight.Bold)
                    }
                    
                    if (showConfirmDialog) {
                        AlertDialog(
                            onDismissRequest = { showConfirmDialog = false },
                            title = { Text("Confirm Data Deletion") },
                            text = { Text("Are you absolutely sure? This action is permanent and cannot be undone.") },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        showConfirmDialog = false
                                        scope.launch {
                                            settingsStore.clearAllData(context)
                                        }
                                    }
                                ) {
                                    Text("Delete Everything", color = colors.critical, fontWeight = FontWeight.Bold)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showConfirmDialog = false }) {
                                    Text("Cancel", color = colors.textMuted)
                                }
                            },
                            containerColor = colors.cardBackground,
                            titleContentColor = colors.textPrimary,
                            textContentColor = colors.textSecondary
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
                    "Privacy-preserving mobile endpoint protection utilizing on-device NLP classifiers, " +
                    "local speech-to-text transcription, DNS-level VPN sinkholing, and cross-channel " +
                    "threat correlation to defend against zero-day social engineering and network attacks " +
                    "without cloud telemetry.",
                    style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary
                )
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = colors.border.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(12.dp))
                
                AboutDetailRow(label = "App Version", value = "$appVersionName ($appVersionCode)")
                AboutDetailRow(label = "On-Device AI Model", value = "v$modelVersion")
                AboutDetailRow(label = "Rule Signature Engine", value = "v$ruleVersion")
                AboutDetailRow(label = "MITRE ATT&CK Mappings", value = "${MitreAttackMapper.getAllTechniques().size} Techniques Mapped")
                AboutDetailRow(label = "Threat Database", value = "SQLCipher AES-256")
                AboutDetailRow(label = "Developer", value = "InnovateX")
            }

            RakshakXFooter()
        }
    }
}

@Composable
private fun ClickablePermissionRow(
    icon: ImageVector,
    title: String,
    desc: String,
    active: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalRakshakXColors.current
    val haptics = rememberHaptics()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                haptics.tick()
                onClick()
            }
            .padding(vertical = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (active) colors.safe else colors.critical,
            modifier = Modifier.size(20.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
            Text(desc, style = MaterialTheme.typography.bodySmall, color = colors.textMuted)
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .background(
                        if (active) colors.safe.copy(alpha = 0.12f) else colors.critical.copy(alpha = 0.1f),
                        RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = if (active) "ACTIVE" else "INACTIVE",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (active) colors.safe else colors.critical,
                    fontWeight = FontWeight.Bold
                )
            }
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = colors.textMuted,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun AboutDetailRow(label: String, value: String) {
    val colors = LocalRakshakXColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textMuted,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textPrimary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

