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
    val primary: Color = Cyan400,
    val primaryVariant: Color = Cyan500,
    val primaryPressed: Color = Cyan600,
    val teal: Color = Teal400,
    val tealSecondary: Color = Teal500,
    val critical: Color = RedCritical,
    val criticalLight: Color = RedHigh,
    val criticalBg: Color = RedMuted,
    val warning: Color = OrangeWarn,
    val warningLight: Color = AmberWarn,
    val warningBg: Color = OrangeMuted,
    val safe: Color = GreenSafe,
    val safeLight: Color = GreenLight,
    val safeBg: Color = GreenMuted,
    val textPrimary: Color = TextPrimary,
    val textSecondary: Color = TextSecondary,
    val textMuted: Color = TextMuted,
    val textOnPrimary: Color = TextOnCyan,
    val glowCyan: Color = GlowCyan,
    val glowRed: Color = GlowRed,
    val glowGreen: Color = GlowGreen,
    val glowOrange: Color = GlowOrange,
    val glassBg: Color = GlassBg,
)

val LocalRakshakXColors = staticCompositionLocalOf { RakshakXColors() }

private val DarkScheme = darkColorScheme(
    primary = Cyan400,
    onPrimary = TextOnCyan,
    secondary = Teal400,
    onSecondary = TextOnCyan,
    tertiary = Cyan500,
    background = Navy900,
    onBackground = TextPrimary,
    surface = Navy800,
    onSurface = TextPrimary,
    surfaceVariant = Navy700,
    onSurfaceVariant = TextSecondary,
    error = RedCritical,
    onError = Color.White,
    outline = Navy500,
    outlineVariant = Navy600,
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