package com.ayush.vallpaper.data

import android.media.session.PlaybackState
import android.os.SystemClock
import kotlin.math.max

data class PlaybackClockState(
    val state: Int = PlaybackState.STATE_NONE,
    val positionMs: Long = 0L,
    val speed: Float = 1f,
    val sampledAtElapsedRealtime: Long = 0L
)

class PlaybackClock {

    @Volatile
    private var snapshot = PlaybackClockState()

    @Synchronized
    fun update(state: PlaybackState?) {
        if (state == null) return

        val reportedPosition = state.position
        val position = if (
            reportedPosition == PlaybackState.PLAYBACK_POSITION_UNKNOWN
        ) {
            snapshot.positionMs
        } else {
            reportedPosition.coerceAtLeast(0L)
        }

        snapshot = PlaybackClockState(
            state = state.state,
            positionMs = position,
            speed = state.playbackSpeed.takeIf { it.isFinite() } ?: 1f,
            sampledAtElapsedRealtime = SystemClock.elapsedRealtime()
        )
    }

    @Synchronized
    fun reset() {
        snapshot = PlaybackClockState()
    }

    fun state(): PlaybackClockState = snapshot

    fun positionMs(nowElapsedRealtime: Long = SystemClock.elapsedRealtime()): Long {
        val current = snapshot
        if (current.sampledAtElapsedRealtime <= 0L) {
            return current.positionMs.coerceAtLeast(0L)
        }

        if (!isPlaying(current.state)) {
            return current.positionMs.coerceAtLeast(0L)
        }

        val elapsed = max(
            0L,
            nowElapsedRealtime - current.sampledAtElapsedRealtime
        )

        return (
            current.positionMs +
                (elapsed * current.speed).toLong()
            ).coerceAtLeast(0L)
    }

    private fun isPlaying(state: Int): Boolean =
        state == PlaybackState.STATE_PLAYING ||
            state == PlaybackState.STATE_FAST_FORWARDING ||
            state == PlaybackState.STATE_REWINDING
}
