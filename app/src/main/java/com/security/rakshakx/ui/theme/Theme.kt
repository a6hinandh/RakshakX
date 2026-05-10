package com.security.rakshakx.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Extended color palette for RakshakX cybersecurity theme.
 * Accessible via LocalRakshakXColors.current
 */
data class RakshakXColors(
    val background: Color = Navy900,
    val cardBackground: Color = Navy800,
    val surfaceElevated: Color = Navy700,
    val surfaceActive: Color = Navy600,
    val border: Color = Navy500,
    val primary: Color = PremiumBlue,
    val primaryVariant: Color = PremiumPurple,
    val primaryPressed: Color = PremiumBlue.copy(alpha = 0.8f),
    val teal: Color = PremiumGreen,
    val tealSecondary: Color = PremiumGreen.copy(alpha = 0.7f),
    val critical: Color = PremiumRed,
    val criticalLight: Color = PremiumRed.copy(alpha = 0.8f),
    val criticalBg: Color = PremiumRed.copy(alpha = 0.1f),
    val warning: Color = PremiumOrange,
    val warningLight: Color = PremiumOrange.copy(alpha = 0.8f),
    val warningBg: Color = PremiumOrange.copy(alpha = 0.1f),
    val safe: Color = PremiumGreen,
    val safeLight: Color = PremiumGreen.copy(alpha = 0.8f),
    val safeBg: Color = PremiumGreen.copy(alpha = 0.1f),
    val textPrimary: Color = Color.White,
    val textSecondary: Color = Color.White.copy(alpha = 0.7f),
    val textMuted: Color = Color.White.copy(alpha = 0.5f),
    val textOnPrimary: Color = Color.Black,
    val glowCyan: Color = PremiumBlue.copy(alpha = 0.2f),
    val glowRed: Color = PremiumRed.copy(alpha = 0.2f),
    val glowGreen: Color = PremiumGreen.copy(alpha = 0.2f),
    val glowOrange: Color = PremiumOrange.copy(alpha = 0.2f),
    val glassBg: Color = Color.White.copy(alpha = 0.1f),
)

val LocalRakshakXColors = staticCompositionLocalOf { RakshakXColors() }

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF4776E6),
    onPrimary = Color.Black,
    secondary = Color(0xFF8E54E9),
    onSecondary = Color.White,
    tertiary = Color(0xFF4F46E5),
    background = Color(0xFF0F172A),
    onBackground = Color.White,
    surface = Color(0xFF1E293B),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF334155),
    onSurfaceVariant = Color(0xFF94A3B8),
    error = Color(0xFFEF4444),
    onError = Color.White,
    outline = Color(0xFF334155),
    outlineVariant = Color(0xFF475569),
)

@Composable
fun RakshakXTheme(content: @Composable () -> Unit) {
    val rakshakXColors = RakshakXColors()

    CompositionLocalProvider(LocalRakshakXColors provides rakshakXColors) {
        MaterialTheme(
            colorScheme = DarkScheme,
            typography = Typography,
            shapes = RakshakXShapes,
            content = content
        )
    }
}