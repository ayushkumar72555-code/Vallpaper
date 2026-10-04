package com.ayush.vallpaper

import android.app.Application
import android.util.Log
import com.ayush.vallpaper.data.AppSettingsRepository
import com.ayush.vallpaper.data.MediaSessionTrackRepository
import com.ayush.vallpaper.domain.model.AppSettings
import com.ayush.vallpaper.domain.model.Track
import com.ayush.vallpaper.ui.screens.WallpaperTarget
import com.ayush.vallpaper.wallpaper.ArtworkLoader
import com.ayush.vallpaper.wallpaper.WallpaperApplier
import com.ayush.vallpaper.wallpaper.WallpaperGenerator
import com.ayush.vallpaper.wallpaper.WallpaperRenderer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

class VallpaperApplication : Application() {

    companion object {
        private const val TAG = "VallpaperAuto"
    }

    val trackRepository by lazy {
        MediaSessionTrackRepository(applicationContext)
    }

    val settingsRepository by lazy {
        AppSettingsRepository(applicationContext)
    }

    private val wallpaperGenerator by lazy {
        WallpaperGenerator(
            artworkLoader = ArtworkLoader(applicationContext),
            renderer = WallpaperRenderer()
        )
    }

    private val applicationScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        trackRepository.start()
        startAutomaticWallpaperUpdates()
    }

    private fun startAutomaticWallpaperUpdates() {
        applicationScope.launch {
            combine(
                trackRepository.currentTrack,
                settingsRepository.settings
            ) { track: Track?, settings: AppSettings ->
                AutomaticWallpaperRequest(track, settings)
            }
                .distinctUntilChanged { old, new ->
                    old.key == new.key
                }
                .collectLatest { request ->
                    if (!request.settings.automaticWallpaper) return@collectLatest

                    if (!request.settings.applyToHomeScreen &&
                        !request.settings.applyToLockScreen
                    ) {
                        Log.d(TAG, "Automatic wallpaper enabled, but no target is enabled")
                        return@collectLatest
                    }

                    val track = request.track ?: return@collectLatest

                    updateWallpaperAutomatically(
                        track = track,
                        settings = request.settings
                    )
                }
        }
    }

    private suspend fun updateWallpaperAutomatically(
        track: Track,
        settings: AppSettings
    ) {
        ensureActive()

        if (track.artworkUrl.isBlank()) {
            Log.d(
                TAG,
                "Skipping automatic wallpaper: no readable artwork for ${track.title}"
            )
            return
        }

        val metrics = resources.displayMetrics
        val width = metrics.widthPixels.coerceAtLeast(1)
        val height = metrics.heightPixels.coerceAtLeast(1)

        var bitmap: android.graphics.Bitmap? = null

        try {
            Log.d(
                TAG,
                "Generating automatic wallpaper for: ${track.title}"
            )

            bitmap = wallpaperGenerator.generate(
                track = track,
                style = settings.selectedStyle,
                width = width,
                height = height
            )

            ensureActive()

            if (settings.applyToHomeScreen) {
                WallpaperApplier.apply(
                    context = applicationContext,
                    bitmap = bitmap,
                    target = WallpaperTarget.HOME
                ).onFailure { exception ->
                    Log.e(
                        TAG,
                        "Automatic home wallpaper update failed",
                        exception
                    )
                }
            }

            ensureActive()

            if (settings.applyToLockScreen) {
                WallpaperApplier.apply(
                    context = applicationContext,
                    bitmap = bitmap,
                    target = WallpaperTarget.LOCK
                ).onFailure { exception ->
                    Log.e(
                        TAG,
                        "Automatic lock wallpaper update failed",
                        exception
                    )
                }
            }

            Log.d(TAG, "Automatic wallpaper update complete")
        } catch (exception: kotlinx.coroutines.CancellationException) {
            Log.d(TAG, "Automatic wallpaper update cancelled for ${track.title}")
            throw exception
        } catch (exception: Exception) {
            Log.e(
                TAG,
                "Automatic wallpaper generation failed",
                exception
            )
        } finally {
            bitmap?.recycle()
        }
    }

    override fun onTerminate() {
        trackRepository.stop()
        applicationScope.cancel()
        super.onTerminate()
    }

    private data class AutomaticWallpaperRequest(
        val track: Track?,
        val settings: AppSettings
    ) {
        val key: String
            get() = listOf(
                track?.id.orEmpty(),
                settings.automaticWallpaper.toString(),
                settings.selectedStyle.name,
                settings.applyToHomeScreen.toString(),
                settings.applyToLockScreen.toString()
            ).joinToString("|")
    }
}
