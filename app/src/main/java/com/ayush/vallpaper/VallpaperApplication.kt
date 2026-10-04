package com.ayush.vallpaper

import android.app.Application
import com.ayush.vallpaper.data.AppSettingsRepository
import com.ayush.vallpaper.data.MockTrackRepository

class VallpaperApplication : Application() {

    val trackRepository by lazy {
        MockTrackRepository()
    }

    val settingsRepository by lazy {
        AppSettingsRepository(applicationContext)
    }
}
