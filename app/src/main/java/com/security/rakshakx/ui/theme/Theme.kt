package com.security.rakshakx.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

data class RakshakXColors(
    // Background hierarchy — deep navy spectrum
    val background: Color = Charcoal,
    val backgroundDeep: Color = Obsidian,
    val cardBackground: Color = Gunmetal,
    val surfaceElevated: Color = Slate,
    val surfaceActive: Color = SlateLight,
    val border: Color = SlateBorder,

    // Brand anchors (for accent lines, brand moments)
    val brandDeep: Color = NavyDeep,
    val brandMid: Color = NavyMid,
    val brandSlate: Color = SlateBlue,

    // CTA / Interactive — Azure
    val primary: Color = RoyalBlue,
    val primaryDark: Color = RoyalBlueDark,
    val primaryLight: Color = RoyalBlueLight,
    val primaryMuted: Color = RoyalBlueMuted,

    // Secondary accent — Calm Teal (Call channel, secondary UI)
    val primaryVariant: Color = Amethyst,
    val primaryVariantDark: Color = AmethystDark,
    val primaryVariantLight: Color = AmethystLight,
    val primaryVariantMuted: Color = AmethystMuted,

    // Status: Risk
    val critical: Color = Crimson,
    val criticalDark: Color = CrimsonDark,
    val criticalLight: Color = CrimsonLight,
    val criticalBg: Color = CrimsonMuted,

    // Status: Caution
    val warning: Color = Amber,
    val warningDark: Color = AmberDark,
    val warningLight: Color = AmberLight,
    val warningBg: Color = AmberMuted,

    // Status: Safe — Emerald ONLY for confirmed protected states
    val safe: Color = Emerald,
    val safeDark: Color = EmeraldDark,
    val safeLight: Color = EmeraldLight,
    val safeBg: Color = EmeraldMuted,

    // Warm accent
    val gold: Color = Gold,
    val goldLight: Color = GoldLight,
    val goldMuted: Color = GoldMuted,

    // Text hierarchy
    val textPrimary: Color = TextWhite,
    val textSecondary: Color = TextSecondary,
    val textMuted: Color = TextMuted,
    val textOnPrimary: Color = TextOnPrimary,

    // Surface overlays (used sparingly)
    val glassWhite: Color = GlassWhite,
    val glassBorder: Color = GlassBorder,
    val glassHighlight: Color = GlassHighlight,
    val scrim: Color = Scrim,

    // Semantic glow references (minimal use; no full glow effects)
    val glowCyan: Color = RoyalBlueMuted,
    val glowRed: Color = CrimsonMuted,
    val glowGreen: Color = EmeraldMuted,
    val glowOrange: Color = AmberMuted,
    val glassBg: Color = GlassWhite,

    // Channel identity colors
    val channelSms: Color = RoyalBlue,
    val channelCall: Color = Amethyst,
    val channelWeb: Color = Emerald,
    val channelEmail: Color = Crimson,
)

val LocalRakshakXColors = staticCompositionLocalOf { RakshakXColors() }

private val DarkScheme = darkColorScheme(
    primary = RoyalBlue,
    onPrimary = TextOnPrimary,
    secondary = Amethyst,
    onSecondary = TextWhite,
    tertiary = Emerald,
    onTertiary = Obsidian,
    background = Charcoal,
    onBackground = TextWhite,
    surface = Gunmetal,
    onSurface = TextWhite,
    surfaceVariant = Slate,
    onSurfaceVariant = TextSecondary,
    error = Crimson,
    onError = TextWhite,
    outline = SlateBorder,
    outlineVariant = SlateLight,
    inverseSurface = TextWhite,
    inverseOnSurface = Obsidian,
    scrim = Scrim,
)

@Composable
fun RakshakXTheme(content: @Composable () -> Unit) {
    val rakshakXColors = RakshakXColors()

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = Obsidian.toArgb()
            window.navigationBarColor = Charcoal.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    CompositionLocalProvider(LocalRakshakXColors provides rakshakXColors) {
        MaterialTheme(
            colorScheme = DarkScheme,
            typography = Typography,
            shapes = RakshakXShapes,
            content = content
        )
    }
}
