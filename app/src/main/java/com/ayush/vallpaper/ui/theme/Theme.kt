package com.ayush.vallpaper.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val VallpaperDarkColors = darkColorScheme(
    primary = VallpaperDarkPrimary,
    onPrimary = VallpaperDarkOnPrimary,
    primaryContainer = VallpaperDarkPrimaryContainer,
    onPrimaryContainer = VallpaperDarkOnPrimaryContainer,
    secondary = VallpaperDarkSecondary,
    onSecondary = VallpaperDarkOnSecondary,
    background = VallpaperDarkBackground,
    onBackground = VallpaperDarkOnBackground,
    surface = VallpaperDarkSurface,
    onSurface = VallpaperDarkOnSurface,
    surfaceVariant = VallpaperDarkSurfaceVariant,
    onSurfaceVariant = VallpaperDarkOnSurfaceVariant
)

private val VallpaperLightColors = lightColorScheme(
    primary = VallpaperLightPrimary,
    onPrimary = VallpaperLightOnPrimary,
    primaryContainer = VallpaperLightPrimaryContainer,
    onPrimaryContainer = VallpaperLightOnPrimaryContainer,
    secondary = VallpaperLightSecondary,
    onSecondary = VallpaperLightOnSecondary,
    background = VallpaperLightBackground,
    onBackground = VallpaperLightOnBackground,
    surface = VallpaperLightSurface,
    onSurface = VallpaperLightOnSurface,
    surfaceVariant = VallpaperLightSurfaceVariant,
    onSurfaceVariant = VallpaperLightOnSurfaceVariant
)

@Composable
fun VallpaperTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        VallpaperDarkColors
    } else {
        VallpaperLightColors
    }

    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window

            WindowCompat.setDecorFitsSystemWindows(window, true)

            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()

            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isStatusBarContrastEnforced = false
                window.isNavigationBarContrastEnforced = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = VallpaperTypography,
        content = content
    )
}
