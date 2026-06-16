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

    val isOptedIn by manager.isOptedIn.collectAsState()
    val blocklistSize = manager.getBlocklistSize()
    val lastSync = manager.getLastSyncTime()

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

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { haptics.tick(); onBack() },
                    modifier = Modifier.size(40.dp).background(colors.surfaceElevated, RoundedCornerShape(12.dp))
                ) {
                    Icon(Icons.Filled.ArrowBack, null, tint = colors.textPrimary, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text("Threat Intelligence", style = MaterialTheme.typography.headlineSmall, color = colors.textPrimary, fontWeight = FontWeight.Bold)
                    Text("Community protection network", style = MaterialTheme.typography.bodySmall, color = colors.textMuted)
                }
            }

            // Opt-in Card
            GlassCard(borderColor = if (isOptedIn) colors.primary.copy(alpha = 0.15f) else colors.border.copy(alpha = 0.3f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Community Sharing", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Anonymously share threat indicators (hashed phone numbers & domains only). No message content is ever shared.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textMuted
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Switch(
                        checked = isOptedIn,
                        onCheckedChange = { manager.setOptIn(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = colors.primary,
                            checkedTrackColor = colors.primary.copy(alpha = 0.3f)
                        )
                    )
                }
            }

            // Privacy Guarantee
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = colors.safeBg)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Privacy Guarantee", style = MaterialTheme.typography.titleSmall, color = colors.safe, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    PrivacyGuaranteeRow("Only SHA-256 hashes shared, never raw data")
                    PrivacyGuaranteeRow("No message content leaves your device")
                    PrivacyGuaranteeRow("Differential privacy for all shared data")
                    PrivacyGuaranteeRow("Community data supplements, never overrides ML")
                }
            }

            // Stats
            SectionHeader(title = "Local Blocklist")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GlassSurface(modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$blocklistSize", style = MaterialTheme.typography.headlineSmall, color = colors.primary, fontWeight = FontWeight.Bold)
                        Text("Blocked Entries", style = MaterialTheme.typography.labelSmall, color = colors.textMuted, letterSpacing = 0.5.sp)
                    }
                }
                GlassSurface(modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            if (lastSync > 0L)
                                SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(lastSync))
                            else "Never",
                            style = MaterialTheme.typography.headlineSmall,
                            color = colors.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text("Last Sync", style = MaterialTheme.typography.labelSmall, color = colors.textMuted, letterSpacing = 0.5.sp)
                    }
                }
            }

            // How it works
            SectionHeader(title = "How It Works")
            GlassCard {
                StepRow("1", "Threat detected on your device by ML models")
                Spacer(Modifier.height(8.dp))
                StepRow("2", "Phone/domain hashed with SHA-256 (irreversible)")
                Spacer(Modifier.height(8.dp))
                StepRow("3", "Hash shared anonymously if opted in")
                Spacer(Modifier.height(8.dp))
                StepRow("4", "Community blocklist updated for all users")
            }

            RakshakXFooter()
        }
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
