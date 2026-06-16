package com.security.rakshakx.ui.screens

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.security.rakshakx.ui.anim.StaggeredEntry
import com.security.rakshakx.ui.anim.rememberHaptics
import com.security.rakshakx.ui.components.*
import com.security.rakshakx.ui.data.*
import com.security.rakshakx.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun CorrelationScreen() {
    val colors = LocalRakshakXColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptics = rememberHaptics()

    var showDemo by remember { mutableStateOf(false) }
    var realEvents by remember { mutableStateOf<List<CorrelationEvent>>(emptyList()) }

    LaunchedEffect(Unit) {
        val threats = try { ThreatLogRepository.getAllThreats(context) } catch (_: Exception) { emptyList() }
        if (threats.isNotEmpty()) {
            realEvents = threats.sortedBy { it.timestamp }.map { entry ->
                CorrelationEvent(
                    id = entry.id,
                    channel = entry.channel,
                    severity = entry.severity,
                    title = entry.title,
                    description = entry.description,
                    timestamp = entry.timestamp,
                    riskScore = entry.riskScore
                )
            }
        }
    }

    val events = if (showDemo) ThreatLogRepository.getDemoCorrelationTimeline() else realEvents

    PremiumBackground {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Timeline", style = MaterialTheme.typography.headlineLarge, color = colors.textPrimary)
                    Text("Cross-channel attack correlation", style = MaterialTheme.typography.bodySmall, color = colors.textMuted)
                }
                IconButton(onClick = {
                    scope.launch(Dispatchers.IO) {
                        val threats = try { ThreatLogRepository.getAllThreats(context) } catch (_: Exception) { emptyList() }
                        if (threats.isNotEmpty()) {
                            realEvents = threats.sortedBy { it.timestamp }.map { entry ->
                                CorrelationEvent(entry.id, entry.channel, entry.severity, entry.title, entry.description, entry.timestamp, entry.riskScore)
                            }
                        }
                    }
                }) {
                    Icon(Icons.Filled.Refresh, "Refresh", tint = colors.textSecondary)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = showDemo,
                    onClick = { haptics.tick(); showDemo = true },
                    label = { Text("Demo Scenario") },
                    leadingIcon = { Icon(Icons.Filled.PlayCircle, null, modifier = Modifier.size(14.dp)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = colors.primaryMuted,
                        selectedLabelColor = colors.primary,
                        containerColor = colors.surfaceElevated,
                        labelColor = colors.textMuted
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = colors.border.copy(alpha = 0.3f),
                        selectedBorderColor = colors.primary.copy(alpha = 0.2f),
                        enabled = true, selected = showDemo
                    ),
                    shape = RoundedCornerShape(10.dp)
                )
                FilterChip(
                    selected = !showDemo,
                    onClick = { haptics.tick(); showDemo = false },
                    label = { Text("Live Data") },
                    leadingIcon = { Icon(Icons.Filled.DataUsage, null, modifier = Modifier.size(14.dp)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = colors.safeBg,
                        selectedLabelColor = colors.safe,
                        containerColor = colors.surfaceElevated,
                        labelColor = colors.textMuted
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = colors.border.copy(alpha = 0.3f),
                        selectedBorderColor = colors.safe.copy(alpha = 0.2f),
                        enabled = true, selected = !showDemo
                    ),
                    shape = RoundedCornerShape(10.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (events.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .border(1.dp, colors.critical.copy(alpha = 0.12f), RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.criticalBg)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Risk Escalation", style = MaterialTheme.typography.titleSmall, color = colors.critical, fontWeight = FontWeight.SemiBold)
                            Text("${events.size} linked events • ${events.map { it.channel }.toSet().size} channels",
                                style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                        }
                        val maxRisk = events.maxOfOrNull { it.riskScore } ?: 0f
                        Column(horizontalAlignment = Alignment.End) {
                            Text("${"%.0f".format(maxRisk * 100)}%",
                                style = MaterialTheme.typography.headlineSmall, color = colors.critical, fontWeight = FontWeight.Bold)
                            Text("Peak Risk", style = MaterialTheme.typography.labelSmall, color = colors.textMuted)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (events.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.Timeline, null, tint = colors.textMuted.copy(alpha = 0.4f), modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No Correlated Events", style = MaterialTheme.typography.titleLarge, color = colors.textSecondary)
                        Text("Switch to Demo to see a multi-stage attack", style = MaterialTheme.typography.bodySmall, color = colors.textMuted)
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 100.dp)
                ) {
                    if (showDemo) {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                                .border(1.dp, colors.primary.copy(alpha = 0.12f), RoundedCornerShape(14.dp)),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = colors.primaryMuted)
                        ) {
                            Row(modifier = Modifier.padding(14.dp)) {
                                Icon(Icons.Filled.Info, null, tint = colors.primary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Cross-Channel Detection", style = MaterialTheme.typography.titleSmall, color = colors.primary, fontWeight = FontWeight.SemiBold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "RakshakX correlates events across SMS, calls, email, and web to detect multi-stage scams no single filter can catch.",
                                        style = MaterialTheme.typography.bodySmall, color = colors.textSecondary
                                    )
                                }
                            }
                        }
                    }
                    events.forEachIndexed { index, event ->
                        StaggeredEntry(index = index, baseDelayMs = 150, durationMs = 400) {
                            TimelineNode(event = event, isLast = index == events.lastIndex)
                        }
                    }
                    RakshakXFooter()
                }
            }
        }
    }
}
