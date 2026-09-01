package com.mindnova.edutopia.core.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = BrandIndigo,
    onPrimary = TextWhitePrimary,
    primaryContainer = BrandIndigoDark,
    onPrimaryContainer = TextWhitePrimary,
    secondary = AccentCyan,
    onSecondary = TextWhitePrimary,
    secondaryContainer = SurfaceDarkCard,
    onSecondaryContainer = AccentCyanLight,
    tertiary = AccentAmber,
    onTertiary = TextDarkPrimary,
    background = BackgroundDark,
    onBackground = TextWhitePrimary,
    surface = SurfaceDark,
    onSurface = TextWhitePrimary,
    surfaceVariant = SurfaceDarkVariant,
    onSurfaceVariant = TextWhiteSecondary,
    outline = SurfaceBorderDark,
    error = AccentRose,
    onError = TextWhitePrimary
)

private val LightColorScheme = lightColorScheme(
    primary = BrandIndigo,
    onPrimary = TextWhitePrimary,
    primaryContainer = BrandIndigoLight,
    onPrimaryContainer = TextWhitePrimary,
    secondary = AccentCyan,
    onSecondary = TextWhitePrimary,
    secondaryContainer = SurfaceLightCard,
    onSecondaryContainer = BrandIndigoDark,
    tertiary = AccentAmber,
    onTertiary = TextWhitePrimary,
    background = BackgroundLight,
    onBackground = TextDarkPrimary,
    surface = SurfaceLight,
    onSurface = TextDarkPrimary,
    surfaceVariant = SurfaceLightElevated,
    onSurfaceVariant = TextDarkSecondary,
    outline = SurfaceBorderLight,
    error = AccentRose,
    onError = TextWhitePrimary
)

@Composable
fun EdutopiaTheme(
    darkTheme: Boolean = true, // Default to deep dark pro aesthetic
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = EdutopiaTypography,
        shapes = EdutopiaShapes,
        content = content
    )
}
