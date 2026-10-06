package com.ayush.vallpaper.widget

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ayush.vallpaper.domain.model.Lyrics
import com.ayush.vallpaper.domain.model.LyricLine
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject

private val Context.lyricsWidgetDataStore by preferencesDataStore(
    name = "lyrics_widget"
)

data class LyricsWidgetState(
    val trackId: String = "",
    val title: String = "",
    val artist: String = "",
    val currentLine: String = "",
    val previousLine: String = "",
    val nextLine: String = "",
    val currentIndex: Int = -1
)

object LyricsWidgetStore {

    private val trackIdKey = stringPreferencesKey("track_id")
    private val titleKey = stringPreferencesKey("title")
    private val artistKey = stringPreferencesKey("artist")
    private val linesKey = stringPreferencesKey("lines")
    private val currentIndexKey = intPreferencesKey("current_index")
    private val updatedAtKey = longPreferencesKey("updated_at")

    suspend fun saveLyrics(
        context: Context,
        trackId: String,
        title: String,
        artist: String,
        lyrics: Lyrics
    ) {
        val json = JSONArray().apply {
            lyrics.lines.forEach { line ->
                put(
                    JSONObject()
                        .put("t", line.timestampMs)
                        .put("s", line.text)
                )
            }
        }.toString()

        context.lyricsWidgetDataStore.edit { preferences ->
            preferences[trackIdKey] = trackId
            preferences[titleKey] = title
            preferences[artistKey] = artist
            preferences[linesKey] = json
            preferences[currentIndexKey] = -1
            preferences[updatedAtKey] = System.currentTimeMillis()
        }
    }

    suspend fun clear(context: Context) {
        context.lyricsWidgetDataStore.edit { it.clear() }
    }

    suspend fun updateCurrentIndex(context: Context, index: Int) {
        context.lyricsWidgetDataStore.edit { preferences ->
            preferences[currentIndexKey] = index
            preferences[updatedAtKey] = System.currentTimeMillis()
        }
    }

    suspend fun read(context: Context): LyricsWidgetState {
        val preferences = context.lyricsWidgetDataStore.data.first()
        val lines = parseLines(preferences[linesKey].orEmpty())
        val index = preferences[currentIndexKey] ?: -1

        return LyricsWidgetState(
            trackId = preferences[trackIdKey].orEmpty(),
            title = preferences[titleKey].orEmpty(),
            artist = preferences[artistKey].orEmpty(),
            currentLine = lines.getOrNull(index)?.text.orEmpty(),
            previousLine = lines.getOrNull(index - 1)?.text.orEmpty(),
            nextLine = lines.getOrNull(index + 1)?.text.orEmpty(),
            currentIndex = index
        )
    }

    private fun parseLines(json: String): List<LyricLine> {
        if (json.isBlank()) return emptyList()

        return runCatching {
            val array = JSONArray(json)
            buildList(array.length()) {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    add(
                        LyricLine(
                            timestampMs = item.getLong("t"),
                            text = item.getString("s")
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }
}
