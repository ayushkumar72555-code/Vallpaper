package com.ayush.vallpaper.data

import android.content.Context
import com.ayush.vallpaper.domain.model.Lyrics
import com.ayush.vallpaper.domain.model.LyricLine
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

class PersistentLyricsCache(
    context: Context
) {
    companion object {
        private const val CACHE_DIRECTORY = "lyrics_cache"
        private const val CACHE_VERSION = 1
        private const val MAX_CACHE_FILES = 100
        private val MAX_CACHE_AGE_MS = TimeUnit.DAYS.toMillis(30)
    }

    private val directory = File(context.cacheDir, CACHE_DIRECTORY).apply { mkdirs() }

    fun read(trackId: String): Lyrics? {
        val file = fileFor(trackId)
        if (!file.exists() || file.length() == 0L) return null

        if (System.currentTimeMillis() - file.lastModified() > MAX_CACHE_AGE_MS) {
            file.delete()
            return null
        }

        return runCatching {
            val root = JSONObject(file.readText())

            if (root.optInt("version", 0) != CACHE_VERSION) {
                file.delete()
                return@runCatching null
            }

            val linesArray = root.optJSONArray("lines") ?: return@runCatching null
            val lines = buildList(linesArray.length()) {
                for (index in 0 until linesArray.length()) {
                    val item = linesArray.optJSONObject(index) ?: continue
                    val timestamp = item.optLong("t", -1L)
                    val text = item.optString("s").trim()
                    if (timestamp >= 0L && text.isNotBlank()) {
                        add(LyricLine(timestampMs = timestamp, text = text))
                    }
                }
            }

            if (lines.isEmpty()) {
                file.delete()
                return@runCatching null
            }

            Lyrics(
                lines = lines,
                source = root.optString("source", "LRCLIB")
            )
        }.getOrElse {
            file.delete()
            null
        }
    }

    fun write(trackId: String, lyrics: Lyrics) {
        val file = fileFor(trackId)

        runCatching {
            val root = JSONObject()
                .put("version", CACHE_VERSION)
                .put("source", lyrics.source)
                .put(
                    "lines",
                    JSONArray().apply {
                        lyrics.lines.forEach { line ->
                            put(
                                JSONObject()
                                    .put("t", line.timestampMs)
                                    .put("s", line.text)
                            )
                        }
                    }
                )

            file.writeText(root.toString())
            pruneIfNeeded()
        }
    }

    fun clear() {
        directory.listFiles()?.forEach { file ->
            if (file.isFile) file.delete()
        }
    }

    private fun fileFor(trackId: String): File =
        File(directory, "${sha256(trackId)}.json")

    private fun pruneIfNeeded() {
        val files = directory
            .listFiles()
            ?.filter { it.isFile }
            ?.sortedByDescending { it.lastModified() }
            ?: return

        files.drop(MAX_CACHE_FILES).forEach { it.delete() }
    }

    private fun sha256(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))

        return digest.joinToString("") { byte ->
            "%02x".format(byte)
        }
    }
}
