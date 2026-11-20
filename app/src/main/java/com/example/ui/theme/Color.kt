package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Broadcast Dark Mode Palette (Green, Black, White)
val DarkBackground = Color(0xFF070B09)
val DarkSurface = Color(0xFF0E1511)
val DarkSurfaceVariant = Color(0xFF16211B)
val DarkSurfaceElevated = Color(0xFF1C2A22)
val DarkOutline = Color(0xFF2B3F33)

val EmeraldPrimaryDark = Color(0xFF00E676)
val OnEmeraldPrimaryDark = Color(0xFF032111)
val EmeraldContainerDark = Color(0xFF083D22)
val OnEmeraldContainerDark = Color(0xFFA7F3D0)

val ChalkWhite = Color(0xFFFFFFFF)
val ChalkMuted = Color(0xFFB3C4BA)
val AmberBoundaryDark = Color(0xFFFFB300)
val WicketCrimsonDark = Color(0xFFFF5252)

// Daylight Stadium Light Mode Palette (Green, White, Black)
val LightBackground = Color(0xFFF5FAF7)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFE9F2EC)
val LightSurfaceElevated = Color(0xFFDCEBE2)
val LightOutline = Color(0xFFC2D6C9)

val EmeraldPrimaryLight = Color(0xFF047857)
val OnEmeraldPrimaryLight = Color(0xFFFFFFFF)
val EmeraldContainerLight = Color(0xFFD1FAE5)
val OnEmeraldContainerLight = Color(0xFF022C22)

val StadiumBlack = Color(0xFF0A120E)
val StadiumSlate = Color(0xFF47594E)
val AmberBoundaryLight = Color(0xFFD97706)
val WicketCrimsonLight = Color(0xFFDC2626)

@Immutable
data class CricketExtendedColors(
    val boundaryFour: Color,
    val boundarySix: Color,
    val wicket: Color,
    val liveBadge: Color,
    val dotBall: Color,
    val extraAmber: Color,
    val goldMvp: Color,
    val pitchTurf: Color,
    val pitchCrease: Color
)

val DarkCricketColors = CricketExtendedColors(
    boundaryFour = Color(0xFF00E676),
    boundarySix = Color(0xFFFFB300),
    wicket = Color(0xFFFF5252),
    liveBadge = Color(0xFF00E676),
    dotBall = Color(0xFF4F6659),
    extraAmber = Color(0xFFFFC107),
    goldMvp = Color(0xFFFFD54F),
    pitchTurf = Color(0xFF0B2B1B),
    pitchCrease = Color(0x33FFFFFF)
)

val LightCricketColors = CricketExtendedColors(
    boundaryFour = Color(0xFF047857),
    boundarySix = Color(0xFFD97706),
    wicket = Color(0xFFDC2626),
    liveBadge = Color(0xFF059669),
    dotBall = Color(0xFF6B7D72),
    extraAmber = Color(0xFFB45309),
    goldMvp = Color(0xFFB45309),
    pitchTurf = Color(0xFF065F46),
    pitchCrease = Color(0x44FFFFFF)
)

val LocalCricketColors = staticCompositionLocalOf { DarkCricketColors }
