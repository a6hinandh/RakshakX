package com.security.rakshakx.ui.screens

import android.content.Context
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.security.rakshakx.ui.anim.rememberHaptics
import com.security.rakshakx.ui.components.*
import com.security.rakshakx.ui.data.*
import com.security.rakshakx.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ReportScreen(onBack: () -> Unit) {
    val colors = LocalRakshakXColors.current
    val context = LocalContext.current
    val haptics = rememberHaptics()
    val scope = rememberCoroutineScope()

    var threats by remember { mutableStateOf<List<ThreatLogEntry>>(emptyList()) }
    var isExporting by remember { mutableStateOf(false) }
    var exportMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            threats = try { ThreatLogRepository.getAllThreats(context) } catch (_: Exception) { emptyList() }
        }
    }

    val totalThreats = threats.size
    val criticalCount = threats.count { it.severity == Severity.CRITICAL }
    val highCount = threats.count { it.severity == Severity.HIGH }
    val mediumCount = threats.count { it.severity == Severity.MEDIUM }
    val lowCount = threats.count { it.severity == Severity.LOW }
    val smsCount = threats.count { it.channel == Channel.SMS }
    val callCount = threats.count { it.channel == Channel.CALL }
    val webCount = threats.count { it.channel == Channel.WEB }
    val emailCount = threats.count { it.channel == Channel.EMAIL }

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

            PageHeader(
                title = "Security Report",
                infoText = "View threat analytics, channel breakdowns, and export security reports as CSV.",
                onBack = { haptics.tick(); onBack() }
            )

            // ── Summary Stats ──
            SectionHeader(title = "Overview")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("Total", "$totalThreats", colors.primary, Modifier.weight(1f))
                StatCard("Critical", "$criticalCount", colors.critical, Modifier.weight(1f))
                StatCard("High", "$highCount", colors.warning, Modifier.weight(1f))
                StatCard("Medium", "$mediumCount", colors.gold, Modifier.weight(1f))
            }

            // ── Channel Breakdown ──
            SectionHeader(title = "By Channel")
            GlassCard {
                ChannelBreakdownRow("SMS", smsCount, totalThreats, colors.channelSms)
                Spacer(Modifier.height(10.dp))
                ChannelBreakdownRow("Call", callCount, totalThreats, colors.channelCall)
                Spacer(Modifier.height(10.dp))
                ChannelBreakdownRow("Web", webCount, totalThreats, colors.channelWeb)
                Spacer(Modifier.height(10.dp))
                ChannelBreakdownRow("Email", emailCount, totalThreats, colors.channelEmail)
            }

            // ── Risk Distribution ──
            SectionHeader(title = "Risk Distribution")
            GlassCard {
                RiskDistRow("Critical", criticalCount, totalThreats, colors.critical)
                Spacer(Modifier.height(8.dp))
                RiskDistRow("High", highCount, totalThreats, colors.warning)
                Spacer(Modifier.height(8.dp))
                RiskDistRow("Medium", mediumCount, totalThreats, colors.gold)
                Spacer(Modifier.height(8.dp))
                RiskDistRow("Low", lowCount, totalThreats, colors.safe)
            }

            // ── Export Actions ──
            SectionHeader(title = "Export & Share")

            Button(
                onClick = {
                    isExporting = true
                    scope.launch(Dispatchers.IO) {
                        try {
                            val csv = generateCsvReport(threats)
                            val path = saveCsvReport(context, csv)
                            exportMessage = "Report saved to $path"
                        } catch (e: Exception) {
                            exportMessage = "Export failed: ${e.message}"
                        }
                        isExporting = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                enabled = !isExporting && threats.isNotEmpty()
            ) {
                if (isExporting) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = TextWhite, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Filled.FileDownload, null, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(8.dp))
                Text("Export CSV Report", fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = {
                    scope.launch(Dispatchers.IO) {
                        try {
                            val csv = generateCsvReport(threats)
                            val uri = getShareReportUri(context, csv)
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/csv"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                putExtra(Intent.EXTRA_SUBJECT, "RakshakX Security Report")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(intent, "Share Report"))
                        } catch (e: Exception) {
                            exportMessage = "Share failed: ${e.message}"
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.primaryVariant),
                enabled = threats.isNotEmpty()
            ) {
                Icon(Icons.Filled.Share, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Share Report", fontWeight = FontWeight.Bold)
            }

            // Cyber Crime Portal
            OutlinedButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://cybercrime.gov.in"))
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.critical),
                border = ButtonDefaults.outlinedButtonBorder(true).copy(
                    brush = androidx.compose.ui.graphics.SolidColor(colors.critical.copy(alpha = 0.2f))
                )
            ) {
                Icon(Icons.Filled.Policy, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Report to Cyber Crime Portal", fontWeight = FontWeight.Bold)
            }

            if (exportMessage != null) {
                GlassCard(borderColor = colors.safe.copy(alpha = 0.15f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CheckCircle, null, tint = colors.safe, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(exportMessage!!, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                    }
                }
            }

            RakshakXFooter()
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    GlassSurface(modifier = modifier, borderColor = color.copy(alpha = 0.1f)) {
        Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.headlineSmall, color = color, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextMuted, letterSpacing = 0.5.sp)
        }
    }
}

@Composable
private fun ChannelBreakdownRow(name: String, count: Int, total: Int, color: Color) {
    val colors = LocalRakshakXColors.current
    val fraction = if (total > 0) count.toFloat() / total else 0f
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(name, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary, modifier = Modifier.width(50.dp))
        Spacer(Modifier.width(10.dp))
        RiskBar(score = fraction, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(10.dp))
        Text("$count", style = MaterialTheme.typography.labelLarge, color = color, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun RiskDistRow(name: String, count: Int, total: Int, color: Color) {
    val colors = LocalRakshakXColors.current
    val fraction = if (total > 0) count.toFloat() / total else 0f
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        StatusChip(text = name, color = color)
        Spacer(Modifier.width(10.dp))
        RiskBar(score = fraction, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(10.dp))
        Text("${"%.0f".format(fraction * 100)}%", style = MaterialTheme.typography.labelMedium, color = colors.textSecondary)
    }
}

private fun generateCsvReport(threats: List<ThreatLogEntry>): String {
    val sb = StringBuilder()
    sb.appendLine("ID,Channel,Severity,Title,Description,Source,Risk Score,Timestamp,Action")
    threats.forEach { t ->
        val desc = t.description.replace("\"", "\"\"").replace("\n", " ")
        val title = t.title.replace("\"", "\"\"")
        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(t.timestamp))
        sb.appendLine("${t.id},${t.channel.name},${t.severity.label},\"$title\",\"$desc\",\"${t.source}\",${t.riskScore},$dateStr,${t.action}")
    }
    return sb.toString()
}

private fun saveCsvReport(context: Context, csv: String): String {
    val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val fileName = "rakshakx_report_$dateStr.csv"
    
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/RakshakX")
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
        if (uri != null) {
            resolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(csv.toByteArray())
            }
            return "Downloads/RakshakX/$fileName"
        }
    }
    
    // Legacy fallback (API < 29)
    val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "RakshakX")
    dir.mkdirs()
    val file = File(dir, fileName)
    file.writeText(csv)
    return file.absolutePath
}

private fun getShareReportUri(context: Context, csv: String): Uri {
    val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val dir = File(context.cacheDir, "shared_reports")
    dir.mkdirs()
    val file = File(dir, "rakshakx_report_$dateStr.csv")
    file.writeText(csv)
    return androidx.core.content.FileProvider.getUriForFile(
        context, "com.security.rakshakx.provider", file
    )
}
