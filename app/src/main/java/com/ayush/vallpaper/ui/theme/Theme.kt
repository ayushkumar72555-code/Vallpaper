package com.ayush.vallpaper.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val VallpaperDarkColors = darkColorScheme(
    primary = VallpaperPrimary,
    secondary = VallpaperSecondary,
    background = VallpaperBackground,
    surface = VallpaperSurface,
    surfaceVariant = VallpaperSurfaceVariant
)

@Composable
fun VallpaperTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = VallpaperDarkColors,
        typography = VallpaperTypography,
        content = content
    )
}