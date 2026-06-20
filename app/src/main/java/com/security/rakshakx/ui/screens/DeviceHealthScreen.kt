package com.security.rakshakx.ui.screens

import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.security.rakshakx.core.integrity.DeviceIntegrityResult
import com.security.rakshakx.core.integrity.DeviceIntegrityScanner
import com.security.rakshakx.core.integrity.FindingSeverity
import com.security.rakshakx.core.integrity.SecurityFinding
import com.security.rakshakx.core.integrity.SecurityPostureScore
import com.security.rakshakx.ui.anim.ShimmerPlaceholder
import com.security.rakshakx.ui.anim.StaggeredEntry
import com.security.rakshakx.ui.anim.rememberHaptics
import com.security.rakshakx.ui.components.*
import com.security.rakshakx.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────
// DeviceHealthScreen
// ─────────────────────────────────────────────────────────────────

@Composable
fun DeviceHealthScreen(onBack: () -> Unit) {
    val colors = LocalRakshakXColors.current
    val context = LocalContext.current
    val haptics = rememberHaptics()
    val scope   = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(true) }
    var result by remember { mutableStateOf<DeviceIntegrityResult?>(null) }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            val scanner = DeviceIntegrityScanner.getInstance(context)
            result = scanner.scan()
        } catch (e: Exception) {
            errorMessage = "Scan failed: ${e.localizedMessage ?: "Unknown error"}"
        } finally {
            isLoading = false
        }
    }

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

            // ── Header ────────────────────────────────────────────
            PageHeader(
                title = "Device Health",
                infoText = "Check your device security posture including root detection, OS patch level, and encryption status.",
                onBack = { haptics.tick(); onBack() }
            )

            if (isLoading) {
                // ── Loading shimmer ───────────────────────────────
                repeat(4) { idx ->
                    StaggeredEntry(index = idx) {
                        ShimmerPlaceholder(height = if (idx == 0) 220.dp else 80.dp)
                    }
                }
            } else if (errorMessage != null) {
                StaggeredEntry(index = 0) {
                    GlassCard {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.ErrorOutline,
                                contentDescription = null,
                                tint = colors.critical,
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    "Scan Error",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = colors.critical,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    errorMessage!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textSecondary
                                )
                            }
                        }
                    }
                }
            } else {
                result?.let { r ->
                    val posture = SecurityPostureScore.compute(
                        deviceScore  = r.overallScore,
                        networkScore = 80,
                        threatCount  = r.findings.count { it.severity == FindingSeverity.CRITICAL || it.severity == FindingSeverity.HIGH }
                    )

                    // ── Score ring card ───────────────────────────
                    StaggeredEntry(index = 0) {
                        GlassCard {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Spacer(Modifier.height(8.dp))
                                DeviceScoreRing(
                                    score = r.overallScore,
                                    grade = posture.grade
                                )
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    text = posture.label,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = scoreColor(r.overallScore),
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "${r.findings.size} finding${if (r.findings.size == 1) "" else "s"} detected",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textMuted
                                )
                                Spacer(Modifier.height(8.dp))
                            }
                        }
                    }

                    // ── Sub-scores row ────────────────────────────
                    StaggeredEntry(index = 1) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            SubScoreChip(
                                label = "Device",
                                score = posture.deviceScore,
                                modifier = Modifier.weight(1f)
                            )
                            SubScoreChip(
                                label = "Network",
                                score = posture.networkScore,
                                modifier = Modifier.weight(1f)
                            )
                            SubScoreChip(
                                label = "Threats",
                                score = posture.threatScore,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // ── Findings section ──────────────────────────
                    if (r.findings.isNotEmpty()) {
                        StaggeredEntry(index = 2) {
                            SectionHeader(title = "SECURITY FINDINGS")
                        }
                        r.findings.forEachIndexed { idx, finding ->
                            StaggeredEntry(index = 3 + idx) {
                                FindingCard(finding = finding)
                            }
                        }
                    } else {
                        StaggeredEntry(index = 2) {
                            GlassCard {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        tint = colors.safe,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "No Issues Found",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = colors.safe,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "Your device passed all security checks.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colors.textSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ── Device details section ────────────────────
                    val detailsStart = 3 + r.findings.size

                    StaggeredEntry(index = detailsStart) {
                        SectionHeader(title = "DEVICE DETAILS")
                    }
                    StaggeredEntry(index = detailsStart + 1) {
                        GlassCard {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                DeviceDetailRow(
                                    icon = Icons.Filled.Security,
                                    label = "Security Patch",
                                    value = r.securityPatchDate,
                                    valueColor = if (r.securityPatchAge > 90) colors.warning else colors.safe
                                )
                                HorizontalDivider(color = colors.border.copy(alpha = 0.3f), thickness = 0.5.dp)
                                DeviceDetailRow(
                                    icon = Icons.Filled.Lock,
                                    label = "Encryption",
                                    value = r.encryptionStatus,
                                    valueColor = if (r.encryptionStatus == "Encrypted") colors.safe else colors.critical
                                )
                                HorizontalDivider(color = colors.border.copy(alpha = 0.3f), thickness = 0.5.dp)
                                DeviceDetailRow(
                                    icon = Icons.Filled.PhoneAndroid,
                                    label = "Screen Lock",
                                    value = if (r.screenLockEnabled) "Enabled" else "Not Set",
                                    valueColor = if (r.screenLockEnabled) colors.safe else colors.critical
                                )
                                HorizontalDivider(color = colors.border.copy(alpha = 0.3f), thickness = 0.5.dp)
                                DeviceDetailRow(
                                    icon = Icons.Filled.Shield,
                                    label = "Play Protect",
                                    value = r.googlePlayProtectStatus,
                                    valueColor = when (r.googlePlayProtectStatus) {
                                        "Active"   -> colors.safe
                                        "Disabled" -> colors.critical
                                        else       -> colors.warning
                                    }
                                )
                                HorizontalDivider(color = colors.border.copy(alpha = 0.3f), thickness = 0.5.dp)
                                DeviceDetailRow(
                                    icon = Icons.Filled.Code,
                                    label = "Dev Options",
                                    value = if (r.devOptionsEnabled) "Enabled" else "Disabled",
                                    valueColor = if (r.devOptionsEnabled) colors.warning else colors.safe
                                )
                                HorizontalDivider(color = colors.border.copy(alpha = 0.3f), thickness = 0.5.dp)
                                DeviceDetailRow(
                                    icon = Icons.Filled.Cable,
                                    label = "USB Debugging",
                                    value = if (r.adbEnabled) "On" else "Off",
                                    valueColor = if (r.adbEnabled) colors.warning else colors.safe
                                )
                                HorizontalDivider(color = colors.border.copy(alpha = 0.3f), thickness = 0.5.dp)
                                DeviceDetailRow(
                                    icon = Icons.Filled.InstallMobile,
                                    label = "Unknown Sources",
                                    value = if (r.unknownSourcesEnabled) "Allowed" else "Blocked",
                                    valueColor = if (r.unknownSourcesEnabled) colors.warning else colors.safe
                                )
                                if (r.isRooted) {
                                    HorizontalDivider(color = colors.border.copy(alpha = 0.3f), thickness = 0.5.dp)
                                    DeviceDetailRow(
                                        icon = Icons.Filled.BugReport,
                                        label = "Root Status",
                                        value = r.rootMethod ?: "Rooted",
                                        valueColor = colors.critical
                                    )
                                }
                            }
                        }
                    }

                    StaggeredEntry(index = detailsStart + 2) {
                        RakshakXFooter()
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Score ring with animated arc
// ─────────────────────────────────────────────────────────────────

@Composable
private fun DeviceScoreRing(score: Int, grade: String) {
    val ringColor = scoreColor(score)
    val animatedScore by animateFloatAsState(
        targetValue = score.toFloat(),
        animationSpec = tween(1200, easing = EaseOutCubic),
        label = "scoreAnim"
    )
    val colors = LocalRakshakXColors.current

    Box(
        modifier = Modifier.size(160.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val sweep = (animatedScore / 100f) * 300f
            drawArc(
                color = colors.border.copy(alpha = 0.20f),
                startAngle = 120f,
                sweepAngle = 300f,
                useCenter = false,
                style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
            )
            drawArc(
                color = ringColor,
                startAngle = 120f,
                sweepAngle = sweep,
                useCenter = false,
                style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${animatedScore.toInt()}",
                fontSize = 40.sp,
                color = ringColor,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Grade $grade",
                style = MaterialTheme.typography.labelMedium,
                color = ringColor.copy(alpha = 0.8f),
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Sub-score chip
// ─────────────────────────────────────────────────────────────────

@Composable
private fun SubScoreChip(label: String, score: Int, modifier: Modifier = Modifier) {
    val colors = LocalRakshakXColors.current
    val color  = scoreColor(score)
    GlassSurface(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$score",
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
// Finding card with severity left-border
// ─────────────────────────────────────────────────────────────────

@Composable
private fun FindingCard(finding: SecurityFinding) {
    val colors = LocalRakshakXColors.current
    val severityColor = findingSeverityColor(finding.severity)
    val severityLabel = finding.severity.name

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            // Colored left border
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(severityColor.copy(alpha = 0.85f))
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = finding.title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textPrimary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    StatusChip(text = severityLabel, color = severityColor)
                }
                Text(
                    text = finding.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Filled.Info,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(13.dp).padding(top = 1.dp)
                    )
                    Text(
                        text = finding.recommendation,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.primary
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Device detail row
// ─────────────────────────────────────────────────────────────────

@Composable
private fun DeviceDetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    valueColor: Color
) {
    val colors = LocalRakshakXColors.current
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
            Icon(icon, contentDescription = null, tint = colors.textMuted, modifier = Modifier.size(16.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = valueColor,
            fontWeight = FontWeight.Medium
        )
    }
}

// ─────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────

@Composable
private fun scoreColor(score: Int): Color {
    val colors = LocalRakshakXColors.current
    return when {
        score >= 80 -> colors.safe
        score >= 60 -> RoyalBlue
        score >= 40 -> colors.warning
        else        -> colors.critical
    }
}

@Composable
private fun findingSeverityColor(severity: FindingSeverity): Color {
    val colors = LocalRakshakXColors.current
    return when (severity) {
        FindingSeverity.CRITICAL -> colors.critical
        FindingSeverity.HIGH     -> Amber
        FindingSeverity.MEDIUM   -> Gold
        FindingSeverity.LOW      -> RoyalBlue
        FindingSeverity.INFO     -> colors.safe
    }
}
