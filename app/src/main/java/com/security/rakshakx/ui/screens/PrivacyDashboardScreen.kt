package com.security.rakshakx.ui.screens

import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.security.rakshakx.core.privacy.TrackerCategory
import com.security.rakshakx.core.privacy.TrackerDatabase
import com.security.rakshakx.core.privacy.TrackerSignature
import com.security.rakshakx.core.privacy.TrackerDetection
import com.security.rakshakx.ui.anim.StaggeredEntry
import com.security.rakshakx.ui.anim.rememberHaptics
import com.security.rakshakx.ui.components.*
import com.security.rakshakx.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class AppTrackerInfo(
    val packageName: String,
    val appName: String,
    val detections: List<TrackerDetection>
)

@Composable
fun PrivacyDashboardScreen(onBack: () -> Unit) {
    val colors = LocalRakshakXColors.current
    val context = LocalContext.current
    val haptics = rememberHaptics()
    val scope = rememberCoroutineScope()

    var deepScanEnabled by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var appTrackerList by remember { mutableStateOf<List<AppTrackerInfo>>(emptyList()) }
    var categoryBreakdown by remember { mutableStateOf<Map<TrackerCategory, Int>>(emptyMap()) }
    val expandedApps = remember { mutableStateListOf<String>() }

    var scannedCount by remember { mutableStateOf(0) }
    var totalAppsCount by remember { mutableStateOf(0) }
    var currentlyScanningApp by remember { mutableStateOf("") }

    LaunchedEffect(deepScanEnabled) {
        isLoading = true
        scannedCount = 0
        currentlyScanningApp = "Initializing package scanner..."
        scope.launch(Dispatchers.IO) {
            val pm = context.packageManager
            val installedApps = try {
                pm.getInstalledPackages(0).map { it.packageName }
            } catch (e: Exception) {
                emptyList()
            }
            withContext(Dispatchers.Main) {
                totalAppsCount = installedApps.size
            }

            val detectedMap = TrackerDatabase.detectTrackers(
                context = context,
                installedPackages = installedApps,
                deepScan = deepScanEnabled,
                onProgress = { index, pkgName ->
                    if (index % 5 == 0 || index == installedApps.size - 1) {
                        scannedCount = index + 1
                        currentlyScanningApp = try {
                            pm.getApplicationLabel(pm.getApplicationInfo(pkgName, 0)).toString()
                        } catch (e: Exception) {
                            pkgName.substringAfterLast(".")
                        }
                    }
                }
            )
            val allDetections = detectedMap.values.flatten()
            val breakdown = TrackerDatabase.countDetectionsByCategory(allDetections)
            val appInfoList = detectedMap.entries
                .filter { it.value.isNotEmpty() }
                .map { (pkg, detectionList) ->
                    val appLabel = try {
                        pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
                    } catch (e: Exception) {
                        pkg.substringAfterLast(".")
                    }
                    AppTrackerInfo(pkg, appLabel, detectionList)
                }
                .sortedByDescending { it.detections.size }

            withContext(Dispatchers.Main) {
                appTrackerList = appInfoList
                categoryBreakdown = breakdown
                isLoading = false
            }
        }
    }

    val totalTrackerDomains = TrackerDatabase.getBlockDomains().size
    val totalAppsWithTrackers = appTrackerList.size
    val totalTrackerInstances = appTrackerList.sumOf { it.detections.size }

    PremiumBackground {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                PageHeader(
                    title = "Privacy Dashboard",
                    infoText = "Analyze tracker exposure per app with 400+ tracker signatures to see which apps collect your data.",
                    onBack = { haptics.click(); onBack() }
                )
            }

            item {
                StaggeredEntry(index = 0) {
                    GlassCard {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(colors.warning.copy(alpha = 0.1f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Filled.Security,
                                        null,
                                        tint = colors.warning,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        "Deep Component Scan",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = colors.textPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        "Audits app services, receivers, and providers",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.textMuted
                                    )
                                }
                            }
                            Switch(
                                checked = deepScanEnabled,
                                onCheckedChange = {
                                    haptics.click()
                                    deepScanEnabled = it
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = colors.warning,
                                    checkedTrackColor = colors.warning.copy(alpha = 0.4f),
                                    uncheckedThumbColor = colors.textMuted,
                                    uncheckedTrackColor = colors.border.copy(alpha = 0.3f)
                                )
                            )
                        }
                    }
                }
            }

            item {
                StaggeredEntry(index = 1) {
                    GlassCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            TrackerStatItem(
                                value = totalAppsWithTrackers.toString(),
                                label = "Apps with\nTrackers",
                                color = colors.warning
                            )
                            TrackerStatItem(
                                value = totalTrackerInstances.toString(),
                                label = "Tracker\nInstances",
                                color = colors.critical
                            )
                            TrackerStatItem(
                                value = totalTrackerDomains.toString(),
                                label = "Domains\nBlocked",
                                color = colors.safe
                            )
                        }
                    }
                }
            }

            item {
                StaggeredEntry(index = 2) {
                    SectionHeader(title = "Tracker Category Breakdown")
                }
            }

            if (isLoading) {
                item {
                    StaggeredEntry(index = 3) {
                        GlassCard(borderColor = colors.warning.copy(alpha = 0.3f)) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = colors.warning,
                                    modifier = Modifier.size(48.dp)
                                )
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        "Analyzing System Packages...",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = colors.textPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "Scanned: $scannedCount of $totalAppsCount apps",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = colors.warning,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        currentlyScanningApp,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.textMuted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = if (totalAppsCount > 0) scannedCount.toFloat() / totalAppsCount else 0f,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(CircleShape),
                                    color = colors.warning,
                                    trackColor = colors.border.copy(alpha = 0.2f)
                                )
                            }
                        }
                    }
                }
                items(3) { i ->
                    ShimmerTrackerRow()
                }
            } else {
                item {
                    StaggeredEntry(index = 2) {
                        GlassCard {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                val categoryTotal = categoryBreakdown.values.sum().coerceAtLeast(1)
                                TrackerCategory.values().forEach { cat ->
                                    val count = categoryBreakdown[cat] ?: 0
                                    if (count > 0 || categoryBreakdown.isEmpty()) {
                                        CategoryProgressBar(
                                            category = cat,
                                            count = count,
                                            total = categoryTotal,
                                            colors = colors
                                        )
                                    }
                                }
                                if (categoryBreakdown.isEmpty()) {
                                    Text(
                                        "No trackers detected",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = colors.textMuted
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    StaggeredEntry(index = 3) {
                        GlassCard(borderColor = colors.safe.copy(alpha = 0.3f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(colors.safeBg, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.Security, null, tint = colors.safe, modifier = Modifier.size(24.dp))
                                }
                                Column {
                                    Text(
                                        "$totalTrackerDomains tracker domains identified",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = colors.safe,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        "Based on Exodus Privacy tracker database",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.textMuted
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    StaggeredEntry(index = 4) {
                        SectionHeader(title = "App Tracker Exposure (${appTrackerList.size} apps)")
                    }
                }

                if (appTrackerList.isEmpty()) {
                    item {
                        StaggeredEntry(index = 5) {
                            GlassCard {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Filled.VerifiedUser, null, tint = colors.safe, modifier = Modifier.size(40.dp))
                                    Text(
                                        "No trackers detected",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = colors.textPrimary
                                    )
                                    Text(
                                        "Your installed apps don't match any known tracker signatures",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.textMuted
                                    )
                                }
                            }
                        }
                    }
                } else {
                    itemsIndexed(appTrackerList) { index, info ->
                        StaggeredEntry(index = 5 + index) {
                            AppTrackerRow(
                                info = info,
                                isExpanded = expandedApps.contains(info.packageName),
                                onToggle = {
                                    haptics.tick()
                                    if (expandedApps.contains(info.packageName)) {
                                        expandedApps.remove(info.packageName)
                                    } else {
                                        expandedApps.add(info.packageName)
                                    }
                                },
                                colors = colors
                            )
                        }
                    }
                }
            }

            item { RakshakXFooter() }
        }
    }
}

@Composable
private fun TrackerStatItem(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineMedium, color = color, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = LocalRakshakXColors.current.textMuted, fontSize = 10.sp)
    }
}

@Composable
private fun CategoryProgressBar(
    category: TrackerCategory,
    count: Int,
    total: Int,
    colors: RakshakXColors
) {
    val fraction = if (total > 0) count.toFloat() / total else 0f
    val catColor = when (category) {
        TrackerCategory.ANALYTICS -> RoyalBlue
        TrackerCategory.ADVERTISING -> Crimson
        TrackerCategory.CRASH_REPORTING -> Amber
        TrackerCategory.FINGERPRINTING -> Amethyst
        TrackerCategory.SOCIAL -> EmeraldLight
        TrackerCategory.PROFILING -> CrimsonLight
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            category.name.lowercase().replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary,
            modifier = Modifier.width(100.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .clip(CircleShape)
                .background(colors.border.copy(alpha = 0.3f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .clip(CircleShape)
                    .background(catColor)
            )
        }
        Text(
            count.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = catColor,
            modifier = Modifier.width(24.dp),
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun AppTrackerRow(
    info: AppTrackerInfo,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    colors: RakshakXColors
) {
    val badgeColor = when {
        info.detections.size >= 5 -> colors.critical
        info.detections.size >= 3 -> colors.warning
        else -> colors.primary
    }

    GlassSurface(onClick = onToggle) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(colors.primaryMuted, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Apps, null, tint = colors.primary, modifier = Modifier.size(18.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        info.appName,
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.textPrimary,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        info.detections.take(3).joinToString(", ") { it.signature.name },
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Box(
                    modifier = Modifier
                        .background(badgeColor.copy(alpha = 0.15f), CircleShape)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        "${info.detections.size}",
                        style = MaterialTheme.typography.labelMedium,
                        color = badgeColor,
                        fontWeight = FontWeight.Bold
                    )
                }
                Icon(
                    if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    null,
                    tint = colors.textMuted,
                    modifier = Modifier.size(20.dp)
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HorizontalDivider(color = colors.border.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(4.dp))
                    info.detections.forEach { detection ->
                        val tracker = detection.signature
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(trackerCategoryColor(tracker.category), CircleShape)
                                )
                                Text(
                                    tracker.name,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textSecondary,
                                    modifier = Modifier.weight(1f)
                                )
                                Box(
                                    modifier = Modifier
                                        .background(
                                            trackerCategoryColor(tracker.category).copy(alpha = 0.15f),
                                            RoundedCornerShape(4.dp)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        tracker.category.name.take(4),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = trackerCategoryColor(tracker.category),
                                        fontSize = 9.sp
                                    )
                                }
                            }
                            Text(
                                text = "Evidence (${detection.evidenceType}): ${detection.matchedComponent}",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textMuted,
                                modifier = Modifier.padding(start = 14.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun trackerCategoryColor(category: TrackerCategory): Color = when (category) {
    TrackerCategory.ANALYTICS -> RoyalBlue
    TrackerCategory.ADVERTISING -> Crimson
    TrackerCategory.CRASH_REPORTING -> Amber
    TrackerCategory.FINGERPRINTING -> Amethyst
    TrackerCategory.SOCIAL -> Emerald
    TrackerCategory.PROFILING -> CrimsonLight
}

@Composable
private fun ShimmerTrackerRow() {
    val colors = LocalRakshakXColors.current
    GlassSurface {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(colors.border.copy(alpha = 0.3f)))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(modifier = Modifier.fillMaxWidth(0.6f).height(14.dp).clip(RoundedCornerShape(4.dp)).background(colors.border.copy(alpha = 0.3f)))
                Box(modifier = Modifier.fillMaxWidth(0.9f).height(10.dp).clip(RoundedCornerShape(4.dp)).background(colors.border.copy(alpha = 0.2f)))
            }
            Box(modifier = Modifier.size(32.dp, 20.dp).clip(CircleShape).background(colors.border.copy(alpha = 0.3f)))
        }
    }
}

