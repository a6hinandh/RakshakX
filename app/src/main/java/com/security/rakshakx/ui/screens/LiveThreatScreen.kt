package com.security.rakshakx.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.security.rakshakx.ui.anim.StaggeredEntry
import com.security.rakshakx.ui.anim.breathingScale
import com.security.rakshakx.ui.anim.rememberHaptics
import com.security.rakshakx.ui.components.*
import com.security.rakshakx.ui.theme.*

@Composable
fun LiveThreatScreen(onBack: () -> Unit) {
    val colors = LocalRakshakXColors.current
    val haptics = rememberHaptics()

    var currentRiskScore by remember { mutableStateOf(0.15f) }

    val pulseAnim = rememberInfiniteTransition(label = "livePulse")
    val pulse by pulseAnim.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200, easing = EaseInOutCubic), RepeatMode.Reverse),
        label = "liveAlpha"
    )

    val indicators = listOf(
        Triple("OTP Request", Icons.Filled.Sms, false),
        Triple("Bank Impersonation", Icons.Filled.AccountBalance, false),
        Triple("Urgency Language", Icons.Filled.Warning, false),
        Triple("Suspicious URL", Icons.Filled.Link, false),
    )

    PremiumBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .statusBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { haptics.tick(); onBack() },
                        modifier = Modifier.size(38.dp).background(colors.surfaceElevated, RoundedCornerShape(12.dp))
                    ) {
                        Icon(Icons.Filled.ArrowBack, null, tint = colors.textPrimary, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Live Intelligence", style = MaterialTheme.typography.headlineSmall, color = colors.textPrimary, fontWeight = FontWeight.Bold)
                        Text("Real-time telemetry", style = MaterialTheme.typography.bodySmall, color = colors.textMuted)
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.background(colors.safeBg, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(colors.safe.copy(alpha = pulse)))
                    Text("LIVE", style = MaterialTheme.typography.labelSmall, color = colors.safe, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                }
            }

            val gaugeBreathing = breathingScale(durationMs = 3000)
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                SecurityScoreGauge(
                    score = ((1f - currentRiskScore) * 100).toInt(),
                    size = 160.dp,
                    strokeWidth = (11f + gaugeBreathing).dp
                )
            }

            val riskLevel = when { currentRiskScore >= 0.7f -> "CRITICAL RISK"; currentRiskScore >= 0.5f -> "HIGH RISK"; currentRiskScore >= 0.3f -> "ELEVATED"; else -> "LOW RISK" }
            val riskColor = when { currentRiskScore >= 0.7f -> colors.critical; currentRiskScore >= 0.5f -> colors.criticalLight; currentRiskScore >= 0.3f -> colors.warning; else -> colors.safe }
            AnimatedContent(
                targetState = riskLevel,
                transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
                label = "riskText",
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) { label ->
                Text(label, style = MaterialTheme.typography.titleLarge, color = riskColor, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            }

            SectionHeader(title = "Threat Indicators")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                indicators.chunked(2).forEach { row ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { (name, icon, detected) ->
                            GlassSurface(
                                modifier = Modifier.weight(1f),
                                borderColor = if (detected) colors.critical.copy(alpha = 0.2f) else colors.border.copy(alpha = 0.3f)
                            ) {
                                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Box(
                                        modifier = Modifier.size(34.dp).background(
                                            if (detected) colors.criticalBg else colors.surfaceElevated, RoundedCornerShape(10.dp)
                                        ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(icon, null, tint = if (detected) colors.critical else colors.textMuted, modifier = Modifier.size(18.dp))
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(name, style = MaterialTheme.typography.labelSmall, color = if (detected) colors.critical else colors.textSecondary, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                        Text(if (detected) "DETECTED" else "Clear", style = MaterialTheme.typography.labelSmall,
                                            color = if (detected) colors.critical.copy(alpha = 0.7f) else colors.safe, fontSize = 9.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            SectionHeader(title = "Channel Status")
            GlassCard {
                ChannelStatusRow("SMS Channel", "Monitoring notifications", colors.channelSms)
                Spacer(Modifier.height(12.dp))
                ChannelStatusRow("Call Channel", "Listening for incoming calls", colors.channelCall)
                Spacer(Modifier.height(12.dp))
                ChannelStatusRow("Web Channel", "VPN traffic analysis", colors.channelWeb)
                Spacer(Modifier.height(12.dp))
                ChannelStatusRow("Email Channel", "Monitoring email notifications", colors.channelEmail)
            }

            SectionHeader(title = "Actions")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { haptics.heavyClick() }, modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.critical),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Block, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(4.dp))
                    Text("Block", fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { haptics.warning() }, modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.warning),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Warning, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(4.dp))
                    Text("Warn", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = { haptics.tick() }, modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textSecondary),
                    border = ButtonDefaults.outlinedButtonBorder(true).copy(brush = androidx.compose.ui.graphics.SolidColor(colors.border)),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Ignore", fontWeight = FontWeight.Bold) }
            }

            RakshakXFooter()
        }
    }
}

@Composable
private fun ChannelStatusRow(name: String, status: String, color: androidx.compose.ui.graphics.Color) {
    val colors = LocalRakshakXColors.current
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column {
            Text(name, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
            Text(status, style = MaterialTheme.typography.bodySmall, color = colors.textMuted)
        }
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
    }
}
