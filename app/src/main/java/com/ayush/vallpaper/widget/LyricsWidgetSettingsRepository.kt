package com.ayush.vallpaper.widget

import android.content.Context

data class LyricsWidgetSettings(
    val fontSizeSp: Float = 19f,
    val fontFamily: String = FONT_SANS,
    val transition: String = TRANSITION_FADE,
    val showSongName: Boolean = true,
    val lyricColorHex: String = "#FFFFFFFF"
) {
    companion object {
        const val FONT_SANS = "sans"
        const val FONT_SERIF = "serif"
        const val FONT_MONOSPACE = "monospace"

        const val TRANSITION_NONE = "none"
        const val TRANSITION_FADE = "fade"
        const val TRANSITION_SLIDE = "slide"
    }
}

class LyricsWidgetSettingsRepository(context: Context) {

    private val preferences = context.applicationContext.getSharedPreferences(
        "lyrics_widget_settings",
        Context.MODE_PRIVATE
    )

    fun read(): LyricsWidgetSettings =
        LyricsWidgetSettings(
            fontSizeSp = preferences.getFloat("font_size_sp", 19f)
                .coerceIn(14f, 28f),
            fontFamily = preferences.getString(
                "font_family",
                LyricsWidgetSettings.FONT_SANS
            ) ?: LyricsWidgetSettings.FONT_SANS,
            transition = preferences.getString(
                "transition",
                LyricsWidgetSettings.TRANSITION_FADE
            ) ?: LyricsWidgetSettings.TRANSITION_FADE,
            showSongName = preferences.getBoolean("show_song_name", true),
            lyricColorHex = preferences.getString(
                "lyric_color",
                preferences.getString("current_color", "#FFFFFFFF")
            ) ?: "#FFFFFFFF"
        )

    fun setFontSize(sizeSp: Float) {
        preferences.edit().putFloat("font_size_sp", sizeSp.coerceIn(14f, 28f)).apply()
    }

    fun setFontFamily(fontFamily: String) {
        preferences.edit().putString("font_family", fontFamily).apply()
    }

    fun setTransition(transition: String) {
        preferences.edit().putString("transition", transition).apply()
    }

    fun setShowSongName(show: Boolean) {
        preferences.edit().putBoolean("show_song_name", show).apply()
    }

    fun setLyricColor(hex: String) {
        preferences.edit().putString("lyric_color", hex).apply()
    }
}
