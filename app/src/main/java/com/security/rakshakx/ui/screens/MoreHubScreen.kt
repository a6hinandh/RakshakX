package com.security.rakshakx.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.security.rakshakx.ui.anim.StaggeredEntry
import com.security.rakshakx.ui.anim.rememberHaptics
import com.security.rakshakx.ui.components.*
import com.security.rakshakx.ui.theme.*

@Composable
fun MoreHubScreen(
    onNavigateToDeviceHealth: () -> Unit,
    onNavigateToAppAudit: () -> Unit,
    onNavigateToPrivacyDashboard: () -> Unit,
    onNavigateToVault: () -> Unit,
    onNavigateToThreatAnalytics: () -> Unit,
    onNavigateToPasswordStudio: () -> Unit,
    onNavigateToForensicExport: () -> Unit,
    onNavigateToThreatIntel: () -> Unit,
    onNavigateToFamily: () -> Unit,
    onNavigateToReport: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val colors = LocalRakshakXColors.current

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

            StaggeredEntry(index = 0) {
                PageHeader(
                    title = "More Tools",
                    infoText = "Access additional security tools including device health checks, privacy audits, secure vault, threat analytics, forensic exports, and more."
                )
            }

            // Device Security
            StaggeredEntry(index = 1, baseDelayMs = 60) {
                SectionHeader(title = "Device Security")
            }
            StaggeredEntry(index = 2, baseDelayMs = 70) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    HubMenuItem(
                        icon = Icons.Filled.PhoneAndroid,
                        iconColor = RoyalBlue,
                        title = "Device Health",
                        subtitle = "Root detection, patch level, encryption status",
                        badge = null,
                        onClick = onNavigateToDeviceHealth
                    )
                    HubMenuItem(
                        icon = Icons.Filled.Apps,
                        iconColor = Emerald,
                        title = "App Security Audit",
                        subtitle = "Scan installed apps for dangerous permissions",
                        badge = null,
                        onClick = onNavigateToAppAudit
                    )
                }
            }

            // Privacy
            StaggeredEntry(index = 3, baseDelayMs = 80) {
                SectionHeader(title = "Privacy")
            }
            StaggeredEntry(index = 4, baseDelayMs = 90) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    HubMenuItem(
                        icon = Icons.Filled.TrackChanges,
                        iconColor = Amber,
                        title = "Privacy Dashboard",
                        subtitle = "Tracker exposure per app, 400+ tracker signatures",
                        badge = null,
                        onClick = onNavigateToPrivacyDashboard
                    )
                    HubMenuItem(
                        icon = Icons.Filled.VpnKey,
                        iconColor = Amethyst,
                        title = "Secure Vault",
                        subtitle = "AES-256 encrypted local storage for credentials",
                        badge = null,
                        onClick = onNavigateToVault
                    )
                    HubMenuItem(
                        icon = Icons.Filled.Password,
                        iconColor = CrimsonLight,
                        title = "Password Studio",
                        subtitle = "Offline password analyzer and generator",
                        badge = null,
                        onClick = onNavigateToPasswordStudio
                    )
                }
            }

            // Intelligence
            StaggeredEntry(index = 5, baseDelayMs = 100) {
                SectionHeader(title = "Threat Intelligence")
            }
            StaggeredEntry(index = 6, baseDelayMs = 110) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    HubMenuItem(
                        icon = Icons.Filled.Analytics,
                        iconColor = RoyalBlue,
                        title = "Threat Analytics",
                        subtitle = "Heatmap, trends, and attack vector breakdown",
                        badge = null,
                        onClick = onNavigateToThreatAnalytics
                    )
                    HubMenuItem(
                        icon = Icons.Filled.Inventory,
                        iconColor = Emerald,
                        title = "Forensic Export",
                        subtitle = "STIX 2.1 bundle with SHA-256 integrity hash",
                        badge = null,
                        onClick = onNavigateToForensicExport
                    )
                    HubMenuItem(
                        icon = Icons.Filled.Cloud,
                        iconColor = Amber,
                        title = "Threat Intelligence",
                        subtitle = "Local blocklist, manual blocking & auto-detection",
                        badge = null,
                        onClick = onNavigateToThreatIntel
                    )
                }
            }

            // Other
            StaggeredEntry(index = 7, baseDelayMs = 120) {
                SectionHeader(title = "Other")
            }
            StaggeredEntry(index = 8, baseDelayMs = 130) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    HubMenuItem(
                        icon = Icons.Filled.FamilyRestroom,
                        iconColor = Amethyst,
                        title = "Family Protection",
                        subtitle = "Concept preview — local profiles & simplified UI",
                        badge = "Preview",
                        onClick = onNavigateToFamily
                    )
                    HubMenuItem(
                        icon = Icons.Filled.Assessment,
                        iconColor = RoyalBlue,
                        title = "Security Report",
                        subtitle = "Analytics, channel breakdown, and CSV export",
                        badge = null,
                        onClick = onNavigateToReport
                    )
                    HubMenuItem(
                        icon = Icons.Filled.Settings,
                        iconColor = colors.textMuted,
                        title = "Settings",
                        subtitle = "Detection thresholds, data retention, services",
                        badge = null,
                        onClick = onNavigateToSettings
                    )
                }
            }

            RakshakXFooter()
        }
    }
}

@Composable
private fun HubMenuItem(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    badge: String?,
    onClick: () -> Unit
) {
    val colors = LocalRakshakXColors.current
    val haptics = rememberHaptics()
    GlassSurface(onClick = { haptics.tick(); onClick() }) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier.size(40.dp).background(iconColor.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = iconColor, modifier = Modifier.size(22.dp))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(title, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary, fontWeight = FontWeight.SemiBold)
                    if (badge != null) {
                        StatusChip(text = badge, color = colors.critical)
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colors.textMuted)
            }
            Icon(Icons.Filled.ChevronRight, null, tint = colors.textMuted.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
        }
    }
}
