package com.security.rakshakx.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
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
import com.security.rakshakx.ui.data.*
import com.security.rakshakx.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ═══════════════════════════════════════════════════════════════
// Shield Status Hero Card
// ═══════════════════════════════════════════════════════════════

@Composable
fun ShieldStatusCard(
    protectionLevel: ProtectionLevel,
    securityScore: Int,
    modifier: Modifier = Modifier
) {
    val colors = LocalRakshakXColors.current
    val glowColor = when (protectionLevel) {
        ProtectionLevel.PROTECTED -> colors.glowGreen
        ProtectionLevel.ELEVATED -> colors.glowOrange
        ProtectionLevel.THREAT_DETECTED -> colors.glowRed
    }
    val statusColor = protectionLevel.color
    val pulseAnim = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by pulseAnim.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Box(modifier = modifier.fillMaxWidth()) {
        // Glow background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.radialGradient(
                        colors = listOf(glowColor.copy(alpha = pulseAlpha * 0.5f), Color.Transparent),
                        radius = 400f
                    )
                )
        )
        // Card content
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, colors.border, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = colors.cardBackground.copy(alpha = 0.9f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Shield icon with glow
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(statusColor.copy(alpha = pulseAlpha * 0.2f))
                    )
                    Icon(
                        imageVector = Icons.Filled.Shield,
                        contentDescription = "Shield",
                        tint = statusColor,
                        modifier = Modifier.size(48.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = protectionLevel.label,
                    style = MaterialTheme.typography.headlineMedium,
                    color = statusColor,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Security Score: $securityScore/100",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// Security Score Gauge — Animated circular ring
// ═══════════════════════════════════════════════════════════════

@Composable
fun SecurityScoreGauge(
    score: Int,
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    strokeWidth: Dp = 10.dp
) {
    val colors = LocalRakshakXColors.current
    val animatedScore by animateFloatAsState(
        targetValue = score.toFloat(),
        animationSpec = tween(1200, easing = EaseOutCubic),
        label = "scoreAnim"
    )
    val scoreColor = when {
        score >= 80 -> colors.safe
        score >= 50 -> colors.warning
        else -> colors.critical
    }

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val sweep = (animatedScore / 100f) * 270f
            // Background arc
            drawArc(
                color = colors.border,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            )
            // Score arc
            drawArc(
                brush = Brush.sweepGradient(listOf(scoreColor.copy(alpha = 0.4f), scoreColor)),
                startAngle = 135f,
                sweepAngle = sweep,
                useCenter = false,
                style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${animatedScore.toInt()}",
                style = MaterialTheme.typography.headlineLarge,
                color = scoreColor,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "SCORE",
                style = MaterialTheme.typography.labelSmall,
                color = colors.textMuted,
                letterSpacing = 2.sp
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// Channel Shield Card — Status card for each channel
// ═══════════════════════════════════════════════════════════════

@Composable
fun ChannelShieldCard(
    status: ChannelStatus,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val colors = LocalRakshakXColors.current
    val bgColor = if (status.isActive) colors.cardBackground else colors.surfaceElevated
    val borderColor = if (status.isActive) status.channel.color.copy(alpha = 0.3f) else colors.border

    Card(
        onClick = onClick,
        modifier = modifier
            .border(1.dp, borderColor, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor)
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
                    tint = status.channel.color,
                    modifier = Modifier.size(24.dp)
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (status.isActive) colors.safeBg
                            else colors.criticalBg
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (status.isActive) "ACTIVE" else "OFF",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (status.isActive) colors.safe else colors.critical,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Text(
                text = "${status.channel.label} Shield",
                style = MaterialTheme.typography.titleSmall,
                color = colors.textPrimary
            )
            if (status.threatCount > 0) {
                Text(
                    text = "${status.threatCount} threats blocked",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.warning
                )
            } else {
                Text(
                    text = "No threats",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textMuted
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// Threat Card — Log entry card
// ═══════════════════════════════════════════════════════════════

@Composable
fun ThreatCard(
    entry: ThreatLogEntry,
    modifier: Modifier = Modifier
) {
    val colors = LocalRakshakXColors.current
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, colors.border, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = entry.channel.icon,
                        contentDescription = null,
                        tint = entry.channel.color,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = entry.channel.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = entry.channel.color
                    )
                }
                SeverityBadge(entry.severity)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = entry.title,
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = entry.description,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "From: ${entry.source}",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textMuted,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = formatTimestamp(entry.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textMuted
                )
            }
            // Indicators
            if (entry.indicators.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    entry.indicators.take(3).forEach { indicator ->
                        IndicatorChip(text = indicator)
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// Timeline Node — For correlation timeline
// ═══════════════════════════════════════════════════════════════

@Composable
fun TimelineNode(
    event: CorrelationEvent,
    isLast: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = LocalRakshakXColors.current

    Row(modifier = modifier.fillMaxWidth()) {
        // Timeline indicator column
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(40.dp)
        ) {
            // Dot
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(event.severity.color)
                    .border(2.dp, event.severity.color.copy(alpha = 0.3f), CircleShape)
            )
            // Connecting line
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(100.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(event.severity.color.copy(alpha = 0.6f), colors.border)
                            )
                        )
                )
            }
        }

        // Content card
        Card(
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp, bottom = if (isLast) 0.dp else 12.dp)
                .border(1.dp, event.severity.color.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
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
                        Icon(
                            imageVector = event.channel.icon,
                            contentDescription = null,
                            tint = event.channel.color,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = event.channel.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = event.channel.color
                        )
                    }
                    Text(
                        text = formatTimestamp(event.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textMuted
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleSmall,
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
                // Risk score bar
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
                            fontWeight = FontWeight.Bold
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
    val colors = LocalRakshakXColors.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        FilledIconButton(
            onClick = onClick,
            modifier = Modifier.size(52.dp),
            shape = CircleShape,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = color.copy(alpha = 0.15f),
                contentColor = color
            )
        ) {
            Icon(imageVector = icon, contentDescription = label, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = colors.textSecondary,
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
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.safe,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = colors.safeLight
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// Helpers
// ═══════════════════════════════════════════════════════════════

@Composable
fun SeverityBadge(severity: Severity) {
    val colors = LocalRakshakXColors.current
    val bgColor = when (severity) {
        Severity.CRITICAL -> colors.criticalBg
        Severity.HIGH -> colors.criticalBg.copy(alpha = 0.7f)
        Severity.MEDIUM -> colors.warningBg
        Severity.LOW -> colors.safeBg
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = severity.label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = severity.color,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun IndicatorChip(text: String) {
    val colors = LocalRakshakXColors.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(colors.surfaceElevated)
            .border(1.dp, colors.border, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = colors.primary
        )
    }
}

@Composable
fun RiskBar(score: Float, modifier: Modifier = Modifier) {
    val colors = LocalRakshakXColors.current
    val barColor = when {
        score >= 0.7f -> colors.critical
        score >= 0.4f -> colors.warning
        else -> colors.safe
    }
    val animatedWidth by animateFloatAsState(
        targetValue = score.coerceIn(0f, 1f),
        animationSpec = tween(800, easing = EaseOutCubic),
        label = "riskBar"
    )

    Box(
        modifier = modifier
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(colors.surfaceElevated)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(animatedWidth)
                .clip(RoundedCornerShape(3.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(barColor.copy(alpha = 0.5f), barColor)
                    )
                )
        )
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: @Composable (() -> Unit)? = null
) {
    val colors = LocalRakshakXColors.current
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = colors.textPrimary,
            fontWeight = FontWeight.SemiBold
        )
        action?.invoke()
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    return when {
        diff < 60_000 -> "Just now"
        diff < 3600_000 -> "${diff / 60_000}m ago"
        diff < 86400_000 -> "${diff / 3600_000}h ago"
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))
    }
}
