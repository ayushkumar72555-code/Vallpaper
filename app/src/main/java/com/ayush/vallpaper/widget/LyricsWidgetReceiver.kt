package com.ayush.vallpaper.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
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
        super.onReceive(context, intent)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val settings = LyricsWidgetSettingsRepository(context).read()
        val initial = createViews(context, settings).apply {
            setTextViewText(R.id.lyrics_widget_title, "Nothing playing")
            setTextViewText(R.id.lyrics_widget_next_sans, "")
            setTextViewText(R.id.lyrics_widget_next_serif, "")
            setTextViewText(R.id.lyrics_widget_next_mono, "")
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
            applyAnimatedWidgetSettings(initial, settings)
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

        private val activeBuffer = AtomicInteger(0)

        fun updateFull(context: Context, state: LyricsWidgetState) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                ComponentName(context, LyricsWidgetReceiver::class.java)
            )
            if (ids.isEmpty()) return

            val settings = LyricsWidgetSettingsRepository(context).read()
            val views = createViews(context, settings).apply {
                setTextViewText(
                    R.id.lyrics_widget_title,
                    state.title.ifBlank { "Vallpaper Lyrics" }
                )
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
                setOnClickPendingIntent(
                    R.id.lyrics_widget_root,
                    openAppPendingIntent(context)
                )
            }

            if (isAnimated(settings)) {
                val line = state.currentLine.ifBlank { "Lyrics unavailable" }
                setAnimatedBufferText(views, 0, line)
                setAnimatedBufferText(views, 1, line)
                applyAnimatedWidgetSettings(views, settings)
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

            if (isAnimated(settings)) {
                val targetBuffer = 1 - activeBuffer.get()
                setAnimatedBufferText(
                    views,
                    targetBuffer,
                    state.currentLine.ifBlank { "Lyrics unavailable" }
                )
                applyAnimatedWidgetSettings(views, settings)

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
                    setTextViewText(
                        R.id.lyrics_widget_title,
                        state.title.ifBlank { "Vallpaper Lyrics" }
                    )
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
                    setOnClickPendingIntent(
                        R.id.lyrics_widget_root,
                        openAppPendingIntent(context)
                    )
                }

                if (isAnimated(settings)) {
                    val line = state.currentLine.ifBlank { "Lyrics unavailable" }
                    setAnimatedBufferText(views, 0, line)
                    setAnimatedBufferText(views, 1, line)
                    applyAnimatedWidgetSettings(views, settings)
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
