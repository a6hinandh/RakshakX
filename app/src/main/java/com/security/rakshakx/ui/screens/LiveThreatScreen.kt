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
fun LiveThreatScreen(onBack: () -> Unit) {
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                androidx.compose.ui.graphics.Brush.verticalGradient(
                    listOf(Color(0xFF0F172A), Color(0xFF1E293B))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with back button and live indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .background(Color.White.copy(alpha = 0.05f), androidx.compose.foundation.shape.CircleShape)
                            .size(36.dp)
                    ) {
                        Icon(Icons.Filled.ArrowBack, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                    Column {
                        Text(
                            "Live Intelligence",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Real-time telemetry",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }
                // Live indicator dot
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .background(Color(0xFF10B981).copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981).copy(alpha = pulse))
                    )
                    Text("LIVE", style = MaterialTheme.typography.labelSmall, color = Color(0xFF10B981), fontWeight = FontWeight.Bold, fontSize = 10.sp)
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
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            indicators.chunked(2).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    row.forEach { indicator ->
                        Card(
                            modifier = Modifier
                                .weight(1f),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (indicator.detected) Color(0xFFEF4444).copy(alpha = 0.1f) else Color.White.copy(alpha = 0.05f)
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (indicator.detected) Color(0xFFEF4444).copy(alpha = 0.3f) else Color.White.copy(alpha = 0.1f))
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(
                                            if (indicator.detected) Color(0xFFEF4444).copy(alpha = 0.15f) else Color.White.copy(alpha = 0.1f),
                                            RoundedCornerShape(10.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        indicator.icon,
                                        null,
                                        tint = if (indicator.detected) Color(0xFFEF4444) else Color.White.copy(alpha = 0.4f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        indicator.name,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (indicator.detected) Color(0xFFEF4444) else Color.White.copy(alpha = 0.6f),
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                    Text(
                                        if (indicator.detected) "DETECTED" else "Clear",
                                        style = androidx.compose.ui.text.TextStyle(fontSize = 10.sp),
                                        color = if (indicator.detected) Color(0xFFEF4444).copy(alpha = 0.7f) else Color(0xFF10B981)
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
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AnalysisRow("SMS Channel", "Monitoring notifications", Color(0xFF4776E6))
                AnalysisRow("Call Channel", "Listening for incoming calls", Color(0xFF8E54E9))
                AnalysisRow("Web Channel", "VPN traffic analysis", Color(0xFF10B981))
                AnalysisRow("Email Channel", "Monitoring email notifications", Color(0xFFEF4444))
            }
        }

        // Recommended Actions
        SectionHeader(title = "Recommended Actions")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { /* Block action */ },
                modifier = Modifier.weight(1.1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Icon(Icons.Filled.Block, null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Block", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = { /* Warn action */ },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Icon(Icons.Filled.Warning, null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Warn", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick = { /* Ignore action */ },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Text("Ignore", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
        }

        RakshakXFooter()
    }
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
