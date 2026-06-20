package com.security.rakshakx.ui.screens

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.security.rakshakx.core.correlation.AttackTactic
import com.security.rakshakx.core.correlation.AttackTechnique
import com.security.rakshakx.core.correlation.MitreAttackMapper
import com.security.rakshakx.ui.anim.StaggeredEntry
import com.security.rakshakx.ui.anim.rememberHaptics
import com.security.rakshakx.ui.components.*
import com.security.rakshakx.ui.data.ThreatLogRepository
import com.security.rakshakx.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun AttackMatrixScreen(onBack: () -> Unit) {
    val colors = LocalRakshakXColors.current
    val context = LocalContext.current
    val haptics = rememberHaptics()
    val scope = rememberCoroutineScope()

    var detectedTechniques by remember { mutableStateOf<List<AttackTechnique>>(emptyList()) }
    var selectedTechnique by remember { mutableStateOf<AttackTechnique?>(null) }

    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            val threats = try { ThreatLogRepository.getAllThreats(context) } catch (_: Exception) { emptyList() }
            val categories = threats.map { it.title.uppercase().replace(" ", "_") }
            detectedTechniques = MitreAttackMapper.mapMultiple(categories)
                .distinctBy { it.techniqueId }
        }
    }

    val allTechniques = MitreAttackMapper.getAllTechniques()
    val detectedIds = detectedTechniques.map { it.techniqueId }.toSet()

    PremiumBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
        ) {
            // Header
            PageHeader(
                title = "MITRE ATT&CK Matrix",
                infoText = "View MITRE ATT&CK mobile threat technique coverage and see which attack vectors RakshakX defends against.",
                onBack = { haptics.tick(); onBack() },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
            )

            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Coverage summary
                StaggeredEntry(index = 0) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.primaryMuted)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                            MatrixStat("${detectedTechniques.size}", "Detected\nTechniques", colors.primary)
                            MatrixStat("${allTechniques.size}", "Total\nCoverage", colors.textSecondary)
                            MatrixStat(
                                "${if (allTechniques.isNotEmpty()) (detectedTechniques.size * 100 / allTechniques.size) else 0}%",
                                "Detection\nRate",
                                colors.safe
                            )
                        }
                    }
                }

                // Legend
                StaggeredEntry(index = 1) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        LegendItem(colors.critical, "Detected in your threats")
                        LegendItem(colors.surfaceElevated, "Monitored / Not detected")
                    }
                }

                // Tactic groups
                AttackTactic.entries.forEach { tactic ->
                    val tacticsForTactic = allTechniques.filter { it.tactic == tactic }
                    if (tacticsForTactic.isNotEmpty()) {
                        SectionHeader(title = tactic.displayName())
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            tacticsForTactic.forEachIndexed { i, technique ->
                                val isDetected = technique.techniqueId in detectedIds
                                StaggeredEntry(index = i, baseDelayMs = 30) {
                                    TechniqueCard(
                                        technique = technique,
                                        isDetected = isDetected,
                                        isSelected = selectedTechnique?.techniqueId == technique.techniqueId,
                                        onClick = {
                                            haptics.tick()
                                            selectedTechnique = if (selectedTechnique?.techniqueId == technique.techniqueId) null else technique
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                RakshakXFooter()
            }
        }
    }
}

@Composable
private fun TechniqueCard(
    technique: AttackTechnique,
    isDetected: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalRakshakXColors.current
    val borderColor = when {
        isSelected -> colors.primary
        isDetected -> colors.critical.copy(alpha = 0.5f)
        else -> colors.border.copy(alpha = 0.2f)
    }
    GlassSurface(
        onClick = onClick,
        borderColor = borderColor
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(
                    modifier = Modifier.size(48.dp).background(
                        if (isDetected) colors.critical.copy(alpha = 0.12f) else colors.surfaceElevated,
                        RoundedCornerShape(8.dp)
                    ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        technique.techniqueId.take(5),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isDetected) colors.critical else colors.textMuted,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(technique.name, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary, fontWeight = FontWeight.SemiBold)
                    Text(technique.tactic.displayName(), style = MaterialTheme.typography.bodySmall, color = colors.textMuted)
                }
                if (isDetected) {
                    StatusChip("Detected", colors.critical)
                }
                Icon(
                    imageVector = if (isSelected) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (isSelected) "Collapse" else "Expand",
                    tint = colors.textMuted.copy(alpha = 0.5f),
                    modifier = Modifier.size(18.dp)
                )
            }

            if (isSelected) {
                HorizontalDivider(
                    color = colors.border.copy(alpha = 0.15f),
                    thickness = 1.dp,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = technique.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                    if (technique.mitigations.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Mitigations",
                            style = MaterialTheme.typography.titleSmall,
                            color = colors.textPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        technique.mitigations.forEach { mit ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    Icons.Filled.CheckCircle,
                                    null,
                                    tint = colors.safe,
                                    modifier = Modifier.size(14.dp).padding(top = 2.dp)
                                )
                                Text(
                                    text = mit,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MatrixStat(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineSmall, color = color, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = LocalRakshakXColors.current.textMuted, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    val colors = LocalRakshakXColors.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(modifier = Modifier.size(12.dp).background(color, RoundedCornerShape(3.dp)))
        Text(label, style = MaterialTheme.typography.labelSmall, color = colors.textMuted)
    }
}

private fun AttackTactic.displayName(): String = when (this) {
    AttackTactic.INITIAL_ACCESS -> "Initial Access"
    AttackTactic.EXECUTION -> "Execution"
    AttackTactic.PERSISTENCE -> "Persistence"
    AttackTactic.PRIVILEGE_ESCALATION -> "Privilege Escalation"
    AttackTactic.DEFENSE_EVASION -> "Defense Evasion"
    AttackTactic.CREDENTIAL_ACCESS -> "Credential Access"
    AttackTactic.DISCOVERY -> "Discovery"
    AttackTactic.LATERAL_MOVEMENT -> "Lateral Movement"
    AttackTactic.COLLECTION -> "Collection"
    AttackTactic.COMMAND_AND_CONTROL -> "Command & Control"
    AttackTactic.EXFILTRATION -> "Exfiltration"
    AttackTactic.IMPACT -> "Impact"
    AttackTactic.RESOURCE_DEVELOPMENT -> "Resource Development"
}
