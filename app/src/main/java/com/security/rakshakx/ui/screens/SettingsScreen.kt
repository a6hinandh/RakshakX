package com.security.rakshakx.ui.screens

import android.Manifest
import android.app.Activity
import android.net.VpnService
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
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
import com.security.rakshakx.ui.components.*
import com.security.rakshakx.ui.theme.*
import com.security.rakshakx.web.services.FraudVpnService
import com.security.rakshakx.web.utils.VpnStatusStore

@Composable
fun SettingsScreen(activity: Activity, onBack: () -> Unit) {
    val context = LocalContext.current
    val settingsStore = remember { SettingsStore.getInstance(context) }

    val smsEnabled by settingsStore.smsEnabled.collectAsState()
    val callEnabled by settingsStore.callEnabled.collectAsState()
    val emailEnabled by settingsStore.emailEnabled.collectAsState()
    val sensitivity by settingsStore.sensitivity.collectAsState()
    val autoDeleteDays by settingsStore.autoDeleteDays.collectAsState()

    // Real VPN state for the toggle logic
    val vpnRunning by VpnStatusStore.isRunning.collectAsState()
    val vpnLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            FraudVpnService.start(activity.applicationContext)
            settingsStore.setWebEnabled(true)
        }
    }

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
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header with back button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .background(Color.White.copy(alpha = 0.05f), androidx.compose.foundation.shape.CircleShape)
                        .size(40.dp)
                ) {
                    Icon(Icons.Filled.ArrowBack, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Column {
                    Text(
                        "Settings",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Configure your protection preferences",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }

        // ── Protection Sensitivity ──
        SectionHeader(title = "Protection Sensitivity")
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Detection Threshold", style = MaterialTheme.typography.titleSmall, color = Color.White)
                    Text(
                        when {
                            sensitivity >= 0.7f -> "Aggressive"
                            sensitivity >= 0.4f -> "Balanced"
                            else -> "Permissive"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = when {
                            sensitivity >= 0.7f -> Color(0xFFEF4444)
                            sensitivity >= 0.4f -> Color(0xFFF59E0B)
                            else -> Color(0xFF10B981)
                        }
                    )
                }
                Slider(
                    value = sensitivity,
                    onValueChange = { settingsStore.setSensitivity(it) },
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF4776E6),
                        activeTrackColor = Color(0xFF4776E6),
                        inactiveTrackColor = Color.White.copy(alpha = 0.1f)
                    )
                )
                Text(
                    "Higher sensitivity = more proactive but may result in more warnings.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        }

        // ── Channel Toggles ──
        SectionHeader(title = "Channel Monitoring")
        SettingsToggle(
            icon = Icons.Filled.Sms,
            title = "SMS Shield",
            description = "Monitor SMS for pre-action fraud interception",
            isChecked = smsEnabled,
            onCheckedChange = { settingsStore.setSmsEnabled(it) },
            iconColor = Color(0xFF4776E6)
        )
        SettingsToggle(
            icon = Icons.Filled.Call,
            title = "Call Shield",
            description = "Intercept scams during live calls",
            isChecked = callEnabled,
            onCheckedChange = { settingsStore.setCallEnabled(it) },
            iconColor = Color(0xFF8E54E9)
        )
        SettingsToggle(
            icon = Icons.Filled.Language,
            title = "Web Shield (VPN)",
            description = "Block phishing and malicious URLs",
            isChecked = vpnRunning,
            onCheckedChange = { enable ->
                if (enable) {
                    val intent = VpnService.prepare(activity)
                    if (intent != null) vpnLauncher.launch(intent)
                    else {
                        FraudVpnService.start(activity.applicationContext)
                        settingsStore.setWebEnabled(true)
                    }
                } else {
                    FraudVpnService.stop(activity.applicationContext)
                    settingsStore.setWebEnabled(false)
                }
            },
            iconColor = Color(0xFF10B981)
        )
        SettingsToggle(
            icon = Icons.Filled.Email,
            title = "Email Shield",
            description = "Scan mail notifications for phishing intent",
            isChecked = emailEnabled,
            onCheckedChange = { settingsStore.setEmailEnabled(it) },
            iconColor = Color(0xFFEF4444)
        )

        // ── Auto-delete ──
        SectionHeader(title = "Data Retention")
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Auto-delete threat logs after:", style = MaterialTheme.typography.titleSmall, color = Color.White)
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(7, 14, 30, 90).forEach { days ->
                        FilterChip(
                            selected = autoDeleteDays == days,
                            onClick = { settingsStore.setAutoDeleteDays(days) },
                            label = { Text("${days}d") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF4776E6).copy(alpha = 0.2f),
                                selectedLabelColor = Color(0xFF4776E6),
                                containerColor = Color.White.copy(alpha = 0.05f),
                                labelColor = Color.White.copy(alpha = 0.6f)
                            )
                        )
                    }
                }
            }
        }

        // ── About ──
        SectionHeader(title = "About RakshakX")
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("RakshakX", style = MaterialTheme.typography.titleLarge, color = Color(0xFF4776E6), fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "RakshakX is a zero-configuration, privacy-first AI guardian that intercepts scams across calls, SMS, email, and web browsing. It leverages on-device AI and edge intelligence to analyze voice patterns, text intent, URLs, and behavioral signals in real time, enabling silent pre-action interception, cross-channel fraud correlation, and adaptive risk-based blocking or warnings—all without transmitting raw user data.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Version 1.2.0 • On-Device Neural Engine",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Developed by InnovateX",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF8E54E9)
                )
            }
        }

        RakshakXFooter()
    }
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
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF1E293B),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(iconColor.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = iconColor, modifier = Modifier.size(20.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = Color.White)
                Text(description, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.5f))
            }
            Switch(
                checked = isChecked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = iconColor,
                    uncheckedThumbColor = Color.White.copy(alpha = 0.5f),
                    uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
                )
            )
        }
    }
}
