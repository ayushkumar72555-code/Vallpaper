package com.ayush.vallpaper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.ayush.vallpaper.ui.VallpaperApp
import com.ayush.vallpaper.ui.theme.VallpaperTheme

class MainActivity : ComponentActivity() {

    private val vallpaperApplication: VallpaperApplication
        get() = application as VallpaperApplication

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        hideSystemBars()

        setContent {
            VallpaperTheme {
                VallpaperApp()
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)

        if (hasFocus) {
            hideSystemBars()
        }
    }

    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(
            WindowInsetsCompat.Type.statusBars() or
                    WindowInsetsCompat.Type.navigationBars()
        )
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    override fun onResume() {
        super.onResume()

        // Refresh only. The NotificationListenerService owns the media-session
        // lifecycle and calls onListenerConnected() before access is attempted.
        vallpaperApplication.trackRepository.refresh()

        hideSystemBars()
    }
}
