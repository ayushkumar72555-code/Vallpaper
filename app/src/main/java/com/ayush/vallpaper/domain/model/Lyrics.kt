package com.ayush.vallpaper.domain.model

data class LyricLine(
    val timestampMs: Long,
    val text: String
)

data class Lyrics(
    val lines: List<LyricLine>,
    val source: String
) {
    fun lineIndexAt(positionMs: Long): Int {
        if (lines.isEmpty()) return -1

        var low = 0
        var high = lines.lastIndex
        var result = -1

        while (low <= high) {
            val middle = (low + high) ushr 1
            if (lines[middle].timestampMs <= positionMs) {
                result = middle
                low = middle + 1
            } else {
                high = middle - 1
            }
        }

        return result
    }

    fun nextTimestampAfter(positionMs: Long): Long? =
        lines.firstOrNull { it.timestampMs > positionMs }?.timestampMs
}
