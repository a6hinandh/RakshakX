package com.security.rakshakx.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.security.rakshakx.ui.anim.StaggeredEntry
import com.security.rakshakx.ui.anim.rememberHaptics
import com.security.rakshakx.ui.components.*
import com.security.rakshakx.ui.theme.*
import kotlin.math.pow

@Composable
fun PasswordStudioScreen(onBack: () -> Unit) {
    val colors = LocalRakshakXColors.current
    val haptics = rememberHaptics()
    var selectedTab by remember { mutableIntStateOf(0) }

    PremiumBackground {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            // Header
            PageHeader(
                title = "Password Studio",
                infoText = "Analyze password strength and generate secure passwords entirely offline on your device.",
                onBack = { haptics.tick(); onBack() },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
            )

            // Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = colors.primary,
                divider = { HorizontalDivider(color = colors.border.copy(alpha = 0.2f)) }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0; haptics.tick() },
                    text = { Text("Analyzer", color = if (selectedTab == 0) colors.primary else colors.textMuted) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1; haptics.tick() },
                    text = { Text("Generator", color = if (selectedTab == 1) colors.primary else colors.textMuted) }
                )
            }

            AnimatedContent(targetState = selectedTab, label = "PasswordStudioTabs") { tab ->
                when (tab) {
                    0 -> PasswordAnalyzerTab()
                    1 -> PasswordGeneratorTab()
                }
            }
        }
    }
}

// ── Analyzer Tab ─────────────────────────────────────────────────────────────

@Composable
private fun PasswordAnalyzerTab() {
    val colors = LocalRakshakXColors.current
    var password by remember { mutableStateOf("") }
    
    val entropy = calculateEntropy(password)
    val strength = evaluateStrength(entropy, password)
    
    val barColor by animateColorAsState(targetValue = strength.color, label = "StrengthColor")
    val fillFraction = (entropy / 100.0).coerceIn(0.0, 1.0).toFloat()
    
    LazyColumn(
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Enter a password to test") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace)
            )
        }
        
        if (password.isNotEmpty()) {
            item {
                GlassCard(borderColor = barColor.copy(alpha=0.5f)) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Strength Score", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary, fontWeight = FontWeight.Bold)
                            Text(strength.label, style = MaterialTheme.typography.titleMedium, color = barColor, fontWeight = FontWeight.Bold)
                        }
                        
                        // Progress bar
                        Box(modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(colors.surfaceElevated)) {
                            Box(modifier = Modifier.fillMaxWidth(fillFraction).fillMaxHeight().background(barColor))
                        }
                        
                        // Metrics
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            MetricItem("Length", "${password.length} chars", colors)
                            MetricItem("Entropy", "${entropy.toInt()} bits", colors)
                            MetricItem("Crack Time", calculateCrackTime(entropy), colors)
                        }
                        
                        HorizontalDivider(color = colors.border.copy(alpha=0.2f))
                        
                        // Analysis tips
                        val tips = generateTips(password)
                        if (tips.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Recommendations", style = MaterialTheme.typography.labelMedium, color = colors.textMuted)
                                tips.forEach { tip ->
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                                        Icon(Icons.Filled.Info, null, tint = colors.primary, modifier = Modifier.size(16.dp))
                                        Text(tip, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                                    }
                                }
                            }
                        } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.VerifiedUser, null, tint = colors.safe, modifier = Modifier.size(16.dp))
                                Text("This password follows best practices.", style = MaterialTheme.typography.bodySmall, color = colors.safe)
                            }
                        }
                    }
                }
            }
        } else {
            item {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.SecurityUpdateGood, null, tint = colors.primaryMuted, modifier = Modifier.size(48.dp))
                        Text("Type a password to analyze it", color = colors.textMuted)
                        Text("100% Offline. Never saved.", style = MaterialTheme.typography.labelSmall, color = colors.textMuted.copy(alpha=0.5f))
                    }
                }
            }
        }
    }
}

// ── Generator Tab ────────────────────────────────────────────────────────────

