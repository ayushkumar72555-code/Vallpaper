package com.ayush.vallpaper.data

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import com.ayush.vallpaper.domain.model.Track
import com.ayush.vallpaper.domain.repository.TrackRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MediaSessionTrackRepository(
    private val context: Context
) : TrackRepository {

    private val mediaSessionManager =
        context.getSystemService(MediaSessionManager::class.java)

    private val notificationListenerComponent =
        ComponentName(
            context,
            VallpaperNotificationListenerService::class.java
        )

    private val _currentTrack = MutableStateFlow<Track?>(null)

    override val currentTrack: StateFlow<Track?> =
        _currentTrack.asStateFlow()

    private val _history =
        MutableStateFlow<List<Track>>(emptyList())

    override val history: StateFlow<List<Track>> =
        _history.asStateFlow()

    private val controllerCallbacks =
        mutableMapOf<MediaController, MediaController.Callback>()

    private var isListening = false

    private val activeSessionsListener =
        MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
            updateControllers(controllers.orEmpty())
        }

    init {
        refresh()
    }

    fun refresh() {
        try {
            val controllers =
                mediaSessionManager?.getActiveSessions(
                    notificationListenerComponent
                ).orEmpty()

            updateControllers(controllers)
        } catch (_: SecurityException) {
            _currentTrack.value = null
            _history.value = emptyList()
        }
    }

    fun start() {
        if (isListening) {
            refresh()
            return
        }

        try {
            mediaSessionManager?.addOnActiveSessionsChangedListener(
                activeSessionsListener,
                notificationListenerComponent
            )

            isListening = true
            refresh()
        } catch (_: SecurityException) {
            isListening = false
            _currentTrack.value = null
        }
    }

    fun stop() {
        if (isListening) {
            mediaSessionManager?.removeOnActiveSessionsChangedListener(
                activeSessionsListener
            )
        }

        isListening = false

        controllerCallbacks.keys.toList().forEach { controller ->
            controllerCallbacks[controller]?.let { callback ->
                controller.unregisterCallback(callback)
            }
        }

        controllerCallbacks.clear()
    }

    private fun updateControllers(
        controllers: List<MediaController>
    ) {
        controllerCallbacks.keys
            .filter { it !in controllers }
            .forEach { controller ->
                controllerCallbacks.remove(controller)?.let { callback ->
                    controller.unregisterCallback(callback)
                }
            }

        controllers.forEach { controller ->
            if (!controllerCallbacks.containsKey(controller)) {
                val callback = object : MediaController.Callback() {
                    override fun onMetadataChanged(
                        metadata: MediaMetadata?
                    ) {
                        refresh()
                    }

                    override fun onPlaybackStateChanged(
                        state: android.media.session.PlaybackState?
                    ) {
                        refresh()
                    }
                }

                controllerCallbacks[controller] = callback
                controller.registerCallback(callback)
            }
        }

        updateCurrentTrack(controllers)
    }

    private fun updateCurrentTrack(
        controllers: List<MediaController>
    ) {
        val playingController =
            controllers.firstOrNull { controller ->
                controller.playbackState?.state ==
                    android.media.session.PlaybackState.STATE_PLAYING
            }

        val controller =
            playingController
                ?: controllers.firstOrNull { it.metadata != null }

        val track = controller?.let(::toTrack)

        _currentTrack.value = track

        if (track != null) {
            val currentHistory = _history.value
            _history.value =
                listOf(track) +
                    currentHistory.filter { it.id != track.id }
        }
    }

    private fun toTrack(
        controller: MediaController
    ): Track? {
        val metadata = controller.metadata
            ?: return null

        val title = metadata.getString(
            MediaMetadata.METADATA_KEY_TITLE
        ) ?: return null

        val artist = metadata.getString(
            MediaMetadata.METADATA_KEY_ARTIST
        ).orEmpty()

        val album = metadata.getString(
            MediaMetadata.METADATA_KEY_ALBUM
        ).orEmpty()

        val artworkUri = findArtworkUri(metadata)

        return Track(
            id = "${controller.packageName}:$title:$artist",
            title = title,
            artist = artist.ifBlank { "Unknown artist" },
            album = album.ifBlank { "Unknown album" },
            artworkUrl = artworkUri
        )
    }

    private fun findArtworkUri(
        metadata: MediaMetadata
    ): String {
        val artworkKeys = listOf(
            MediaMetadata.METADATA_KEY_ART_URI,
            MediaMetadata.METADATA_KEY_ALBUM_ART_URI,
            MediaMetadata.METADATA_KEY_DISPLAY_ICON_URI
        )

        for (key in artworkKeys) {
            val uri = metadata.getString(key)
            if (!uri.isNullOrBlank()) {
                return uri
            }
        }

        return metadata.description
            ?.iconUri
            ?.toString()
            .orEmpty()
    }
}
