package com.security.rakshakx.ui.theme

import androidx.compose.ui.graphics.Color

// ── Background Hierarchy — Deep Navy ────────────────────────────
// Anchored to brand Deep Navy (#163A5F). Dark, calm, navy-toned.
val Obsidian       = Color(0xFF07101C)   // Deepest background
val Charcoal       = Color(0xFF0C1828)   // Primary app background
val Gunmetal       = Color(0xFF112030)   // Card / container background
val Slate          = Color(0xFF16283E)   // Elevated surface
val SlateLight     = Color(0xFF1C3250)   // Hover / active state
val SlateBorder    = Color(0xFF223C62)   // Borders and dividers

// ── Brand — Deep Navy (#163A5F from brief) ───────────────────────
val NavyDeep       = Color(0xFF163A5F)   // Primary brand anchor
val NavyMid        = Color(0xFF1E4F82)   // Brand accent (lighter)
val SlateBlue      = Color(0xFF3F5D7D)   // Secondary brand (from brief)

// ── CTA / Interactive — Azure Blue ──────────────────────────────
// Distinct from brand navy; used for buttons, links, active states.
val RoyalBlue      = Color(0xFF2769BF)
val RoyalBlueDark  = Color(0xFF1D559F)
val RoyalBlueLight = Color(0xFF4A8CD9)
val RoyalBlueMuted = Color(0x1A2769BF)

// ── Secondary Accent — Calm Teal ────────────────────────────────
// Replaces purple; used for Call channel and secondary accents.
val Amethyst       = Color(0xFF1A8875)
val AmethystDark   = Color(0xFF12665A)
val AmethystLight  = Color(0xFF28B09A)
val AmethystMuted  = Color(0x1A1A8875)

// ── Status: Safe — Emerald (#2FBF71 from brief) ─────────────────
// Used ONLY for confirmed safe/protected states.
val Emerald        = Color(0xFF2FBF71)
val EmeraldDark    = Color(0xFF249C5C)
val EmeraldLight   = Color(0xFF4AD688)
val EmeraldMuted   = Color(0x1A2FBF71)

// ── Status: Critical — Muted Rose ───────────────────────────────
// Calm deep red; avoids aggressive crimson.
val Crimson        = Color(0xFFBF3B4A)
val CrimsonDark    = Color(0xFF9E2E3B)
val CrimsonLight   = Color(0xFFD95060)
val CrimsonMuted   = Color(0x1ABF3B4A)

// ── Status: Caution — Muted Amber ───────────────────────────────
val Amber          = Color(0xFFC07C00)
val AmberDark      = Color(0xFFA06600)
val AmberLight     = Color(0xFFD99E20)
val AmberMuted     = Color(0x1AC07C00)

// ── Accent: Warm Tone — Subdued Gold ────────────────────────────
val Gold           = Color(0xFFA88530)
val GoldLight      = Color(0xFFC4A04A)
val GoldMuted      = Color(0x19A88530)

// ── Text Hierarchy ──────────────────────────────────────────────
val TextWhite      = Color(0xFFDDE5EF)   // Highest contrast text
val TextPrimary    = Color(0xFFD3DCE9)   // Body text
val TextSecondary  = Color(0xFF8897AC)   // Secondary / labels
val TextMuted      = Color(0xFF56687E)   // Placeholders / meta
val TextOnPrimary  = Color(0xFFDDE5EF)   // Text on colored surfaces

// ── Surface Overlay ─────────────────────────────────────────────
// Kept minimal; glass effects are deprecated as primary language.
val GlassWhite     = Color(0x08FFFFFF)   // Ultra-subtle surface tint
val GlassBorder    = Color(0x10FFFFFF)   // Subtle white border
val GlassHighlight = Color(0x05FFFFFF)   // Barely-visible highlight
val Scrim          = Color(0xCC07101C)   // Modal backdrop

// ── Legacy Aliases ───────────────────────────────────────────────
// These are preserved so existing code (Models.kt, screens) compiles
// without modification. Update call-sites incrementally.

val Navy900     = Charcoal
val Navy800     = Gunmetal
val Navy700     = Slate
val Navy600     = SlateLight
val Navy500     = SlateBorder

// Channel colors used in Models.kt
val PremiumBlue   = RoyalBlue        // SMS channel
val PremiumPurple = Amethyst         // Call channel (teal)
val PremiumGreen  = Emerald          // Web channel
val PremiumRed    = Crimson          // Email channel
val PremiumOrange = Amber

// Status color aliases used across screens
val RedCritical = Crimson
val RedHigh     = CrimsonLight
val RedMuted    = CrimsonMuted

val OrangeWarn  = Amber
val AmberWarn   = AmberLight
val OrangeMuted = AmberMuted

val GreenSafe   = Emerald
val GreenLight  = EmeraldLight
val GreenMuted  = EmeraldMuted

// Misc compatibility aliases
val Cyan400     = RoyalBlue
val TextOnCyan  = Color(0xFFDDE5EF)
val GlowCyan    = RoyalBlueMuted
val GlowRed     = CrimsonMuted
val GlowGreen   = EmeraldMuted
val GlowOrange  = AmberMuted
val CardBorder  = SlateBorder
val GlassBg     = GlassWhite
