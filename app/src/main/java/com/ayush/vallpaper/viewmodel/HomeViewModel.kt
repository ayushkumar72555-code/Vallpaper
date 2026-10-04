package com.ayush.vallpaper.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayush.vallpaper.data.AppSettingsRepository
import com.ayush.vallpaper.domain.model.Track
import com.ayush.vallpaper.domain.model.WallpaperStyle
import com.ayush.vallpaper.domain.repository.TrackRepository
import com.ayush.vallpaper.wallpaper.WallpaperGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val track: Track? = null,
    val selectedStyle: WallpaperStyle =
        WallpaperStyle.AMBIENT,
    val automaticWallpaper: Boolean = true,
    val generatedWallpaper: Bitmap? = null,
    val isGenerating: Boolean = false,
    val generationError: String? = null
)

class HomeViewModel(
    private val trackRepository: TrackRepository,
    private val settingsRepository: AppSettingsRepository,
    private val wallpaperGenerator: WallpaperGenerator
) : ViewModel() {

    private val _uiState =
        MutableStateFlow(
            HomeUiState()
        )

    val uiState: StateFlow<HomeUiState> =
        _uiState.asStateFlow()

    init {

        viewModelScope.launch {

            trackRepository.currentTrack
                .collect { track ->

                    _uiState.value =
                        _uiState.value.copy(
                            track = track
                        )
                }
        }

        viewModelScope.launch {

            settingsRepository.settings
                .collect { settings ->

                    _uiState.value =
                        _uiState.value.copy(
                            selectedStyle =
                                settings.selectedStyle,
                            automaticWallpaper =
                                settings.automaticWallpaper
                        )
                }
        }
    }

    fun selectStyle(
        style: WallpaperStyle
    ) {

        viewModelScope.launch {

            settingsRepository
                .setSelectedStyle(style)
        }
    }

    fun toggleAutomaticWallpaper() {

        viewModelScope.launch {

            settingsRepository
                .setAutomaticWallpaper(
                    !_uiState.value
                        .automaticWallpaper
                )
        }
    }

    fun generateWallpaper(
        width: Int,
        height: Int
    ) {

        val currentTrack =
            _uiState.value.track
                ?: return

        val currentStyle =
            _uiState.value.selectedStyle

        viewModelScope.launch(
            Dispatchers.Default
        ) {

            _uiState.value =
                _uiState.value.copy(
                    isGenerating = true,
                    generationError = null
                )

            try {

                val wallpaper =
                    wallpaperGenerator.generate(
                        track =
                            currentTrack,
                        style =
                            currentStyle,
                        width =
                            width,
                        height =
                            height
                    )

                _uiState.value =
                    _uiState.value.copy(
                        generatedWallpaper =
                            wallpaper,
                        isGenerating = false,
                        generationError = null
                    )

            } catch (exception: Exception) {

                _uiState.value =
                    _uiState.value.copy(
                        isGenerating = false,
                        generationError =
                            exception.message
                                ?: "Unable to generate wallpaper"
                    )
            }
        }
    }

    fun clearGeneratedWallpaper() {

        _uiState.value =
            _uiState.value.copy(
                generatedWallpaper = null,
                generationError = null
            )
    }
}