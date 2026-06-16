package com.security.rakshakx.ui.screens

import android.app.Activity
import android.content.Intent
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
import com.security.rakshakx.ui.anim.rememberHaptics
import com.security.rakshakx.ui.components.*
import com.security.rakshakx.ui.theme.*

@Composable
fun ScanningScreen(activity: Activity, onBack: () -> Unit) {
    val context = LocalContext.current
    val colors = LocalRakshakXColors.current
    val haptics = rememberHaptics()

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

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { haptics.tick(); onBack() },
                    modifier = Modifier.size(40.dp).background(colors.surfaceElevated, RoundedCornerShape(12.dp))
                ) {
                    Icon(Icons.Filled.ArrowBack, null, tint = colors.textPrimary, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text("Threat Scanner", style = MaterialTheme.typography.headlineSmall, color = colors.textPrimary, fontWeight = FontWeight.Bold)
                    Text("Analyze suspicious links & QR codes", style = MaterialTheme.typography.bodySmall, color = colors.textMuted)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Manual URL Entry
            var urlText by remember { mutableStateOf("") }
            GlassCard(borderColor = colors.primary.copy(alpha = 0.1f)) {
                Text("Manual URL Analysis", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = urlText,
                    onValueChange = { urlText = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("https://suspicious-link.com", color = colors.textMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                        focusedBorderColor = colors.primary,
                        unfocusedBorderColor = colors.border.copy(alpha = 0.3f),
                        cursorColor = colors.primary
                    ),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                    trailingIcon = {
                        if (urlText.isNotEmpty()) {
                            IconButton(onClick = { urlText = "" }) {
                                Icon(Icons.Filled.Clear, null, tint = colors.textMuted)
                            }
                        }
                    }
                )
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = {
                        if (urlText.isNotEmpty()) {
                            val intent = Intent(context, com.security.rakshakx.web.ui.UrlScanActivity::class.java)
                                .putExtra("URL_TO_SCAN", urlText)
                            context.startActivity(intent)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                ) {
                    Icon(Icons.Filled.Search, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Analyze Link", fontWeight = FontWeight.Bold)
                }
            }

            SectionHeader(title = "Quick Tools")

            ScanToolCard(
                title = "Link Interceptor",
                description = "Analyze URL risk score and phishing probability",
                icon = Icons.Filled.Link,
                accentColor = colors.primary,
                onClick = {
                    context.startActivity(Intent(context, com.security.rakshakx.web.ui.UrlScanActivity::class.java))
                }
            )

            ScanToolCard(
                title = "QR Guardian",
                description = "Scan physical QR codes to verify destinations",
                icon = Icons.Filled.QrCodeScanner,
                accentColor = colors.primaryVariant,
                onClick = {
                    context.startActivity(Intent(context, com.security.rakshakx.web.ui.QrScannerActivity::class.java))
                }
            )

            GlassCard(borderColor = colors.border.copy(alpha = 0.2f)) {
                Text("Automatic Protection", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))
                Text("RakshakX automatically intercepts suspicious links in your browser when VPN protection is active.",
                    style = MaterialTheme.typography.bodySmall, color = colors.textMuted)
            }

            RakshakXFooter()
        }
    }
}

@Composable
private fun ScanToolCard(
    title: String,
    description: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit
) {
    GlassSurface(onClick = onClick, borderColor = accentColor.copy(alpha = 0.08f)) {
        Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(50.dp).background(accentColor.copy(alpha = 0.08f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = accentColor, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = TextWhite, fontWeight = FontWeight.SemiBold)
                Text(description, style = MaterialTheme.typography.bodySmall, color = TextMuted)
            }
            Icon(Icons.Filled.ArrowForwardIos, null, tint = TextMuted, modifier = Modifier.size(14.dp))
        }
    }
}
