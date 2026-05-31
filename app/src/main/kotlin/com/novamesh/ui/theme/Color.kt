package com.novamesh.ui.theme

import androidx.compose.ui.graphics.Color

// ═══════════════════════════════════════════════════════════════
// NovaMesh Messenger — Color Palette
// Material You dynamic colors + custom accent palette
// ═══════════════════════════════════════════════════════════════

// ─── Primary palette (Nova signature purple/indigo) ───
val PrimaryLight = Color(0xFF6750A4)       // Material You purple
val OnPrimaryLight = Color(0xFFFFFFFF)
val PrimaryContainerLight = Color(0xFFEADDFF)
val OnPrimaryContainerLight = Color(0xFF21005D)

val PrimaryDark = Color(0xFFD0BCFF)
val OnPrimaryDark = Color(0xFF381E72)
val PrimaryContainerDark = Color(0xFF4F378B)
val OnPrimaryContainerDark = Color(0xFFEADDFF)

// ─── Secondary palette ───
val SecondaryLight = Color(0xFF625B71)
val OnSecondaryLight = Color(0xFFFFFFFF)
val SecondaryContainerLight = Color(0xFFE8DEF8)
val OnSecondaryContainerLight = Color(0xFF1D192B)

val SecondaryDark = Color(0xFFCCC2DC)
val OnSecondaryDark = Color(0xFF332D41)
val SecondaryContainerDark = Color(0xFF4A4458)
val OnSecondaryContainerDark = Color(0xFFE8DEF8)

// ─── Tertiary palette ───
val TertiaryLight = Color(0xFF7D5260)
val OnTertiaryLight = Color(0xFFFFFFFF)
val TertiaryContainerLight = Color(0xFFFFD8E4)
val OnTertiaryContainerLight = Color(0xFF31111D)

val TertiaryDark = Color(0xFFEFB8C8)
val OnTertiaryDark = Color(0xFF492532)
val TertiaryContainerDark = Color(0xFF633B48)
val OnTertiaryContainerDark = Color(0xFFFFD8E4)

// ─── Error palette ───
val ErrorLight = Color(0xFFB3261E)
val OnErrorLight = Color(0xFFFFFFFF)
val ErrorContainerLight = Color(0xFFF9DEDC)
val OnErrorContainerLight = Color(0xFF410E0B)

val ErrorDark = Color(0xFFF2B8B5)
val OnErrorDark = Color(0xFF601410)
val ErrorContainerDark = Color(0xFF8C1D18)
val OnErrorContainerDark = Color(0xFFF9DEDC)

// ─── Surface / Background ───
val BackgroundLight = Color(0xFFFFFBFE)
val OnBackgroundLight = Color(0xFF1C1B1F)
val SurfaceLight = Color(0xFFFFFBFE)
val OnSurfaceLight = Color(0xFF1C1B1F)
val SurfaceVariantLight = Color(0xFFE7E0EC)
val OnSurfaceVariantLight = Color(0xFF49454F)
val OutlineLight = Color(0xFF79747E)

val BackgroundDark = Color(0xFF1C1B1F)
val OnBackgroundDark = Color(0xFFE6E1E5)
val SurfaceDark = Color(0xFF1C1B1F)
val OnSurfaceDark = Color(0xFFE6E1E5)
val SurfaceVariantDark = Color(0xFF49454F)
val OnSurfaceVariantDark = Color(0xFFCAC4D0)
val OutlineDark = Color(0xFF938F99)

// ─── Custom NovaMesh colors ───
val NovaPrimary = Color(0xFF7C4DFF)        // Bright purple (brand)
val NovaSecondary = Color(0xFF00E5FF)      // Cyan accent (stories)
val NovaTertiary = Color(0xFFFF6D00)       // Orange (snap streaks)
val NovaSuccess = Color(0xFF00C853)        // Green (online/verified)
val NovaWarning = Color(0xFFFFD600)        // Yellow (warning)
val NovaError = Color(0xFFFF1744)          // Red (error/delete)
val NovaSurfaceDark = Color(0xFF121212)    // Dark surface
val NovaSurfaceLight = Color(0xFFFAFAFA)   // Light surface
val NovaGlass = Color(0x80FFFFFF)          // Glassmorphism overlay
val NovaGlassDark = Color(0x80000000)      // Dark glass overlay

// ─── Chat bubble colors ───
val BubbleSentLight = Color(0xFFDCF8C6)    // WhatsApp green (sent)
val BubbleReceivedLight = Color(0xFFFFFFFF)
val BubbleSentDark = Color(0xFF005C4B)     // Dark mode sent
val BubbleReceivedDark = Color(0xFF202C33) // Dark mode received

// ─── Story ring colors ───
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
