package com.security.rakshakx.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.security.rakshakx.core.network.*
import com.security.rakshakx.ui.anim.StaggeredEntry
import com.security.rakshakx.ui.anim.rememberHaptics
import com.security.rakshakx.ui.components.*
import com.security.rakshakx.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun WifiAuditScreen(onBack: () -> Unit) {
    val colors = LocalRakshakXColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptics = rememberHaptics()

    var isLoading by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<WifiSecurityResult?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

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

            // ── Header ────────────────────────────────────────────────────
            PageHeader(
                title = "Wi-Fi Security Audit",
                infoText = "Analyze your current Wi-Fi network for security vulnerabilities and configuration issues.",
                onBack = { haptics.tick(); onBack() }
            )

            // ── Scan button ───────────────────────────────────────────────
            StaggeredEntry(index = 0) {
                Button(
                    onClick = {
                        haptics.click()
                        errorMessage = null
                        isLoading = true
                        scope.launch(Dispatchers.IO) {
                            try {
                                val r = WifiSecurityAnalyzer.getInstance(context).analyze()
                                result = r
                            } catch (e: Exception) {
                                errorMessage = "Scan failed: ${e.localizedMessage}"
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                ) {
                    Icon(
                        Icons.Filled.Wifi,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (isLoading) "Analyzing…" else "Analyze Wi-Fi Network",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // ── Loading ───────────────────────────────────────────────────
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(260.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        SonarRadarScanner(color = colors.primary)
                        Text(
                            "Analyzing Network Security...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textSecondary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // ── Error ─────────────────────────────────────────────────────
            errorMessage?.let { msg ->
                StaggeredEntry(index = 1) {
                    GlassCard {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.ErrorOutline,
                                contentDescription = null,
                                tint = colors.critical,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(msg, style = MaterialTheme.typography.bodySmall, color = colors.critical)
                        }
                    }
                }
            }

            // ── Idle state ────────────────────────────────────────────────
            if (!isLoading && result == null && errorMessage == null) {
                StaggeredEntry(index = 1) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(250.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .background(colors.primaryMuted, RoundedCornerShape(20.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.WifiFind,
                                    contentDescription = null,
                                    tint = colors.primary,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            Text(
                                "Tap 'Analyze' to audit your current Wi-Fi network.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.textSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // ── Results ───────────────────────────────────────────────────
            result?.let { r ->

                // Score arc
                StaggeredEntry(index = 1) {
                    GlassCard {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            WifiScoreArc(score = r.securityScore)

                            Spacer(modifier = Modifier.height(4.dp))

                            // Pulse Status Indicator
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val statusText = when {
                                    r.securityScore >= 80 -> "Secure Connection"
                                    r.securityScore >= 50 -> "Warning — Potential Risk"
                                    else -> "Compromised / Highly Vulnerable"
                                }
                                val statusColor = when {
                                    r.securityScore >= 80 -> colors.safe
                                    r.securityScore >= 50 -> colors.warning
                                    else -> colors.critical
                                }

                                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                                val pulseAlpha by infiniteTransition.animateFloat(
                                    initialValue = 0.2f,
                                    targetValue = 1f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(1200, easing = FastOutLinearInEasing),
                                        repeatMode = RepeatMode.Reverse
                                    ),
                                    label = "pulseAlpha"
                                )

                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(statusColor.copy(alpha = pulseAlpha), CircleShape)
                                )
                                Text(
                                    text = statusText,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = statusColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // 2x2 Network Info Grid
                StaggeredEntry(index = 2) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                GridInfoItem(
                                    icon = Icons.Filled.Wifi,
                                    label = "SSID",
                                    value = r.ssid,
                                    color = colors.primary
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                GridInfoItem(
                                    icon = Icons.Filled.Router,
                                    label = "BSSID (MAC)",
                                    value = r.bssid.takeIf { it.isNotEmpty() } ?: "N/A",
                                    color = colors.primary
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                GridInfoItem(
                                    icon = Icons.Filled.SignalCellularAlt,
                                    label = "Signal Strength",
                                    value = "${r.signalStrength} dBm",
                                    color = when {
                                        r.signalStrength > -50 -> colors.safe
                                        r.signalStrength > -70 -> colors.warning
                                        else -> colors.critical
                                    }
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                val bandLabel = if (r.frequency >= 5000) "5 GHz" else "2.4 GHz"
                                GridInfoItem(
                                    icon = Icons.Filled.NetworkCheck,
                                    label = "Frequency Band",
                                    value = "$bandLabel (${r.frequency} MHz)",
                                    color = colors.primary
                                )
                            }
                        }
                    }
                }

                // Encryption card
                StaggeredEntry(index = 3) {
                    GlassCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.Lock,
                                    contentDescription = null,
                                    tint = colors.textSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        "Encryption",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.textMuted
                                    )
                                    Text(
                                        r.encryptionType.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = colors.textPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            EncryptionBadge(r.encryptionType)
                        }
                    }
                }

                // ── Wi-Fi Protocol & Link Speed ───────────────────────────────
                StaggeredEntry(index = 4) {
                    GlassCard {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Filled.SettingsSystemDaydream,
                                        contentDescription = null,
                                        tint = colors.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column {
                                        Text("Protocol Standard", style = MaterialTheme.typography.bodySmall, color = colors.textMuted)
                                        Text(r.wifiStandard, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                            HorizontalDivider(color = colors.border.copy(alpha = 0.3f))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Filled.Speed,
                                        contentDescription = null,
                                        tint = colors.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column {
                                        Text("Link Speed", style = MaterialTheme.typography.bodySmall, color = colors.textMuted)
                                        val txRxLabel = if (r.txSpeed >= 0 && r.rxSpeed >= 0) {
                                            "Tx: ${r.txSpeed} Mbps / Rx: ${r.rxSpeed} Mbps"
                                        } else {
                                            "${r.linkSpeed} Mbps"
                                        }
                                        Text(txRxLabel, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }
                }

                // ── IP Configuration & Routing ────────────────────────────────
                StaggeredEntry(index = 5) {
                    GlassCard {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Filled.SettingsEthernet,
                                        contentDescription = null,
                                        tint = colors.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column {
                                        Text("Local IP / Subnet Mask", style = MaterialTheme.typography.bodySmall, color = colors.textMuted)
                                        Text("${r.localIp} / ${r.subnetMask}", style = MaterialTheme.typography.titleSmall, color = colors.textPrimary, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                            HorizontalDivider(color = colors.border.copy(alpha = 0.3f))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Filled.Router,
                                        contentDescription = null,
                                        tint = colors.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column {
                                        Text("Gateway / Router", style = MaterialTheme.typography.bodySmall, color = colors.textMuted)
                                        Text(r.gatewayIp, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }
                }

                // ── DNS Security & Performance Diagnostics ────────────────────
                StaggeredEntry(index = 6) {
                    GlassCard {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Filled.Dns,
                                        contentDescription = null,
                                        tint = colors.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column {
                                        Text("DNS Servers", style = MaterialTheme.typography.bodySmall, color = colors.textMuted)
                                        val dnsLabel = if (r.dns2 != "0.0.0.0" && r.dns2.isNotEmpty()) {
                                            "${r.dns1} / ${r.dns2}"
                                        } else {
                                            r.dns1
                                        }
                                        Text(dnsLabel, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                            HorizontalDivider(color = colors.border.copy(alpha = 0.3f))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        if (r.isPrivateDnsActive) Icons.Filled.Security else Icons.Filled.LockOpen,
                                        contentDescription = null,
                                        tint = if (r.isPrivateDnsActive) colors.safe else colors.warning,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column {
                                        Text("Private DNS Encryption", style = MaterialTheme.typography.bodySmall, color = colors.textMuted)
                                        val privateDnsLabel = if (r.isPrivateDnsActive) "Encrypted (DoT/DoH Active)" else "Disabled (Plaintext)"
                                        Text(privateDnsLabel, style = MaterialTheme.typography.titleSmall, color = if (r.isPrivateDnsActive) colors.safe else colors.warning, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            HorizontalDivider(color = colors.border.copy(alpha = 0.3f))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Filled.CompassCalibration,
                                        contentDescription = null,
                                        tint = colors.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column {
                                        Text("Diagnostic Latency", style = MaterialTheme.typography.bodySmall, color = colors.textMuted)
                                        val gatewayPing = if (r.gatewayLatencyMs >= 0) "${r.gatewayLatencyMs}ms" else "Timeout / N/A"
                                        val dnsPing = if (r.dnsResolutionLatencyMs >= 0) "${r.dnsResolutionLatencyMs}ms" else "Timeout / N/A"
                                        Text("Gateway: $gatewayPing | DNS Query: $dnsPing", style = MaterialTheme.typography.titleSmall, color = colors.textPrimary, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }
                }

                // Threats list
                if (r.threats.isNotEmpty()) {
                    StaggeredEntry(index = 7) {
                        SectionHeader(title = "Detected Threats (${r.threats.size})")
                    }
                    r.threats.forEachIndexed { i, threat ->
                        StaggeredEntry(index = 8 + i) {
                            WifiThreatCard(threat)
                        }
                    }
                } else {
                    StaggeredEntry(index = 7) {
                        GlassCard {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = colors.safe,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    "No threats detected on this network.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.safe
                                )
                            }
                        }
                    }
                }

                // Recommendations
                if (r.recommendations.isNotEmpty()) {
                    StaggeredEntry(index = 8 + r.threats.size) {
                        SectionHeader(title = "Recommendations")
                    }
                    r.recommendations.forEachIndexed { i, rec ->
                        StaggeredEntry(index = 9 + r.threats.size + i) {
                            RecommendationRow(index = i + 1, text = rec)
                        }
                    }
                }

                StaggeredEntry(index = 10 + r.threats.size + r.recommendations.size) {
                    RakshakXFooter()
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ── Sub-composables ──────────────────────────────────────────────────────────

@Composable
private fun WifiScoreArc(score: Int) {
    val colors = LocalRakshakXColors.current
    val animatedScore by animateFloatAsState(
        targetValue = score.toFloat(),
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "wifiScore"
    )
    val scoreColor = when {
        score >= 80 -> colors.safe
        score >= 50 -> colors.warning
        else        -> colors.critical
    }

    Box(modifier = Modifier.size(140.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
            drawArc(
                color = colors.border.copy(alpha = 0.25f),
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                style = stroke
            )
            drawArc(
                color = scoreColor,
                startAngle = 135f,
                sweepAngle = (animatedScore / 100f) * 270f,
                useCenter = false,
                style = stroke
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "${animatedScore.toInt()}",
                style = MaterialTheme.typography.displaySmall,
                color = scoreColor,
                fontWeight = FontWeight.Bold
            )
            Text(
                "/ 100",
                style = MaterialTheme.typography.labelSmall,
                color = colors.textMuted
            )
        }
    }
}

@Composable
private fun EncryptionBadge(encryption: WifiEncryption) {
    val colors = LocalRakshakXColors.current
    val (bg, fg) = when (encryption) {
        WifiEncryption.WPA3    -> colors.safeBg    to colors.safe
        WifiEncryption.WPA2    -> colors.safeBg    to colors.safeLight
        WifiEncryption.WPA     -> colors.warningBg to colors.warning
        WifiEncryption.WEP     -> colors.criticalBg to colors.critical
        WifiEncryption.OPEN    -> colors.criticalBg to colors.critical
        WifiEncryption.UNKNOWN -> colors.surfaceElevated to colors.textMuted
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .padding(horizontal = 12.dp, vertical = 5.dp)
    ) {
        Text(
            text = encryption.name,
            style = MaterialTheme.typography.labelMedium,
            color = fg,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun WifiThreatCard(threat: WifiThreat) {
    val colors = LocalRakshakXColors.current
    var isExpanded by remember { mutableStateOf(false) }

    val severityColor = when (threat.severity) {
        ThreatSeverity.CRITICAL -> colors.critical
        ThreatSeverity.HIGH     -> colors.criticalLight
        ThreatSeverity.MEDIUM   -> colors.warning
        ThreatSeverity.LOW      -> colors.safe
    }

    val icon = when (threat.type) {
        WifiThreatType.OPEN_NETWORK   -> Icons.Filled.LockOpen
        WifiThreatType.WEAK_ENCRYPTION -> Icons.Filled.LockPerson
        WifiThreatType.EVIL_TWIN      -> Icons.Filled.WifiOff
        WifiThreatType.DNS_HIJACK     -> Icons.Filled.Dns
        WifiThreatType.CAPTIVE_PORTAL -> Icons.Filled.Web
    }

    val techDescription = when (threat.type) {
        WifiThreatType.OPEN_NETWORK -> "The access point does not enforce encryption. Any nearby antenna can intercept raw network packets over the air, exposing unencrypted traffic, session tokens, or personal identifiers."
        WifiThreatType.WEAK_ENCRYPTION -> "Legacy protocols (WEP or WPA-TKIP) have deep cryptographic flaws. Their keystreams can be reconstructed and decrypted in minutes using automated traffic replay tools."
        WifiThreatType.EVIL_TWIN -> "A rogue wireless device is broadcasting the exact same network name (SSID). This aims to trick your phone into connecting to the attacker's server to intercept credentials."
        WifiThreatType.DNS_HIJACK -> "The local router resolves standard hostnames to unverified IP addresses. Attackers use this to redirect queries to server clones or run phishing pages."
        WifiThreatType.CAPTIVE_PORTAL -> "An authentication gateway is capturing outgoing traffic. Until you authenticate, traffic might be sniffed or modified."
    }

    val impactLevel = when (threat.severity) {
        ThreatSeverity.CRITICAL -> "Extremely High (Immediate Data Exfiltration Risk)"
        ThreatSeverity.HIGH     -> "High (Potential Credential Theft / Decryption Risk)"
        ThreatSeverity.MEDIUM   -> "Medium (Vulnerable to Advanced Targeted Attacks)"
        ThreatSeverity.LOW      -> "Low (Informational / Captive Portal Intercept)"
    }

    GlassSurface(
        onClick = { isExpanded = !isExpanded },
        borderColor = severityColor.copy(alpha = 0.5f)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(severityColor.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = severityColor, modifier = Modifier.size(18.dp))
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            threat.type.name.replace('_', ' ').lowercase()
                                .replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.titleSmall,
                            color = colors.textPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            StatusChip(threat.severity.name, severityColor)
                            Icon(
                                if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                null,
                                tint = colors.textMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Text(
                        threat.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HorizontalDivider(color = colors.border.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 4.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            "Technical Context:",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.warning,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            techDescription,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            "Severity Impact:",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.critical,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            impactLevel,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecommendationRow(index: Int, text: String) {
    val colors = LocalRakshakXColors.current
    GlassCard {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(colors.primaryMuted, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "$index",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SonarRadarScanner(modifier: Modifier = Modifier, color: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "radar")

    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val waveScale1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave1"
    )
    val waveScale2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, delayMillis = 1000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave2"
    )

    Box(
        modifier = modifier.size(160.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = size.center
            val maxRadius = size.minDimension / 2

            drawCircle(
                color = color.copy(alpha = 0.08f),
                radius = maxRadius,
                center = center
            )
            drawCircle(
                color = color.copy(alpha = 0.12f),
                radius = maxRadius * 0.66f,
                center = center
            )
            drawCircle(
                color = color.copy(alpha = 0.15f),
                radius = maxRadius * 0.33f,
                center = center
            )

            drawCircle(
                color = color.copy(alpha = 0.3f * (1f - waveScale1)),
                radius = maxRadius * waveScale1,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )
            drawCircle(
                color = color.copy(alpha = 0.3f * (1f - waveScale2)),
                radius = maxRadius * waveScale2,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )

            drawLine(
                color = color.copy(alpha = 0.15f),
                start = Offset(center.x - maxRadius, center.y),
                end = Offset(center.x + maxRadius, center.y),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = color.copy(alpha = 0.15f),
                start = Offset(center.x, center.y - maxRadius),
                end = Offset(center.x, center.y + maxRadius),
                strokeWidth = 1.dp.toPx()
            )

            val rad = Math.toRadians(rotationAngle.toDouble())
            val lineEndX = center.x + maxRadius * Math.cos(rad).toFloat()
            val lineEndY = center.y + maxRadius * Math.sin(rad).toFloat()

            drawLine(
                color = color.copy(alpha = 0.6f),
                start = center,
                end = Offset(lineEndX, lineEndY),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )

            drawArc(
                color = color.copy(alpha = 0.08f),
                startAngle = rotationAngle - 45f,
                sweepAngle = 45f,
                useCenter = true,
                size = size,
                topLeft = Offset.Zero
            )

            drawCircle(
                color = color,
                radius = 6.dp.toPx(),
                center = center
            )
        }
    }
}

@Composable
private fun GridInfoItem(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color
) {
    val colors = LocalRakshakXColors.current
    GlassCard(
        borderColor = colors.border.copy(alpha = 0.2f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(color.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textMuted,
                    fontSize = 10.sp
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
