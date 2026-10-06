package com.ayush.vallpaper.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.RemoteViews
import com.ayush.vallpaper.MainActivity
import com.ayush.vallpaper.VallpaperApplication
import com.ayush.vallpaper.R

class LyricsWidgetReceiver : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val repository = (context.applicationContext as? VallpaperApplication)?.trackRepository
            ?: return
        when (intent.action) {
            ACTION_PREVIOUS -> repository.skipToPrevious()
            ACTION_PLAY_PAUSE -> repository.togglePlayPause()
            ACTION_NEXT -> repository.skipToNext()
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val initial = RemoteViews(context.packageName, R.layout.lyrics_widget_layout).apply {
            setTextViewText(R.id.lyrics_widget_status, "WAITING")
            setTextViewText(R.id.lyrics_widget_title, "Nothing playing")
            setTextViewText(R.id.lyrics_widget_artist, "")
            setImageViewResource(R.id.lyrics_widget_artwork, android.R.drawable.ic_media_play)
            setTextViewText(R.id.lyrics_widget_current, "PLAY MUSIC TO SHOW LYRICS")
            setTextViewText(R.id.lyrics_widget_next, "")
            setOnClickPendingIntent(R.id.lyrics_widget_root, openAppPendingIntent(context))
            setOnClickPendingIntent(R.id.lyrics_widget_previous_button, actionPendingIntent(context, ACTION_PREVIOUS, 2001))
            setOnClickPendingIntent(R.id.lyrics_widget_play_pause_button, actionPendingIntent(context, ACTION_PLAY_PAUSE, 2002))
            setOnClickPendingIntent(R.id.lyrics_widget_next_button, actionPendingIntent(context, ACTION_NEXT, 2003))
        }
        appWidgetManager.updateAppWidget(appWidgetIds, initial)

        (context.applicationContext as? com.ayush.vallpaper.VallpaperApplication)
            ?.startLyricsWidgetSync()
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        (context.applicationContext as? com.ayush.vallpaper.VallpaperApplication)
            ?.startLyricsWidgetSync()
    }

    override fun onDisabled(context: Context) {
        (context.applicationContext as? com.ayush.vallpaper.VallpaperApplication)
            ?.stopLyricsWidgetSync()
        super.onDisabled(context)
    }

    companion object {
        private const val ACTION_PREVIOUS = "com.ayush.vallpaper.widget.PREVIOUS"
        private const val ACTION_PLAY_PAUSE = "com.ayush.vallpaper.widget.PLAY_PAUSE"
        private const val ACTION_NEXT = "com.ayush.vallpaper.widget.NEXT"


        fun updateFull(context: Context, state: LyricsWidgetState) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                ComponentName(context, LyricsWidgetReceiver::class.java)
            )
            if (ids.isEmpty()) return

            val views = RemoteViews(
                context.packageName,
                R.layout.lyrics_widget_layout
            ).apply {
                setTextViewText(R.id.lyrics_widget_status, statusLabel(state.playbackStatus))
                setTextViewText(
                    R.id.lyrics_widget_title,
                    state.title.ifBlank { "Vallpaper Lyrics" }
                )
                setTextViewText(R.id.lyrics_widget_artist, state.artist)
                setTextViewText(
                    R.id.lyrics_widget_current,
                    state.currentLine.ifBlank { "Lyrics unavailable" }
                )
                applyArtwork(this, state.artworkUrl)
                setTextViewText(
                    R.id.lyrics_widget_next,
                    state.nextLine.ifBlank { " " }
                )
                setImageViewResource(R.id.lyrics_widget_play_pause_button, playPauseIcon(state.playbackStatus))
                setOnClickPendingIntent(
                    R.id.lyrics_widget_root,
                    openAppPendingIntent(context)
                )
                setOnClickPendingIntent(R.id.lyrics_widget_previous_button, actionPendingIntent(context, ACTION_PREVIOUS, 2001))
                setOnClickPendingIntent(R.id.lyrics_widget_play_pause_button, actionPendingIntent(context, ACTION_PLAY_PAUSE, 2002))
                setOnClickPendingIntent(R.id.lyrics_widget_next_button, actionPendingIntent(context, ACTION_NEXT, 2003))
            }

            manager.updateAppWidget(ids, views)
        }

        fun updateLyrics(context: Context, state: LyricsWidgetState) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                ComponentName(context, LyricsWidgetReceiver::class.java)
            )
            if (ids.isEmpty()) return

            val views = RemoteViews(
                context.packageName,
                R.layout.lyrics_widget_layout
            ).apply {
                setTextViewText(
                    R.id.lyrics_widget_current,
                    state.currentLine.ifBlank { "Lyrics unavailable" }
                )
                setTextViewText(
                    R.id.lyrics_widget_next,
                    state.nextLine.ifBlank { " " }
                )
                setTextViewText(R.id.lyrics_widget_status, statusLabel(state.playbackStatus))
                setImageViewResource(R.id.lyrics_widget_play_pause_button, playPauseIcon(state.playbackStatus))
            }

            manager.partiallyUpdateAppWidget(ids, views)
        }

        fun updateProgress(
            context: Context,
            positionMs: Long?,
            durationMs: Long
        ) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                ComponentName(context, LyricsWidgetReceiver::class.java)
            )
            if (ids.isEmpty()) return

            val duration = durationMs.coerceAtLeast(0L)
            val position = (positionMs ?: 0L).coerceIn(0L, duration.coerceAtLeast(1L))
            val progress = if (duration > 0L) {
                ((position * 1000L) / duration).toInt().coerceIn(0, 1000)
            } else 0

            val views = RemoteViews(context.packageName, R.layout.lyrics_widget_layout).apply {
                setProgressBar(R.id.lyrics_widget_progress, 1000, progress, false)
                setTextViewText(R.id.lyrics_widget_elapsed, formatTime(position))
                setTextViewText(R.id.lyrics_widget_duration, formatTime(duration))
            }
            manager.partiallyUpdateAppWidget(ids, views)
        }

        private fun applyArtwork(views: RemoteViews, artworkUrl: String) {
            val uri = runCatching { Uri.parse(artworkUrl) }.getOrNull()
            val bitmap = if (uri?.scheme == "file" && !uri.path.isNullOrBlank()) {
                runCatching { BitmapFactory.decodeFile(uri.path) }.getOrNull()
            } else {
                null
            }

            if (bitmap != null) {
                views.setImageViewBitmap(R.id.lyrics_widget_artwork, bitmap)
            } else {
                views.setImageViewResource(
                    R.id.lyrics_widget_artwork,
                    android.R.drawable.ic_media_play
                )
            }
        }

        private fun statusLabel(status: String): String = when (status) {
            "PLAYING" -> "NOW PLAYING"
            "PAUSED" -> "PAUSED"
            "BUFFERING" -> "BUFFERING"
            else -> status
        }

        private fun playPauseIcon(status: String): Int =
            if (status == "PLAYING") R.drawable.ic_widget_pause
            else R.drawable.ic_widget_play

        private fun formatTime(milliseconds: Long): String {
            val totalSeconds = (milliseconds / 1000L).coerceAtLeast(0L)
            val minutes = totalSeconds / 60L
            val seconds = totalSeconds % 60L
            return "%d:%02d".format(minutes, seconds)
        }

        private fun actionPendingIntent(
            context: Context,
            action: String,
            requestCode: Int
        ): PendingIntent {
            val intent = Intent(context, LyricsWidgetReceiver::class.java).apply {
                this.action = action
            }
            return PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
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
