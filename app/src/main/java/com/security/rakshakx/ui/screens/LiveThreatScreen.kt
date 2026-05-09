package com.security.rakshakx.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.security.rakshakx.ui.components.*
import com.security.rakshakx.ui.data.*
import com.security.rakshakx.ui.theme.*

@Composable
fun LiveThreatScreen() {
    val colors = LocalRakshakXColors.current

    // Simulated live analysis state
    var isAnalyzing by remember { mutableStateOf(true) }
    val pulseAnim = rememberInfiniteTransition(label = "livePulse")
    val pulse by pulseAnim.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "liveAlpha"
    )

    // Simulated risk score
    var currentRiskScore by remember { mutableStateOf(0.15f) }

    // Threat indicators
    val indicators = listOf(
        ThreatIndicator("OTP Request", Icons.Filled.Sms, detected = false),
        ThreatIndicator("Bank Impersonation", Icons.Filled.AccountBalance, detected = false),
        ThreatIndicator("Urgency Language", Icons.Filled.Warning, detected = false),
        ThreatIndicator("Suspicious URL", Icons.Filled.Link, detected = false),
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header with live indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Live Detection",
                    style = MaterialTheme.typography.headlineMedium,
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Real-time threat analysis",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textMuted
                )
            }
            // Live indicator dot
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(colors.safe.copy(alpha = pulse))
                )
                Text("LIVE", style = MaterialTheme.typography.labelSmall, color = colors.safe, fontWeight = FontWeight.Bold)
            }
        }

        // Large Risk Gauge
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            SecurityScoreGauge(
                score = ((1f - currentRiskScore) * 100).toInt(),
                size = 160.dp,
                strokeWidth = 14.dp
            )
        }

        // Risk level label
        val riskLevel = when {
            currentRiskScore >= 0.7f -> "CRITICAL RISK"
            currentRiskScore >= 0.5f -> "HIGH RISK"
            currentRiskScore >= 0.3f -> "ELEVATED RISK"
            else -> "LOW RISK"
        }
        val riskColor = when {
            currentRiskScore >= 0.7f -> colors.critical
            currentRiskScore >= 0.5f -> colors.criticalLight
            currentRiskScore >= 0.3f -> colors.warning
            else -> colors.safe
        }
        Text(
            text = riskLevel,
            style = MaterialTheme.typography.titleLarge,
            color = riskColor,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterHorizontally),
            letterSpacing = 2.sp
        )

        // Threat Indicators Grid
        SectionHeader(title = "Threat Indicators")
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            indicators.chunked(2).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEach { indicator ->
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .border(
                                    1.dp,
                                    if (indicator.detected) colors.critical.copy(alpha = 0.3f) else colors.border,
                                    RoundedCornerShape(12.dp)
                                ),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (indicator.detected) colors.criticalBg else colors.cardBackground
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    indicator.icon,
                                    null,
                                    tint = if (indicator.detected) colors.critical else colors.textMuted,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        indicator.name,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (indicator.detected) colors.critical else colors.textSecondary
                                    )
                                    Text(
                                        if (indicator.detected) "DETECTED" else "Clear",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (indicator.detected) colors.critical else colors.safe
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Analysis Status
        SectionHeader(title = "Analysis Status")
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, colors.border, RoundedCornerShape(14.dp)),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = colors.cardBackground)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AnalysisRow("SMS Channel", "Monitoring notifications", colors.safe)
                AnalysisRow("Call Channel", "Listening for incoming calls", colors.safe)
                AnalysisRow("Web Channel", "VPN traffic analysis", colors.warning)
                AnalysisRow("Email Channel", "Monitoring email notifications", colors.safe)
            }
        }

        // Recommended Actions
        SectionHeader(title = "Recommended Actions")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { /* Block action */ },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = colors.critical),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(Icons.Filled.Block, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Block")
            }
            Button(
                onClick = { /* Warn action */ },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = colors.warning),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(Icons.Filled.Warning, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Warn")
            }
            OutlinedButton(
                onClick = { /* Ignore action */ },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textSecondary),
                shape = MaterialTheme.shapes.medium,
                border = ButtonDefaults.outlinedButtonBorder(true).copy(
                    brush = androidx.compose.ui.graphics.SolidColor(colors.border)
                )
            ) {
                Text("Ignore")
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

private data class ThreatIndicator(
    val name: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val detected: Boolean
)

@Composable
private fun AnalysisRow(name: String, status: String, statusColor: Color) {
    val colors = LocalRakshakXColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(name, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
            Text(status, style = MaterialTheme.typography.bodySmall, color = colors.textMuted)
        }
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(statusColor)
        )
    }
}
