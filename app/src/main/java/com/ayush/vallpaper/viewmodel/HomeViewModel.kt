package com.ayush.vallpaper.viewmodel

import androidx.lifecycle.ViewModel
import com.ayush.vallpaper.data.MockTrackRepository
import com.ayush.vallpaper.domain.model.Track
import com.ayush.vallpaper.domain.model.WallpaperStyle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class HomeUiState(
    val track: Track? = null,
    val selectedStyle: WallpaperStyle = WallpaperStyle.AMBIENT,
    val automaticWallpaper: Boolean = true
)

class HomeViewModel : ViewModel() {

    private val repository = MockTrackRepository()

    private val _uiState = MutableStateFlow(
        HomeUiState(
            track = repository.getCurrentTrack()
        )
    )

    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun selectStyle(style: WallpaperStyle) {

        _uiState.value = _uiState.value.copy(
            selectedStyle = style
        )
    }

    fun toggleAutomaticWallpaper() {

        _uiState.value = _uiState.value.copy(
            automaticWallpaper =
                !_uiState.value.automaticWallpaper
        )
    }
}