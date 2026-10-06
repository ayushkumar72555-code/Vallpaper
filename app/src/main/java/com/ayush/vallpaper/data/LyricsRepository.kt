package com.ayush.vallpaper.data

import android.util.Log
import com.ayush.vallpaper.domain.model.Lyrics
import com.ayush.vallpaper.domain.model.LyricLine
import com.ayush.vallpaper.domain.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class LyricsRepository {

    companion object {
        private const val TAG = "VallpaperLyrics"
        private const val BASE_URL = "https://lrclib.net/api/search"
    }

    suspend fun findLyrics(track: Track): Lyrics? = withContext(Dispatchers.IO) {
        runCatching {
            val query = buildString {
                append("track_name=")
                append(URLEncoder.encode(track.title, "UTF-8"))
                append("&artist_name=")
                append(URLEncoder.encode(track.artist, "UTF-8"))
                if (track.album.isNotBlank() && track.album != "Unknown album") {
                    append("&album_name=")
                    append(URLEncoder.encode(track.album, "UTF-8"))
                }
            }

            val connection = (URL(BASE_URL + "?" + query).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 6_000
                readTimeout = 6_000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "Vallpaper/1.0")
            }

            try {
                if (connection.responseCode !in 200..299) {
                    Log.d(TAG, "Lyrics request failed: " + connection.responseCode)
                    return@runCatching null
                }

                val body = connection.inputStream.bufferedReader().use { it.readText() }
                selectBestResult(JSONArray(body), track)
            } finally {
                connection.disconnect()
            }
        }.onFailure {
            Log.d(TAG, "Lyrics lookup failed for " + track.title, it)
        }.getOrNull()
    }

    private fun selectBestResult(results: JSONArray, track: Track): Lyrics? {
        var best: Lyrics? = null
        var bestScore = Int.MIN_VALUE

        for (index in 0 until results.length()) {
            val item = results.optJSONObject(index) ?: continue
            val synced = item.optString("syncedLyrics").trim()
            if (synced.isBlank()) continue

            val title = item.optString("trackName")
            val artist = item.optString("artistName")
            val album = item.optString("albumName")

            var score = 0
            if (normalize(title) == normalize(track.title)) score += 4
            if (normalize(artist) == normalize(track.artist)) score += 4
            if (album.isNotBlank() && normalize(album) == normalize(track.album)) score += 2

            val lines = parseLrc(synced)
            if (lines.isNotEmpty() && score > bestScore) {
                best = Lyrics(lines = lines, source = "LRCLIB")
                bestScore = score
            }
        }

        return best
    }

    private fun parseLrc(lrc: String): List<LyricLine> {
        val timePattern = Regex("""\[(\d{1,3}):(\d{2})(?:[.:](\d{1,3}))?\]""")
        val parsed = mutableListOf<LyricLine>()

        lrc.lineSequence().forEach { rawLine ->
            val matches = timePattern.findAll(rawLine).toList()
            if (matches.isEmpty()) return@forEach

            val text = rawLine.substring(matches.last().range.last + 1).trim()
            if (text.isBlank()) return@forEach

            matches.forEach { match ->
                val minutes = match.groupValues[1].toLongOrNull() ?: return@forEach
                val seconds = match.groupValues[2].toLongOrNull() ?: return@forEach
                val fraction = match.groupValues[3]
                val millis = when (fraction.length) {
                    1 -> (fraction.toLongOrNull() ?: 0L) * 100
                    2 -> (fraction.toLongOrNull() ?: 0L) * 10
                    else -> fraction.take(3).toLongOrNull() ?: 0L
                }

                parsed += LyricLine(
                    timestampMs = minutes * 60_000L + seconds * 1_000L + millis,
                    text = text
                )
            }
        }

        return parsed.sortedBy { it.timestampMs }
    }

    private fun normalize(value: String): String =
        value.lowercase()
            .replace(Regex("""\([^)]*\)"""), "")
            .replace(Regex("""[^a-z0-9]+"""), " ")
            .trim()
}
