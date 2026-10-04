package com.ayush.vallpaper

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import com.ayush.vallpaper.data.AppSettingsRepository
import com.ayush.vallpaper.data.MediaSessionTrackRepository
import com.ayush.vallpaper.domain.model.AppSettings
import com.ayush.vallpaper.domain.model.Track
import com.ayush.vallpaper.ui.screens.WallpaperTarget
import com.ayush.vallpaper.wallpaper.ArtworkLoader
import com.ayush.vallpaper.wallpaper.WallpaperApplier
import com.ayush.vallpaper.wallpaper.WallpaperDimensions
import com.ayush.vallpaper.wallpaper.WallpaperGenerator
import com.ayush.vallpaper.wallpaper.WallpaperRenderer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

class VallpaperApplication : Application() {

    companion object {
        private const val TAG = "VallpaperAuto"
    }

    val trackRepository by lazy { MediaSessionTrackRepository(applicationContext) }
    val settingsRepository by lazy { AppSettingsRepository(applicationContext) }

    private val wallpaperGenerator by lazy {
        WallpaperGenerator(
            artworkLoader = ArtworkLoader(applicationContext),
            renderer = WallpaperRenderer()
        )
    }

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _automaticWallpaperStatus = MutableStateFlow(AutomaticWallpaperStatus.IDLE)
    val automaticWallpaperStatus: StateFlow<AutomaticWallpaperStatus> = _automaticWallpaperStatus.asStateFlow()

    @Volatile
    private var lastSuccessfulRequestKey: String? = null

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
                .distinctUntilChanged { old, new -> old.key == new.key }
                .collectLatest { request ->
                    currentCoroutineContext().ensureActive()

                    if (!request.settings.automaticWallpaper) {
                        _automaticWallpaperStatus.value = AutomaticWallpaperStatus.IDLE
                        return@collectLatest
                    }

                    if (!request.settings.applyToHomeScreen && !request.settings.applyToLockScreen) {
                        _automaticWallpaperStatus.value = AutomaticWallpaperStatus.NO_TARGET
                        return@collectLatest
                    }

                    val track = request.track
                    if (track == null) {
                        _automaticWallpaperStatus.value = AutomaticWallpaperStatus.WAITING_FOR_MUSIC
                        return@collectLatest
                    }

                    if (track.artworkUrl.isBlank()) {
                        _automaticWallpaperStatus.value = AutomaticWallpaperStatus.WAITING_FOR_ARTWORK
                        return@collectLatest
                    }

                    if (request.key == lastSuccessfulRequestKey) {
                        _automaticWallpaperStatus.value = AutomaticWallpaperStatus.APPLIED
                        return@collectLatest
                    }

                    updateWallpaperAutomatically(track, request.settings, request.key)
                }
        }
    }

    private suspend fun updateWallpaperAutomatically(
        track: Track,
        settings: AppSettings,
        requestKey: String
    ) {
        currentCoroutineContext().ensureActive()
        var bitmap: Bitmap? = null

        try {
            _automaticWallpaperStatus.value = AutomaticWallpaperStatus.GENERATING

            val (width, height) = WallpaperDimensions.get(applicationContext)
            Log.d(TAG, "Generating ${width}x${height} wallpaper for ${track.title}")

            bitmap = wallpaperGenerator.generate(
                track = track,
                style = settings.selectedStyle,
                width = width,
                height = height
            )

            currentCoroutineContext().ensureActive()
            _automaticWallpaperStatus.value = AutomaticWallpaperStatus.APPLYING

            var allTargetsSucceeded = true

            if (settings.applyToHomeScreen) {
                currentCoroutineContext().ensureActive()
                val result = WallpaperApplier.apply(
                    applicationContext,
                    bitmap,
                    WallpaperTarget.HOME
                )
                if (result.isFailure) {
                    allTargetsSucceeded = false
                    Log.e(TAG, "Automatic home wallpaper update failed", result.exceptionOrNull())
                }
            }

            if (settings.applyToLockScreen) {
                currentCoroutineContext().ensureActive()
                val result = WallpaperApplier.apply(
                    applicationContext,
                    bitmap,
                    WallpaperTarget.LOCK
                )
                if (result.isFailure) {
                    allTargetsSucceeded = false
                    Log.e(TAG, "Automatic lock wallpaper update failed", result.exceptionOrNull())
                }
            }

            currentCoroutineContext().ensureActive()

            if (allTargetsSucceeded) {
                lastSuccessfulRequestKey = requestKey
                _automaticWallpaperStatus.value = AutomaticWallpaperStatus.APPLIED
            } else {
                _automaticWallpaperStatus.value = AutomaticWallpaperStatus.ERROR
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            _automaticWallpaperStatus.value = AutomaticWallpaperStatus.ERROR
            Log.e(TAG, "Automatic wallpaper update failed", exception)
        } finally {
            if (bitmap != null && !bitmap.isRecycled) bitmap.recycle()
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
                track?.title.orEmpty(),
                track?.artist.orEmpty(),
                track?.artworkUrl.orEmpty(),
                settings.selectedStyle.name,
                settings.applyToHomeScreen.toString(),
                settings.applyToLockScreen.toString(),
                settings.automaticWallpaper.toString()
            ).joinToString("|")
    }
}

enum class AutomaticWallpaperStatus {
    IDLE,
    WAITING_FOR_MUSIC,
    WAITING_FOR_ARTWORK,
    GENERATING,
    APPLYING,
    APPLIED,
    NO_TARGET,
    ERROR
}
