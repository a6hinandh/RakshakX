package com.security.rakshakx.ui.screens

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
import com.security.rakshakx.ui.anim.StaggeredEntry
import com.security.rakshakx.ui.anim.rememberHaptics
import com.security.rakshakx.ui.components.*
import com.security.rakshakx.ui.theme.*
import com.security.rakshakx.web.utils.VpnStatusStore

@Composable
fun NetworkHubScreen(
    onNavigateToWifiAudit: () -> Unit,
    onNavigateToFirewall: () -> Unit,
    onNavigateToNetworkScan: () -> Unit,
    onNavigateToTrafficMonitor: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToShield: () -> Unit
) {
    val colors = LocalRakshakXColors.current
    val haptics = rememberHaptics()
    val context = LocalContext.current
    val vpnRunning by VpnStatusStore.isRunning.collectAsState()

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
                    title = "Network Security",
                    infoText = "Monitor network security, audit Wi-Fi connections, manage firewall rules, and scan for devices on your local network."
                )
            }

            // VPN Status Banner
            StaggeredEntry(index = 1, baseDelayMs = 60) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .let {
                            if (!vpnRunning) {
                                it.clickable {
                                    haptics.click()
                                    onNavigateToShield()
                                }
                            } else {
                                it
                            }
                        },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (vpnRunning) colors.safeBg else colors.criticalBg
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(44.dp).background(
                                if (vpnRunning) colors.safe.copy(alpha = 0.15f) else colors.critical.copy(alpha = 0.15f),
                                RoundedCornerShape(12.dp)
                            ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.VpnLock,
                                null,
                                tint = if (vpnRunning) colors.safe else colors.critical,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                if (vpnRunning) "DNS Threat Monitor Active" else "DNS Monitor Inactive",
                                style = MaterialTheme.typography.titleSmall,
                                color = if (vpnRunning) colors.safe else colors.critical,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                if (vpnRunning) "All DNS queries are being analyzed for threats"
                                else "Enable in Settings to activate DNS-level threat detection",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary
                            )
                        }
                        StatusChip(
                            text = if (vpnRunning) "Active" else "Off",
                            color = if (vpnRunning) colors.safe else colors.critical
                        )
                    }
                }
            }

            // Primary Tools
            StaggeredEntry(index = 2, baseDelayMs = 80) {
                SectionHeader(title = "Network Tools")
            }

            StaggeredEntry(index = 3, baseDelayMs = 80) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    NetworkToolCard(
                        icon = Icons.Filled.Wifi,
                        title = "Wi-Fi Audit",
                        subtitle = "Encryption & rogue AP detection",
                        color = RoyalBlue,
                        onClick = onNavigateToWifiAudit,
                        modifier = Modifier.weight(1f)
                    )
                    NetworkToolCard(
                        icon = Icons.Filled.Security,
                        title = "App Firewall",
                        subtitle = "Per-app network control",
                        color = Emerald,
                        onClick = onNavigateToFirewall,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            StaggeredEntry(index = 4, baseDelayMs = 80) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    NetworkToolCard(
                        icon = Icons.Filled.Router,
                        title = "Network Scan",
                        subtitle = "Discover devices on LAN",
                        color = Amber,
                        onClick = onNavigateToNetworkScan,
                        modifier = Modifier.weight(1f)
                    )
                    NetworkToolCard(
                        icon = Icons.Filled.ShowChart,
                        title = "Traffic Monitor",
                        subtitle = "Anomaly & DGA detection",
                        color = Amethyst,
                        onClick = onNavigateToTrafficMonitor,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Security Layers Info
            StaggeredEntry(index = 5, baseDelayMs = 80) {
                SectionHeader(title = "Protection Layers")
            }

            StaggeredEntry(index = 6, baseDelayMs = 80) {
                GlassCard {
                    ProtectionLayer(
                        index = "L1",
                        title = "DNS Filtering",
                        desc = "Blocks malicious domains before connection",
                        active = vpnRunning,
                        colors = colors
                    )
                    Spacer(Modifier.height(12.dp))
                    ProtectionLayer(
                        index = "L2",
                        title = "Domain Risk Analysis",
                        desc = "Entropy, typosquatting, and homograph detection",
                        active = vpnRunning,
                        colors = colors
                    )
                    Spacer(Modifier.height(12.dp))
                    ProtectionLayer(
                        index = "L3",
                        title = "Traffic Anomaly Detection",
                        desc = "Beaconing, DGA, DNS tunneling, cryptomining",
                        active = vpnRunning,
                        colors = colors
                    )
                    Spacer(Modifier.height(12.dp))
                    ProtectionLayer(
                        index = "L4",
                        title = "App Firewall",
                        desc = "UID-based per-app network policy enforcement",
                        active = false,
                        colors = colors
                    )
                }
            }

            // Network Security Facts
            StaggeredEntry(index = 7, baseDelayMs = 80) {
                GlassCard {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Filled.Info,
                            null,
                            tint = colors.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            "All network analysis runs locally. Traffic is inspected on-device " +
                            "via a local VPN tunnel — no data leaves your device for analysis.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary
                        )
                    }
                }
            }

            RakshakXFooter()
        }
    }
}

@Composable
private fun NetworkToolCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalRakshakXColors.current
    val haptics = rememberHaptics()
    GlassSurface(
        onClick = { haptics.click(); onClick() },
        modifier = modifier,
        borderColor = color.copy(alpha = 0.25f)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier.size(40.dp).background(color.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
            }
            Text(title, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colors.textMuted)
        }
    }
}

@Composable
private fun ProtectionLayer(
    index: String,
    title: String,
    desc: String,
    active: Boolean,
    colors: RakshakXColors
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            modifier = Modifier.size(32.dp).background(
                if (active) colors.primaryMuted else colors.border.copy(alpha = 0.15f),
                RoundedCornerShape(8.dp)
            ),
            contentAlignment = Alignment.Center
        ) {
            Text(index, style = MaterialTheme.typography.labelSmall, color = if (active) colors.primary else colors.textMuted, fontWeight = FontWeight.Bold)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
            Text(desc, style = MaterialTheme.typography.bodySmall, color = colors.textMuted)
        }
        Icon(
            if (active) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
            null,
            tint = if (active) colors.safe else colors.textMuted.copy(alpha = 0.4f),
            modifier = Modifier.size(18.dp)
        )
    }
}
