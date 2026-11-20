package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

enum class ThemeMode {
    DARK,
    LIGHT,
    SYSTEM
}

private val BroadcastDarkColorScheme = darkColorScheme(
    primary = EmeraldPrimaryDark,
    onPrimary = OnEmeraldPrimaryDark,
    primaryContainer = EmeraldContainerDark,
    onPrimaryContainer = OnEmeraldContainerDark,
    secondary = ChalkWhite,
    onSecondary = DarkBackground,
    secondaryContainer = DarkSurfaceElevated,
    onSecondaryContainer = ChalkWhite,
    tertiary = AmberBoundaryDark,
    onTertiary = DarkBackground,
    background = DarkBackground,
    onBackground = ChalkWhite,
    surface = DarkSurface,
    onSurface = ChalkWhite,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = ChalkMuted,
    surfaceContainerLowest = DarkBackground,
    surfaceContainerLow = DarkSurface,
    surfaceContainer = DarkSurfaceVariant,
    surfaceContainerHigh = DarkSurfaceElevated,
    outline = DarkOutline,
    outlineVariant = DarkOutline,
    error = WicketCrimsonDark,
    onError = ChalkWhite
)

private val DaylightLightColorScheme = lightColorScheme(
    primary = EmeraldPrimaryLight,
    onPrimary = OnEmeraldPrimaryLight,
    primaryContainer = EmeraldContainerLight,
    onPrimaryContainer = OnEmeraldContainerLight,
    secondary = StadiumBlack,
    onSecondary = LightSurface,
    secondaryContainer = LightSurfaceElevated,
    onSecondaryContainer = StadiumBlack,
    tertiary = AmberBoundaryLight,
    onTertiary = LightSurface,
    background = LightBackground,
    onBackground = StadiumBlack,
    surface = LightSurface,
    onSurface = StadiumBlack,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = StadiumSlate,
    surfaceContainerLowest = LightSurface,
    surfaceContainerLow = LightBackground,
    surfaceContainer = LightSurfaceVariant,
    surfaceContainerHigh = LightSurfaceElevated,
    outline = LightOutline,
    outlineVariant = LightOutline,
    error = WicketCrimsonLight,
    onError = LightSurface
)

val CricketShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun CricketProTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val useDark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = if (useDark) BroadcastDarkColorScheme else DaylightLightColorScheme
    val extendedColors = if (useDark) DarkCricketColors else LightCricketColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !useDark
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !useDark
            }
        }
    }

    CompositionLocalProvider(LocalCricketColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = CricketTypography,
            shapes = CricketShapes,
            content = content
        )
    }
}
