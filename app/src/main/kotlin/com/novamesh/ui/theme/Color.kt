package com.novamesh.ui.theme

import androidx.compose.ui.graphics.Color

// ═══════════════════════════════════════════════════════════════
// NovaMesh Messenger — Color Palette
// Silver Futuristic Theme with Glassmorphism
// Inspired by WhatsApp + Snapchat premium aesthetics
// ═══════════════════════════════════════════════════════════════

// ─── Silver / Metallic Futuristic Palette ───
val SilverLight = Color(0xFFF5F5F5)          // Light silver surface
val SilverDark = Color(0xFF1A1A2E)           // Dark futuristic navy-silver
val SilverGlass = Color(0xB3F0F0F0)          // Glassmorphism light overlay
val SilverGlassDark = Color(0x801A1A2E)      // Glassmorphism dark overlay
val SilverGradientStart = Color(0xFFE8E8E8)  // Silver gradient light
val SilverGradientEnd = Color(0xFFC0C0C0)    // Silver gradient dark
val SilverAccent = Color(0xFF9C27B0)         // Purple-silver accent (Nova signature)
val SilverGlow = Color(0xFF7C4DFF)           // Neon glow accent
val SilverDivider = Color(0xFFE0E0E0)        // Subtle silver divider
val SilverSurfaceLight = Color(0xFFFAFAFA)   // Light surface
val SilverSurfaceDark = Color(0xFF16213E)    // Dark futuristic surface

// ─── Primary palette (Nova signature purple/silver) ───
val PrimaryLight = Color(0xFF7C4DFF)          // Nova purple (brand)
val OnPrimaryLight = Color(0xFFFFFFFF)
val PrimaryContainerLight = Color(0xFFEDE7F6)
val OnPrimaryContainerLight = Color(0xFF1A0033)

val PrimaryDark = Color(0xFFB388FF)
val OnPrimaryDark = Color(0xFF2A0052)
val PrimaryContainerDark = Color(0xFF4A0072)
val OnPrimaryContainerDark = Color(0xFFEDE7F6)

// ─── Secondary palette (silver/cyan) ───
val SecondaryLight = Color(0xFF00BFA5)        // Teal-cyan accent
val OnSecondaryLight = Color(0xFFFFFFFF)
val SecondaryContainerLight = Color(0xFFB2DFDB)
val OnSecondaryContainerLight = Color(0xFF00251E)

val SecondaryDark = Color(0xFF64FFDA)
val OnSecondaryDark = Color(0xFF00392E)
val SecondaryContainerDark = Color(0xFF00796B)
val OnSecondaryContainerDark = Color(0xFFB2DFDB)

// ─── Tertiary palette ───
val TertiaryLight = Color(0xFFFF6D00)         // Orange accent (streaks)
val OnTertiaryLight = Color(0xFFFFFFFF)
val TertiaryContainerLight = Color(0xFFFFF3E0)
val OnTertiaryContainerLight = Color(0xFF3E1A00)

val TertiaryDark = Color(0xFFFFAB40)
val OnTertiaryDark = Color(0xFF5D2E00)
val TertiaryContainerDark = Color(0xFF994A00)
val OnTertiaryContainerDark = Color(0xFFFFF3E0)

// ─── Error palette ───
val ErrorLight = Color(0xFFFF1744)
val OnErrorLight = Color(0xFFFFFFFF)
val ErrorContainerLight = Color(0xFFFFCDD2)
val OnErrorContainerLight = Color(0xFF4A0011)

val ErrorDark = Color(0xFFFF8A80)
val OnErrorDark = Color(0xFF73001E)
val ErrorContainerDark = Color(0xFFB71C1C)
val OnErrorContainerDark = Color(0xFFFFCDD2)

// ─── Surface / Background ───
val BackgroundLight = Color(0xFFF5F5F5)       // Silver-light background
val OnBackgroundLight = Color(0xFF1C1B1F)
val SurfaceLight = Color(0xFFFAFAFA)          // Silver surface
val OnSurfaceLight = Color(0xFF1C1B1F)
val SurfaceVariantLight = Color(0xFFF0F0F0)
val OnSurfaceVariantLight = Color(0xFF49454F)
val OutlineLight = Color(0xFFC0C0C0)

val BackgroundDark = Color(0xFF0D1117)        // Dark silver-navy
val OnBackgroundDark = Color(0xFFE6E1E5)
val SurfaceDark = Color(0xFF16213E)           // Futuristic dark surface
val OnSurfaceDark = Color(0xFFE6E1E5)
val SurfaceVariantDark = Color(0xFF2A2A3E)
val OnSurfaceVariantDark = Color(0xFFCAC4D0)
val OutlineDark = Color(0xFF4A4A5E)

// ─── Custom NovaMesh colors ───
val NovaPrimary = Color(0xFF7C4DFF)           // Bright purple (brand)
val NovaSecondary = Color(0xFF00E5FF)         // Cyan accent (stories)
val NovaTertiary = Color(0xFFFF6D00)          // Orange (snap streaks)
val NovaSuccess = Color(0xFF00C853)           // Green (online/verified)
val NovaWarning = Color(0xFFFFD600)           // Yellow (warning)
val NovaError = Color(0xFFFF1744)             // Red (error/delete)
val NovaSurfaceDark = Color(0xFF16213E)       // Dark futuristic surface
val NovaSurfaceLight = Color(0xFFF5F5F5)      // Light silver surface

// ─── Glassmorphism Overlays ───
val NovaGlass = Color(0xCCF0F0F0)            // Glassmorphism overlay (light)
val NovaGlassDark = Color(0xCC1A1A2E)        // Glassmorphism overlay (dark)
val NovaGlassBorder = Color(0x33FFFFFF)       // Glass border (subtle white)
val NovaGlassBlur = Color(0x0DFFFFFF)         // Subtle blur tint

// ─── Chat bubble colors ───
val BubbleSentLight = Color(0xFF7C4DFF)       // Nova purple (sent)
val BubbleSentLightText = Color(0xFFFFFFFF)
val BubbleReceivedLight = Color(0xFFF0F0F0)   // Silver (received)
val BubbleReceivedLightText = Color(0xFF1C1B1F)
val BubbleSentDark = Color(0xFF4A0072)        // Dark mode sent
val BubbleSentDarkText = Color(0xFFFFFFFF)
val BubbleReceivedDark = Color(0xFF2A2A3E)    // Dark mode received
val BubbleReceivedDarkText = Color(0xFFE6E1E5)

// ─── Story ring colors (gradient) ───
val StoryRingColors = listOf(
    Color(0xFF7C4DFF),  // Purple
    Color(0xFF00E5FF),  // Cyan
    Color(0xFFFF6D00),  // Orange
    Color(0xFFFF1744),  // Red
    Color(0xFF00C853),  // Green
    Color(0xFFFFD600),  // Yellow
)

// ─── Snap streak fire colors ───
val StreakFireGradient = listOf(
    Color(0xFFFF6D00),
    Color(0xFFFFAB00),
    Color(0xFFFFD600),
)

// ─── Navigation bar colors ───
val NavBarLight = Color(0xE6F5F5F5)          // Semi-transparent silver
val NavBarDark = Color(0xE616213E)           // Semi-transparent dark navy
val NavBarIndicator = Color(0xFF7C4DFF)      // Active tab indicator

// ─── Status bar colors ───
val StatusBarBackground = Color(0x99000000)   // Semi-transparent dark
val StatusBarProgress = Color(0xFF7C4DFF)     // Purple progress
val StatusBarProgressBg = Color(0x4DFFFFFF)   // White semi-transparent bg
