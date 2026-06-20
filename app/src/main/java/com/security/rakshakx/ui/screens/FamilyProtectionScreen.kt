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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.security.rakshakx.core.family.FamilyMember
import com.security.rakshakx.core.family.FamilyProtectionManager
import com.security.rakshakx.core.family.FamilyRole
import com.security.rakshakx.ui.anim.rememberHaptics
import com.security.rakshakx.ui.components.*
import com.security.rakshakx.ui.theme.*
import java.util.UUID

@Composable
fun FamilyProtectionScreen(onBack: () -> Unit) {
    val colors = LocalRakshakXColors.current
    val context = LocalContext.current
    val haptics = rememberHaptics()
    val manager = remember { FamilyProtectionManager.getInstance(context) }

    val isEnabled by manager.isEnabled.collectAsState()
    val simplifiedUi by manager.simplifiedUi.collectAsState()
    val userRole by manager.userRole.collectAsState()
    var members by remember { mutableStateOf(manager.getMembers()) }
    var showAddMemberDialog by remember { mutableStateOf(false) }

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
                title = "Family Protection",
                infoText = "Preview family protection features with local profiles and simplified UI for family members.",
                onBack = { haptics.tick(); onBack() }
            )

            // Concept Disclaimer Banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = colors.warningBg)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Filled.Info, null, tint = colors.warning, modifier = Modifier.size(20.dp))
                    Column {
                        Text(
                            "Concept Demonstration",
                            style = MaterialTheme.typography.titleSmall,
                            color = colors.warning,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Family Protection requires a cloud backend for cross-device communication, " +
                                "which is not feasible in the current offline-first architecture. " +
                                "This screen demonstrates the planned UX and local profile management. " +
                                "Remote monitoring, push alerts, and cross-device sync are planned for a future upgrade.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary
                        )
                    }
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
                            "Enable local family profiles and simplified UI options",
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SectionHeader(title = "Family Members (${members.size})")
                    IconButton(
                        onClick = { showAddMemberDialog = true },
                        modifier = Modifier.size(36.dp).background(colors.primary.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                    ) {
                        Icon(Icons.Filled.PersonAdd, null, tint = colors.primary, modifier = Modifier.size(18.dp))
                    }
                }

                if (members.isEmpty()) {
                    GlassCard {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Filled.GroupAdd, null, tint = colors.textMuted, modifier = Modifier.size(40.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("No family members added", style = MaterialTheme.typography.bodyMedium, color = colors.textMuted)
                            Text("Add profiles to organize protection roles", style = MaterialTheme.typography.bodySmall, color = colors.textMuted)
                        }
                    }
                } else {
                    members.forEach { member ->
                        FamilyMemberCard(
                            member = member,
                            colors = colors,
                            onRemove = {
                                manager.removeFamilyMember(member.id)
                                members = manager.getMembers()
                            }
                        )
                    }
                }

                // What works now vs. future
                SectionHeader(title = "Available Now")
                FeatureRow(Icons.Filled.Person, "Local Profiles", "Create family member profiles with roles")
                FeatureRow(Icons.Filled.TextFields, "Simplified UI", "Large text mode for accessibility")
                FeatureRow(Icons.Filled.Security, "Role Labels", "Organize family members by Admin, Elder, Child")

                SectionHeader(title = "Planned (Requires Cloud Backend)")
                FeatureRow(Icons.Filled.Visibility, "Remote Monitoring", "View threat alerts across devices", dimmed = true)
                FeatureRow(Icons.Filled.Notifications, "Push Alerts", "Cross-device threat notifications", dimmed = true)
                FeatureRow(Icons.Filled.Block, "Remote Blocklist", "Shared blocklist across family", dimmed = true)
                FeatureRow(Icons.Filled.Sync, "Device Sync", "Real-time sync between family devices", dimmed = true)
            }

            RakshakXFooter()
        }
    }

    // Add member dialog
    if (showAddMemberDialog) {
        var memberName by remember { mutableStateOf("") }
        var memberRole by remember { mutableStateOf(FamilyRole.ELDER) }
        var alertEnabled by remember { mutableStateOf(true) }

        AlertDialog(
            onDismissRequest = { showAddMemberDialog = false },
            containerColor = colors.surfaceElevated,
            title = { Text("Add Family Member", color = colors.textPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = memberName,
                        onValueChange = { memberName = it },
                        label = { Text("Name", color = colors.textMuted) },
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
                    Text("Role", style = MaterialTheme.typography.labelMedium, color = colors.textMuted)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(FamilyRole.ELDER, FamilyRole.CHILD).forEach { role ->
                            FilterChip(
                                selected = memberRole == role,
                                onClick = { memberRole = role },
                                label = { Text(role.label.split(" ").first()) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = colors.primary.copy(alpha = 0.2f),
                                    selectedLabelColor = colors.primary
                                )
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Alert on critical threats", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                        Switch(
                            checked = alertEnabled,
                            onCheckedChange = { alertEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = colors.primary,
                                checkedTrackColor = colors.primary.copy(alpha = 0.3f)
                            )
                        )
                    }
                    Text(
                        "Profiles are stored locally on this device only.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textMuted
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (memberName.isNotBlank()) {
                            manager.addFamilyMember(
                                FamilyMember(
                                    id = UUID.randomUUID().toString(),
                                    name = memberName.trim(),
                                    role = memberRole,
                                    alertOnCritical = alertEnabled
                                )
                            )
                            members = manager.getMembers()
                            showAddMemberDialog = false
                        }
                    }
                ) {
                    Text("Add", color = colors.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddMemberDialog = false }) {
                    Text("Cancel", color = colors.textMuted)
                }
            }
        )
    }
}

@Composable
private fun FamilyMemberCard(member: FamilyMember, colors: RakshakXColors, onRemove: () -> Unit) {
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
            IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.Close, null, tint = colors.textMuted, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun FeatureRow(icon: ImageVector, title: String, desc: String, dimmed: Boolean = false) {
    val colors = LocalRakshakXColors.current
    val alpha = if (dimmed) 0.45f else 1f
    GlassSurface {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, null, tint = colors.primary.copy(alpha = alpha), modifier = Modifier.size(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary.copy(alpha = alpha))
                Text(desc, style = MaterialTheme.typography.bodySmall, color = colors.textMuted.copy(alpha = alpha))
            }
            if (dimmed) {
                StatusChip(text = "Future", color = colors.textMuted)
            }
        }
    }
}
