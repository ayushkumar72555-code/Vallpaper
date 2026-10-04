package com.ayush.vallpaper.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val VallpaperDarkColors = darkColorScheme(
    primary = VallpaperDarkPrimary,
    onPrimary = VallpaperDarkOnPrimary,
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = VallpaperTypography,
        content = content
    )
}
