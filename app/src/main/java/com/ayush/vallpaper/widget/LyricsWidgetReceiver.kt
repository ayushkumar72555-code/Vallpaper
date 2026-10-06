package com.ayush.vallpaper.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.view.View
import android.widget.RemoteViews
import com.ayush.vallpaper.MainActivity
import com.ayush.vallpaper.R
import com.ayush.vallpaper.VallpaperApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger

class LyricsWidgetReceiver : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_PLAY_PAUSE -> {
                (context.applicationContext as? VallpaperApplication)
                    ?.trackRepository?.togglePlayPause()
                updatePlaybackFromRepository(context)
                return
            }
            ACTION_PREVIOUS -> {
                (context.applicationContext as? VallpaperApplication)
                    ?.trackRepository?.skipToPrevious()
                return
            }
            ACTION_NEXT -> {
                (context.applicationContext as? VallpaperApplication)
                    ?.trackRepository?.skipToNext()
                return
            }
        }
        super.onReceive(context, intent)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val settings = LyricsWidgetSettingsRepository(context).read()
        val initial = createViews(context, settings).apply {
            applyCommonAppearance(this, settings)
            setTextViewText(R.id.lyrics_widget_title, "Nothing playing")
            setTextViewText(R.id.lyrics_widget_artist, "Play music to begin")
            setProgressBar(R.id.lyrics_widget_progress, 100, 0, true)
            setImageViewResource(R.id.lyrics_widget_play_pause, android.R.drawable.ic_media_play)
            setOnClickPendingIntent(R.id.lyrics_widget_previous, actionPendingIntent(context, ACTION_PREVIOUS, 2001))
            setOnClickPendingIntent(R.id.lyrics_widget_play_pause, actionPendingIntent(context, ACTION_PLAY_PAUSE, 2002))
            setOnClickPendingIntent(R.id.lyrics_widget_next, actionPendingIntent(context, ACTION_NEXT, 2003))
            if (settings.transition != LyricsWidgetSettings.TRANSITION_SLIDE) {
                setTextViewText(R.id.lyrics_widget_next_sans, "")
                setTextViewText(R.id.lyrics_widget_next_serif, "")
                setTextViewText(R.id.lyrics_widget_next_mono, "")
            }
            setOnClickPendingIntent(
                R.id.lyrics_widget_root,
                openAppPendingIntent(context)
            )
        }

        if (isAnimated(settings)) {
            setAnimatedBufferText(
                initial,
                0,
                "PLAY MUSIC TO SHOW LYRICS"
            )
            setAnimatedBufferText(
                initial,
                1,
                "PLAY MUSIC TO SHOW LYRICS"
            )
            if (settings.transition == LyricsWidgetSettings.TRANSITION_SLIDE) {
                setSlideNextText(initial, "")
                applySlideWidgetSettings(initial, settings)
            } else {
                applyAnimatedWidgetSettings(initial, settings)
            }
            initial.setDisplayedChild(R.id.lyrics_widget_current_flipper, 0)
            activeBuffer.set(0)
        } else {
            setStaticCurrentText(initial, "PLAY MUSIC TO SHOW LYRICS")
            applyWidgetSettings(initial, settings)
        }

        appWidgetManager.updateAppWidget(appWidgetIds, initial)

        (context.applicationContext as? VallpaperApplication)
            ?.startLyricsWidgetSync()
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        (context.applicationContext as? VallpaperApplication)
            ?.startLyricsWidgetSync()
    }

    override fun onDisabled(context: Context) {
        (context.applicationContext as? VallpaperApplication)
            ?.stopLyricsWidgetSync()
        super.onDisabled(context)
    }

    companion object {

        private const val ACTION_PLAY_PAUSE = "com.ayush.vallpaper.widget.PLAY_PAUSE"
        private const val ACTION_PREVIOUS = "com.ayush.vallpaper.widget.PREVIOUS"
        private const val ACTION_NEXT = "com.ayush.vallpaper.widget.NEXT"

        private val activeBuffer = AtomicInteger(0)

        fun updateFull(context: Context, state: LyricsWidgetState) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                ComponentName(context, LyricsWidgetReceiver::class.java)
            )
            if (ids.isEmpty()) return

            val settings = LyricsWidgetSettingsRepository(context).read()
            val views = createViews(context, settings).apply {
                applyCommonAppearance(this, settings)
                setSongInfo(context, this, state, settings)
                setTextViewText(
                    R.id.lyrics_widget_title,
                    state.title.ifBlank { "Vallpaper Lyrics" }
                )
                if (settings.transition != LyricsWidgetSettings.TRANSITION_SLIDE) {
                    setTextViewText(
                        R.id.lyrics_widget_next_sans,
                        state.nextLine.ifBlank { " " }
                    )
                    setTextViewText(
                        R.id.lyrics_widget_next_serif,
                        state.nextLine.ifBlank { " " }
                    )
                    setTextViewText(
                        R.id.lyrics_widget_next_mono,
                        state.nextLine.ifBlank { " " }
                    )
                }
                setOnClickPendingIntent(
                    R.id.lyrics_widget_root,
                    openAppPendingIntent(context)
                )
            }

            if (isAnimated(settings)) {
                val line = state.currentLine.ifBlank { "Lyrics unavailable" }
                setAnimatedBufferText(views, 0, line)
                setAnimatedBufferText(views, 1, line)
                if (settings.transition == LyricsWidgetSettings.TRANSITION_SLIDE) {
                    setSlideNextText(views, state.nextLine)
                    applySlideWidgetSettings(views, settings)
                } else {
                    applyAnimatedWidgetSettings(views, settings)
                }
                views.setDisplayedChild(R.id.lyrics_widget_current_flipper, 0)
                activeBuffer.set(0)
            } else {
                setStaticCurrentText(
                    views,
                    state.currentLine.ifBlank { "Lyrics unavailable" }
                )
                applyWidgetSettings(views, settings)
            }

            manager.updateAppWidget(ids, views)
        }

        fun updateLyrics(context: Context, state: LyricsWidgetState) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                ComponentName(context, LyricsWidgetReceiver::class.java)
            )
            if (ids.isEmpty()) return

            val settings = LyricsWidgetSettingsRepository(context).read()
            val views = createViews(context, settings).apply {
                if (settings.transition != LyricsWidgetSettings.TRANSITION_SLIDE) {
                    setTextViewText(
                        R.id.lyrics_widget_next_sans,
                        state.nextLine.ifBlank { " " }
                    )
                    setTextViewText(
                        R.id.lyrics_widget_next_serif,
                        state.nextLine.ifBlank { " " }
                    )
                    setTextViewText(
                        R.id.lyrics_widget_next_mono,
                        state.nextLine.ifBlank { " " }
                    )
                }
            }

            if (isAnimated(settings)) {
                val targetBuffer = 1 - activeBuffer.get()
                setAnimatedBufferText(
                    views,
                    targetBuffer,
                    state.currentLine.ifBlank { "Lyrics unavailable" }
                )

                if (settings.transition == LyricsWidgetSettings.TRANSITION_SLIDE) {
                    setSlideNextText(views, state.nextLine)
                    applySlideWidgetSettings(views, settings)
                } else {
                    applyAnimatedWidgetSettings(views, settings)
                }

                views.setDisplayedChild(
                    R.id.lyrics_widget_current_flipper,
                    targetBuffer
                )
                activeBuffer.set(targetBuffer)
            } else {
                setStaticCurrentText(
                    views,
                    state.currentLine.ifBlank { "Lyrics unavailable" }
                )
                applyWidgetSettings(views, settings)
            }

            manager.partiallyUpdateAppWidget(ids, views)
        }

        private fun applyCommonAppearance(
            views: RemoteViews,
            settings: LyricsWidgetSettings
        ) {
            val lyricColor = parseColor(
                settings.lyricColorHex,
                android.graphics.Color.WHITE
            )
            val titleVisibility = if (settings.showSongName) {
                android.view.View.VISIBLE
            } else {
                android.view.View.GONE
            }

            views.setViewVisibility(R.id.lyrics_widget_song_info, titleVisibility)
            views.setViewVisibility(R.id.lyrics_widget_title_divider, titleVisibility)
            views.setViewVisibility(R.id.lyrics_widget_progress, titleVisibility)
            views.setViewVisibility(R.id.lyrics_widget_controls, titleVisibility)

            views.setTextColor(R.id.lyrics_widget_title, lyricColor)
            views.setTextColor(R.id.lyrics_widget_artist, lyricColor)
            views.setTextColor(R.id.lyrics_widget_progress, lyricColor)
            views.setTextColor(R.id.lyrics_widget_previous, lyricColor)
            views.setTextColor(R.id.lyrics_widget_play_pause, lyricColor)
            views.setTextColor(R.id.lyrics_widget_next, lyricColor)

            if (settings.transition == LyricsWidgetSettings.TRANSITION_NONE) {
                intArrayOf(
                    R.id.lyrics_widget_current_sans,
                    R.id.lyrics_widget_current_serif,
                    R.id.lyrics_widget_current_mono
                ).forEach { views.setTextColor(it, lyricColor) }

                intArrayOf(
                    R.id.lyrics_widget_next_sans,
                    R.id.lyrics_widget_next_serif,
                    R.id.lyrics_widget_next_mono
                ).forEach { views.setTextColor(it, lyricColor) }
            } else {
                intArrayOf(
                    R.id.lyrics_widget_current_a_sans,
                    R.id.lyrics_widget_current_a_serif,
                    R.id.lyrics_widget_current_a_mono,
                    R.id.lyrics_widget_current_b_sans,
                    R.id.lyrics_widget_current_b_serif,
                    R.id.lyrics_widget_current_b_mono
                ).forEach { views.setTextColor(it, lyricColor) }

                if (settings.transition == LyricsWidgetSettings.TRANSITION_SLIDE) {
                    intArrayOf(
                        R.id.lyrics_widget_next_a_sans,
                        R.id.lyrics_widget_next_a_serif,
                        R.id.lyrics_widget_next_a_mono
                    ).forEach { views.setTextColor(it, lyricColor) }
                } else {
                    intArrayOf(
                        R.id.lyrics_widget_next_sans,
                        R.id.lyrics_widget_next_serif,
                        R.id.lyrics_widget_next_mono
                    ).forEach { views.setTextColor(it, lyricColor) }
                }
            }
        }

        private fun parseColor(hex: String, fallback: Int): Int =
            try {
                android.graphics.Color.parseColor(hex)
            } catch (_: IllegalArgumentException) {
                fallback
            }

        private fun setSongInfo(
            context: Context,
            views: RemoteViews,
            state: LyricsWidgetState,
            settings: LyricsWidgetSettings
        ) {
            views.setTextViewText(R.id.lyrics_widget_title, state.title.ifBlank { "Nothing playing" })
            views.setTextViewText(R.id.lyrics_widget_artist, state.artist.ifBlank { "Unknown artist" })
            views.setProgressBar(
                R.id.lyrics_widget_progress,
                state.durationMs.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
                state.positionMs.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
                state.durationMs <= 0L
            )
            views.setImageViewResource(
                R.id.lyrics_widget_play_pause,
                if (state.playbackStatus == "PLAYING") {
                    android.R.drawable.ic_media_pause
                } else {
                    android.R.drawable.ic_media_play
                }
            )
            views.setOnClickPendingIntent(R.id.lyrics_widget_previous, actionPendingIntent(context, ACTION_PREVIOUS, 2001))
            views.setOnClickPendingIntent(R.id.lyrics_widget_play_pause, actionPendingIntent(context, ACTION_PLAY_PAUSE, 2002))
            views.setOnClickPendingIntent(R.id.lyrics_widget_next, actionPendingIntent(context, ACTION_NEXT, 2003))

            if (state.artworkUrl.isNotBlank()) {
                runCatching {
                    val uri = Uri.parse(state.artworkUrl)
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        BitmapFactory.decodeStream(input)
                    }?.let { bitmap ->
                        views.setImageViewBitmap(R.id.lyrics_widget_artwork, bitmap)
                    }
                }
            }
        }

        private fun actionPendingIntent(
            context: Context,
            action: String,
            requestCode: Int
        ): android.app.PendingIntent {
            val intent = Intent(context, LyricsWidgetReceiver::class.java).setAction(action)
            return android.app.PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or
                    android.app.PendingIntent.FLAG_IMMUTABLE
            )
        }

        private fun updatePlaybackFromRepository(context: Context) {
            CoroutineScope(Dispatchers.IO).launch {
                val application = context.applicationContext as? VallpaperApplication ?: return@launch
                val state = LyricsWidgetStore.read(context)
                val position = application.trackRepository.currentPlaybackPositionMs() ?: state.positionMs
                val playbackState = application.trackRepository.currentPlaybackState()
                    ?: android.media.session.PlaybackState.STATE_NONE
                updatePlayback(
                    context,
                    position,
                    state.durationMs,
                    playbackState
                )
            }
        }

        fun updatePlayback(
            context: Context,
            positionMs: Long,
            durationMs: Long,
            playbackState: Int
        ) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                ComponentName(context, LyricsWidgetReceiver::class.java)
            )
            if (ids.isEmpty()) return

            val settings = LyricsWidgetSettingsRepository(context).read()
            val state = runCatching {
                kotlinx.coroutines.runBlocking { LyricsWidgetStore.read(context) }
            }.getOrNull() ?: return

            val views = createViews(context, settings)
            applyCommonAppearance(views, settings)
            setSongInfo(context, views, state.copy(
                positionMs = positionMs,
                durationMs = durationMs,
                playbackStatus = when (playbackState) {
                    android.media.session.PlaybackState.STATE_PLAYING -> "PLAYING"
                    android.media.session.PlaybackState.STATE_PAUSED -> "PAUSED"
                    else -> state.playbackStatus
                }
            ), settings)
            manager.partiallyUpdateAppWidget(ids, views)
        }

        private fun createViews(
            context: Context,
            settings: LyricsWidgetSettings
        ): RemoteViews {
            val layout = when (settings.transition) {
                LyricsWidgetSettings.TRANSITION_FADE ->
                    R.layout.lyrics_widget_layout_fade
                LyricsWidgetSettings.TRANSITION_SLIDE ->
                    R.layout.lyrics_widget_layout_slide
                else ->
                    R.layout.lyrics_widget_layout
            }
            return RemoteViews(context.packageName, layout)
        }

        private fun isAnimated(settings: LyricsWidgetSettings): Boolean =
            settings.transition != LyricsWidgetSettings.TRANSITION_NONE

        private fun setStaticCurrentText(
            views: RemoteViews,
            text: String
        ) {
            views.setTextViewText(R.id.lyrics_widget_current_sans, text)
            views.setTextViewText(R.id.lyrics_widget_current_serif, text)
            views.setTextViewText(R.id.lyrics_widget_current_mono, text)
        }

        private fun setAnimatedBufferText(
            views: RemoteViews,
            buffer: Int,
            text: String
        ) {
            val ids = if (buffer == 0) {
                intArrayOf(
                    R.id.lyrics_widget_current_a_sans,
                    R.id.lyrics_widget_current_a_serif,
                    R.id.lyrics_widget_current_a_mono
                )
            } else {
                intArrayOf(
                    R.id.lyrics_widget_current_b_sans,
                    R.id.lyrics_widget_current_b_serif,
                    R.id.lyrics_widget_current_b_mono
                )
            }

            ids.forEach { views.setTextViewText(it, text) }
        }

        private fun applyWidgetSettings(
            views: RemoteViews,
            settings: LyricsWidgetSettings
        ) {
            val currentIds = intArrayOf(
                R.id.lyrics_widget_current_sans,
                R.id.lyrics_widget_current_serif,
                R.id.lyrics_widget_current_mono
            )
            val nextIds = intArrayOf(
                R.id.lyrics_widget_next_sans,
                R.id.lyrics_widget_next_serif,
                R.id.lyrics_widget_next_mono
            )

            currentIds.forEach {
                views.setViewVisibility(it, android.view.View.GONE)
                views.setTextViewTextSize(
                    it,
                    android.util.TypedValue.COMPLEX_UNIT_SP,
                    settings.fontSizeSp
                )
            }
            nextIds.forEach {
                views.setViewVisibility(it, android.view.View.GONE)
                views.setTextViewTextSize(
                    it,
                    android.util.TypedValue.COMPLEX_UNIT_SP,
                    (settings.fontSizeSp * 0.63f).coerceAtLeast(10f)
                )
            }

            val currentIndex = fontIndex(settings)
            views.setViewVisibility(
                currentIds[currentIndex],
                android.view.View.VISIBLE
            )
            views.setViewVisibility(
                nextIds[currentIndex],
                android.view.View.VISIBLE
            )
        }

        private fun setSlideNextText(
            views: RemoteViews,
            text: String
        ) {
            views.setTextViewText(R.id.lyrics_widget_next_a_sans, text)
            views.setTextViewText(R.id.lyrics_widget_next_a_serif, text)
            views.setTextViewText(R.id.lyrics_widget_next_a_mono, text)
        }

        private fun applyAnimatedWidgetSettings(
            views: RemoteViews,
            settings: LyricsWidgetSettings
        ) {
            val currentIds = arrayOf(
                intArrayOf(
                    R.id.lyrics_widget_current_a_sans,
                    R.id.lyrics_widget_current_a_serif,
                    R.id.lyrics_widget_current_a_mono
                ),
                intArrayOf(
                    R.id.lyrics_widget_current_b_sans,
                    R.id.lyrics_widget_current_b_serif,
                    R.id.lyrics_widget_current_b_mono
                )
            )
            val nextIds = intArrayOf(
                R.id.lyrics_widget_next_sans,
                R.id.lyrics_widget_next_serif,
                R.id.lyrics_widget_next_mono
            )

            currentIds.forEach { buffer ->
                buffer.forEach {
                    views.setViewVisibility(it, android.view.View.GONE)
                    views.setTextViewTextSize(
                        it,
                        android.util.TypedValue.COMPLEX_UNIT_SP,
                        settings.fontSizeSp
                    )
                }
                views.setViewVisibility(
                    buffer[fontIndex(settings)],
                    android.view.View.VISIBLE
                )
            }

            nextIds.forEach {
                views.setViewVisibility(it, android.view.View.GONE)
                views.setTextViewTextSize(
                    it,
                    android.util.TypedValue.COMPLEX_UNIT_SP,
                    (settings.fontSizeSp * 0.63f).coerceAtLeast(10f)
                )
            }

            views.setViewVisibility(
                nextIds[fontIndex(settings)],
                android.view.View.VISIBLE
            )
        }

        private fun applySlideWidgetSettings(
            views: RemoteViews,
            settings: LyricsWidgetSettings
        ) {
            val currentIds = arrayOf(
                intArrayOf(
                    R.id.lyrics_widget_current_a_sans,
                    R.id.lyrics_widget_current_a_serif,
                    R.id.lyrics_widget_current_a_mono
                ),
                intArrayOf(
                    R.id.lyrics_widget_current_b_sans,
                    R.id.lyrics_widget_current_b_serif,
                    R.id.lyrics_widget_current_b_mono
                )
            )

            currentIds.forEach { buffer ->
                buffer.forEach {
                    views.setViewVisibility(it, android.view.View.GONE)
                    views.setTextViewTextSize(
                        it,
                        android.util.TypedValue.COMPLEX_UNIT_SP,
                        settings.fontSizeSp
                    )
                }
                views.setViewVisibility(
                    buffer[fontIndex(settings)],
                    android.view.View.VISIBLE
                )
            }

            val nextIds = intArrayOf(
                R.id.lyrics_widget_next_a_sans,
                R.id.lyrics_widget_next_a_serif,
                R.id.lyrics_widget_next_a_mono
            )

            nextIds.forEach {
                views.setViewVisibility(it, android.view.View.GONE)
                views.setTextViewTextSize(
                    it,
                    android.util.TypedValue.COMPLEX_UNIT_SP,
                    (settings.fontSizeSp * 0.63f).coerceAtLeast(10f)
                )
            }

            views.setViewVisibility(
                nextIds[fontIndex(settings)],
                android.view.View.VISIBLE
            )
        }

        private fun fontIndex(settings: LyricsWidgetSettings): Int =
            when (settings.fontFamily) {
                LyricsWidgetSettings.FONT_SERIF -> 1
                LyricsWidgetSettings.FONT_MONOSPACE -> 2
                else -> 0
            }

        fun refreshAppearance(context: Context) {
            CoroutineScope(Dispatchers.IO).launch {
                val state = LyricsWidgetStore.read(context)
                val manager = AppWidgetManager.getInstance(context)
                val ids = manager.getAppWidgetIds(
                    ComponentName(context, LyricsWidgetReceiver::class.java)
                )
                if (ids.isEmpty()) return@launch

                val settings = LyricsWidgetSettingsRepository(context).read()
                val views = createViews(context, settings).apply {
                    applyCommonAppearance(this, settings)
                    setSongInfo(context, this, state, settings)
                    setTextViewText(
                        R.id.lyrics_widget_title,
                        state.title.ifBlank { "Vallpaper Lyrics" }
                    )
                    if (settings.transition != LyricsWidgetSettings.TRANSITION_SLIDE) {
                        setTextViewText(
                            R.id.lyrics_widget_next_sans,
                            state.nextLine
                        )
                        setTextViewText(
                            R.id.lyrics_widget_next_serif,
                            state.nextLine
                        )
                        setTextViewText(
                            R.id.lyrics_widget_next_mono,
                            state.nextLine
                        )
                    }
                    setOnClickPendingIntent(
                        R.id.lyrics_widget_root,
                        openAppPendingIntent(context)
                    )
                }

                if (isAnimated(settings)) {
                    val line = state.currentLine.ifBlank { "Lyrics unavailable" }
                    setAnimatedBufferText(views, 0, line)
                    setAnimatedBufferText(views, 1, line)
                    if (settings.transition == LyricsWidgetSettings.TRANSITION_SLIDE) {
                        setSlideNextText(views, state.nextLine)
                        applySlideWidgetSettings(views, settings)
                    } else {
                        applyAnimatedWidgetSettings(views, settings)
                    }
                    views.setDisplayedChild(
                        R.id.lyrics_widget_current_flipper,
                        0
                    )
                    activeBuffer.set(0)
                } else {
                    setStaticCurrentText(
                        views,
                        state.currentLine.ifBlank { "Lyrics unavailable" }
                    )
                    applyWidgetSettings(views, settings)
                }

                manager.updateAppWidget(ids, views)
            }
        }

        private fun openAppPendingIntent(context: Context): PendingIntent {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            return PendingIntent.getActivity(
                context,
                1001,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}
