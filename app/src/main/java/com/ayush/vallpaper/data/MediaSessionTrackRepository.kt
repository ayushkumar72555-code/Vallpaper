package com.ayush.vallpaper.data

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Build
import android.service.notification.StatusBarNotification
import android.net.Uri
import android.util.Log
import com.ayush.vallpaper.domain.model.Track
import com.ayush.vallpaper.domain.repository.TrackRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileOutputStream

data class PlaybackSnapshot(
    val state: Int,
    val positionMs: Long,
    val playbackSpeed: Float,
    val lastPositionUpdateTime: Long
)

class MediaSessionTrackRepository(private val context: Context) : TrackRepository {
    companion object { private const val TAG = "VallpaperMedia" }

    private val mediaSessionManager = context.getSystemService(MediaSessionManager::class.java)
    private val notificationListenerComponent =
        ComponentName(context, VallpaperNotificationListenerService::class.java)

    private val _currentTrack = MutableStateFlow<Track?>(null)
    override val currentTrack: StateFlow<Track?> = _currentTrack.asStateFlow()

    private val controllerCallbacks =
        mutableMapOf<MediaController, MediaController.Callback>()

    // Recent controller activity lets us choose the session the user actually
    // interacted with when Android exposes several media sessions at once.
    private val controllerLastActivity =
        mutableMapOf<MediaController, Long>()

    // Some media apps expose their MediaSession through the media notification
    // even when the session is not returned by getActiveSessions(). Keep those
    // controllers as a fallback, keyed by notification instance. A package-only
    // key is unsafe because an old notification can be removed after a new one
    // has already replaced it.
    private val notificationControllers =
        mutableMapOf<String, List<MediaController>>()

    @Volatile
    private var listenerConnected = false

    @Volatile
    private var currentController: MediaController? = null

    private val playbackClock = PlaybackClock()

