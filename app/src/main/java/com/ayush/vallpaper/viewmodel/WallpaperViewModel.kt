package com.ayush.vallpaper.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayush.vallpaper.domain.model.Track
import com.ayush.vallpaper.domain.model.WallpaperStyle
import com.ayush.vallpaper.wallpaper.WallpaperGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class WallpaperPreviewState(
    val bitmap: Bitmap? = null,
    val isGenerating: Boolean = false,
    val error: String? = null
)

class WallpaperViewModel(
    private val generator: WallpaperGenerator
) : ViewModel() {

    private val _previewState =
        MutableStateFlow(
            WallpaperPreviewState()
        )

    val previewState: StateFlow<WallpaperPreviewState> =
        _previewState.asStateFlow()

    fun generateWallpaper(
        track: Track,
        style: WallpaperStyle,
        width: Int,
        height: Int
    ) {

        viewModelScope.launch(
            Dispatchers.Default
        ) {

            _previewState.value =
                WallpaperPreviewState(
                    isGenerating = true
                )

            try {

                val wallpaper =
                    generator.generate(
                        track = track,
                        style = style,
                        width = width,
                        height = height
                    )

                _previewState.value =
                    WallpaperPreviewState(
                        bitmap = wallpaper,
                        isGenerating = false
                    )

            } catch (exception: Exception) {

                _previewState.value =
                    WallpaperPreviewState(
                        isGenerating = false,
                        error =
                            exception.message
                                ?: "Unable to generate wallpaper"
                    )
            }
        }
    }

    fun clearPreview() {

        _previewState.value =
            WallpaperPreviewState()
    }
}