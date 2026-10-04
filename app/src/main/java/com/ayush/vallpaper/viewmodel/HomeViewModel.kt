package com.ayush.vallpaper.viewmodel

import androidx.lifecycle.ViewModel
import com.ayush.vallpaper.data.AppSettingsRepository
import com.ayush.vallpaper.data.MockTrackRepository
import com.ayush.vallpaper.domain.model.Track
import com.ayush.vallpaper.domain.model.WallpaperStyle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class HomeUiState(
    val track: Track? = null,
    val selectedStyle: WallpaperStyle =
        WallpaperStyle.AMBIENT,
    val automaticWallpaper: Boolean = true
)

class HomeViewModel(
    private val trackRepository: MockTrackRepository,
    private val settingsRepository: AppSettingsRepository
) : ViewModel() {

    private val _uiState =
        MutableStateFlow(
            HomeUiState(
                track =
                    trackRepository.getCurrentTrack(),
                selectedStyle =
                    settingsRepository.settings.value
                        .selectedStyle,
                automaticWallpaper =
                    settingsRepository.settings.value
                        .automaticWallpaper
            )
        )

    val uiState: StateFlow<HomeUiState> =
        _uiState.asStateFlow()

    fun selectStyle(
        style: WallpaperStyle
    ) {

        settingsRepository.setSelectedStyle(
            style
        )

        _uiState.value =
            _uiState.value.copy(
                selectedStyle = style
            )
    }

    fun toggleAutomaticWallpaper() {

        val enabled =
            !_uiState.value.automaticWallpaper

        settingsRepository.setAutomaticWallpaper(
            enabled
        )

        _uiState.value =
            _uiState.value.copy(
                automaticWallpaper = enabled
            )
    }
}