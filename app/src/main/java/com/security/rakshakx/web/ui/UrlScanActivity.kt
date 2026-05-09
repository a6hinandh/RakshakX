package com.security.rakshakx.web.ui

import android.util.Log
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.security.rakshakx.web.analyzers.DomainRiskAnalyzer
import com.security.rakshakx.web.analyzers.ThreatIntelRepository
import com.security.rakshakx.web.models.ThreatAssessment
import com.security.rakshakx.web.models.ThreatLevel
import com.security.rakshakx.web.storage.ThreatDatabase
import com.security.rakshakx.web.storage.ThreatEntity
import com.security.rakshakx.notifications.vpn.VpnProtectionNotifier
import kotlinx.coroutines.launch
import java.net.URL

class UrlScanActivity : ComponentActivity() {

    private lateinit var analyzer: DomainRiskAnalyzer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val intelRepo = ThreatIntelRepository(applicationContext)
        analyzer = DomainRiskAnalyzer(intelRepo)

        var urlToScan = ""
        
        // Handle PROCESS_TEXT or SEND intent
        if (intent.action == Intent.ACTION_PROCESS_TEXT) {
            val text = intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString() ?: ""
            urlToScan = extractUrl(text)
        } else if (intent.action == Intent.ACTION_SEND) {
            val text = intent.getStringExtra(Intent.EXTRA_TEXT) ?: ""
            urlToScan = extractUrl(text)
        } else if (intent.hasExtra("EXTRA_URL")) {
            urlToScan = intent.getStringExtra("EXTRA_URL") ?: ""
        }

        setContent {
            MaterialTheme {
                if (urlToScan.isBlank()) {
                    ErrorDialog("No valid URL found to scan.")
                } else {
                    ScanResultDialog(urlToScan)
                }
            }
        }
    }

    private fun extractUrl(text: String): String {
        val matcher = Patterns.WEB_URL.matcher(text)
        if (matcher.find()) {
            return matcher.group() ?: ""
        }
        return text.split(" ").firstOrNull { Patterns.WEB_URL.matcher(it).matches() } ?: ""
    }

    @Composable
    fun ScanResultDialog(url: String) {
        val scope = rememberCoroutineScope()
        var assessment by remember { mutableStateOf<ThreatAssessment?>(null) }
        var isScanning by remember { mutableStateOf(true) }

        LaunchedEffect(url) {
            scope.launch {
                try {
                    val domain = getDomain(url)
                    val result = analyzer.assess(
                        domain = domain,
                        redirectCount = 0,
                        tlsMismatch = false,
                        dnsFlags = emptyList(),
                        url = url
                    )
                    assessment = result
                    
                    // Log to database if HIGH or CRITICAL
                    if (result.level == ThreatLevel.HIGH || result.level == ThreatLevel.CRITICAL) {
                        val entity = ThreatEntity(
                            timestamp = result.timestamp,
                            domain = result.domain,
                            level = result.level.name,
                            action = result.action.name,
                            reasons = result.reasons.joinToString(", "),
                            fraudScore = 0, // Calculate appropriate score if needed
                            fraudCategory = "Phishing URL",
                            recommendedAction = "Do not visit",
                            blockReason = "Manual scan detection",
                            visibleSignals = "",
                            correlationData = "",
                            browserPackage = "Manual Scan",
                            url = url,
                            destinationIp = "",
                            sniHost = domain
                        )
                        ThreatDatabase.getInstance(applicationContext).threatDao().insert(entity)
                        
                        val notifier = VpnProtectionNotifier(applicationContext)
                        notifier.createChannel()
                        val reasons = result.reasons.joinToString("\n• ")
                        notifier.notifyThreat("RakshakX Threat Blocked", "URL: $url\nReasons:\n• $reasons")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    isScanning = false
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f)) // Dim background
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "RakshakX Threat Scan", 
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    if (isScanning) {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Analyzing safety...", style = MaterialTheme.typography.bodyMedium)
                    } else {
                        val result = assessment
                        if (result != null) {
                            val color = when (result.level) {
                                ThreatLevel.CRITICAL -> Color(0xFFD32F2F)
                                ThreatLevel.HIGH -> Color(0xFFF57C00)
                                ThreatLevel.MEDIUM -> Color(0xFFFFB300)
                                ThreatLevel.LOW -> Color(0xFF388E3C)
                            }
                            
                            Card(
                                colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        "Level: ${result.level.name}",
                                        color = color,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "URL: $url", 
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            
                            if (result.reasons.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text("Detection Signals:", fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    result.reasons.forEach { reason ->
                                        Row(modifier = Modifier.padding(vertical = 2.dp)) {
                                            Text("• ", fontWeight = FontWeight.Bold, color = color)
                                            Text(reason, style = MaterialTheme.typography.bodyMedium)
                                        }
                                    }
                                }
                            }
                        } else {
                            Text("Unable to analyze this URL.")
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                    Button(
                        onClick = { 
                            Log.d("UrlScanActivity", "User clicked Close")
                            finish() 
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Dismiss", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    @Composable
    fun ErrorDialog(message: String) {
        AlertDialog(
            onDismissRequest = { finish() },
            title = { Text("Scan Failed") },
            text = { Text(message) },
            confirmButton = {
                Button(onClick = { finish() }) {
                    Text("OK")
                }
            }
        )
    }

    private fun getDomain(urlStr: String): String {
        return try {
            var formattedUrl = urlStr
            if (!formattedUrl.startsWith("http://") && !formattedUrl.startsWith("https://")) {
                formattedUrl = "https://$formattedUrl"
            }
            URL(formattedUrl).host
        } catch (e: Exception) {
            urlStr
        }
    }
}
