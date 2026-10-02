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

/**
 * The app is dark-first (premium coaching aesthetic). Status/nav bar appearance
 * is aligned with the scheme; MainActivity uses enableEdgeToEdge() so content
 * draws behind system bars and screens apply their own insets.
 */
@Composable
fun EdutopiaTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // Edutopia ships a hand-tuned dark palette; keep it regardless of OS toggle
    // so brand visuals stay consistent (a deliberate product decision).
    val colorScheme = DarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as? Activity ?: return@SideEffect
            val window = activity.window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
            @Suppress("DEPRECATION")
            window.statusBarColor = colorScheme.background.toArgb()
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = EdutopiaTypography,
        shapes = EdutopiaShapes,
        content = content
    )
}
