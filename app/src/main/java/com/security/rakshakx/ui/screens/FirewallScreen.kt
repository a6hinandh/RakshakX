package com.security.rakshakx.ui.screens

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.security.rakshakx.core.firewall.FirewallRule
import com.security.rakshakx.core.firewall.FirewallRuleStore
import com.security.rakshakx.ui.anim.StaggeredEntry
import com.security.rakshakx.ui.anim.rememberHaptics
import com.security.rakshakx.ui.components.*
import com.security.rakshakx.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ─── Data carrier for UI ─────────────────────────────────────────────────────

private data class AppFirewallEntry(
    val packageName: String,
    val appName: String,
    val icon: ImageBitmap?,
    val sensitivePermissions: List<String>,
    val riskLevel: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FirewallScreen(onBack: () -> Unit) {
    val colors = LocalRakshakXColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptics = rememberHaptics()

    var isLoading by remember { mutableStateOf(true) }
    var apps by remember { mutableStateOf<List<AppFirewallEntry>>(emptyList()) }
    var selectedApp by remember { mutableStateOf<AppFirewallEntry?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var riskFilter by remember { mutableStateOf("ALL") }

    // Load installed non-system apps + merge with saved rules
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val installed = pm.getInstalledApplications(PackageManager.GET_META_DATA)

            val entries = installed
                .filter { app ->
                    // Exclude system packages
                    (app.flags and ApplicationInfo.FLAG_SYSTEM) == 0 &&
                            app.packageName != context.packageName
                }
                .map { app ->
                    val label = pm.getApplicationLabel(app).toString()
                    
                    val iconDrawable = app.loadIcon(pm)
                    val iconBitmap = try {
                        iconDrawable.toBitmap(120, 120).asImageBitmap()
                    } catch (e: Exception) {
                        null
                    }
                    
                    val packageInfo = try {
                        pm.getPackageInfo(app.packageName, PackageManager.GET_PERMISSIONS)
                    } catch (e: Exception) {
                        null
                    }
                    val reqPermissions = packageInfo?.requestedPermissions?.toList() ?: emptyList()
                    val sensitive = reqPermissions.filter {
                        it.contains("LOCATION") || it.contains("CAMERA") || it.contains("MICROPHONE") || it.contains("CONTACTS") || it.contains("STORAGE")
                    }
                    val riskLevel = when {
                        sensitive.size >= 4 -> "HIGH"
                        sensitive.isNotEmpty() -> "MEDIUM"
                        else -> "LOW"
                    }

                    AppFirewallEntry(
                        packageName = app.packageName,
                        appName = label,
                        icon = iconBitmap,
                        sensitivePermissions = sensitive,
                        riskLevel = riskLevel
                    )
                }
                .sortedBy { it.appName.lowercase() }

            apps = entries
            isLoading = false
        }
    }

    PremiumBackground {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {

            // ── Header ────────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 8.dp, bottom = 4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                StaggeredEntry(index = 0) {
                    PageHeader(
                        title = "App Privacy Audit",
                        infoText = "Audit app permissions and control which apps can access sensitive device features.",
                        onBack = { haptics.tick(); onBack() }
                    )
                }

                Spacer(Modifier.height(12.dp))

                // ── Info banner ───────────────────────────────────────────
                StaggeredEntry(index = 1) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.warningBg)
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Icons.Filled.Info,
                            contentDescription = null,
                            tint = colors.warning,
                            modifier = Modifier.size(16.dp).padding(top = 1.dp)
                        )
                        Text(
                            "The App Firewall is currently a conceptual preview. Network enforcement relies on deep packet inspection which is not yet available in the lightweight DNS relay architecture.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.warning
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Search & Filter
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search apps...") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                        focusedBorderColor = colors.primary,
                        unfocusedBorderColor = colors.border
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf("ALL", "HIGH", "MEDIUM", "LOW").forEach { risk ->
                        FilterChip(
                            selected = riskFilter == risk,
                            onClick = { riskFilter = risk },
                            label = { Text(risk) }
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))
            }

            // ── Body ──────────────────────────────────────────────────────
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(color = colors.primary, strokeWidth = 2.dp)
                        Text(
                            "Loading installed apps…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textSecondary
                        )
                    }
                }
            } else {
                // Apply filters
                val filteredApps = apps.filter { app ->
                    val matchesSearch = app.appName.contains(searchQuery, ignoreCase = true) || 
                                        app.packageName.contains(searchQuery, ignoreCase = true)
                    val matchesRisk = riskFilter == "ALL" || app.riskLevel == riskFilter
                    matchesSearch && matchesRisk
                }

                // Column header row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${filteredApps.size} apps",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textMuted
                    )
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredApps, key = { it.packageName }) { entry ->
                        AppFirewallRow(
                            entry = entry,
                            onClick = { selectedApp = entry }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        RakshakXFooter()
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
    
    // App Details Dialog
    if (selectedApp != null) {
        AppDetailsDialog(
            entry = selectedApp!!,
            onDismiss = { selectedApp = null }
        )
    }
}

// ── App row ───────────────────────────────────────────────────────────────────

@Composable
private fun AppFirewallRow(
    entry: AppFirewallEntry,
    onClick: () -> Unit
) {
    val colors = LocalRakshakXColors.current
    val borderColor = colors.border

    GlassCard(borderColor = borderColor) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar + name
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon or First-letter avatar
                if (entry.icon != null) {
                    Image(
                        bitmap = entry.icon,
                        contentDescription = "App Icon",
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.surfaceElevated),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = entry.appName.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = entry.appName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textPrimary,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = entry.packageName,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(3.dp))
                    // Status badges
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val riskColor = when(entry.riskLevel) {
                            "HIGH" -> colors.critical
                            "MEDIUM" -> colors.warning
                            else -> colors.safe
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(riskColor.copy(alpha = 0.12f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Risk: ${entry.riskLevel}",
                                style = MaterialTheme.typography.labelSmall,
                                color = riskColor,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppDetailsDialog(
    entry: AppFirewallEntry,
    onDismiss: () -> Unit
) {
    val colors = LocalRakshakXColors.current
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surfaceElevated,
        titleContentColor = colors.textPrimary,
        textContentColor = colors.textSecondary,
        title = { Text(entry.appName, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Risk Level: ${entry.riskLevel}")
                Text("Sensitive Permissions: ${entry.sensitivePermissions.size}")
                
                if (entry.sensitivePermissions.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text("Detected Permissions:", color = colors.textPrimary, fontWeight = FontWeight.SemiBold)
                    entry.sensitivePermissions.forEach { perm ->
                        val shortName = perm.substringAfterLast(".")
                        Text("• $shortName", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close", color = colors.primary) }
        }
    )
}
