package com.security.rakshakx.ui.theme

import androidx.compose.ui.graphics.Color

// ── Background & Surface ──────────────────────────────────────
val Navy900     = Color(0xFF0F172A)   // Deepest background
val Navy800     = Color(0xFF1E293B)   // Card background
val Navy700     = Color(0xFF334155)   // Elevated surfaces
val Navy600     = Color(0xFF475569)   // Hover / active state
val Navy500     = Color(0xFF1E293B).copy(alpha = 0.5f)   // Borders

// ── Primary ── Premium Blue & Purple ─────────────────────────
val PremiumBlue   = Color(0xFF4776E6)
val PremiumPurple = Color(0xFF8E54E9)
val PremiumGreen  = Color(0xFF10B981)
val PremiumRed    = Color(0xFFEF4444)
val PremiumOrange = Color(0xFFF59E0B)

// ── Legacy Status Colors (Mapping to new palette) ──────────────
val Cyan400     = PremiumBlue
val RedCritical = PremiumRed
val RedHigh     = PremiumRed.copy(alpha = 0.8f)
val RedMuted    = PremiumRed.copy(alpha = 0.1f)

val OrangeWarn  = PremiumOrange
val AmberWarn   = PremiumOrange.copy(alpha = 0.8f)
val OrangeMuted = PremiumOrange.copy(alpha = 0.1f)

val GreenSafe   = PremiumGreen
val GreenLight  = PremiumGreen.copy(alpha = 0.8f)
val GreenMuted  = PremiumGreen.copy(alpha = 0.1f)

// ── Text ───────────────────────────────────────────────────────
val TextPrimary   = Color.White
val TextSecondary = Color.White.copy(alpha = 0.7f)
val TextMuted     = Color.White.copy(alpha = 0.5f)
val TextOnCyan    = Color.Black

// ── Glassmorphism / Glow ───────────────────────────────────────
val GlowCyan    = PremiumBlue.copy(alpha = 0.2f)
val GlowRed     = PremiumRed.copy(alpha = 0.2f)
val GlowGreen   = PremiumGreen.copy(alpha = 0.2f)
val GlowOrange  = PremiumOrange.copy(alpha = 0.2f)
val CardBorder  = Color(0x33FFFFFF)
val GlassBg     = Color(0x1AFFFFFF)