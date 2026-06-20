package com.security.rakshakx.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.security.rakshakx.core.forensics.ForensicBundle
import com.security.rakshakx.core.forensics.ForensicExporter
import com.security.rakshakx.ui.anim.StaggeredEntry
import com.security.rakshakx.ui.anim.rememberHaptics
import com.security.rakshakx.ui.components.*
import com.security.rakshakx.ui.data.ThreatLogRepository
import com.security.rakshakx.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ForensicExportScreen(onBack: () -> Unit) {
    val colors = LocalRakshakXColors.current
    val context = LocalContext.current
    val haptics = rememberHaptics()
    val scope = rememberCoroutineScope()

    var isExporting by remember { mutableStateOf(false) }
    var exportedBundle by remember { mutableStateOf<ForensicBundle?>(null) }
    var exportedFile by remember { mutableStateOf<File?>(null) }
    var exportError by remember { mutableStateOf<String?>(null) }
    var exportMode by remember { mutableStateOf<ExportMode?>(null) }

    fun doExport(mode: ExportMode) {
        isExporting = true
        exportError = null
        exportMode = mode
        haptics.click()
        scope.launch(Dispatchers.IO) {
            try {
                val threats = ThreatLogRepository.getAllThreats(context)
                val bundle = ForensicExporter.export(context, threats)
                val file = ForensicExporter.saveToFile(context, bundle)
                withContext(Dispatchers.Main) {
                    exportedBundle = bundle
                    exportedFile = file
                    isExporting = false
                    haptics.success()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    exportError = "Export failed: ${e.message}"
                    isExporting = false
                    haptics.warning()
                }
            }
        }
    }

    fun shareBundle() {
        val file = exportedFile ?: return
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "RakshakX Forensic Bundle — ${file.name}")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "RakshakX Threat Intelligence Report\nBundle ID: ${exportedBundle?.bundleId}\nIntegrity Hash: ${exportedBundle?.integrityHash}"
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share Forensic Bundle"))
        } catch (e: Exception) {
            exportError = "Share failed: ${e.message}"
        }
    }

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
                    title = "Forensic Export",
                    infoText = "Create STIX 2.1 forensic bundles with SHA-256 integrity hashes for evidence sharing.",
                    onBack = { haptics.click(); onBack() }
                )
            }

            item {
                StaggeredEntry(index = 0) {
                    GlassCard(borderColor = colors.primary.copy(alpha = 0.3f)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(Icons.Filled.Gavel, null, tint = colors.primary, modifier = Modifier.size(20.dp).padding(top = 2.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    "Evidence-Grade Export",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = colors.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    "Bundles include SHA-256 integrity hashing and STIX 2.1 formatted threat indicators for ingestion into SIEM platforms (Splunk, Microsoft Sentinel, IBM QRadar). Device fingerprint anchors the report to this device.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textSecondary,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }
            }

            item {
                StaggeredEntry(index = 1) {
                    SectionHeader(title = "Export Options")
                }
            }

            item {
                StaggeredEntry(index = 2) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        ExportOptionCard(
                            icon = Icons.Filled.DataObject,
                            title = "Export JSON Bundle",
                            description = "Full threat log with device fingerprint, timestamps, and raw event data in JSON format",
                            color = colors.primary,
                            isLoading = isExporting && exportMode == ExportMode.JSON,
                            onClick = { doExport(ExportMode.JSON) }
                        )
                        ExportOptionCard(
                            icon = Icons.Filled.Shield,
                            title = "Export STIX 2.1",
                            description = "Structured Threat Information Expression bundle with indicators, threat actors, and relationships",
                            color = Amethyst,
                            isLoading = isExporting && exportMode == ExportMode.STIX,
                            onClick = { doExport(ExportMode.STIX) }
                        )
                        ExportOptionCard(
                            icon = Icons.Filled.Share,
                            title = "Share to Incident Response",
                            description = "Share the forensic bundle via any installed app — email, Slack, or IR platform",
                            color = Emerald,
                            isLoading = false,
                            enabled = exportedFile != null,
                            onClick = { shareBundle() }
                        )
                    }
                }
            }

            exportError?.let { error ->
                item {
                    StaggeredEntry(index = 3) {
                        GlassCard(borderColor = colors.critical.copy(alpha = 0.4f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Filled.Error, null, tint = colors.critical, modifier = Modifier.size(20.dp))
                                Text(error, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary, modifier = Modifier.weight(1f))
                                TextButton(onClick = { exportError = null }) {
                                    Text("Dismiss", style = MaterialTheme.typography.labelSmall, color = colors.textMuted)
                                }
                            }
                        }
                    }
                }
            }

            exportedBundle?.let { bundle ->
                item {
                    StaggeredEntry(index = 4) {
                        SectionHeader(title = "Export Details")
                    }
                }

                item {
                    StaggeredEntry(index = 5) {
                        GlassCard(borderColor = colors.safe.copy(alpha = 0.3f)) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(Icons.Filled.CheckCircle, null, tint = colors.safe, modifier = Modifier.size(22.dp))
                                    Text(
                                        "Export Successful",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = colors.safe,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                HorizontalDivider(color = colors.border.copy(alpha = 0.3f))
                                BundleDetailRow("Bundle ID", bundle.bundleId.take(18) + "…", colors)
                                BundleDetailRow("Threat Count", "${bundle.threatCount} events", colors)
                                BundleDetailRow("App Version", bundle.appVersion, colors)
                                BundleDetailRow("Exported At", formatTimestampFull(bundle.exportedAt), colors)
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("Integrity Hash (SHA-256)", style = MaterialTheme.typography.labelSmall, color = colors.textMuted)
                                    Text(
                                        bundle.integrityHash,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.textPrimary,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        lineHeight = 15.sp
                                    )
                                }
                                exportedFile?.let { file ->
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("Saved to", style = MaterialTheme.typography.labelSmall, color = colors.textMuted)
                                        Text(
                                            file.absolutePath,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colors.textSecondary,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 10.sp,
                                            lineHeight = 15.sp
                                        )
                                    }
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("Device Fingerprint", style = MaterialTheme.typography.labelSmall, color = colors.textMuted)
                                    Text(
                                        bundle.deviceFingerprint.take(32) + "…",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.textSecondary,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                StaggeredEntry(index = 6) {
                    SectionHeader(title = "Report to Authorities")
                }
            }

            item {
                StaggeredEntry(index = 7) {
                    GlassSurface(
                        onClick = {
                            haptics.click()
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://cybercrime.gov.in"))
                            context.startActivity(intent)
                        }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(colors.criticalBg, RoundedCornerShape(14.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.GppBad, null, tint = colors.critical, modifier = Modifier.size(26.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Report to Cybercrime Portal",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = colors.textPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    "cybercrime.gov.in — India's National Cybercrime Reporting Portal",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textMuted
                                )
                            }
                            Icon(Icons.Filled.OpenInNew, null, tint = colors.textMuted, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            item {
                StaggeredEntry(index = 8) {
                    GlassCard {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                "Supported SIEM Platforms",
                                style = MaterialTheme.typography.titleSmall,
                                color = colors.textPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                            listOf("Splunk Enterprise Security", "Microsoft Sentinel", "IBM QRadar", "Elastic SIEM", "Palo Alto XSOAR").forEach { platform ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(modifier = Modifier.size(5.dp).background(colors.primary, CircleShape))
                                    Text(platform, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
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

@Composable
private fun ExportOptionCard(
    icon: ImageVector,
    title: String,
    description: String,
    color: Color,
    isLoading: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val colors = LocalRakshakXColors.current
    GlassSurface(
        onClick = if (enabled && !isLoading) onClick else null,
        modifier = Modifier
            .border(
                1.dp,
                if (enabled) color.copy(alpha = 0.3f) else colors.border.copy(alpha = 0.2f),
                RoundedCornerShape(16.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        if (enabled) color.copy(alpha = 0.12f) else colors.border.copy(alpha = 0.15f),
                        RoundedCornerShape(14.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.5.dp,
                        color = color
                    )
                } else {
                    Icon(
                        icon,
                        null,
                        tint = if (enabled) color else colors.textMuted,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (enabled) colors.textPrimary else colors.textMuted,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textMuted,
                    lineHeight = 16.sp
                )
                if (!enabled) {
                    Text(
                        "Export data first to enable sharing",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textMuted.copy(alpha = 0.6f)
                    )
                }
            }
            if (enabled && !isLoading) {
                Icon(Icons.Filled.ChevronRight, null, tint = colors.textMuted, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun BundleDetailRow(label: String, value: String, colors: RakshakXColors) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = colors.textMuted, modifier = Modifier.weight(1f))
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            color = colors.textPrimary,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1.5f)
        )
    }
}

private fun formatTimestampFull(timestamp: Long): String =
    SimpleDateFormat("dd MMM yyyy, HH:mm:ss z", Locale.getDefault()).format(Date(timestamp))

private enum class ExportMode { JSON, STIX }