    private val activeSessionsListener =
        MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
            if (listenerConnected) {
                updateControllers(controllers.orEmpty() + notificationControllers.values.flatten())
            }
        }

    /**
     * Must only be called from NotificationListenerService.onListenerConnected().
     * Android requires the notification listener to be connected before
     * accessing notification/media-session state.
     */
    fun onListenerConnected() {
        if (listenerConnected) {
            refresh()
            return
        }

        listenerConnected = true
        try {
            mediaSessionManager?.addOnActiveSessionsChangedListener(
                activeSessionsListener,
                notificationListenerComponent
            )
            Log.d(TAG, "Notification listener connected; media session listener registered")
            refresh()
        } catch (exception: SecurityException) {
            Log.e(TAG, "Unable to access active media sessions", exception)
            listenerConnected = false
            clearControllers()
            _currentTrack.value = null
        }
    }

    fun onListenerDisconnected() {
        listenerConnected = false

        try {
            mediaSessionManager?.removeOnActiveSessionsChangedListener(activeSessionsListener)
        } catch (exception: Exception) {
            Log.d(TAG, "Could not remove media session listener", exception)
        }

        clearControllers()
        notificationControllers.clear()
        _currentTrack.value = null
        Log.d(TAG, "Notification listener disconnected")
    }

    fun refresh() {
        if (!listenerConnected) {
            Log.d(TAG, "Refresh skipped: notification listener is not connected")
            return
        }

        try {
            val controllers = mediaSessionManager
                ?.getActiveSessions(notificationListenerComponent)
                .orEmpty()

            Log.d(TAG, "Active media sessions: ${controllers.size}")
            controllers.forEach { controller ->
                Log.d(
                    TAG,
                    "Session ${controller.packageName}: state=${controller.playbackState?.state}, " +
                        "title=${controller.metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)}"
                )
            }

            updateControllers(controllers + notificationControllers.values.flatten())
        } catch (exception: SecurityException) {
            Log.e(TAG, "SecurityException while reading active media sessions", exception)
            _currentTrack.value = null
        } catch (exception: Exception) {
            Log.e(TAG, "Unexpected error while reading active media sessions", exception)
        }
    }

    /**
     * MediaStyle notifications can carry the MediaSession.Token directly.
     * This is an important fallback for players whose session is not present
     * in MediaSessionManager.getActiveSessions(), while their notification is.
     */
    fun onMediaNotificationPosted(sbn: StatusBarNotification) {
        if (!listenerConnected) return

        // Android 37+ can query sessions for the exact package. This is
        // more reliable than the global active-session list for players such
        // as Joytify that may publish a package session without appearing in
        // the global ordering.
        if (Build.VERSION.SDK_INT >= 37) {
            try {
                val packageTokens = mediaSessionManager
                    ?.getActiveSessionsForPackage(
                        sbn.packageName,
                        notificationListenerComponent
                    )
                    .orEmpty()

                if (packageTokens.isNotEmpty()) {
                    Log.d(
                        TAG,
                        "Package media sessions found: package=" +
                            sbn.packageName + ", count=" + packageTokens.size
                    )

                    val controllers = packageTokens.map { token ->
                        MediaController(context, token)
                    }
                    notificationControllers[sbn.key] = controllers
                    controllers.forEach {
                        registerControllerCallback(it)
                        markControllerActive(it)
                    }

                    refresh()
                    return
                }
            } catch (exception: SecurityException) {
                Log.d(
                    TAG,
                    "Package session access denied for " + sbn.packageName +
                        "; falling back to notification token",
                    exception
                )
            } catch (exception: Exception) {
                Log.d(
                    TAG,
                    "Package session lookup failed for " + sbn.packageName,
                    exception
                )
            }
        }

        val token = extractMediaSessionToken(sbn) ?: return

        try {
            val controller = MediaController(context, token)
            notificationControllers[sbn.key] = listOf(controller)

            Log.d(
                TAG,
                "Media notification session found: package=" + sbn.packageName + ", " +
                    "title=${controller.metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)}, " +
                    "state=${controller.playbackState?.state}"
            )

            registerControllerCallback(controller)
            markControllerActive(controller)
            refresh()
        } catch (exception: Exception) {
            Log.e(
                TAG,
                "Could not create MediaController from notification: " + sbn.packageName,
                exception
            )
        }
    }

    fun onMediaNotificationRemoved(sbn: StatusBarNotification) {
        if (notificationControllers.remove(sbn.key) != null) {
            Log.d(TAG, "Media notification removed: package=${sbn.packageName}, key=${sbn.key}")
            refresh()
        }
    }

    private fun extractMediaSessionToken(sbn: StatusBarNotification): MediaSession.Token? {
        val extras = sbn.notification.extras

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                extras.getParcelable(
                    android.app.Notification.EXTRA_MEDIA_SESSION,
                    MediaSession.Token::class.java
                )
            } else {
                @Suppress("DEPRECATION")
                extras.getParcelable(
                    android.app.Notification.EXTRA_MEDIA_SESSION
                ) as? MediaSession.Token
            }
        } catch (exception: Exception) {
            Log.d(
                TAG,
                "Could not extract MediaSession.Token from ${sbn.packageName}",
                exception
            )
            null
        }
    }

    private fun clearControllers() {
        controllerCallbacks.keys.toList().forEach { controller ->
            controllerCallbacks.remove(controller)?.let { callback ->
                try {
                    controller.unregisterCallback(callback)
                } catch (exception: Exception) {
                    Log.d(TAG, "Could not unregister media controller callback", exception)
                }
            }
        }
        controllerCallbacks.clear()
        controllerLastActivity.clear()
        currentController = null
        playbackClock.reset()
    }

    private fun updateControllers(controllers: List<MediaController>) {
        controllerCallbacks.keys
            .filter { it !in controllers }
            .forEach { controller ->
                controllerCallbacks.remove(controller)?.let { callback ->
                    try {
                        controller.unregisterCallback(callback)
                    } catch (exception: Exception) {
                        Log.d(TAG, "Could not unregister removed controller", exception)
                    }
                }
                controllerLastActivity.remove(controller)
            }

        controllers.forEach { controller ->
            registerControllerCallback(controller)
        }

        updateCurrentTrack(controllers)
    }

    private fun registerControllerCallback(controller: MediaController) {
        if (controllerCallbacks.containsKey(controller)) return

        val callback = object : MediaController.Callback() {
            override fun onMetadataChanged(metadata: MediaMetadata?) {
                markControllerActive(controller)
                Log.d(TAG, "Metadata changed: " + controller.packageName)
                refresh()
            }

            override fun onPlaybackStateChanged(state: PlaybackState?) {
                markControllerActive(controller)
                Log.d(
                    TAG,
                    "Playback state changed: " + controller.packageName + " -> " + state?.state
                )
                refresh()
            }
        }

        try {
            controller.registerCallback(callback)
            controllerCallbacks[controller] = callback
        } catch (exception: Exception) {
            Log.e(
                TAG,
                "Could not register callback for ${controller.packageName}",
                exception
            )
        }
    }

    private fun updateCurrentTrack(controllers: List<MediaController>) {
        val controller = selectBestController(controllers)

        val previousController = currentController
        currentController = controller

        if (controller != null) {
            playbackClock.update(controller.playbackState)
        } else if (previousController != null) {
            playbackClock.reset()
        }

        val track = controller?.let(::toTrack)
        _currentTrack.value = track

        if (track != null) {
            Log.d(
                TAG,
                "Current track: " + track.title + " - " + track.artist +
                    " (" + controller?.packageName + "), state=" + controller?.playbackState?.state
            )
        } else {
            Log.d(TAG, "No usable media metadata found")
        }
    }

    /** Select the session that most likely represents the user's actual listening session. */
    private fun selectBestController(
        controllers: List<MediaController>
    ): MediaController? {
        val candidates = controllers.filter {
            it.metadata != null || it.playbackState != null
        }
        if (candidates.isEmpty()) return null

        val now = android.os.SystemClock.elapsedRealtime()
        return candidates.maxWithOrNull(
            compareBy<MediaController> { playbackPriority(it.playbackState?.state) }
                .thenBy { if (it.metadata != null) 1 else 0 }
                .thenBy { recencyScore(it, now) }
        )
    }

    private fun playbackPriority(state: Int?): Int = when (state) {
        PlaybackState.STATE_PLAYING -> 5
        PlaybackState.STATE_BUFFERING,
        PlaybackState.STATE_FAST_FORWARDING,
        PlaybackState.STATE_REWINDING -> 4
        PlaybackState.STATE_PAUSED -> 3
        PlaybackState.STATE_CONNECTING -> 2
        PlaybackState.STATE_NONE,
        PlaybackState.STATE_ERROR -> 0
        else -> 1
    }

    private fun recencyScore(
        controller: MediaController,
        nowElapsedRealtime: Long
    ): Long {
        val lastActivity = controllerLastActivity[controller] ?: return 0L
        return (nowElapsedRealtime - lastActivity)
            .coerceIn(0L, 60_000L)
            .let { 60_000L - it }
    }

    private fun markControllerActive(controller: MediaController) {
        controllerLastActivity[controller] =
            android.os.SystemClock.elapsedRealtime()
    }

    fun currentPlaybackState(): Int? {
        return playbackClock.state().state
    }

    fun currentPlaybackPositionMs(): Long? {
        if (currentController == null) return null
        return playbackClock.positionMs()
    }

    private fun toTrack(controller: MediaController): Track? {
        val metadata = controller.metadata ?: return null

        val title =
            metadata.getString(MediaMetadata.METADATA_KEY_TITLE)
                ?.takeIf { it.isNotBlank() }
                ?: metadata.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
                    ?.takeIf { it.isNotBlank() }
                ?: return null

        val artist =
            metadata.getString(MediaMetadata.METADATA_KEY_ARTIST)
                ?.takeIf { it.isNotBlank() }
                ?: metadata.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE)
                    ?.takeIf { it.isNotBlank() }
                ?: "Unknown artist"

        val album =
            metadata.getString(MediaMetadata.METADATA_KEY_ALBUM)
                ?.takeIf { it.isNotBlank() }
                ?: "Unknown album"

        return Track(
            id = buildTrackId(controller, metadata, title, artist, album),
            title = title,
            artist = artist,
            album = album,
            artworkUrl = resolveArtwork(metadata),
            durationMs = metadata
                .getLong(MediaMetadata.METADATA_KEY_DURATION)
                .coerceAtLeast(0L)
        )
    }

    private fun buildTrackId(
        controller: MediaController,
        metadata: MediaMetadata,
        title: String,
        artist: String,
        album: String
    ): String {
        val mediaId = metadata.getString(MediaMetadata.METADATA_KEY_MEDIA_ID)
            ?.takeIf { it.isNotBlank() }

        return if (mediaId != null) {
            "${controller.packageName}:${mediaId}"
        } else {
            "${controller.packageName}:${title}:${artist}:${album}"
        }
    }

    private fun resolveArtwork(metadata: MediaMetadata): String {
        // Prefer an actual bitmap supplied by the media session over a URI.
        // Some players, including Spotify in some contexts, expose a
        // notification/canvas image through the URI while the bitmap contains
        // the actual artwork.
        findArtworkBitmap(metadata)?.let { bitmap ->
            val cached = saveArtwork(bitmap, metadata)
            if (cached.isNotBlank()) return cached
        }

        val artworkKeys = listOf(
            MediaMetadata.METADATA_KEY_ALBUM_ART_URI,
            MediaMetadata.METADATA_KEY_ART_URI,
            MediaMetadata.METADATA_KEY_DISPLAY_ICON_URI
        )

        for (key in artworkKeys) {
            val uriString = metadata.getString(key)
            if (!uriString.isNullOrBlank()) {
                val uri = Uri.parse(uriString)
                val normalizedUri = copyUriToCacheIfNeeded(uri)
                if (normalizedUri.isNotBlank()) return normalizedUri
                Log.d(TAG, "Could not read artwork URI: $uri")
            }
        }

        metadata.description?.iconUri?.let { uri ->
            val normalizedUri = copyUriToCacheIfNeeded(uri)
            if (normalizedUri.isNotBlank()) return normalizedUri
        }

        metadata.description?.iconBitmap?.let { bitmap ->
            val cached = saveArtwork(bitmap, metadata)
            if (cached.isNotBlank()) return cached
        }

        Log.d(TAG, "No readable artwork found in MediaMetadata")
        return ""
    }

    private fun findArtworkBitmap(metadata: MediaMetadata): Bitmap? {
        val bitmapKeys = listOf(
            MediaMetadata.METADATA_KEY_ALBUM_ART,
            MediaMetadata.METADATA_KEY_ART,
            MediaMetadata.METADATA_KEY_DISPLAY_ICON
        )

        for (key in bitmapKeys) {
            metadata.getBitmap(key)?.let { return it }
        }
        return null
    }

    private fun copyUriToCacheIfNeeded(uri: Uri): String {
        return try {
            when (uri.scheme?.lowercase()) {
                "http", "https" -> uri.toString()
                "file" -> {
                    val file = File(uri.path ?: return "")
                    if (file.exists() && file.length() > 0) uri.toString() else ""
                }
                else -> copyProviderUriToCache(uri)
            }
        } catch (exception: Exception) {
            Log.d(TAG, "Artwork URI read failed: ${uri}", exception)
            ""
        }
    }

    private fun copyProviderUriToCache(uri: Uri): String {
        val artworkDirectory = File(context.cacheDir, "media_artwork")
        if (!artworkDirectory.exists() && !artworkDirectory.mkdirs()) return ""

        val safeName = "uri_" + Integer.toHexString(uri.toString().hashCode())
        val file = File(artworkDirectory, "${safeName}.art")

        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(file).use { output -> input.copyTo(output) }
            } ?: return ""

            if (file.length() <= 0) {
                file.delete()
                return ""
            }

            Uri.fromFile(file).toString()
        } catch (exception: Exception) {
            Log.d(TAG, "Content provider artwork could not be copied: ${uri}", exception)
            file.delete()
            ""
        }
    }

    private fun saveArtwork(bitmap: Bitmap, metadata: MediaMetadata): String {
        return try {
            val artworkDirectory = File(context.cacheDir, "media_artwork")
            if (!artworkDirectory.exists() && !artworkDirectory.mkdirs()) return ""

            val title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE).orEmpty()
            val safeName = title
                .ifBlank { "unknown" }
                .replace(Regex("[^A-Za-z0-9._-]"), "_")
                .take(80)

            val file = File(artworkDirectory, "${safeName}.jpg")

            FileOutputStream(file).use { output ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, output)
            }

            if (file.length() <= 0) {
                file.delete()
                return ""
            }

            Uri.fromFile(file).toString()
        } catch (exception: Exception) {
            Log.d(TAG, "Bitmap artwork could not be saved", exception)
            ""
        }
    }
}
