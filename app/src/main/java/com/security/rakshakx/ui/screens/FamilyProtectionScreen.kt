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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.security.rakshakx.core.family.FamilyMember
import com.security.rakshakx.core.family.FamilyProtectionManager
import com.security.rakshakx.core.family.FamilyRole
import com.security.rakshakx.ui.anim.rememberHaptics
import com.security.rakshakx.ui.components.*
import com.security.rakshakx.ui.theme.*

@Composable
fun FamilyProtectionScreen(onBack: () -> Unit) {
    val colors = LocalRakshakXColors.current
    val context = LocalContext.current
    val haptics = rememberHaptics()
    val manager = remember { FamilyProtectionManager.getInstance(context) }

    val isEnabled by manager.isEnabled.collectAsState()
    val simplifiedUi by manager.simplifiedUi.collectAsState()
    val userRole by manager.userRole.collectAsState()
    val members = remember(isEnabled) { manager.getMembers() }

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
                    Text("Family Protection", style = MaterialTheme.typography.headlineSmall, color = colors.textPrimary, fontWeight = FontWeight.Bold)
                    Text("Protect your loved ones", style = MaterialTheme.typography.bodySmall, color = colors.textMuted)
                }
            }

            // Enable Toggle
            GlassCard(borderColor = if (isEnabled) colors.primary.copy(alpha = 0.15f) else colors.border.copy(alpha = 0.3f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Family Mode", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Enable remote monitoring and simplified UI for family members",
                            style = MaterialTheme.typography.bodySmall, color = colors.textMuted
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { manager.setEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = colors.primary,
                            checkedTrackColor = colors.primary.copy(alpha = 0.3f)
                        )
                    )
                }
            }

            if (isEnabled) {
                // Role Selection
                SectionHeader(title = "Your Role")
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FamilyRole.entries.forEach { role ->
                        val isSelected = userRole == role
                        GlassSurface(
                            onClick = { manager.setRole(role) },
                            modifier = Modifier.weight(1f),
                            borderColor = if (isSelected) colors.primary.copy(alpha = 0.3f) else colors.border.copy(alpha = 0.3f)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    when (role) {
                                        FamilyRole.ADMIN -> Icons.Filled.AdminPanelSettings
                                        FamilyRole.ELDER -> Icons.Filled.Elderly
                                        FamilyRole.CHILD -> Icons.Filled.ChildCare
                                        FamilyRole.SELF -> Icons.Filled.Person
                                    },
                                    null,
                                    tint = if (isSelected) colors.primary else colors.textMuted,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    role.label.split(" ").first(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) colors.primary else colors.textMuted,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                // Simplified UI toggle
                GlassCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Simplified Interface", style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                            Text("Large text, fewer options for non-tech-savvy users", style = MaterialTheme.typography.bodySmall, color = colors.textMuted)
                        }
                        Switch(
                            checked = simplifiedUi,
                            onCheckedChange = { manager.setSimplifiedUi(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = colors.primary,
                                checkedTrackColor = colors.primary.copy(alpha = 0.3f)
                            )
                        )
                    }
                }

                // Family Members
                SectionHeader(title = "Family Members (${members.size})")
                if (members.isEmpty()) {
                    GlassCard {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Filled.GroupAdd, null, tint = colors.textMuted, modifier = Modifier.size(40.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("No family members added", style = MaterialTheme.typography.bodyMedium, color = colors.textMuted)
                            Text("Invite members to share protection", style = MaterialTheme.typography.bodySmall, color = colors.textMuted)
                        }
                    }
                } else {
                    members.forEach { member ->
                        FamilyMemberCard(member, colors)
                    }
                }

                // Features Description
                SectionHeader(title = "Features")
                FeatureRow(Icons.Filled.Visibility, "Remote Monitoring", "View threat alerts for monitored members")
                FeatureRow(Icons.Filled.Notifications, "Push Alerts", "Get notified when critical threats are detected")
                FeatureRow(Icons.Filled.Block, "Remote Blocklist", "Manage blocked numbers for family members")
                FeatureRow(Icons.Filled.TextFields, "Large Text Mode", "Simplified UI with bigger fonts")
            }

            RakshakXFooter()
        }
    }
}

@Composable
private fun FamilyMemberCard(member: FamilyMember, colors: RakshakXColors) {
    GlassSurface {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier.size(40.dp).background(colors.primary.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Person, null, tint = colors.primary, modifier = Modifier.size(22.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(member.name, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                Text(member.role.label, style = MaterialTheme.typography.bodySmall, color = colors.textMuted)
            }
            if (member.alertOnCritical) {
                StatusChip(text = "Alerts On", color = colors.safe)
            }
        }
    }
}

@Composable
private fun FeatureRow(icon: ImageVector, title: String, desc: String) {
    val colors = LocalRakshakXColors.current
    GlassSurface {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, null, tint = colors.primary, modifier = Modifier.size(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                Text(desc, style = MaterialTheme.typography.bodySmall, color = colors.textMuted)
            }
        }
    }
}
