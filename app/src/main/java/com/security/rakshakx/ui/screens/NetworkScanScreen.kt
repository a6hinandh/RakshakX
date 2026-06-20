package com.security.rakshakx.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.security.rakshakx.core.network.*
import com.security.rakshakx.ui.anim.StaggeredEntry
import com.security.rakshakx.ui.anim.rememberHaptics
import com.security.rakshakx.ui.components.*
import com.security.rakshakx.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun NetworkScanScreen(onBack: () -> Unit) {
    val colors = LocalRakshakXColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptics = rememberHaptics()
    
    val store = remember { NetworkDeviceStore(context) }
    var isScanning by remember { mutableStateOf(false) }
    var devices by remember { mutableStateOf<List<NetworkDevice>?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    var selectedDevice by remember { mutableStateOf<NetworkDevice?>(null) }

    PremiumBackground {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {

            // ── Header ────────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 8.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StaggeredEntry(index = 0) {
                    PageHeader(
                        title = "Home Network Manager",
                        infoText = "Discover and manage devices on your local network. Mark trusted devices and monitor for unknown connections.",
                        onBack = { haptics.tick(); onBack() }
                    )
                }

                // ── Warning banner ────────────────────────────────────────
                StaggeredEntry(index = 1) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.warningBg)
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Schedule,
                            contentDescription = null,
                            tint = colors.warning,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            "Scanning may take 30–60 seconds depending on network size.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.warning
                        )
                    }
                }

                // ── Scan button ───────────────────────────────────────────
                StaggeredEntry(index = 2) {
                    Button(
                        onClick = {
                            haptics.click()
                            errorMessage = null
                            isScanning = true
                            scope.launch(Dispatchers.IO) {
                                try {
                                    val result = LocalNetworkScanner.getInstance(context).scanNetwork()
                                    devices = result
                                } catch (e: Exception) {
                                    errorMessage = "Scan failed: ${e.localizedMessage}"
                                } finally {
                                    isScanning = false
                                }
                            }
                        },
                        enabled = !isScanning,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                    ) {
                        if (isScanning) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Scanning…", fontWeight = FontWeight.SemiBold)
                        } else {
                            Icon(
                                Icons.Filled.NetworkCheck,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Scan Local Network", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // ── Content ───────────────────────────────────────────────────
            when {
                isScanning -> ScanningIndicator()

                errorMessage != null -> {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        GlassCard(borderColor = colors.critical) {
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
                                Text(
                                    errorMessage!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.critical
                                )
                            }
                        }
                    }
                }

                devices == null -> {
                    // Idle state
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .background(colors.primaryMuted, RoundedCornerShape(20.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.DeviceHub,
                                    contentDescription = null,
                                    tint = colors.primary,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            Text(
                                "Tap 'Scan Local Network' to discover devices connected to your Wi-Fi.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.textSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                else -> {
                    val list = devices!!
                    val highRiskCount = list.count { it.riskLevel == DeviceRisk.HIGH_RISK }
                    val suspiciousCount = list.count { it.riskLevel == DeviceRisk.SUSPICIOUS }
                    val untrustedCount = list.count { !it.isTrusted }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Summary stats
                        item {
                            StaggeredEntry(index = 0) {
                                ScanSummaryCard(
                                    total = list.size,
                                    untrusted = untrustedCount,
                                    highRisk = highRiskCount,
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            SectionHeader(title = "Discovered Devices (${list.size})")
                            Spacer(Modifier.height(4.dp))
                        }

                        // Order: High Risk > Untrusted > Safe/Trusted, then by IP
                        items(list.sortedWith(
                            compareByDescending<NetworkDevice> { it.riskLevel == DeviceRisk.HIGH_RISK }
                                .thenBy { it.isTrusted }
                                .thenBy { it.ipAddress }
                        ), key = { it.ipAddress }) { device ->
                            NetworkDeviceCard(device) {
                                selectedDevice = device
                            }
                        }

                        item {
                            Spacer(Modifier.height(16.dp))
                            RakshakXFooter()
                            Spacer(Modifier.height(16.dp))
                        }
                    }
                }
            }
        }
    }
    
    // Manage Device Dialog
    if (selectedDevice != null) {
        ManageDeviceDialog(
            device = selectedDevice!!,
            store = store,
            onDismiss = { selectedDevice = null },
            onUpdate = { updatedDevice ->
                // Update local list
                devices = devices?.map { if (it.ipAddress == updatedDevice.ipAddress) updatedDevice else it }
                selectedDevice = null
            }
        )
    }
}

// ── Summary card ──────────────────────────────────────────────────────────────

@Composable
private fun ScanSummaryCard(total: Int, untrusted: Int, highRisk: Int) {
    val colors = LocalRakshakXColors.current
    GlassCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            SummaryStatItem("Devices", total.toString(), colors.primary)
            VerticalDivider(
                modifier = Modifier.height(40.dp),
                color = colors.border.copy(alpha = 0.4f)
            )
            SummaryStatItem("High Risk", highRisk.toString(), if (highRisk > 0) colors.critical else colors.safe)
            VerticalDivider(
                modifier = Modifier.height(40.dp),
                color = colors.border.copy(alpha = 0.4f)
            )
            SummaryStatItem("Untrusted", untrusted.toString(), if (untrusted > 0) colors.warning else colors.safe)
        }
    }
}

