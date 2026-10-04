package com.ayush.vallpaper.domain.model

data class AppSettings(
    val automaticWallpaper: Boolean = true,
    val selectedStyle: WallpaperStyle = WallpaperStyle.AMBIENT,
    val applyToHomeScreen: Boolean = true,
    val applyToLockScreen: Boolean = true
)
