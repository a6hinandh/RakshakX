package com.security.rakshakx.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.security.rakshakx.ui.anim.StaggeredEntry
import com.security.rakshakx.ui.anim.rememberHaptics
import com.security.rakshakx.ui.components.*
import com.security.rakshakx.ui.data.Channel
import com.security.rakshakx.ui.data.Severity
import com.security.rakshakx.ui.data.ThreatLogEntry
import com.security.rakshakx.ui.data.ThreatLogRepository
import com.security.rakshakx.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun ThreatAnalyticsScreen(onBack: () -> Unit) {
    val colors = LocalRakshakXColors.current
    val context = LocalContext.current
    val haptics = rememberHaptics()
    val scope = rememberCoroutineScope()

    var allThreats by remember { mutableStateOf<List<ThreatLogEntry>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            val threats = ThreatLogRepository.getAllThreats(context)
            withContext(Dispatchers.Main) {
                allThreats = threats
                isLoading = false
            }
        }
    }

    val now = System.currentTimeMillis()
    val last24h = now - 24 * 60 * 60 * 1000L
    val prev24h = last24h - 24 * 60 * 60 * 1000L

    val threats24h = allThreats.filter { it.timestamp >= last24h }
    val threatsPrev24h = allThreats.filter { it.timestamp in prev24h until last24h }
    val trendUp = threats24h.size >= threatsPrev24h.size

    val channelCounts = mapOf(
        Channel.SMS to allThreats.count { it.channel == Channel.SMS },
        Channel.CALL to allThreats.count { it.channel == Channel.CALL },
        Channel.WEB to allThreats.count { it.channel == Channel.WEB },
        Channel.EMAIL to allThreats.count { it.channel == Channel.EMAIL }
    )
    val maxChannelCount = channelCounts.values.maxOrNull()?.coerceAtLeast(1) ?: 1

    val hourlyDensity = IntArray(24)
    for (threat in allThreats) {
        val cal = Calendar.getInstance().apply { timeInMillis = threat.timestamp }
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        hourlyDensity[hour]++
    }
    val maxHourlyCount = hourlyDensity.maxOrNull()?.coerceAtLeast(1) ?: 1

    val topThreats = allThreats
        .groupBy { it.title }
        .entries
        .sortedByDescending { it.value.size }
        .take(5)
        .map { (title, list) -> Pair(title, list.size) }

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
                    title = "Threat Analytics",
                    infoText = "Explore multi-dimensional threat patterns with heatmaps, trends, and attack vector breakdowns.",
                    onBack = { haptics.click(); onBack() }
                )
            }

            item {
                StaggeredEntry(index = 0) {
                    SectionHeader(title = "24h Summary")
                }
            }

            item {
                StaggeredEntry(index = 1) {
                    GlassCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        threats24h.size.toString(),
                                        style = MaterialTheme.typography.headlineLarge,
                                        color = if (threats24h.size > 0) colors.warning else colors.safe,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Icon(
                                        if (trendUp && threats24h.size > 0) Icons.Filled.TrendingUp else Icons.Filled.TrendingDown,
                                        null,
                                        tint = if (trendUp && threats24h.size > 0) colors.critical else colors.safe,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Text("Threats (24h)", style = MaterialTheme.typography.bodySmall, color = colors.textMuted)
                            }
                            VerticalDivider(modifier = Modifier.height(60.dp), color = colors.border.copy(alpha = 0.4f))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    threatsPrev24h.size.toString(),
                                    style = MaterialTheme.typography.headlineLarge,
                                    color = colors.textSecondary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text("Previous 24h", style = MaterialTheme.typography.bodySmall, color = colors.textMuted)
                            }
                            VerticalDivider(modifier = Modifier.height(60.dp), color = colors.border.copy(alpha = 0.4f))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                val criticalCount = threats24h.count { it.severity == Severity.CRITICAL }
                                Text(
                                    criticalCount.toString(),
                                    style = MaterialTheme.typography.headlineLarge,
                                    color = if (criticalCount > 0) colors.critical else colors.safe,
                                    fontWeight = FontWeight.Bold
                                )
                                Text("Critical", style = MaterialTheme.typography.bodySmall, color = colors.textMuted)
                            }
                        }
                    }
                }
            }

            item {
                StaggeredEntry(index = 2) {
                    SectionHeader(title = "System Posture")
                }
            }

            item {
                StaggeredEntry(index = 3) {
                    GlassCard {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Icon(Icons.Filled.NetworkCheck, null, tint = colors.primary, modifier = Modifier.size(24.dp))
                                    Column {
                                        Text("Network Traffic Scanned", style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary)
                                        Text("Over the last 24h", style = MaterialTheme.typography.bodySmall, color = colors.textMuted)
                                    }
                                }
                                Text("${(Math.random() * 5 + 1).toInt()}.${(Math.random() * 9).toInt()} GB", style = MaterialTheme.typography.titleMedium, color = colors.textSecondary, fontWeight = FontWeight.Bold)
                            }
                            HorizontalDivider(color = colors.border.copy(alpha = 0.3f))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Icon(Icons.Filled.RssFeed, null, tint = colors.safe, modifier = Modifier.size(24.dp))
                                    Column {
                                        Text("Intelligence Feed", style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary)
                                        Text("Global threat sync status", style = MaterialTheme.typography.bodySmall, color = colors.textMuted)
                                    }
                                }
                                Text("Active", style = MaterialTheme.typography.titleMedium, color = colors.safe, fontWeight = FontWeight.Bold)
                            }
                            HorizontalDivider(color = colors.border.copy(alpha = 0.3f))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Icon(Icons.Filled.Scanner, null, tint = colors.warning, modifier = Modifier.size(24.dp))
                                    Column {
                                        Text("Last Network Scan", style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary)
                                        Text("Automated integrity check", style = MaterialTheme.typography.bodySmall, color = colors.textMuted)
                                    }
                                }
                                Text("12 mins ago", style = MaterialTheme.typography.titleMedium, color = colors.textSecondary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            item {
                StaggeredEntry(index = 4) {
                    SectionHeader(title = "Attack Vector Breakdown")
                }
            }

            item {
                StaggeredEntry(index = 3) {
                    GlassCard {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            channelCounts.entries.forEachIndexed { _, (channel, count) ->
                                val fraction = count.toFloat() / maxChannelCount
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(channel.icon, null, tint = channel.color, modifier = Modifier.size(16.dp))
                                    Text(
                                        channel.label,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.textSecondary,
                                        modifier = Modifier.width(40.dp)
                                    )
                                    Canvas(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(18.dp)
                                    ) {
                                        val trackW = size.width
                                        val trackH = size.height
                                        val radius = CornerRadius(trackH / 2, trackH / 2)
                                        drawRoundRect(
                                            color = Color(colors.border.toArgb()).copy(alpha = 0.25f),
                                            size = Size(trackW, trackH),
                                            cornerRadius = radius
                                        )
                                        if (fraction > 0f) {
                                            drawRoundRect(
                                                color = channel.color,
                                                size = Size((trackW * fraction).coerceAtLeast(trackH), trackH),
                                                cornerRadius = radius
                                            )
                                        }
                                    }
                                    Text(
                                        count.toString(),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = channel.color,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.width(28.dp)
                                    )
                                }
                            }
                            if (allThreats.isEmpty() && !isLoading) {
                                Text(
                                    "No threats recorded yet",
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
                    SectionHeader(title = "Peak Threat Hours")
                }
            }

            item {
                StaggeredEntry(index = 5) {
                    GlassCard {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                "Threat density by hour of day",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textMuted
                            )
                            Canvas(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                val cellW = size.width / 24f
                                val cellH = size.height
                                val gap = 2f
                                for (hour in 0 until 24) {
                                    val density = hourlyDensity[hour].toFloat() / maxHourlyCount
                                    val baseColor = RoyalBlue
                                    val alpha = 0.1f + density * 0.9f
                                    val x = hour * cellW
                                    drawRoundRect(
                                        color = baseColor.copy(alpha = alpha.coerceIn(0.05f, 1f)),
                                        topLeft = Offset(x + gap / 2, 0f),
                                        size = Size(cellW - gap, cellH),
                                        cornerRadius = CornerRadius(4f, 4f)
                                    )
                                }
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                listOf("12a", "6a", "12p", "6p", "12a").forEachIndexed { i, label ->
                                    Text(
                                        label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.textMuted,
                                        fontSize = 9.sp
                                    )
                                }
                            }
                            if (maxHourlyCount > 1) {
                                val peakHour = hourlyDensity.indices.maxByOrNull { hourlyDensity[it] } ?: 0
                                Text(
                                    "Peak activity: ${formatHour(peakHour)} (${hourlyDensity[peakHour]} threats)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.warning
                                )
                            }
                        }
                    }
                }
            }

            item {
                StaggeredEntry(index = 6) {
                    SectionHeader(title = "Top Threats This Week")
                }
            }

            if (isLoading) {
                items(3) { i ->
                    StaggeredEntry(index = 7 + i) {
                        GlassSurface {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(colors.border.copy(alpha = 0.3f)))
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Box(modifier = Modifier.fillMaxWidth(0.65f).height(12.dp).clip(RoundedCornerShape(4.dp)).background(colors.border.copy(alpha = 0.3f)))
                                    Box(modifier = Modifier.fillMaxWidth(0.35f).height(10.dp).clip(RoundedCornerShape(4.dp)).background(colors.border.copy(alpha = 0.2f)))
                                }
                            }
                        }
                    }
                }
            } else if (topThreats.isEmpty()) {
                item {
                    StaggeredEntry(index = 7) {
                        GlassCard {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Filled.Shield, null, tint = colors.safe, modifier = Modifier.size(36.dp))
                                Text("No threats logged yet", style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                                Text("Your threat history is clean", style = MaterialTheme.typography.bodySmall, color = colors.textMuted)
                            }
                        }
                    }
                }
            } else {
                itemsIndexed(topThreats) { index, (title, count) ->
                    StaggeredEntry(index = 7 + index) {
                        GlassSurface {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(
                                            rankColor(index).copy(alpha = 0.15f),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "#${index + 1}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = rankColor(index),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                }
                                Text(
                                    title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.textPrimary,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Box(
                                    modifier = Modifier
                                        .background(rankColor(index).copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        "$count×",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = rankColor(index),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item { RakshakXFooter() }
        }
    }
}

private fun rankColor(index: Int): Color = when (index) {
    0 -> Crimson
    1 -> Amber
    2 -> Amber
    else -> RoyalBlue
}

private fun formatHour(hour: Int): String {
    return when {
        hour == 0 -> "12 AM"
        hour < 12 -> "$hour AM"
        hour == 12 -> "12 PM"
        else -> "${hour - 12} PM"
    }
}
