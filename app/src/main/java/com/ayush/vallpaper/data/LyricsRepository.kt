package com.ayush.vallpaper.data

import android.content.Context
import android.util.Log
import com.ayush.vallpaper.domain.model.Lyrics
import com.ayush.vallpaper.domain.model.LyricLine
import com.ayush.vallpaper.domain.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.Locale

class LyricsRepository(
    context: Context
) {

    companion object {
        private const val TAG = "VallpaperLyrics"
        private const val BASE_URL = "https://lrclib.net/api"
    }

    private val memoryCache = ConcurrentHashMap<String, Lyrics?>()
    private val persistentCache = PersistentLyricsCache(context.applicationContext)

    suspend fun findLyrics(track: Track): Lyrics? = withContext(Dispatchers.IO) {
        memoryCache[track.id]?.let { return@withContext it }

        persistentCache.read(track.id)?.let { lyrics ->
            memoryCache[track.id] = lyrics
            Log.d(TAG, "Lyrics cache hit: ${track.title}")
            return@withContext lyrics
        }

        val result = findExact(track) ?: findBySearch(track)

        if (result != null) {
            memoryCache[track.id] = result
            persistentCache.write(track.id, result)
            Log.d(TAG, "Lyrics cached: ${track.title}")
        }

        result
    }

    private fun findExact(track: Track): Lyrics? {
        val query = buildString {
            append("track_name=")
            append(URLEncoder.encode(track.title, "UTF-8"))
            append("&artist_name=")
            append(URLEncoder.encode(track.artist, "UTF-8"))
            if (track.album.isNotBlank() && track.album != "Unknown album") {
                append("&album_name=")
                append(URLEncoder.encode(track.album, "UTF-8"))
            }
            if (track.durationMs > 0L) {
                append("&duration=")
                append(URLEncoder.encode(
                    (track.durationMs / 1000.0).roundToInt().toString(),
                    "UTF-8"
                ))
            }
        }

        return requestJsonObject(BASE_URL + "/get?" + query)?.let { item ->
            parseResult(item, track)
        }
    }

    private fun findBySearch(track: Track): Lyrics? {
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

        val body = requestString(BASE_URL + "/search?" + query) ?: return null
        return runCatching {
            selectBestResult(JSONArray(body), track)
        }.onFailure {
            Log.d(TAG, "Lyrics search parsing failed for " + track.title, it)
        }.getOrNull()
    }

    private fun requestJsonObject(url: String): JSONObject? {
        val connection = openConnection(url)
        return try {
            if (connection.responseCode !in 200..299) {
                Log.d(TAG, "Lyrics exact lookup failed: " + connection.responseCode)
                null
            } else {
                connection.inputStream.bufferedReader().use { JSONObject(it.readText()) }
            }
        } catch (exception: Exception) {
            Log.d(TAG, "Lyrics exact lookup failed", exception)
            null
        } finally {
            connection.disconnect()
        }
    }

    private fun requestString(url: String): String? {
        val connection = openConnection(url)
        return try {
            if (connection.responseCode !in 200..299) {
                Log.d(TAG, "Lyrics search failed: " + connection.responseCode)
                null
            } else {
                connection.inputStream.bufferedReader().use { it.readText() }
            }
        } catch (exception: Exception) {
            Log.d(TAG, "Lyrics search failed", exception)
            null
        } finally {
            connection.disconnect()
        }
    }

    private fun openConnection(url: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 3_000
            readTimeout = 4_000
            setRequestProperty("Accept", "application/json")
            setRequestProperty(
                "User-Agent",
                "Vallpaper/1.0 (https://github.com/ayushkumar72555-code/Vallpaper)"
            )
        }

    private fun parseResult(item: JSONObject, track: Track): Lyrics? {
        val synced = item.optString("syncedLyrics").trim()
        if (synced.isBlank()) return null

        val title = item.optString("trackName")
        val artist = item.optString("artistName")
        val album = item.optString("albumName")
        val durationSeconds = item.optDouble("duration", 0.0)

        val score = matchScore(
            track = track,
            title = title,
            artist = artist,
            album = album,
            durationSeconds = durationSeconds
        )

        if (score < 0.82) {
            Log.d(TAG, "Rejected exact lyrics match for " + track.title + ", score=" + score)
            return null
        }

        return parseLrc(synced).takeIf { it.isNotEmpty() }?.let {
            Lyrics(lines = it, source = "LRCLIB")
        }
    }

    private fun selectBestResult(results: JSONArray, track: Track): Lyrics? {
        var best: Lyrics? = null
        var bestScore = 0.0
        var bestTitle = ""

        for (index in 0 until results.length()) {
            val item = results.optJSONObject(index) ?: continue
            val synced = item.optString("syncedLyrics").trim()
            if (synced.isBlank()) continue

            val title = item.optString("trackName")
            val artist = item.optString("artistName")
            val album = item.optString("albumName")
            val durationSeconds = item.optDouble("duration", 0.0)

            val score = matchScore(
                track = track,
                title = title,
                artist = artist,
                album = album,
                durationSeconds = durationSeconds
            )

            if (score <= bestScore) continue

            val lines = parseLrc(synced)
            if (lines.isEmpty()) continue

            best = Lyrics(lines = lines, source = "LRCLIB")
            bestScore = score
            bestTitle = title
        }

        if (best == null || bestScore < 0.80) {
            Log.d(
                TAG,
                "No reliable lyrics match for " + track.title +
                    ", bestScore=" + bestScore +
                    ", candidate=" + bestTitle
            )
            return null
        }

        Log.d(
            TAG,
            "Lyrics match: " + track.title +
                " -> " + bestTitle +
                ", score=" + bestScore
        )

        return best
    }

    private fun matchScore(
        track: Track,
        title: String,
        artist: String,
        album: String,
        durationSeconds: Double
    ): Double {
        val titleScore = similarity(
            normalizeTitle(track.title),
            normalizeTitle(title)
        )
        val artistScore = similarity(
            normalizeArtist(track.artist),
            normalizeArtist(artist)
        )

        val albumScore =
            if (
                track.album.isNotBlank() &&
                track.album != "Unknown album" &&
                album.isNotBlank()
            ) {
                similarity(
                    normalizeGeneral(track.album),
                    normalizeGeneral(album)
                )
            } else {
                0.5
            }

        var score =
            titleScore * 0.50 +
                artistScore * 0.40 +
                albumScore * 0.10

        if (track.durationMs > 0L && durationSeconds > 0.0) {
            val difference =
                kotlin.math.abs(track.durationMs / 1000.0 - durationSeconds)

            score = minOf(
                1.0,
                score + when {
                    difference <= 2.0 -> 0.08
                    difference <= 5.0 -> 0.04
                    difference <= 10.0 -> 0.01
                    else -> 0.0
                }
            )
        }

        return score
    }

    private fun similarity(left: String, right: String): Double {
        if (left.isBlank() || right.isBlank()) return 0.0
        if (left == right) return 1.0

        val leftTokens = left.split(' ').filter { it.isNotBlank() }.toSet()
        val rightTokens = right.split(' ').filter { it.isNotBlank() }.toSet()

        if (leftTokens.isNotEmpty() && rightTokens.isNotEmpty()) {
            val intersection = leftTokens.intersect(rightTokens).size.toDouble()
            val union = leftTokens.union(rightTokens).size.toDouble()

            if (union > 0.0 && intersection > 0.0) {
                val jaccard = intersection / union
                if (jaccard >= 0.75) return maxOf(jaccard, 0.85)
            }
        }

        val distance = levenshteinDistance(left, right)
        val longest = maxOf(left.length, right.length)

        return if (longest == 0) {
            1.0
        } else {
            1.0 - distance.toDouble() / longest.toDouble()
        }
    }

    private fun levenshteinDistance(left: String, right: String): Int {
        if (left == right) return 0
        if (left.isEmpty()) return right.length
        if (right.isEmpty()) return left.length

        var previous = IntArray(right.length + 1) { it }

        for (i in left.indices) {
            val current = IntArray(right.length + 1)
            current[0] = i + 1

            for (j in right.indices) {
                val cost = if (left[i] == right[j]) 0 else 1

                current[j + 1] = minOf(
                    current[j] + 1,
                    previous[j + 1] + 1,
                    previous[j] + cost
                )
            }

            previous = current
        }

        return previous[right.length]
    }

    private fun normalizeTitle(value: String): String {
        var normalized = normalizeGeneral(value)

        listOf(
            Regex("""\b(official\s+music\s+video|official\s+video|official\s+audio)\b"""),
            Regex("""\b(music\s+video|lyric\s+video|lyrics\s+video)\b"""),
            Regex("""\b(remastered|remaster|remix|edit|version|live|acoustic)\b"""),
            Regex("""\b(slowed\s*(\+|and)?\s*reverb|sped\s*up|speed\s*up|nightcore)\b""")
        ).forEach { pattern ->
            normalized = normalized.replace(pattern, " ")
        }

        normalized = normalized.replace(
            Regex("""\s+(feat|ft)\s+[a-z0-9 ]+$"""),
            " "
        )

        return collapseSpaces(normalized)
    }

    private fun normalizeArtist(value: String): String =
        collapseSpaces(
            normalizeGeneral(value)
                .replace(Regex("""\b(feat|ft)\b"""), " ")
                .replace(Regex("""\b(official|music|video|audio)\b"""), " ")
        )

    private fun normalizeGeneral(value: String): String =
        value.lowercase(Locale.US)
            .replace("&", " and ")
            .replace("’", "'")
            .replace(Regex("""[\[\](){}]"""), " ")
            .replace(Regex("""[^a-z0-9]+"""), " ")
            .trim()

    private fun collapseSpaces(value: String): String =
        value.replace(Regex("""\s+"""), " ").trim()

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

private fun Double.roundToInt(): Int = kotlin.math.round(this).toInt()
