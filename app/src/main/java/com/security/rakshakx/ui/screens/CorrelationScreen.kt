package com.security.rakshakx.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    var showDemo by remember { mutableStateOf(false) } // Default to false for Live Data
    var realEvents by remember { mutableStateOf<List<CorrelationEvent>>(emptyList()) }

    // Load real events from all channel databases
    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
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
    }

    val events = if (showDemo) ThreatLogRepository.getDemoCorrelationTimeline() else realEvents

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0F172A), Color(0xFF1E293B))
                )
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Security Timeline",
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color.White
                    )
                    Text(
                        "Cross-channel attack correlation",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
                IconButton(
                    onClick = {
                        scope.launch(Dispatchers.IO) {
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
                    }
                ) {
                    Icon(Icons.Filled.Refresh, "Refresh", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Demo / Real toggle
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = showDemo,
                    onClick = { showDemo = true },
                    label = { Text("Demo Scenario") },
                    leadingIcon = { Icon(Icons.Filled.PlayCircle, null, modifier = Modifier.size(16.dp)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = colors.primary.copy(alpha = 0.15f),
                        selectedLabelColor = colors.primary,
                        containerColor = colors.cardBackground,
                        labelColor = colors.textSecondary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = colors.border,
                        selectedBorderColor = colors.primary.copy(alpha = 0.3f),
                        enabled = true,
                        selected = showDemo
                    )
                )
                FilterChip(
                    selected = !showDemo,
                    onClick = { showDemo = false },
                    label = { Text("Live Data") },
                    leadingIcon = { Icon(Icons.Filled.DataUsage, null, modifier = Modifier.size(16.dp)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = colors.safe.copy(alpha = 0.15f),
                        selectedLabelColor = colors.safe,
                        containerColor = colors.cardBackground,
                        labelColor = colors.textSecondary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = colors.border,
                        selectedBorderColor = colors.safe.copy(alpha = 0.3f),
                        enabled = true,
                        selected = !showDemo
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Risk escalation summary card
            if (events.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .border(1.dp, colors.critical.copy(alpha = 0.2f), RoundedCornerShape(14.dp)),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.criticalBg)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Risk Escalation",
                                style = MaterialTheme.typography.titleSmall,
                                color = colors.critical,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "${events.size} linked events • ${events.map { it.channel }.toSet().size} channels",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary
                            )
                        }
                        val maxRisk = events.maxOfOrNull { it.riskScore } ?: 0f
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "${"%.0f".format(maxRisk * 100)}%",
                                style = MaterialTheme.typography.headlineSmall,
                                color = colors.critical,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Peak Risk",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.textMuted
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Timeline
            if (events.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.Timeline, null, tint = colors.textMuted, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No Correlated Events", style = MaterialTheme.typography.titleMedium, color = colors.textSecondary)
                        Text(
                            "Switch to Demo to see a multi-stage scam scenario",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textMuted
                        )
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                                .border(1.dp, colors.primary.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = colors.primary.copy(alpha = 0.05f))
                        ) {
                            Row(modifier = Modifier.padding(14.dp)) {
                                Icon(Icons.Filled.Info, null, tint = colors.primary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        "Why Cross-Channel Matters",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = colors.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "Isolated spam filters miss coordinated attacks. " +
                                        "RakshakX correlates events across SMS, calls, email, and web " +
                                        "to detect multi-stage scams that no single-channel filter can catch.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.textSecondary
                                    )
                                }
                            }
                        }
                    }

                    events.forEachIndexed { index, event ->
                        TimelineNode(
                            event = event,
                            isLast = index == events.lastIndex
                        )
                    }
                    
                    RakshakXFooter()
                }
            }
        }
    }
}
