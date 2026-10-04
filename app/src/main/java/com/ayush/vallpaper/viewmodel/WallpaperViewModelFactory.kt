package com.ayush.vallpaper.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ayush.vallpaper.wallpaper.WallpaperGenerator

class WallpaperViewModelFactory(
    private val generator: WallpaperGenerator
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {

        if (
            modelClass.isAssignableFrom(
                WallpaperViewModel::class.java
            )
        ) {
            return WallpaperViewModel(
                generator = generator
            ) as T
        }

        throw IllegalArgumentException(
            "Unknown ViewModel class: ${modelClass.name}"
        )
    }
}