package com.ayush.vallpaper.data

import com.ayush.vallpaper.domain.model.AppSettings
import com.ayush.vallpaper.domain.model.WallpaperStyle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppSettingsRepository {

    private val _settings = MutableStateFlow(AppSettings())

    val settings: StateFlow<AppSettings> =
        _settings.asStateFlow()

    fun setAutomaticWallpaper(enabled: Boolean) {
        _settings.value =
            _settings.value.copy(
                automaticWallpaper = enabled
            )
    }

    fun setSelectedStyle(style: WallpaperStyle) {
        _settings.value =
            _settings.value.copy(
                selectedStyle = style
            )
    }

    fun setApplyToHomeScreen(enabled: Boolean) {
        _settings.value =
            _settings.value.copy(
                applyToHomeScreen = enabled
            )
    }

    fun setApplyToLockScreen(enabled: Boolean) {
        _settings.value =
            _settings.value.copy(
                applyToLockScreen = enabled
            )
    }
}
