package com.ayush.vallpaper.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ayush.vallpaper.VallpaperApplication
import com.ayush.vallpaper.wallpaper.ArtworkLoader
import com.ayush.vallpaper.wallpaper.WallpaperGenerator
import com.ayush.vallpaper.wallpaper.WallpaperRenderer

class AppViewModelFactory(
    private val application: VallpaperApplication
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {

        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {

            val wallpaperGenerator =
                WallpaperGenerator(
                    artworkLoader =
                        ArtworkLoader(
                            context = application.applicationContext
                        ),
                    renderer =
                        WallpaperRenderer()
                )

            return HomeViewModel(
                trackRepository =
                    application.trackRepository,
                settingsRepository =
                    application.settingsRepository,
                wallpaperGenerator =
                    wallpaperGenerator
            ) as T
        }

        throw IllegalArgumentException(
            "Unknown ViewModel class: ${modelClass.name}"
        )
    }
}
