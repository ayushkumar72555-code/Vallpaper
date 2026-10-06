package com.ayush.vallpaper.widget

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ayush.vallpaper.domain.model.Lyrics
import com.ayush.vallpaper.domain.model.LyricLine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.lyricsWidgetDataStore by preferencesDataStore(
    name = "lyrics_widget"
)

data class LyricsWidgetState(
    val trackId: String = "",
    val title: String = "",
    val artist: String = "",
    val artworkUrl: String = "",
    val durationMs: Long = 0L,
    val positionMs: Long = 0L,
    val currentLine: String = "",
    val previousLine: String = "",
    val nextLine: String = "",
    val currentIndex: Int = -1,
    val playbackStatus: String = "WAITING"
)

object LyricsWidgetStore {

    private val trackIdKey = stringPreferencesKey("track_id")
    private val titleKey = stringPreferencesKey("title")
    private val artistKey = stringPreferencesKey("artist")
    private val artworkUrlKey = stringPreferencesKey("artwork_url")
    private val durationKey = longPreferencesKey("duration_ms")
    private val positionKey = longPreferencesKey("position_ms")
    private val linesKey = stringPreferencesKey("lines")
    private val currentIndexKey = intPreferencesKey("current_index")
    private val updatedAtKey = longPreferencesKey("updated_at")
    private val playbackStatusKey = stringPreferencesKey("playback_status")

    suspend fun saveLyrics(
        context: Context,
        trackId: String,
        title: String,
        artist: String,
        artworkUrl: String,
        durationMs: Long,
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
            preferences[artworkUrlKey] = artworkUrl
            preferences[durationKey] = durationMs
            preferences[positionKey] = 0L
            preferences[linesKey] = json
            preferences[currentIndexKey] = -1
            preferences[playbackStatusKey] = "WAITING"
            preferences[updatedAtKey] = System.currentTimeMillis()
        }
    }

    suspend fun clear(context: Context) {
        context.lyricsWidgetDataStore.edit { it.clear() }
    }

    suspend fun updatePosition(context: Context, positionMs: Long) {
        context.lyricsWidgetDataStore.edit { preferences ->
            preferences[positionKey] = positionMs.coerceAtLeast(0L)
            preferences[updatedAtKey] = System.currentTimeMillis()
        }
    }

    suspend fun updateCurrentIndex(context: Context, index: Int) {
        context.lyricsWidgetDataStore.edit { preferences ->
            preferences[currentIndexKey] = index
            preferences[updatedAtKey] = System.currentTimeMillis()
        }
    }

    suspend fun updatePlaybackStatus(context: Context, status: String) {
        context.lyricsWidgetDataStore.edit { preferences ->
            preferences[playbackStatusKey] = status
            preferences[updatedAtKey] = System.currentTimeMillis()
        }
    }

    fun stateFlow(context: Context): Flow<LyricsWidgetState> =
        context.lyricsWidgetDataStore.data.map { preferences ->
            buildState(preferences)
        }

    suspend fun read(context: Context): LyricsWidgetState {
        return buildState(context.lyricsWidgetDataStore.data.first())
    }

    private fun buildState(preferences: androidx.datastore.preferences.core.Preferences): LyricsWidgetState {
        val lines = parseLines(preferences[linesKey].orEmpty())
        val index = preferences[currentIndexKey] ?: -1

        return LyricsWidgetState(
            trackId = preferences[trackIdKey].orEmpty(),
            title = preferences[titleKey].orEmpty(),
            artist = preferences[artistKey].orEmpty(),
            artworkUrl = preferences[artworkUrlKey].orEmpty(),
            durationMs = preferences[durationKey] ?: 0L,
            positionMs = preferences[positionKey] ?: 0L,
            currentLine = when {
                index >= 0 -> lines.getOrNull(index)?.text.orEmpty()
                lines.isNotEmpty() -> "[Instrumental]"
                else -> ""
            },
            previousLine = if (index > 0) {
                lines.getOrNull(index - 1)?.text.orEmpty()
            } else {
                ""
            },
            nextLine = when {
                index >= 0 -> lines.getOrNull(index + 1)?.text.orEmpty()
                lines.isNotEmpty() -> lines.first().text
                else -> ""
            },
            currentIndex = index,
            playbackStatus = preferences[playbackStatusKey] ?: "WAITING"
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
