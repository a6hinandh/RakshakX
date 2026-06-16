package com.security.rakshakx.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.*
import com.security.rakshakx.R
import com.security.rakshakx.ui.data.*
import com.security.rakshakx.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ═══════════════════════════════════════════════════════════════
// Surface Card — clean navy container, replaces glass/frosted look
// ═══════════════════════════════════════════════════════════════

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    borderColor: Color = SlateBorder,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = LocalRakshakXColors.current
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, borderColor.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            content = content
        )
    }
}

@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    borderColor: Color = SlateBorder,
    shape: RoundedCornerShape = RoundedCornerShape(16.dp),
    content: @Composable () -> Unit
) {
    val colors = LocalRakshakXColors.current
    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = modifier.border(1.dp, borderColor.copy(alpha = 0.35f), shape),
            shape = shape,
            color = colors.cardBackground,
            content = content
        )
    } else {
        Surface(
            modifier = modifier.border(1.dp, borderColor.copy(alpha = 0.35f), shape),
            shape = shape,
            color = colors.cardBackground,
            content = content
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// Protection Status Card — calm, outcome-focused, no spinning ring
// ═══════════════════════════════════════════════════════════════

@Composable
fun ShieldStatusCard(
    protectionLevel: ProtectionLevel,
    securityScore: Int,
    modifier: Modifier = Modifier
) {
    val colors = LocalRakshakXColors.current
    val statusColor = protectionLevel.color

    val statusMessage = when (protectionLevel) {
        ProtectionLevel.PROTECTED     -> "Your device is protected"
        ProtectionLevel.ELEVATED      -> "Elevated risk detected"
        ProtectionLevel.THREAT_DETECTED -> "Active threat — review required"
    }
    val statusIcon = when (protectionLevel) {
        ProtectionLevel.PROTECTED     -> Icons.Filled.CheckCircle
        else                          -> Icons.Filled.Shield
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, statusColor.copy(alpha = 0.18f), RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground)
    ) {
        // Accent line at top edge
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(statusColor.copy(alpha = 0.8f), statusColor.copy(alpha = 0.15f))
                    )
                )
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(statusColor.copy(alpha = 0.10f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = statusIcon,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(26.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = protectionLevel.label,
                    style = MaterialTheme.typography.titleMedium,
                    color = statusColor,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = statusMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$securityScore",
                    style = MaterialTheme.typography.titleLarge,
                    color = statusColor,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "/ 100",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textMuted
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// Security Score Gauge — calm arc, no gradient flash
// ═══════════════════════════════════════════════════════════════

@Composable
fun SecurityScoreGauge(
    score: Int,
    modifier: Modifier = Modifier,
    size: Dp = 140.dp,
    strokeWidth: Dp = 8.dp
) {
    val colors = LocalRakshakXColors.current
    val animatedScore by animateFloatAsState(
        targetValue = score.toFloat(),
        animationSpec = tween(1000, easing = EaseOutCubic),
        label = "scoreAnim"
    )
    val scoreColor = when {
        score >= 80 -> colors.safe
        score >= 50 -> colors.warning
        else        -> colors.critical
    }

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val sweep = (animatedScore / 100f) * 270f
            drawArc(
                color = colors.border.copy(alpha = 0.25f),
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            )
            drawArc(
                color = scoreColor,
                startAngle = 135f,
                sweepAngle = sweep,
                useCenter = false,
                style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${animatedScore.toInt()}",
                style = MaterialTheme.typography.displaySmall,
                color = scoreColor,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "score",
                style = MaterialTheme.typography.labelSmall,
                color = colors.textMuted
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// Channel Monitor Card
// ═══════════════════════════════════════════════════════════════

@Composable
fun ChannelShieldCard(
    status: ChannelStatus,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val colors = LocalRakshakXColors.current
    val channelColor = if (status.isActive) status.channel.color else colors.textMuted

    Card(
        onClick = onClick,
        modifier = modifier.border(
            1.dp,
            if (status.isActive) channelColor.copy(alpha = 0.16f) else colors.border.copy(alpha = 0.2f),
            RoundedCornerShape(14.dp)
        ),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (status.isActive) colors.cardBackground else colors.surfaceElevated.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = status.channel.icon,
                    contentDescription = status.channel.label,
                    tint = channelColor,
                    modifier = Modifier.size(20.dp)
                )
                StatusChip(
                    text = if (status.isActive) "On" else "Off",
                    color = if (status.isActive) colors.safe else colors.textMuted
                )
            }
            Text(
                text = status.channel.label,
                style = MaterialTheme.typography.titleSmall,
                color = if (status.isActive) colors.textPrimary else colors.textMuted,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = if (status.threatCount > 0) "${status.threatCount} flagged" else "Nothing flagged",
                style = MaterialTheme.typography.bodySmall,
                color = if (status.threatCount > 0) colors.warning else colors.textMuted
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// Threat Card — clean log entry with channel accent strip
// ═══════════════════════════════════════════════════════════════

@Composable
fun ThreatCard(
    entry: ThreatLogEntry,
    modifier: Modifier = Modifier
) {
    val colors = LocalRakshakXColors.current
    val channelColor = entry.channel.color

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, colors.border.copy(alpha = 0.3f), RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight()
                    .background(channelColor.copy(alpha = 0.7f))
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(entry.channel.icon, null, tint = channelColor, modifier = Modifier.size(14.dp))
                        Text(
                            text = entry.channel.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = channelColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    SeverityBadge(entry.severity)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = entry.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = entry.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = entry.source,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textMuted,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = formatTimestamp(entry.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textMuted
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// Timeline Node — Correlation timeline
// ═══════════════════════════════════════════════════════════════

@Composable
fun TimelineNode(
    event: CorrelationEvent,
    isLast: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = LocalRakshakXColors.current

    Row(modifier = modifier.fillMaxWidth()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(event.severity.color)
            )
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(96.dp)
                        .background(colors.border.copy(alpha = 0.4f))
                )
            }
        }

        Card(
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp, bottom = if (isLast) 0.dp else 12.dp)
                .border(1.dp, colors.border.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = colors.cardBackground)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(event.channel.icon, null, tint = event.channel.color, modifier = Modifier.size(14.dp))
                        Text(event.channel.label, style = MaterialTheme.typography.labelSmall, color = event.channel.color)
                    }
                    Text(formatTimestamp(event.timestamp), style = MaterialTheme.typography.labelSmall, color = colors.textMuted)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = event.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RiskBar(score = event.riskScore, modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.width(8.dp))
                    if (event.escalationDelta > 0f) {
                        Text(
                            text = "+${"%.0f".format(event.escalationDelta * 100)}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = event.severity.color,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// Quick Action Button
// ═══════════════════════════════════════════════════════════════

@Composable
fun QuickActionButton(
    icon: ImageVector,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        FilledIconButton(
            onClick = onClick,
            modifier = Modifier.size(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = color.copy(alpha = 0.10f),
                contentColor = color
            )
        ) {
            Icon(imageVector = icon, contentDescription = label, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// Privacy Badge
// ═══════════════════════════════════════════════════════════════

@Composable
fun PrivacyBadge(
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier
) {
    val colors = LocalRakshakXColors.current
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(colors.safeBg)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = colors.safe, modifier = Modifier.size(13.dp))
        Text(text, style = MaterialTheme.typography.labelSmall, color = colors.safeLight)
    }
}

// ═══════════════════════════════════════════════════════════════
// Status Chip
// ═══════════════════════════════════════════════════════════════

@Composable
fun StatusChip(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.10f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// Severity Badge
// ═══════════════════════════════════════════════════════════════

@Composable
fun SeverityBadge(severity: Severity) {
    val colors = LocalRakshakXColors.current
    val (bg, fg) = when (severity) {
        Severity.CRITICAL -> colors.criticalBg to colors.critical
        Severity.HIGH     -> colors.criticalBg to colors.criticalLight
        Severity.MEDIUM   -> colors.warningBg  to colors.warning
        Severity.LOW      -> colors.safeBg     to colors.safe
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = severity.label,
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            fontWeight = FontWeight.Medium
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// Indicator Chip
// ═══════════════════════════════════════════════════════════════

@Composable
fun IndicatorChip(text: String) {
    val colors = LocalRakshakXColors.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(colors.surfaceElevated)
            .border(1.dp, colors.border.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = colors.primary)
    }
}

// ═══════════════════════════════════════════════════════════════
// Risk Bar
// ═══════════════════════════════════════════════════════════════

@Composable
fun RiskBar(score: Float, modifier: Modifier = Modifier) {
    val colors = LocalRakshakXColors.current
    val barColor = when {
        score >= 0.7f -> colors.critical
        score >= 0.4f -> colors.warning
        else          -> colors.safe
    }
    val animatedWidth by animateFloatAsState(
        targetValue = score.coerceIn(0f, 1f),
        animationSpec = tween(600, easing = EaseOutCubic),
        label = "riskBar"
    )

    Box(
        modifier = modifier
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(colors.border.copy(alpha = 0.2f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(animatedWidth)
                .clip(RoundedCornerShape(2.dp))
                .background(barColor)
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// Section Header
// ═══════════════════════════════════════════════════════════════

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = TextSecondary,
            fontWeight = FontWeight.SemiBold
        )
        action?.invoke()
    }
}

// ═══════════════════════════════════════════════════════════════
// Footer
// ═══════════════════════════════════════════════════════════════

@Composable
fun RakshakXFooter(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HorizontalDivider(
            modifier = Modifier.width(24.dp),
            thickness = 1.dp,
            color = SlateBorder.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "RakshakX · On-device AI",
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted.copy(alpha = 0.5f)
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// App Background — solid deep navy
// ═══════════════════════════════════════════════════════════════

@Composable
fun PremiumBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Charcoal),
        content = content
    )
}

// ═══════════════════════════════════════════════════════════════
// Empty State
// ═══════════════════════════════════════════════════════════════

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    useLottie: Boolean = true
) {
    val colors = LocalRakshakXColors.current
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.check_success))
    val progress by animateLottieCompositionAsState(composition = composition, iterations = 1)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp, horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (useLottie && composition != null) {
            LottieAnimation(
                composition = composition,
                progress = { progress },
                modifier = Modifier.size(80.dp)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(colors.surfaceElevated, RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = colors.textMuted, modifier = Modifier.size(28.dp))
            }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = colors.textSecondary,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = colors.textMuted,
            textAlign = TextAlign.Center
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// Helpers
// ═══════════════════════════════════════════════════════════════

fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    return when {
        diff < 60_000      -> "Just now"
        diff < 3_600_000   -> "${diff / 60_000}m ago"
        diff < 86_400_000  -> "${diff / 3_600_000}h ago"
        diff < 604_800_000 -> "${diff / 86_400_000}d ago"
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))
    }
}