@Composable
private fun SummaryStatItem(label: String, value: String, color: Color) {
    val colors = LocalRakshakXColors.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(value, style = MaterialTheme.typography.titleLarge, color = color, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = colors.textMuted)
    }
}

// ── Device card ───────────────────────────────────────────────────────────────

@Composable
private fun NetworkDeviceCard(device: NetworkDevice, onClick: () -> Unit) {
    val colors = LocalRakshakXColors.current
    val haptics = rememberHaptics()
    
    val isCritical = device.riskLevel == DeviceRisk.HIGH_RISK
    val riskColor = when {
        isCritical -> colors.critical
        !device.isTrusted -> colors.warning
        else -> colors.safe
    }

    GlassCard(borderColor = if (isCritical) riskColor else colors.border) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { haptics.tick(); onClick() }
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Device type icon
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(riskColor.copy(alpha = 0.10f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = deviceTypeIcon(device.deviceType),
                    contentDescription = device.deviceType.name,
                    tint = riskColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                // Name + trust badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        device.customName ?: device.hostname ?: device.ipAddress,
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.textPrimary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    if (isCritical) RiskBadge(DeviceRisk.HIGH_RISK)
                    else TrustBadge(device.isTrusted)
                }

                // Subtitle
                val subtitle = buildString {
                    if (device.customName != null || device.hostname != null) {
                        append(device.ipAddress)
                    }
                    device.vendor?.let {
                        if (isNotEmpty()) append(" · ")
                        append(it)
                    }
                    if (isEmpty()) append(device.deviceType.name.replace('_', ' ').lowercase()
                        .replaceFirstChar { it.uppercase() })
                }
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // MAC
                device.macAddress?.let { mac ->
                    Text(
                        mac,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textMuted
                    )
                }

                // Open ports
                if (device.openPorts.isNotEmpty()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "Open ports:",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textMuted
                    )
                    // Render ports in rows of up to 4 chips
                    val chunked = device.openPorts.chunked(4)
                    Column(
                        modifier = Modifier.padding(top = 2.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        chunked.forEach { rowPorts ->
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                rowPorts.forEach { port -> PortChip(port) }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Manage Device Dialog ──────────────────────────────────────────────────────

@Composable
private fun ManageDeviceDialog(
    device: NetworkDevice,
    store: NetworkDeviceStore,
    onDismiss: () -> Unit,
    onUpdate: (NetworkDevice) -> Unit
) {
    val colors = LocalRakshakXColors.current
    val context = LocalContext.current
    val haptics = rememberHaptics()
    val scope = rememberCoroutineScope()
    
    var customName by remember { mutableStateOf(device.customName ?: "") }
    var isTrusted by remember { mutableStateOf(device.isTrusted) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.cardBackground,
        titleContentColor = colors.textPrimary,
        title = {
            Text("Manage Device", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Device Info
                GlassCard {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("IP: ${device.ipAddress}", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                        Text("MAC: ${device.macAddress ?: "Unknown"}", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                        if (device.hostname != null) {
                            Text("Hostname: ${device.hostname}", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                        }
                    }
                }
                
                OutlinedTextField(
                    value = customName,
                    onValueChange = { customName = it },
                    label = { Text("Custom Name (e.g. My Phone)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Trust this device?", color = colors.textPrimary)
                    Switch(
                        checked = isTrusted,
                        onCheckedChange = { isTrusted = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = colors.primary, checkedTrackColor = colors.primaryMuted)
                    )
                }
                
                // Actions
                if (device.macAddress != null) {
                    OutlinedButton(
                        onClick = {
                            haptics.click()
                            scope.launch {
                                val success = LocalNetworkScanner.getInstance(context).sendWakeOnLan(device.macAddress)
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, if (success) "Wake-on-LAN packet sent!" else "Failed to send WoL", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.PowerSettingsNew, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Send Wake-on-LAN")
                    }
                }
                
                val httpPort = device.openPorts.find { it.port == 80 || it.port == 443 || it.port == 8080 }
                if (httpPort != null) {
                    OutlinedButton(
                        onClick = {
                            haptics.click()
                            val scheme = if (httpPort.port == 443) "https" else "http"
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("$scheme://${device.ipAddress}:${httpPort.port}"))
                            try { context.startActivity(intent) } catch (_: Exception) {}
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.OpenInBrowser, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Open Web Interface")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    haptics.click()
                    val nameToSave = customName.trim().takeIf { it.isNotEmpty() }
                    if (device.macAddress != null) {
                        store.setDeviceMeta(device.macAddress, DeviceMeta(nameToSave, isTrusted))
                    }
                    onUpdate(device.copy(customName = nameToSave, isTrusted = isTrusted))
                },
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = colors.textMuted)
            }
        }
    )
}

@Composable
private fun PortChip(port: OpenPort) {
    val colors = LocalRakshakXColors.current
    val chipColor = if (port.isRisky) colors.critical else colors.textMuted
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(if (port.isRisky) colors.criticalBg else colors.surfaceElevated)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            "${port.port}/${port.service}",
            style = MaterialTheme.typography.labelSmall,
            color = chipColor,
            fontWeight = if (port.isRisky) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun TrustBadge(isTrusted: Boolean) {
    val colors = LocalRakshakXColors.current
    val bg = if (isTrusted) colors.safeBg else colors.warningBg
    val fg = if (isTrusted) colors.safe else colors.warning
    val label = if (isTrusted) "Trusted" else "Untrusted"
    
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = fg, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun RiskBadge(risk: DeviceRisk) {
    val colors = LocalRakshakXColors.current
    val (bg, fg, label) = when (risk) {
        DeviceRisk.HIGH_RISK  -> Triple(colors.criticalBg, colors.critical, "High Risk")
        DeviceRisk.SUSPICIOUS -> Triple(colors.warningBg,  colors.warning,  "Suspicious")
        DeviceRisk.SAFE       -> Triple(colors.safeBg,     colors.safe,     "Safe")
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = fg, fontWeight = FontWeight.Medium)
    }
}

// ── Scanning animation ────────────────────────────────────────────────────────

@Composable
private fun ScanningIndicator() {
    val colors = LocalRakshakXColors.current
    val infiniteTransition = rememberInfiniteTransition(label = "scanPulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scanAlpha"
    )

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(
                        colors.primaryMuted.copy(alpha = alpha),
                        RoundedCornerShape(24.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Radar,
                    contentDescription = null,
                    tint = colors.primary.copy(alpha = alpha),
                    modifier = Modifier.size(40.dp)
                )
            }
            Text(
                "Scanning network…",
                style = MaterialTheme.typography.titleSmall,
                color = colors.textPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "Probing 254 hosts in parallel.\nThis may take 30–60 seconds.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun deviceTypeIcon(type: DeviceType): ImageVector = when (type) {
    DeviceType.ROUTER    -> Icons.Filled.Router
    DeviceType.COMPUTER  -> Icons.Filled.Computer
    DeviceType.PHONE     -> Icons.Filled.PhoneAndroid
    DeviceType.TABLET    -> Icons.Filled.Tablet
    DeviceType.IOT_DEVICE -> Icons.Filled.DeviceHub
    DeviceType.PRINTER   -> Icons.Filled.Print
    DeviceType.TV        -> Icons.Filled.Tv
    DeviceType.UNKNOWN   -> Icons.Filled.DeviceUnknown
}
