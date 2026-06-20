package com.security.rakshakx.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.security.rakshakx.core.appsecurity.AppRiskLevel
import com.security.rakshakx.core.appsecurity.AppRiskProfile
import com.security.rakshakx.core.appsecurity.AppSecurityAuditor
import com.security.rakshakx.core.appsecurity.InstallSource
import com.security.rakshakx.core.appsecurity.PermissionDetail
import com.security.rakshakx.ui.anim.ShimmerPlaceholder
import com.security.rakshakx.ui.anim.StaggeredEntry
import com.security.rakshakx.ui.anim.rememberHaptics
import com.security.rakshakx.ui.components.*
import com.security.rakshakx.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────
// AppAuditScreen
// ─────────────────────────────────────────────────────────────────

@Composable
fun AppAuditScreen(onBack: () -> Unit) {
    val colors  = LocalRakshakXColors.current
    val context = LocalContext.current
    val haptics = rememberHaptics()
    val scope   = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(true) }
    var apps by remember { mutableStateOf<List<AppRiskProfile>>(emptyList()) }
    var activeFilter by remember { mutableStateOf<AppRiskLevel?>(null) }

    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            val auditor = AppSecurityAuditor.getInstance(context)
            apps      = auditor.auditInstalledApps()
            isLoading = false
        }
    }

    val filteredApps = remember(apps, activeFilter) {
        if (activeFilter == null) apps
        else apps.filter { it.riskLevel == activeFilter }
    }

    val highCriticalCount  = remember(apps) { apps.count { it.riskLevel == AppRiskLevel.HIGH || it.riskLevel == AppRiskLevel.CRITICAL } }
    val sideloadedCount    = remember(apps) { apps.count { it.installSource == InstallSource.SIDELOADED } }

    PremiumBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // ── Sticky header (not scrollable) ────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // Back + title
                PageHeader(
                    title = "App Audit",
                    infoText = "Scan installed apps for dangerous permissions and security risks.",
                    onBack = { haptics.tick(); onBack() }
                )

                // Summary stats
                if (!isLoading) {
                    StaggeredEntry(index = 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            StatChip(
                                label = "Scanned",
                                value = "${apps.size}",
                                color = colors.primary,
                                modifier = Modifier.weight(1f)
                            )
                            StatChip(
                                label = "High Risk",
                                value = "$highCriticalCount",
                                color = if (highCriticalCount > 0) colors.critical else colors.safe,
                                modifier = Modifier.weight(1f)
                            )
                            StatChip(
                                label = "Sideloaded",
                                value = "$sideloadedCount",
                                color = if (sideloadedCount > 0) colors.warning else colors.safe,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Filter chips
                if (!isLoading) {
                    StaggeredEntry(index = 1) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            item {
                                FilterChipItem(
                                    label = "All",
                                    selected = activeFilter == null,
                                    color = colors.primary,
                                    onClick = { haptics.tick(); activeFilter = null }
                                )
                            }
                            item {
                                FilterChipItem(
                                    label = "Critical",
                                    selected = activeFilter == AppRiskLevel.CRITICAL,
                                    color = colors.critical,
                                    onClick = {
                                        haptics.tick()
                                        activeFilter = if (activeFilter == AppRiskLevel.CRITICAL) null else AppRiskLevel.CRITICAL
                                    }
                                )
                            }
                            item {
                                FilterChipItem(
                                    label = "High",
                                    selected = activeFilter == AppRiskLevel.HIGH,
                                    color = Amber,
                                    onClick = {
                                        haptics.tick()
                                        activeFilter = if (activeFilter == AppRiskLevel.HIGH) null else AppRiskLevel.HIGH
                                    }
                                )
                            }
                            item {
                                FilterChipItem(
                                    label = "Medium",
                                    selected = activeFilter == AppRiskLevel.MEDIUM,
                                    color = Gold,
                                    onClick = {
                                        haptics.tick()
                                        activeFilter = if (activeFilter == AppRiskLevel.MEDIUM) null else AppRiskLevel.MEDIUM
                                    }
                                )
                            }
                            item {
                                FilterChipItem(
                                    label = "Safe",
                                    selected = activeFilter == AppRiskLevel.SAFE,
                                    color = Emerald,
                                    onClick = {
                                        haptics.tick()
                                        activeFilter = if (activeFilter == AppRiskLevel.SAFE) null else AppRiskLevel.SAFE
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
            }

            // ── Content (lazy list) ───────────────────────────────
            if (isLoading) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    repeat(5) { idx ->
                        StaggeredEntry(index = idx) {
                            ShimmerPlaceholder(height = 110.dp)
                        }
                    }
                }
            } else if (filteredApps.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyState(
                        icon = Icons.Filled.CheckCircle,
                        title = "All Apps Appear Safe",
                        description = "No risky apps found matching this filter.",
                        useLottie = true
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 20.dp,
                        end   = 20.dp,
                        top   = 4.dp,
                        bottom = 32.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(
                        items = filteredApps,
                        key   = { _, app -> app.packageName }
                    ) { idx, app ->
                        StaggeredEntry(index = idx) {
                            AppRiskCard(profile = app)
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// AppRiskCard
// ─────────────────────────────────────────────────────────────────

@Composable
private fun AppRiskCard(profile: AppRiskProfile) {
    val colors  = LocalRakshakXColors.current
    val riskColor = riskLevelColor(profile.riskLevel)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, riskColor.copy(alpha = 0.15f), RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ── Row 1: app name + risk badge ──────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(riskColor.copy(alpha = 0.10f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Apps,
                            contentDescription = null,
                            tint = riskColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = profile.appName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textPrimary,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = profile.packageName,
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                // Risk score badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(riskColor.copy(alpha = 0.12f))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${profile.riskScore}",
                        style = MaterialTheme.typography.titleSmall,
                        color = riskColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // ── Row 2: risk level chip + install source ───────────
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusChip(text = profile.riskLevel.label, color = riskColor)
                InstallSourceChip(source = profile.installSource)
                if (profile.versionName != "unknown") {
                    Text(
                        text = "v${profile.versionName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textMuted
                    )
                }
            }

            // ── Dangerous permissions (top 3) ─────────────────────
            val topPerms = profile.dangerousPermissions.take(3)
            if (topPerms.isNotEmpty()) {
                HorizontalDivider(color = colors.border.copy(alpha = 0.3f), thickness = 0.5.dp)
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(
                        text = "Risky Permissions",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textMuted
                    )
                    topPerms.forEach { perm ->
                        PermissionRow(perm = perm)
                    }
                    if (profile.dangerousPermissions.size > 3) {
                        Text(
                            text = "+${profile.dangerousPermissions.size - 3} more",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textMuted
                        )
                    }
                }
            }

            // ── Risk reasons ──────────────────────────────────────
            if (profile.riskReasons.isNotEmpty()) {
                HorizontalDivider(color = colors.border.copy(alpha = 0.3f), thickness = 0.5.dp)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    profile.riskReasons.forEach { reason ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                Icons.Filled.Warning,
                                contentDescription = null,
                                tint = colors.warning,
                                modifier = Modifier.size(12.dp).padding(top = 1.dp)
                            )
                            Text(
                                text = reason,
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.warning
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Permission row
// ─────────────────────────────────────────────────────────────────

@Composable
private fun PermissionRow(perm: PermissionDetail) {
    val colors = LocalRakshakXColors.current
    val permColor = when (perm.riskLevel) {
        "Critical" -> colors.critical
        "High"     -> Amber
        "Medium"   -> Gold
        else       -> RoyalBlue
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(permColor)
        )
        Text(
            text = perm.shortName,
            style = MaterialTheme.typography.labelSmall,
            color = colors.textSecondary,
            modifier = Modifier.weight(1f)
        )
        StatusChip(text = perm.riskLevel, color = permColor)
    }
}

// ─────────────────────────────────────────────────────────────────
// Install source chip
// ─────────────────────────────────────────────────────────────────

@Composable
private fun InstallSourceChip(source: InstallSource) {
    val colors = LocalRakshakXColors.current
    val (label, color) = when (source) {
        InstallSource.PLAY_STORE  -> "Play Store"  to colors.safe
        InstallSource.SIDELOADED  -> "Sideloaded"  to colors.critical
        InstallSource.SYSTEM      -> "System"       to colors.primary
        InstallSource.UNKNOWN     -> "Unknown"      to colors.warning
    }
    StatusChip(text = label, color = color)
}

// ─────────────────────────────────────────────────────────────────
// Stat chip
// ─────────────────────────────────────────────────────────────────

@Composable
private fun StatChip(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    val colors = LocalRakshakXColors.current
    GlassSurface(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = color,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = colors.textMuted
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Filter chip
// ─────────────────────────────────────────────────────────────────

@Composable
private fun FilterChipItem(
    label: String,
    selected: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    val colors = LocalRakshakXColors.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (selected) color.copy(alpha = 0.18f) else colors.surfaceElevated,
        modifier = Modifier.border(
            width = 1.dp,
            color = if (selected) color.copy(alpha = 0.5f) else colors.border.copy(alpha = 0.4f),
            shape = RoundedCornerShape(20.dp)
        )
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) color else colors.textSecondary,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────────
// Helper — risk level to color
// ─────────────────────────────────────────────────────────────────

@Composable
private fun riskLevelColor(level: AppRiskLevel): Color {
    val colors = LocalRakshakXColors.current
    return when (level) {
        AppRiskLevel.CRITICAL -> colors.critical
        AppRiskLevel.HIGH     -> Amber
        AppRiskLevel.MEDIUM   -> Gold
        AppRiskLevel.LOW      -> RoyalBlue
        AppRiskLevel.SAFE     -> colors.safe
    }
}
