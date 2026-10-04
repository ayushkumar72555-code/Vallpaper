package com.ayush.vallpaper.data

object AppContainer {

    val trackRepository: MockTrackRepository by lazy {
        MockTrackRepository()
    }

    val settingsRepository: AppSettingsRepository by lazy {
        AppSettingsRepository()
    }
}
