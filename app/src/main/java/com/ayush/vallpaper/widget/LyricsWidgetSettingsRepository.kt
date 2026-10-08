package com.ayush.vallpaper.widget

import android.content.Context

data class LyricsWidgetSettings(
    val fontSizeSp: Float = 19f,
    val fontFamily: String = FONT_SANS,
    val transition: String = TRANSITION_FADE,
    val lyricColorHex: String = "#FFFFFFFF",
    val backgroundMode: String = LyricsWidgetSettings.BACKGROUND_TRANSPARENT,
    val blurColorHex: String = "#B3000000",
    val opaqueColorHex: String = "#FF000000"
) {
    companion object {
        const val FONT_SANS = "sans"
        const val FONT_SERIF = "serif"
        const val FONT_MONOSPACE = "monospace"

        const val TRANSITION_NONE = "none"
        const val TRANSITION_FADE = "fade"
        const val TRANSITION_SLIDE = "slide"

        const val BACKGROUND_TRANSPARENT = "transparent"
        const val BACKGROUND_BLUR = "blur"
        const val BACKGROUND_OPAQUE = "opaque"
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
            ).let { value ->
                when (value) {
                    LyricsWidgetSettings.TRANSITION_NONE,
                    LyricsWidgetSettings.TRANSITION_FADE,
                    LyricsWidgetSettings.TRANSITION_SLIDE -> value
                    else -> LyricsWidgetSettings.TRANSITION_FADE
                }
            },
            lyricColorHex = preferences.getString(
                "lyric_color",
                preferences.getString("current_color", "#FFFFFFFF")
            ) ?: "#FFFFFFFF",
            backgroundMode = preferences.getString("background_mode", LyricsWidgetSettings.BACKGROUND_TRANSPARENT) ?: LyricsWidgetSettings.BACKGROUND_TRANSPARENT,
            blurColorHex = preferences.getString("blur_color", "#B3000000") ?: "#B3000000",
            opaqueColorHex = preferences.getString("opaque_color", "#FF000000") ?: "#FF000000"
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

    fun setLyricColor(hex: String) {
        preferences.edit().putString("lyric_color", hex).apply()
    }

    fun setBackgroundMode(mode: String) {
        preferences.edit().putString("background_mode", mode).apply()
    }

    fun setBlurColor(hex: String) {
        preferences.edit().putString("blur_color", hex).apply()
    }

    fun setOpaqueColor(hex: String) {
        preferences.edit().putString("opaque_color", hex).apply()
    }
}
