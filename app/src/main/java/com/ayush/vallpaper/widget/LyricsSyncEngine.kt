package com.ayush.vallpaper.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.media.session.PlaybackState
import android.util.Log
import com.ayush.vallpaper.data.LyricsRepository
import com.ayush.vallpaper.data.MediaSessionTrackRepository
import com.ayush.vallpaper.domain.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

class LyricsSyncEngine(
    private val context: Context,
    private val trackRepository: MediaSessionTrackRepository,
    private val lyricsRepository: LyricsRepository
) {

    companion object {
        private const val TAG = "VallpaperLyricsSync"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null

    fun start() {
        if (job?.isActive == true) return

        job = scope.launch {
            trackRepository.currentTrack
                .distinctUntilChanged { old, new -> old?.id == new?.id }
                .collectLatest { track ->
                    if (!hasWidgets()) return@collectLatest

                    if (track == null) {
                        LyricsWidgetStore.clear(context)
                        LyricsWidgetReceiver.updateFull(
                            context,
                            LyricsWidgetStore.read(context)
                        )
                        return@collectLatest
                    }

                    syncTrack(track)
                }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    private suspend fun syncTrack(track: Track) {
        Log.d(TAG, "Loading lyrics: ${track.title} - ${track.artist}")

        val lyrics = lyricsRepository.findLyrics(track)
        if (lyrics == null) {
            Log.d(TAG, "No synced lyrics: ${track.title}")
            LyricsWidgetStore.clear(context)
            LyricsWidgetReceiver.updateFull(
                context,
                LyricsWidgetStore.read(context)
            )
            return
        }

        LyricsWidgetStore.saveLyrics(
            context = context,
            trackId = track.id,
            title = track.title,
            artist = track.artist,
            artworkUrl = track.artworkUrl,
            durationMs = track.durationMs,
            lyrics = lyrics
        )

        LyricsWidgetReceiver.updateFull(
            context,
            LyricsWidgetStore.read(context)
        )

        var lastIndex = Int.MIN_VALUE
        var lastStatus = ""
        var lastLogAt = 0L

        while (currentCoroutineContext().isActive && hasWidgets()) {
            val currentTrack = trackRepository.currentTrack.value
            if (currentTrack?.id != track.id) return

            val position = trackRepository.currentPlaybackPositionMs()
            val playbackState = trackRepository.currentPlaybackState()
            val status = statusLabel(playbackState)
            val now = android.os.SystemClock.elapsedRealtime()

            if (status != lastStatus) {
                LyricsWidgetStore.updatePlaybackStatus(context, status)
                LyricsWidgetReceiver.updateLyrics(
                    context,
                    LyricsWidgetStore.read(context)
                )
                lastStatus = status
            }

            if (position != null) {
                LyricsWidgetStore.updatePosition(context, position)
                val index = lyrics.lineIndexAt(position)

                if (index != lastIndex) {
                    LyricsWidgetStore.updateCurrentIndex(context, index)
                    val state = LyricsWidgetStore.read(context)

                    LyricsWidgetReceiver.updateLyrics(context, state)

                    Log.d(
                        TAG,
                        "LYRIC index=$index position=${position}ms " +
                            "status=$status text=${state.currentLine}"
                    )

                    lastIndex = index
                }
            }

            LyricsWidgetReceiver.updatePlayback(
                context,
                positionMs = position ?: 0L,
                durationMs = track.durationMs,
                playbackState = playbackState ?: PlaybackState.STATE_NONE
            )

            if (now - lastLogAt >= 1_000L) {
                Log.d(
                    TAG,
                    "CLOCK track=${track.title} " +
                        "position=${position}ms " +
                        "state=${playbackState}"
                )
                lastLogAt = now
            }

            val nextTimestamp = position?.let(lyrics::nextTimestampAfter)
            val delayMs = if (
                position != null &&
                isPlayingState(playbackState ?: PlaybackState.STATE_NONE) &&
                nextTimestamp != null
            ) {
                val remaining = nextTimestamp - position
                min(500L, max(40L, remaining - 20L))
            } else {
                250L
            }

            delay(delayMs)
        }
    }

    private fun hasWidgets(): Boolean {
        val manager = AppWidgetManager.getInstance(context)
        val provider = ComponentName(context, LyricsWidgetReceiver::class.java)
        return manager.getAppWidgetIds(provider).isNotEmpty()
    }

    private fun statusLabel(state: Int?): String =
        when (state) {
            PlaybackState.STATE_PLAYING,
            PlaybackState.STATE_FAST_FORWARDING,
            PlaybackState.STATE_REWINDING -> "PLAYING"

            PlaybackState.STATE_PAUSED -> "PAUSED"
            PlaybackState.STATE_BUFFERING -> "BUFFERING"
            PlaybackState.STATE_STOPPED,
            PlaybackState.STATE_NONE -> "STOPPED"

            else -> "WAITING"
        }

    private fun isPlayingState(state: Int): Boolean =
        state == PlaybackState.STATE_PLAYING ||
            state == PlaybackState.STATE_FAST_FORWARDING ||
            state == PlaybackState.STATE_REWINDING
}
