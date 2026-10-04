package com.ayush.vallpaper

import android.app.Application
import com.ayush.vallpaper.data.AppSettingsRepository
import com.ayush.vallpaper.data.MediaSessionTrackRepository

class VallpaperApplication : Application() {

    val trackRepository by lazy {
        MediaSessionTrackRepository(applicationContext)
    }

    val settingsRepository by lazy {
        AppSettingsRepository(applicationContext)
    }

    override fun onCreate() {
        super.onCreate()
        trackRepository.start()
    }

    override fun onTerminate() {
        trackRepository.stop()
        super.onTerminate()
    }
}
