package com.ayush.vallpaper.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayush.vallpaper.data.AppSettingsRepository
import com.ayush.vallpaper.domain.model.Track
import com.ayush.vallpaper.domain.model.WallpaperStyle
import com.ayush.vallpaper.domain.repository.TrackRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val track: Track? = null,
    val selectedStyle: WallpaperStyle = WallpaperStyle.AMBIENT,
    val automaticWallpaper: Boolean = true
)

class HomeViewModel(
    private val trackRepository: TrackRepository,
    private val settingsRepository: AppSettingsRepository
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> =
        combine(
            trackRepository.currentTrack,
            settingsRepository.settings
        ) { track, settings ->
            HomeUiState(
                track = track,
                selectedStyle = settings.selectedStyle,
                automaticWallpaper = settings.automaticWallpaper
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState(
                track = trackRepository.currentTrack.value,
                selectedStyle = WallpaperStyle.AMBIENT,
                automaticWallpaper = true
            )
        )

    fun selectStyle(style: WallpaperStyle) {
        viewModelScope.launch {
            settingsRepository.setSelectedStyle(style)
        }
    }

    fun toggleAutomaticWallpaper() {
        viewModelScope.launch {
            settingsRepository.setAutomaticWallpaper(
                !uiState.value.automaticWallpaper
            )
        }
    }
}
