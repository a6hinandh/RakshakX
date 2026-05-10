package com.security.rakshakx.ui.screens

import android.app.Activity
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.security.rakshakx.ui.components.*
import com.security.rakshakx.ui.theme.*

@Composable
fun ScanningScreen(activity: Activity, onBack: () -> Unit) {
    val context = LocalContext.current
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
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
            // Header with back button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .background(Color.White.copy(alpha = 0.05f), androidx.compose.foundation.shape.CircleShape)
                        .size(40.dp)
                ) {
                    Icon(Icons.Filled.ArrowBack, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Column {
                    Text(
                        "Threat Interception",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Scan suspicious links or QR codes",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Manual URL Entry
            var urlText by remember { mutableStateOf("") }
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "Manual Interception",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = urlText,
                        onValueChange = { urlText = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("https://suspicious-link.com", color = Color.White.copy(alpha = 0.3f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF4776E6),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            cursorColor = Color(0xFF4776E6)
                        ),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true,
                        trailingIcon = {
                            if (urlText.isNotEmpty()) {
                                IconButton(onClick = { urlText = "" }) {
                                    Icon(Icons.Filled.Clear, null, tint = Color.White.copy(alpha = 0.5f))
                                }
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            if (urlText.isNotEmpty()) {
                                val intent = Intent(context, com.security.rakshakx.web.ui.UrlScanActivity::class.java).apply {
                                    putExtra("URL_TO_SCAN", urlText)
                                }
                                context.startActivity(intent)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4776E6))
                    ) {
                        Icon(Icons.Filled.Search, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Analyze Link", fontWeight = FontWeight.Bold)
                    }
                }
            }

            SectionHeader(title = "Quick Tools")
            ScanningOptionCard(
                title = "Link Interceptor",
                description = "Paste a URL to analyze its risk score and phishing probability.",
                icon = Icons.Filled.Link,
                accentColor = Color(0xFF4776E6),
                onClick = {
                    val intent = Intent(context, com.security.rakshakx.web.ui.UrlScanActivity::class.java)
                    context.startActivity(intent)
                }
            )

            // QR Scan Option
            ScanningOptionCard(
                title = "QR Guardian",
                description = "Scan physical QR codes to verify their destination before opening.",
                icon = Icons.Filled.QrCodeScanner,
                accentColor = Color(0xFF8E54E9),
                onClick = {
                    val intent = Intent(context, com.security.rakshakx.web.ui.QrScannerActivity::class.java)
                    context.startActivity(intent)
                }
            )

            // Live Intelligence Promo
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        "Automatic Protection",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "RakshakX also automatically intercepts links in your browser and apps when VPN protection is active.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }
            RakshakXFooter()
        }
    }
}

@Composable
private fun ScanningOptionCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF1E293B),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(accentColor.copy(alpha = 0.15f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = accentColor, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.width(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
            Icon(
                Icons.Filled.ArrowForwardIos,
                null,
                tint = Color.White.copy(alpha = 0.3f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
