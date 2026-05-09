package com.security.rakshakx.ui.screens

import android.app.Activity
import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.security.rakshakx.ui.components.SectionHeader
import com.security.rakshakx.ui.theme.*
import com.security.rakshakx.web.services.FraudVpnService
import com.security.rakshakx.web.utils.VpnStatusStore

@Composable
fun SettingsScreen(activity: Activity) {
    val colors = LocalRakshakXColors.current

    // Settings state
    var sensitivity by remember { mutableStateOf(0.5f) }
    var autoDeleteDays by remember { mutableStateOf(30) }
    var smsEnabled by remember { mutableStateOf(true) }
    var callEnabled by remember { mutableStateOf(true) }
    var emailEnabled by remember { mutableStateOf(true) }
    var seniorMode by remember { mutableStateOf(false) }

    // Real VPN state
    val webEnabled by VpnStatusStore.isRunning.collectAsState()
    val vpnLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            FraudVpnService.start(activity.applicationContext)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Text(
            "Settings",
            style = MaterialTheme.typography.headlineMedium,
            color = colors.textPrimary,
            fontWeight = FontWeight.Bold
        )

        // ── Protection Sensitivity ──
        SectionHeader(title = "Protection Sensitivity")
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, colors.border, RoundedCornerShape(14.dp)),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = colors.cardBackground)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Detection Threshold", style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                    Text(
                        when {
                            sensitivity >= 0.7f -> "Aggressive"
                            sensitivity >= 0.4f -> "Balanced"
                            else -> "Permissive"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = when {
                            sensitivity >= 0.7f -> colors.critical
                            sensitivity >= 0.4f -> colors.warning
                            else -> colors.safe
                        }
                    )
                }
                Slider(
                    value = sensitivity,
                    onValueChange = { sensitivity = it },
                    colors = SliderDefaults.colors(
                        thumbColor = colors.primary,
                        activeTrackColor = colors.primary,
                        inactiveTrackColor = colors.surfaceElevated
                    )
                )
                Text(
                    "Higher sensitivity = more false positives but fewer missed threats",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textMuted
                )
            }
        }

        // ── Channel Toggles ──
        SectionHeader(title = "Channel Monitoring")
        SettingsToggle(
            icon = Icons.Filled.Sms,
            title = "SMS Shield",
            description = "Monitor SMS notifications for fraud",
            isChecked = smsEnabled,
            onCheckedChange = { smsEnabled = it },
            iconColor = Cyan400
        )
        SettingsToggle(
            icon = Icons.Filled.Call,
            title = "Call Shield",
            description = "Monitor incoming calls for scams",
            isChecked = callEnabled,
            onCheckedChange = { callEnabled = it },
            iconColor = OrangeWarn
        )
        SettingsToggle(
            icon = Icons.Filled.Language,
            title = "Web Shield (VPN)",
            description = "Intercepts web traffic to block phishing sites",
            isChecked = webEnabled,
            onCheckedChange = { enable ->
                if (enable) {
                    val intent = VpnService.prepare(activity)
                    if (intent != null) vpnLauncher.launch(intent)
                    else FraudVpnService.start(activity.applicationContext)
                } else {
                    FraudVpnService.stop(activity.applicationContext)
                }
            },
            iconColor = GreenSafe
        )
        SettingsToggle(
            icon = Icons.Filled.Email,
            title = "Email Shield",
            description = "Monitor email notifications for phishing",
            isChecked = emailEnabled,
            onCheckedChange = { emailEnabled = it },
            iconColor = RedCritical
        )

        // ── Auto-delete ──
        SectionHeader(title = "Data Retention")
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, colors.border, RoundedCornerShape(14.dp)),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = colors.cardBackground)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Auto-delete threat logs after:", style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(7, 14, 30, 90).forEach { days ->
                        FilterChip(
                            selected = autoDeleteDays == days,
                            onClick = { autoDeleteDays = days },
                            label = { Text("${days}d") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colors.primary.copy(alpha = 0.15f),
                                selectedLabelColor = colors.primary,
                                containerColor = colors.surfaceElevated,
                                labelColor = colors.textSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                borderColor = colors.border,
                                selectedBorderColor = colors.primary.copy(alpha = 0.3f),
                                enabled = true,
                                selected = autoDeleteDays == days
                            )
                        )
                    }
                }
            }
        }

        // ── Special Modes ──
        SectionHeader(title = "Special Modes")
        SettingsToggle(
            icon = Icons.Filled.Elderly,
            title = "Senior Protection Mode",
            description = "Enhanced protection with simplified warnings and auto-block for high-risk events",
            isChecked = seniorMode,
            onCheckedChange = { seniorMode = it },
            iconColor = colors.primary
        )

        // ── About ──
        SectionHeader(title = "About")
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, colors.border, RoundedCornerShape(14.dp)),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = colors.cardBackground)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("RakshakX", style = MaterialTheme.typography.titleMedium, color = colors.primary, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Autonomous Privacy-First Fraud Interception Platform", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Version 1.0.0 • On-Device AI", style = MaterialTheme.typography.labelSmall, color = colors.textMuted)
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
private fun SettingsToggle(
    icon: ImageVector,
    title: String,
    description: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    iconColor: Color
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
            Icon(icon, null, tint = iconColor, modifier = Modifier.size(22.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                Text(description, style = MaterialTheme.typography.bodySmall, color = colors.textMuted)
            }
            Switch(
                checked = isChecked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = colors.primary,
                    checkedTrackColor = colors.primary.copy(alpha = 0.2f),
                    uncheckedThumbColor = colors.textMuted,
                    uncheckedTrackColor = colors.surfaceElevated
                )
            )
        }
    }
}
