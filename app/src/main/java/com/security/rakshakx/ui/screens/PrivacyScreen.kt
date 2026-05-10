package com.security.rakshakx.ui.screens

import android.content.ComponentName
import android.content.Context
import android.content.Intent
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
import com.security.rakshakx.ui.components.*
import com.security.rakshakx.ui.theme.*
import com.security.rakshakx.web.utils.VpnStatusStore
import com.security.rakshakx.core.SettingsStore
import androidx.compose.runtime.collectAsState

@Composable
fun PrivacyScreen() {
    val colors = LocalRakshakXColors.current
    val context = LocalContext.current
    val vpnRunning = VpnStatusStore.isRunning.collectAsState().value

    // Check notification listener status
    val nlsEnabled = remember {
        val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        flat?.contains(context.packageName) == true
    }

    // Check accessibility service status
    val accessibilityEnabled = remember {
        try {
            val expectedComponent = ComponentName(context, "com.security.rakshakx.web.services.AccessibilityMonitorService")
            val enabledServices = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            enabledServices?.contains(expectedComponent.flattenToString()) == true
        } catch (_: Exception) { false }
    }

    val settingsStore = remember { SettingsStore.getInstance(context) }
    val dataRetentionDays by settingsStore.autoDeleteDays.collectAsState(initial = 30)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                androidx.compose.ui.graphics.Brush.verticalGradient(
                    listOf(Color(0xFF0F172A), Color(0xFF1E293B))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Text(
                "Privacy & Security",
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White
            )
            Text(
                "Your data stays on your device",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.6f)
            )

        // On-device processing card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, colors.safe.copy(alpha = 0.2f), RoundedCornerShape(14.dp)),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = colors.safeBg)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Icon(Icons.Filled.VerifiedUser, null, tint = colors.safe, modifier = Modifier.size(36.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "100% On-Device Processing",
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.safe,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "All AI inference, fraud scoring, and threat analysis runs locally on your device. " +
                        "No data is ever sent to external servers.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                }
            }
        }

        // Security transparency
        SectionHeader(title = "Security Transparency")
        TransparencyItem(
            icon = Icons.Filled.Memory,
            title = "On-Device AI Models",
            description = "TFLite & ONNX models for SMS/call/web fraud classification",
            statusColor = colors.safe,
            statusText = "Active"
        )
        TransparencyItem(
            icon = Icons.Filled.CloudOff,
            title = "No Cloud Upload",
            description = "Zero network calls for analysis. All data stays local.",
            statusColor = colors.safe,
            statusText = "Verified"
        )
        TransparencyItem(
            icon = Icons.Filled.Lock,
            title = "Encrypted Storage",
            description = "Threat logs are stored in SQLCipher encrypted database",
            statusColor = colors.safe,
            statusText = "Active"
        )
        TransparencyItem(
            icon = Icons.Filled.AutoDelete,
            title = "Data Retention",
            description = "Logs are auto-deleted after $dataRetentionDays days. Manual clear available below.",
            statusColor = Color(0xFFF59E0B),
            statusText = "$dataRetentionDays Days"
        )

        // Permission status
        SectionHeader(title = "Permission Status")
        PermissionStatusRow(
            icon = Icons.Filled.VpnLock,
            name = "VPN Protection",
            isEnabled = vpnRunning,
            context = context
        )
        PermissionStatusRow(
            icon = Icons.Filled.Notifications,
            name = "Notification Access",
            isEnabled = nlsEnabled,
            context = context
        )
        PermissionStatusRow(
            icon = Icons.Filled.Accessibility,
            name = "Accessibility Service",
            isEnabled = accessibilityEnabled,
            context = context
        )

        // Danger zone
        Spacer(modifier = Modifier.height(8.dp))
        SectionHeader(title = "Data Management")

        var showDeleteDialog by remember { mutableStateOf(false) }

        OutlinedButton(
            onClick = { showDeleteDialog = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.critical),
            border = ButtonDefaults.outlinedButtonBorder(true).copy(
                brush = androidx.compose.ui.graphics.SolidColor(colors.critical.copy(alpha = 0.3f))
            ),
            shape = MaterialTheme.shapes.medium
        ) {
            Icon(Icons.Filled.DeleteForever, null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Delete All Threat Logs")
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                confirmButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Delete All", color = colors.critical)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Cancel", color = colors.textSecondary)
                    }
                },
                title = { Text("Delete All Logs?", color = colors.textPrimary) },
                text = { Text("This will permanently remove all threat history across SMS, Call, Web, and Email channels.", color = colors.textSecondary) },
                containerColor = colors.surfaceElevated,
                shape = MaterialTheme.shapes.large
            )
        }

        RakshakXFooter()
    }
}
}

@Composable
private fun TransparencyItem(
    icon: ImageVector,
    title: String,
    description: String,
    statusColor: Color,
    statusText: String
) {
    val colors = LocalRakshakXColors.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, colors.border, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, null, tint = colors.primary, modifier = Modifier.size(22.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                Text(description, style = MaterialTheme.typography.bodySmall, color = colors.textMuted)
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(statusColor.copy(alpha = 0.1f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(statusText, style = MaterialTheme.typography.labelSmall, color = statusColor, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun PermissionStatusRow(
    icon: ImageVector,
    name: String,
    isEnabled: Boolean,
    context: Context
) {
    val colors = LocalRakshakXColors.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, colors.border, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, null, tint = if (isEnabled) colors.safe else colors.critical, modifier = Modifier.size(22.dp))
            Text(name, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary, modifier = Modifier.weight(1f))
            Icon(
                if (isEnabled) Icons.Filled.CheckCircle else Icons.Filled.Error,
                null,
                tint = if (isEnabled) colors.safe else colors.critical,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
