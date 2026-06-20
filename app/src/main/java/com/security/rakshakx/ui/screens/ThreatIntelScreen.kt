package com.security.rakshakx.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.sp
import com.security.rakshakx.core.threatintel.BlocklistType
import com.security.rakshakx.core.threatintel.ThreatIntelligenceManager
import com.security.rakshakx.ui.anim.rememberHaptics
import com.security.rakshakx.ui.components.*
import com.security.rakshakx.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ThreatIntelScreen(onBack: () -> Unit) {
    val colors = LocalRakshakXColors.current
    val context = LocalContext.current
    val haptics = rememberHaptics()
    val manager = remember { ThreatIntelligenceManager.getInstance(context) }

    val blocklist by manager.blocklist.collectAsState()
    val lastUpdated by manager.lastUpdated.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var addInput by remember { mutableStateOf("") }
    var addType by remember { mutableStateOf(BlocklistType.PHONE) }

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

            PageHeader(
                title = "Threat Intelligence",
                infoText = "Manage local blocklists, manual blocking rules, and auto-detection settings for known threats.",
                onBack = { haptics.tick(); onBack() }
            )

            // Privacy Guarantee
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = colors.safeBg)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Privacy Guarantee", style = MaterialTheme.typography.titleSmall, color = colors.safe, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    PrivacyGuaranteeRow("Only SHA-256 hashes stored, never raw data")
                    PrivacyGuaranteeRow("No message content leaves your device")
                    PrivacyGuaranteeRow("Blocklist auto-populated from detected scams")
                    PrivacyGuaranteeRow("Blocklist boosts risk score for repeat offenders")
                }
            }

            // Stats
            SectionHeader(title = "Local Blocklist")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GlassSurface(modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${blocklist.size}", style = MaterialTheme.typography.headlineSmall, color = colors.primary, fontWeight = FontWeight.Bold)
                        Text("Blocked Entries", style = MaterialTheme.typography.labelSmall, color = colors.textMuted, letterSpacing = 0.5.sp)
                    }
                }
                GlassSurface(modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            if (lastUpdated > 0L)
                                SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(lastUpdated))
                            else "—",
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text("Last Updated", style = MaterialTheme.typography.labelSmall, color = colors.textMuted, letterSpacing = 0.5.sp)
                    }
                }
            }

            // Manual block button
            GlassSurface(onClick = { showAddDialog = true }) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Filled.AddCircle, null, tint = colors.primary, modifier = Modifier.size(22.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Add to Blocklist", style = MaterialTheme.typography.titleSmall, color = colors.textPrimary, fontWeight = FontWeight.SemiBold)
                        Text("Manually block a phone number or domain", style = MaterialTheme.typography.bodySmall, color = colors.textMuted)
                    }
                    Icon(Icons.Filled.ChevronRight, null, tint = colors.textMuted.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
                }
            }

            if (blocklist.isEmpty()) {
                GlassCard {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Filled.Shield, null, tint = colors.textMuted, modifier = Modifier.size(36.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("Blocklist is empty", style = MaterialTheme.typography.bodyMedium, color = colors.textMuted)
                        Text("Scam senders and domains are added automatically when threats are detected, or you can add them manually.", style = MaterialTheme.typography.bodySmall, color = colors.textMuted)
                    }
                }
            }

            // How it works
            SectionHeader(title = "How It Works")
            GlassCard {
                StepRow("1", "SMS flagged as scam by ML models")
                Spacer(Modifier.height(8.dp))
                StepRow("2", "Sender phone & URLs auto-added to local blocklist")
                Spacer(Modifier.height(8.dp))
                StepRow("3", "Future messages from blocked senders get boosted risk scores")
                Spacer(Modifier.height(8.dp))
                StepRow("4", "Hashes are SHA-256 — original data is never stored")
            }

            RakshakXFooter()
        }
    }

    // Add to blocklist dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false; addInput = "" },
            containerColor = colors.surfaceElevated,
            title = { Text("Add to Blocklist", color = colors.textPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = addType == BlocklistType.PHONE,
                            onClick = { addType = BlocklistType.PHONE },
                            label = { Text("Phone") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colors.primary.copy(alpha = 0.2f),
                                selectedLabelColor = colors.primary
                            )
                        )
                        FilterChip(
                            selected = addType == BlocklistType.DOMAIN,
                            onClick = { addType = BlocklistType.DOMAIN },
                            label = { Text("Domain") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colors.primary.copy(alpha = 0.2f),
                                selectedLabelColor = colors.primary
                            )
                        )
                    }
                    OutlinedTextField(
                        value = addInput,
                        onValueChange = { addInput = it },
                        label = {
                            Text(
                                if (addType == BlocklistType.PHONE) "Phone number" else "Domain (e.g. scam-site.com)",
                                color = colors.textMuted
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.primary,
                            unfocusedBorderColor = colors.border,
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary,
                            cursorColor = colors.primary
                        )
                    )
                    Text(
                        "The value will be SHA-256 hashed before storage. The original is never saved.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textMuted
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (addInput.isNotBlank()) {
                            manager.addToBlocklist(addInput.trim(), addType)
                            addInput = ""
                            showAddDialog = false
                        }
                    }
                ) {
                    Text("Block", color = colors.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false; addInput = "" }) {
                    Text("Cancel", color = colors.textMuted)
                }
            }
        )
    }
}

@Composable
private fun PrivacyGuaranteeRow(text: String) {
    val colors = LocalRakshakXColors.current
    Row(
        modifier = Modifier.padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.CheckCircle, null, tint = colors.safe, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
    }
}

@Composable
private fun StepRow(number: String, text: String) {
    val colors = LocalRakshakXColors.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(26.dp).background(colors.primary.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(number, style = MaterialTheme.typography.labelMedium, color = colors.primary, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
    }
}