@Composable
private fun PasswordGeneratorTab() {
    val colors = LocalRakshakXColors.current
    val haptics = rememberHaptics()
    val context = LocalContext.current
    
    var length by remember { mutableFloatStateOf(16f) }
    var useUpper by remember { mutableStateOf(true) }
    var useLower by remember { mutableStateOf(true) }
    var useNumbers by remember { mutableStateOf(true) }
    var useSymbols by remember { mutableStateOf(true) }
    
    var generatedPassword by remember { mutableStateOf(generatePassword(16, true, true, true, true)) }
    
    val regenerate = {
        generatedPassword = generatePassword(length.toInt(), useUpper, useLower, useNumbers, useSymbols)
        haptics.tick()
    }
    
    LazyColumn(
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            GlassCard {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = generatedPassword,
                        style = MaterialTheme.typography.headlineMedium.copy(fontFamily = FontFamily.Monospace),
                        color = colors.textPrimary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                    )
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("password", generatedPassword))
                                Toast.makeText(context, "Password copied to clipboard", Toast.LENGTH_SHORT).show()
                                haptics.success()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = colors.surfaceElevated, contentColor = colors.textPrimary)
                        ) {
                            Icon(Icons.Filled.ContentCopy, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Copy")
                        }
                        Button(
                            onClick = regenerate,
                            colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                        ) {
                            Icon(Icons.Filled.Refresh, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Generate")
                        }
                    }
                }
            }
        }
        
        item {
            SectionHeader("Configuration")
        }
        
        item {
            GlassCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Length", color = colors.textPrimary)
                        Text("${length.toInt()}", color = colors.primary, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = length,
                        onValueChange = { length = it },
                        onValueChangeFinished = regenerate,
                        valueRange = 8f..64f,
                        steps = 55,
                        colors = SliderDefaults.colors(thumbColor = colors.primary, activeTrackColor = colors.primary)
                    )
                    
                    HorizontalDivider(color = colors.border.copy(alpha=0.2f))
                    
                    ToggleRow("Uppercase (A-Z)", useUpper) { useUpper = it; if (!isValidConfig(useUpper, useLower, useNumbers, useSymbols)) useUpper = true else regenerate() }
                    ToggleRow("Lowercase (a-z)", useLower) { useLower = it; if (!isValidConfig(useUpper, useLower, useNumbers, useSymbols)) useLower = true else regenerate() }
                    ToggleRow("Numbers (0-9)", useNumbers) { useNumbers = it; if (!isValidConfig(useUpper, useLower, useNumbers, useSymbols)) useNumbers = true else regenerate() }
                    ToggleRow("Symbols (!@#$)", useSymbols) { useSymbols = it; if (!isValidConfig(useUpper, useLower, useNumbers, useSymbols)) useSymbols = true else regenerate() }
                }
            }
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val colors = LocalRakshakXColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = colors.primary, checkedTrackColor = colors.primaryMuted)
        )
    }
}

private fun isValidConfig(vararg toggles: Boolean) = toggles.any { it }

@Composable
private fun MetricItem(label: String, value: String, colors: RakshakXColors) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = colors.textMuted)
    }
}

// ── Logic ─────────────────────────────────────────────────────────────────────

private fun generatePassword(length: Int, upper: Boolean, lower: Boolean, nums: Boolean, syms: Boolean): String {
    val uppercase = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    val lowercase = "abcdefghijklmnopqrstuvwxyz"
    val numbers = "0123456789"
    val symbols = "!@#$%^&*()-_=+[]{}|;:,.<>/?"
    
    var pool = ""
    if (upper) pool += uppercase
    if (lower) pool += lowercase
    if (nums) pool += numbers
    if (syms) pool += symbols
    
    if (pool.isEmpty()) return ""
    
    val random = java.security.SecureRandom()
    return (1..length).map { pool[random.nextInt(pool.length)] }.joinToString("")
}

private fun calculateEntropy(password: String): Double {
    var poolSize = 0
    if (password.any { it.isLowerCase() }) poolSize += 26
    if (password.any { it.isUpperCase() }) poolSize += 26
    if (password.any { it.isDigit() }) poolSize += 10
    if (password.any { !it.isLetterOrDigit() }) poolSize += 32
    
    if (poolSize == 0) return 0.0
    return password.length * (Math.log(poolSize.toDouble()) / Math.log(2.0))
}

data class StrengthResult(val label: String, val color: Color)

private fun evaluateStrength(entropy: Double, password: String): StrengthResult {
    // Punish short passwords regardless of entropy
    if (password.length < 8) return StrengthResult("Very Weak", Color(0xFFFF5252))
    
    return when {
        entropy < 40 -> StrengthResult("Weak", Color(0xFFFF9800)) // Orange
        entropy < 60 -> StrengthResult("Moderate", Color(0xFFFFEB3B)) // Yellow
        entropy < 80 -> StrengthResult("Strong", Color(0xFF4CAF50)) // Green
        else -> StrengthResult("Very Strong", Color(0xFF00E676)) // Bright Green
    }
}

private fun calculateCrackTime(entropy: Double): String {
    if (entropy <= 0) return "Instant"
    // Approximate: Assume an attacker can guess 10 billion passwords per second (offline fast hash)
    // 10^10 hashes/sec ≈ 2^33.2 hashes/sec
    val hashesPerSecond = 2.0.pow(33.2)
    val secondsToCrack = 2.0.pow(entropy) / hashesPerSecond
    
    return when {
        secondsToCrack < 1 -> "Instant"
        secondsToCrack < 60 -> "< 1 min"
        secondsToCrack < 3600 -> "${(secondsToCrack / 60).toInt()} mins"
        secondsToCrack < 86400 -> "${(secondsToCrack / 3600).toInt()} hours"
        secondsToCrack < 31536000 -> "${(secondsToCrack / 86400).toInt()} days"
        secondsToCrack < 31536000 * 100L -> "${(secondsToCrack / 31536000).toInt()} years"
        else -> "Centuries"
    }
}

private fun generateTips(password: String): List<String> {
    val tips = mutableListOf<String>()
    if (password.length < 12) tips.add("Increase length to at least 12 characters.")
    if (!password.any { it.isUpperCase() }) tips.add("Add uppercase letters.")
    if (!password.any { it.isLowerCase() }) tips.add("Add lowercase letters.")
    if (!password.any { it.isDigit() }) tips.add("Add numbers.")
    if (!password.any { !it.isLetterOrDigit() }) tips.add("Add special symbols (e.g. !@#$).")
    
    // Check consecutive numbers/letters
    if (password.contains(Regex("[0-9]{4,}"))) tips.add("Avoid long sequences of numbers.")
    
    return tips
}
