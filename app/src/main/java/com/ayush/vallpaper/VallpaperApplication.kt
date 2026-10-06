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
import kotlinx.coroutines.delay
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

    private val _automaticWallpaperStatus =
        MutableStateFlow(AutomaticWallpaperStatus.IDLE)

    val automaticWallpaperStatus: StateFlow<AutomaticWallpaperStatus> =
        _automaticWallpaperStatus.asStateFlow()

    @Volatile
    private var lastSuccessfulRequestKey: String? = null

    override fun onCreate() {
        super.onCreate()
        // MediaSessionTrackRepository is activated by
        // VallpaperNotificationListenerService.onListenerConnected().
        // Do not query MediaSessionManager before that lifecycle event.
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
                        _automaticWallpaperStatus.value =
                            AutomaticWallpaperStatus.IDLE
                        return@collectLatest
                    }

                    if (!request.settings.applyToHomeScreen &&
                        !request.settings.applyToLockScreen
                    ) {
                        _automaticWallpaperStatus.value =
                            AutomaticWallpaperStatus.NO_TARGET
                        return@collectLatest
                    }

                    val initialTrack = request.track
                    if (initialTrack == null) {
                        _automaticWallpaperStatus.value =
                            AutomaticWallpaperStatus.WAITING_FOR_MUSIC
                        return@collectLatest
                    }

                    // When the phone is locked, media players often publish
                    // the new metadata before their artwork provider is ready.
                    // Keep checking the repository briefly instead of giving
                    // up after the first metadata callback.
                    val track = awaitTrackWithArtwork(initialTrack)
                    if (track == null) {
                        _automaticWallpaperStatus.value =
                            AutomaticWallpaperStatus.WAITING_FOR_ARTWORK
                        return@collectLatest
                    }

                    val resolvedRequestKey = AutomaticWallpaperRequest(
                        track = track,
                        settings = request.settings
                    ).key

                    if (resolvedRequestKey == lastSuccessfulRequestKey) {
                        _automaticWallpaperStatus.value =
                            AutomaticWallpaperStatus.APPLIED
                        return@collectLatest
                    }

                    updateWallpaperAutomatically(
                        track = track,
                        settings = request.settings,
                        requestKey = resolvedRequestKey
                    )
                }
        }
    }

    private suspend fun awaitTrackWithArtwork(initialTrack: Track): Track? {
        var candidate = initialTrack

        repeat(8) { attempt ->
            if (candidate.artworkUrl.isNotBlank()) {
                return candidate
            }

            // Ask the media-session repository to read the player's current
            // state again. This is especially useful while the screen is off,
            // when metadata and artwork can arrive in separate callbacks.
            trackRepository.refresh()

            delay(300L + (attempt * 150L))

            candidate = trackRepository.currentTrack.value ?: candidate
        }

        return candidate.takeIf { it.artworkUrl.isNotBlank() }
    }

    private suspend fun updateWallpaperAutomatically(
        track: Track,
        settings: AppSettings,
        requestKey: String
    ) {
        currentCoroutineContext().ensureActive()
        var bitmap: Bitmap? = null

        try {
            _automaticWallpaperStatus.value =
                AutomaticWallpaperStatus.GENERATING

            val (width, height) = WallpaperDimensions.get(applicationContext)
            Log.d(
                TAG,
                "Generating ${width}x${height} wallpaper for ${track.title}"
            )

            bitmap = wallpaperGenerator.generate(
                track = track,
                style = settings.selectedStyle,
                width = width,
                height = height
            )

            currentCoroutineContext().ensureActive()
            _automaticWallpaperStatus.value =
                AutomaticWallpaperStatus.APPLYING

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
                    Log.e(
                        TAG,
                        "Automatic home wallpaper update failed",
                        result.exceptionOrNull()
                    )
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
                    Log.e(
                        TAG,
                        "Automatic lock wallpaper update failed",
                        result.exceptionOrNull()
                    )
                }
            }

            currentCoroutineContext().ensureActive()

            if (allTargetsSucceeded) {
                lastSuccessfulRequestKey = requestKey
                _automaticWallpaperStatus.value =
                    AutomaticWallpaperStatus.APPLIED
            } else {
                _automaticWallpaperStatus.value =
                    AutomaticWallpaperStatus.ERROR
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            _automaticWallpaperStatus.value =
                AutomaticWallpaperStatus.ERROR

            Log.e(
                TAG,
                "Automatic wallpaper update failed",
                exception
            )
        } finally {
            if (bitmap != null && !bitmap.isRecycled) {
                bitmap.recycle()
            }
        }
    }

    override fun onTerminate() {
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
