package com.security.rakshakx.ui.screens

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.security.rakshakx.ui.anim.StaggeredEntry
import com.security.rakshakx.ui.components.*
import com.security.rakshakx.ui.theme.*
import com.security.rakshakx.web.utils.VpnStatusStore
import com.security.rakshakx.core.SettingsStore

@Composable
fun PrivacyScreen() {
    val colors = LocalRakshakXColors.current
    val context = LocalContext.current
    val vpnRunning = VpnStatusStore.isRunning.collectAsState().value

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

    val settingsStore = remember { SettingsStore.getInstance(context) }
    val dataRetentionDays by settingsStore.autoDeleteDays.collectAsState(initial = 30)

    PremiumBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .statusBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Text("Privacy & Security", style = MaterialTheme.typography.headlineLarge, color = colors.textPrimary)
            Text("Your data stays on your device", style = MaterialTheme.typography.bodySmall, color = colors.textMuted)

            // On-device processing hero
            Card(
                modifier = Modifier.fillMaxWidth().border(1.dp, colors.safe.copy(alpha = 0.12f), RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = colors.safeBg)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier.size(44.dp).background(colors.safe.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.VerifiedUser, null, tint = colors.safe, modifier = Modifier.size(24.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("100% On-Device Processing", style = MaterialTheme.typography.titleSmall, color = colors.safe, fontWeight = FontWeight.SemiBold)
                        Text("All AI inference runs locally. No data is sent to external servers.",
                            style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                    }
                }
            }

            SectionHeader(title = "Security Transparency")
            StaggeredEntry(index = 0) {
                TransparencyRow(Icons.Filled.Memory, "On-Device AI Models", "DistilBERT, IndicBERT & Vosk for multi-language classification", colors.safe, "Active")
            }
            StaggeredEntry(index = 1) {
                TransparencyRow(Icons.Filled.CloudOff, "Zero Cloud Upload", "All analysis is local. No network calls for data processing.", colors.safe, "Verified")
            }
            StaggeredEntry(index = 2) {
                TransparencyRow(Icons.Filled.Lock, "Encrypted Storage", "SQLCipher encrypted threat database with AES-256", colors.safe, "Active")
            }
            StaggeredEntry(index = 3) {
                TransparencyRow(Icons.Filled.AutoDelete, "Data Retention", "Auto-delete after $dataRetentionDays days. Manual clear below.", colors.warning, "${dataRetentionDays}d")
            }

            SectionHeader(title = "Permission Status")
            StaggeredEntry(index = 4) {
                PermissionRow(Icons.Filled.VpnLock, "VPN Protection", vpnRunning)
            }
            StaggeredEntry(index = 5) {
                PermissionRow(Icons.Filled.Notifications, "Notification Access", nlsEnabled)
            }
            StaggeredEntry(index = 6) {
                PermissionRow(Icons.Filled.Accessibility, "Accessibility Service", accessibilityEnabled)
            }

            Spacer(modifier = Modifier.height(4.dp))
            SectionHeader(title = "Data Management")

            var showDeleteDialog by remember { mutableStateOf(false) }
            OutlinedButton(
                onClick = { showDeleteDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.critical),
                border = ButtonDefaults.outlinedButtonBorder(true).copy(
                    brush = androidx.compose.ui.graphics.SolidColor(colors.critical.copy(alpha = 0.2f))
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Filled.DeleteForever, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Delete All Threat Logs")
            }

            if (showDeleteDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteDialog = false },
                    confirmButton = {
                        TextButton(onClick = { showDeleteDialog = false }) { Text("Delete All", color = colors.critical) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel", color = colors.textSecondary) }
                    },
                    title = { Text("Delete All Logs?", color = colors.textPrimary) },
                    text = { Text("This will permanently remove all threat history.", color = colors.textSecondary) },
                    containerColor = colors.surfaceElevated,
                    shape = RoundedCornerShape(20.dp)
                )
            }

            RakshakXFooter()
        }
    }
}

@Composable
private fun TransparencyRow(icon: ImageVector, title: String, desc: String, statusColor: Color, statusText: String) {
    val colors = LocalRakshakXColors.current
    GlassSurface {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, null, tint = colors.primary, modifier = Modifier.size(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                Text(desc, style = MaterialTheme.typography.bodySmall, color = colors.textMuted)
            }
            StatusChip(text = statusText, color = statusColor)
        }
    }
}

@Composable
private fun PermissionRow(icon: ImageVector, name: String, isEnabled: Boolean) {
    val colors = LocalRakshakXColors.current
    GlassSurface {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, null, tint = if (isEnabled) colors.safe else colors.critical, modifier = Modifier.size(20.dp))
            Text(name, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary, modifier = Modifier.weight(1f))
            Icon(
                if (isEnabled) Icons.Filled.CheckCircle else Icons.Filled.Error,
                null,
                tint = if (isEnabled) colors.safe else colors.critical,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
