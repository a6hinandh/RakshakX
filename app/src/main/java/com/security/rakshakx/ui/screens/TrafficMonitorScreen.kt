package com.security.rakshakx.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.security.rakshakx.ui.anim.StaggeredEntry
import com.security.rakshakx.ui.anim.rememberHaptics
import com.security.rakshakx.ui.components.*
import com.security.rakshakx.ui.theme.*
import com.security.rakshakx.web.analyzers.*
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.delay

@Composable
fun TrafficMonitorScreen(onBack: () -> Unit) {
    val colors = LocalRakshakXColors.current
    val haptics = rememberHaptics()

    // Real-time DNS query stream
    val queries by LiveTrafficStream.recentQueries.collectAsState()
    val rulesChanged by GlobalBlocklistManager.rulesChanged.collectAsState() // Trigger recomposition when rules change

    // Periodic anomaly refresh
    var anomalies by remember { mutableStateOf(TrafficAnomalyDetector.analyze()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(5000) // refresh every 5 seconds
            anomalies = TrafficAnomalyDetector.analyze()
        }
    }

    PremiumBackground {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            // Header
            PageHeader(
                title = "Live DNS Dashboard",
                infoText = "Monitor real-time DNS traffic, see which domains your device contacts, and block suspicious connections.",
                onBack = { haptics.tick(); onBack() },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
            )

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Dashboard stats
                item {
                    val last5Mins = queries.count { System.currentTimeMillis() - it.timestamp < 5 * 60 * 1000 }
                    val blockedCount = queries.count { it.isBlocked || GlobalBlocklistManager.isBlocked(it.domain) }
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCard(modifier = Modifier.weight(1f), title = "Queries (5m)", value = last5Mins.toString(), icon = Icons.Filled.Public, color = colors.primary)
                        StatCard(modifier = Modifier.weight(1f), title = "Blocked", value = blockedCount.toString(), icon = Icons.Filled.Block, color = colors.critical)
                    }
                }

                // Live Stream
                item { Spacer(Modifier.height(8.dp)); SectionHeader(title = "Live DNS Stream") }

                if (queries.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                            Text("Waiting for network traffic...", color = colors.textMuted)
                        }
                    }
                } else {
                    // Show latest 50 queries to prevent UI lag
                    items(queries.take(50), key = { it.id }) { query ->
                        DnsQueryRow(
                            query = query,
                            // pass rulesChanged just to trigger recomposition when blocklist changes
                            rulesChanged = rulesChanged,
                            onToggleBlock = { domain ->
                                haptics.tick()
                                if (GlobalBlocklistManager.isBlocked(domain)) {
                                    GlobalBlocklistManager.removeRule(domain)
                                } else {
                                    GlobalBlocklistManager.blockDomain(domain)
                                }
                            }
                        )
                    }
                }

                // Anomaly results
                item { Spacer(Modifier.height(16.dp)); SectionHeader(title = "Detected Anomalies (${anomalies.size})") }

                if (anomalies.isEmpty()) {
                    item {
                        GlassCard {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Icon(Icons.Filled.VerifiedUser, null, tint = colors.safe, modifier = Modifier.size(24.dp))
                                Column {
                                    Text("No Anomalies", style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                                    Text("Detectors are active and traffic looks normal.", style = MaterialTheme.typography.bodySmall, color = colors.textMuted)
                                }
                            }
                        }
                    }
                } else {
                    items(anomalies) { anomaly ->
                        AnomalyCard(anomaly = anomaly)
                    }
                }

                item { RakshakXFooter() }
            }
        }
    }
}

@Composable
private fun StatCard(modifier: Modifier, title: String, value: String, icon: ImageVector, color: Color) {
    val colors = LocalRakshakXColors.current
    GlassCard(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(modifier = Modifier.size(36.dp).background(color.copy(alpha = 0.15f), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
            }
            Column {
                Text(value, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary, fontWeight = FontWeight.Bold)
                Text(title, style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
            }
        }
    }
}

@Composable
private fun DnsQueryRow(query: DnsQueryRecord, rulesChanged: Int, onToggleBlock: (String) -> Unit) {
    val colors = LocalRakshakXColors.current
    val formatter = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    val timeString = remember(query.timestamp) { formatter.format(Date(query.timestamp)) }
    
    // Evaluate current block status based on GlobalBlocklistManager or the historic blockedBy record
    val isCurrentlyBlocked = GlobalBlocklistManager.isBlocked(query.domain) || query.isBlocked

    val bgColor by animateColorAsState(if (isCurrentlyBlocked) colors.criticalBg else colors.surfaceElevated, label = "bg")
    val textColor by animateColorAsState(if (isCurrentlyBlocked) colors.critical else colors.textPrimary, label = "text")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .clickable { onToggleBlock(query.domain) }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(timeString, style = MaterialTheme.typography.labelSmall, color = colors.textMuted, modifier = Modifier.width(60.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                query.domain,
                style = MaterialTheme.typography.bodyMedium,
                color = textColor,
                fontWeight = if (isCurrentlyBlocked) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (isCurrentlyBlocked && query.blockedBy != null) {
                Text("Blocked by: ${query.blockedBy}", style = MaterialTheme.typography.labelSmall, color = colors.critical.copy(alpha=0.7f))
            } else if (isCurrentlyBlocked) {
                Text("Blocked by: User Blocklist", style = MaterialTheme.typography.labelSmall, color = colors.critical.copy(alpha=0.7f))
            }
        }
        
        // 1-Tap Block Icon
        Icon(
            if (isCurrentlyBlocked) Icons.Filled.RemoveCircle else Icons.Filled.Block,
            contentDescription = if (isCurrentlyBlocked) "Unblock" else "Block",
            tint = if (isCurrentlyBlocked) colors.critical else colors.border,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun AnomalyCard(anomaly: TrafficAnomaly) {
    val colors = LocalRakshakXColors.current
    val severityColor = when (anomaly.severity) {
        AnomalySeverity.HIGH   -> colors.critical
        AnomalySeverity.MEDIUM -> colors.warning
        AnomalySeverity.LOW    -> colors.primary
    }
    val typeIcon = when (anomaly.type) {
        AnomalyType.BEACONING        -> Icons.Filled.Podcasts
        AnomalyType.DGA_DOMAIN       -> Icons.Filled.Shuffle
        AnomalyType.DNS_TUNNELING    -> Icons.Filled.Compress
        AnomalyType.CRYPTOMINING     -> Icons.Filled.CurrencyBitcoin
        AnomalyType.DATA_EXFILTRATION -> Icons.Filled.Upload
    }
    GlassSurface(borderColor = severityColor.copy(alpha = 0.3f)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier.size(40.dp).background(severityColor.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(typeIcon, null, tint = severityColor, modifier = Modifier.size(22.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    anomaly.type.displayName(),
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.textPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(anomaly.domain, style = MaterialTheme.typography.bodySmall, color = colors.textMuted)
                Text(anomaly.description, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
            }
            StatusChip(anomaly.severity.name, severityColor)
        }
    }
}

private fun AnomalyType.displayName(): String = when (this) {
    AnomalyType.BEACONING         -> "C2 Beaconing"
    AnomalyType.DGA_DOMAIN        -> "DGA Domain"
    AnomalyType.DNS_TUNNELING     -> "DNS Tunneling"
    AnomalyType.CRYPTOMINING      -> "Cryptomining Pool"
    AnomalyType.DATA_EXFILTRATION -> "Data Exfiltration"
}
