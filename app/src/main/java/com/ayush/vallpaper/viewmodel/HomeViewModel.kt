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
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class HomeUiState(
    val track: Track? = null,
    val selectedStyle: WallpaperStyle = WallpaperStyle.AMBIENT,
    val automaticWallpaper: Boolean = true,
    val applyToHomeScreen: Boolean = true,
    val applyToLockScreen: Boolean = true,
    val generatedWallpaper: Bitmap? = null,
    val isGenerating: Boolean = false,
    val generationError: String? = null
)

class HomeViewModel(
    private val trackRepository: TrackRepository,
    private val settingsRepository: AppSettingsRepository,
    private val wallpaperGenerator: WallpaperGenerator
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var previewGenerationJob: Job? = null

    init {
        viewModelScope.launch {
            combine(
                trackRepository.currentTrack,
                settingsRepository.settings
            ) { track, settings ->
                track to settings
            }.collect { (track, settings) ->
                _uiState.value = _uiState.value.copy(
                    track = track,
                    selectedStyle = settings.selectedStyle,
                    automaticWallpaper = settings.automaticWallpaper,
                    applyToHomeScreen = settings.applyToHomeScreen,
                    applyToLockScreen = settings.applyToLockScreen
                )

                if (track != null) {
                    generatePreview(
                        style = settings.selectedStyle,
                        track = track,
                        width = currentWidth,
                        height = currentHeight
                    )
                }
            }
        }
    }

    private var currentWidth: Int = 1
    private var currentHeight: Int = 1

    fun updatePreviewSize(width: Int, height: Int) {
        currentWidth = width.coerceAtLeast(1)
        currentHeight = height.coerceAtLeast(1)

        val state = _uiState.value
        val track = state.track ?: return

        generatePreview(
            style = state.selectedStyle,
            track = track,
            width = currentWidth,
            height = currentHeight
        )
    }

    fun selectStyle(style: WallpaperStyle) {
        viewModelScope.launch {
            settingsRepository.setSelectedStyle(style)
        }
    }

    fun toggleAutomaticWallpaper() {
        viewModelScope.launch {
            settingsRepository.setAutomaticWallpaper(
                !_uiState.value.automaticWallpaper
            )
        }
    }

    fun setApplyToHomeScreen(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setApplyToHomeScreen(enabled)
        }
    }

    fun setApplyToLockScreen(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setApplyToLockScreen(enabled)
        }
    }

    fun generateWallpaper(width: Int, height: Int) {
        val currentTrack = _uiState.value.track ?: return
        generatePreview(
            style = _uiState.value.selectedStyle,
            track = currentTrack,
            width = width,
            height = height
        )
    }

    private fun generatePreview(
        style: WallpaperStyle,
        track: Track,
        width: Int,
        height: Int
    ) {
        previewGenerationJob?.cancel()

        previewGenerationJob = viewModelScope.launch(Dispatchers.Default) {
            _uiState.value = _uiState.value.copy(
                isGenerating = true,
                generationError = null
            )

            try {
                val wallpaper = wallpaperGenerator.generate(
                    track = track,
                    style = style,
                    width = width.coerceAtLeast(1),
                    height = height.coerceAtLeast(1)
                )

                _uiState.value = _uiState.value.copy(
                    generatedWallpaper = wallpaper,
                    isGenerating = false,
                    generationError = null
                )
            } catch (exception: Exception) {
                if (!kotlinx.coroutines.currentCoroutineContext().isActive) {
                    return@launch
                }

                _uiState.value = _uiState.value.copy(
                    isGenerating = false,
                    generationError = exception.message
                        ?: "Unable to generate wallpaper"
                )
            }
        }
    }

    override fun onCleared() {
        previewGenerationJob?.cancel()
        super.onCleared()
    }
}
