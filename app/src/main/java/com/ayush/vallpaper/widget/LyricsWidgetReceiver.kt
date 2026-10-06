package com.ayush.vallpaper.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.RemoteViews
import com.ayush.vallpaper.MainActivity
import com.ayush.vallpaper.VallpaperApplication
import com.ayush.vallpaper.R
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
        val initial = RemoteViews(
            context.packageName,
            R.layout.lyrics_widget_layout
        ).apply {
            setTextViewText(R.id.lyrics_widget_title, "Nothing playing")
            setTextViewText(R.id.lyrics_widget_current_sans, "PLAY MUSIC TO SHOW LYRICS")
            setTextViewText(R.id.lyrics_widget_current_serif, "PLAY MUSIC TO SHOW LYRICS")
            setTextViewText(R.id.lyrics_widget_current_mono, "PLAY MUSIC TO SHOW LYRICS")
            setTextViewText(R.id.lyrics_widget_next_sans, "")
            setTextViewText(R.id.lyrics_widget_next_serif, "")
            setTextViewText(R.id.lyrics_widget_next_mono, "")
            setOnClickPendingIntent(
                R.id.lyrics_widget_root,
                openAppPendingIntent(context)
            )
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

        private val transitionHandler = Handler(Looper.getMainLooper())
        private val transitionToken = AtomicInteger(0)

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
                setTextViewText(
                    R.id.lyrics_widget_title,
                    state.title.ifBlank { "Vallpaper Lyrics" }
                )
                setTextViewText(
                    R.id.lyrics_widget_current_sans,
                    state.currentLine.ifBlank { "Lyrics unavailable" }
                )
                setTextViewText(
                    R.id.lyrics_widget_current_serif,
                    state.currentLine.ifBlank { "Lyrics unavailable" }
                )
                setTextViewText(
                    R.id.lyrics_widget_current_mono,
                    state.currentLine.ifBlank { "Lyrics unavailable" }
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
                applyWidgetSettings(this, LyricsWidgetSettingsRepository(context).read())
                setOnClickPendingIntent(
                    R.id.lyrics_widget_root,
                    openAppPendingIntent(context)
                )
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
            val token = transitionToken.incrementAndGet()
            val views = RemoteViews(
                context.packageName,
                R.layout.lyrics_widget_layout
            ).apply {
                setTextViewText(
                    R.id.lyrics_widget_current_sans,
                    state.currentLine.ifBlank { "Lyrics unavailable" }
                )
                setTextViewText(
                    R.id.lyrics_widget_current_serif,
                    state.currentLine.ifBlank { "Lyrics unavailable" }
                )
                setTextViewText(
                    R.id.lyrics_widget_current_mono,
                    state.currentLine.ifBlank { "Lyrics unavailable" }
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
                applyWidgetSettings(this, settings)

                when (settings.transition) {
                    LyricsWidgetSettings.TRANSITION_FADE ->
                        setFloat(
                            R.id.lyrics_widget_current_container,
                            "setAlpha",
                            0f
                        )
                    LyricsWidgetSettings.TRANSITION_SLIDE ->
                        setFloat(
                            R.id.lyrics_widget_current_container,
                            "setTranslationY",
                            -8f
                        )
                }
            }

            manager.partiallyUpdateAppWidget(ids, views)

            if (settings.transition != LyricsWidgetSettings.TRANSITION_NONE) {
                transitionHandler.postDelayed({
                    if (transitionToken.get() != token) return@postDelayed

                    val settle = RemoteViews(
                        context.packageName,
                        R.layout.lyrics_widget_layout
                    ).apply {
                        when (settings.transition) {
                            LyricsWidgetSettings.TRANSITION_FADE ->
                                setFloat(
                                    R.id.lyrics_widget_current_container,
                                    "setAlpha",
                                    1f
                                )
                            LyricsWidgetSettings.TRANSITION_SLIDE ->
                                setFloat(
                                    R.id.lyrics_widget_current_container,
                                    "setTranslationY",
                                    0f
                                )
                        }
                    }
                    manager.partiallyUpdateAppWidget(ids, settle)
                }, 160L)
            }
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
            }
            nextIds.forEach {
                views.setViewVisibility(it, android.view.View.GONE)
            }

            val currentIndex = when (settings.fontFamily) {
                LyricsWidgetSettings.FONT_SERIF -> 1
                LyricsWidgetSettings.FONT_MONOSPACE -> 2
                else -> 0
            }

            views.setViewVisibility(
                currentIds[currentIndex],
                android.view.View.VISIBLE
            )
            views.setViewVisibility(
                nextIds[currentIndex],
                android.view.View.VISIBLE
            )

            currentIds.forEach {
                views.setTextViewTextSize(
                    it,
                    android.util.TypedValue.COMPLEX_UNIT_SP,
                    settings.fontSizeSp
                )
            }
            nextIds.forEach {
                views.setTextViewTextSize(
                    it,
                    android.util.TypedValue.COMPLEX_UNIT_SP,
                    (settings.fontSizeSp * 0.63f).coerceAtLeast(10f)
                )
            }

            when (settings.transition) {
                LyricsWidgetSettings.TRANSITION_NONE -> {
                    views.setFloat(
                        R.id.lyrics_widget_current_container,
                        "setAlpha",
                        1f
                    )
                    views.setFloat(
                        R.id.lyrics_widget_current_container,
                        "setTranslationY",
                        0f
                    )
                }
                LyricsWidgetSettings.TRANSITION_FADE -> {
                    views.setFloat(
                        R.id.lyrics_widget_current_container,
                        "setAlpha",
                        1f
                    )
                }
                LyricsWidgetSettings.TRANSITION_SLIDE -> {
                    views.setFloat(
                        R.id.lyrics_widget_current_container,
                        "setTranslationY",
                        0f
                    )
                }
            }
        }

        fun refreshAppearance(context: Context) {
            CoroutineScope(Dispatchers.IO).launch {
                val state = LyricsWidgetStore.read(context)
                val manager = AppWidgetManager.getInstance(context)
                val ids = manager.getAppWidgetIds(
                    ComponentName(context, LyricsWidgetReceiver::class.java)
                )
                if (ids.isEmpty()) return@launch

                val views = RemoteViews(
                    context.packageName,
                    R.layout.lyrics_widget_layout
                ).apply {
                    setTextViewText(
                        R.id.lyrics_widget_title,
                        state.title.ifBlank { "Vallpaper Lyrics" }
                    )
                    setTextViewText(
                        R.id.lyrics_widget_current_sans,
                        state.currentLine.ifBlank { "Lyrics unavailable" }
                    )
                    setTextViewText(
                        R.id.lyrics_widget_current_serif,
                        state.currentLine.ifBlank { "Lyrics unavailable" }
                    )
                    setTextViewText(
                        R.id.lyrics_widget_current_mono,
                        state.currentLine.ifBlank { "Lyrics unavailable" }
                    )
                    setTextViewText(R.id.lyrics_widget_next_sans, state.nextLine)
                    setTextViewText(R.id.lyrics_widget_next_serif, state.nextLine)
                    setTextViewText(R.id.lyrics_widget_next_mono, state.nextLine)
                    applyWidgetSettings(
                        this,
                        LyricsWidgetSettingsRepository(context).read()
                    )
                    setOnClickPendingIntent(
                        R.id.lyrics_widget_root,
                        openAppPendingIntent(context)
                    )
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
