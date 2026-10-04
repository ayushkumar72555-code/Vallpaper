package com.ayush.vallpaper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.ayush.vallpaper.ui.VallpaperApp
import com.ayush.vallpaper.ui.theme.VallpaperTheme

class MainActivity : ComponentActivity() {

    private val vallpaperApplication: VallpaperApplication
        get() = application as VallpaperApplication

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            VallpaperTheme {
                VallpaperApp()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        vallpaperApplication.trackRepository.start()
    }
}
