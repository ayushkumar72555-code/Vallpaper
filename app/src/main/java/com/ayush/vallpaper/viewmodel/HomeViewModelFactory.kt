package com.ayush.vallpaper.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ayush.vallpaper.data.AppSettingsRepository
import com.ayush.vallpaper.data.MockTrackRepository

class HomeViewModelFactory(
    private val trackRepository: MockTrackRepository,
    private val settingsRepository: AppSettingsRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            return HomeViewModel(
                trackRepository = trackRepository,
                settingsRepository = settingsRepository
            ) as T
        }

        throw IllegalArgumentException(
            "Unknown ViewModel class: ${modelClass.name}"
        )
    }
}
