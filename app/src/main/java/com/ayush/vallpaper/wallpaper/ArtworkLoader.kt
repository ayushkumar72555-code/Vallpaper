package com.ayush.vallpaper.wallpaper

import android.content.Context
import android.graphics.Bitmap
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.request.bitmapConfig
import coil3.toBitmap

class ArtworkLoader(
    private val context: Context
) {

    private val imageLoader = ImageLoader(context)

    suspend fun load(artworkUrl: String): Bitmap? {
        if (artworkUrl.isBlank()) return null

        val request = ImageRequest.Builder(context)
            .data(artworkUrl)
            .allowHardware(false)
            .bitmapConfig(Bitmap.Config.ARGB_8888)
            .build()

        val result = imageLoader.execute(request)
        if (result !is SuccessResult) return null

        val bitmap = result.image.toBitmap(
            result.image.width,
            result.image.height,
            Bitmap.Config.ARGB_8888
        )

        return normalizeArtwork(bitmap)
    }

    /**
     * Media apps sometimes expose a notification/canvas image instead of the
     * actual cover. These images can contain large black bands and even a
     * second small copy of the artwork lower down.
     *
     * Detect large horizontal black gaps and keep the largest real artwork
     * band. This is intentionally conservative and only runs for portrait
     * sources with a substantial internal black gap.
     */
    private fun normalizeArtwork(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        if (width <= 0 || height <= 0) return bitmap
        // Do not assume the bad source is portrait. Spotify and other media
        // players can return a landscape/notification canvas containing the
        // real artwork, a huge black separator, and a second thumbnail.
        val gap = findLargestBlackGap(bitmap)
        if (gap != null) {
            val (gapStart, gapEnd) = gap
            val gapHeight = gapEnd - gapStart + 1
            val minimumGap = (height * 0.08f).toInt()

            if (gapHeight >= minimumGap) {
                val topContentHeight = gapStart
                val bottomContentHeight = height - gapEnd - 1

                // Keep the largest contiguous content region. A notification
                // thumbnail below a large artwork region is therefore ignored.
                val (cropTop, cropBottom) = if (topContentHeight >= bottomContentHeight) {
                    0 to gapStart
                } else {
                    (gapEnd + 1) to height
                }

                val cropHeight = cropBottom - cropTop
                val minimumContentHeight = (height * 0.20f).toInt()

                if (cropHeight >= minimumContentHeight) {
                    return Bitmap.createBitmap(
                        bitmap,
                        0,
                        cropTop,
                        width,
                        cropHeight
                    )
                }
            }
        }

        return removeBottomLetterbox(bitmap)
    }

    private fun findLargestBlackGap(bitmap: Bitmap): Pair<Int, Int>? {
        val width = bitmap.width
        val height = bitmap.height
        val step = (height / 300).coerceAtLeast(1)
        val xSamples = 48

        fun rowIsBlack(y: Int): Boolean {
            var dark = 0

            for (i in 0 until xSamples) {
                val x = ((i + 0.5f) * width / xSamples)
                    .toInt()
                    .coerceIn(0, width - 1)

                val pixel = bitmap.getPixel(x, y)
                val alpha = (pixel ushr 24) and 0xFF
                val red = (pixel ushr 16) and 0xFF
                val green = (pixel ushr 8) and 0xFF
                val blue = pixel and 0xFF

                if (alpha < 20 || (red <= 12 && green <= 12 && blue <= 12)) {
                    dark++
                }
            }

            return dark >= (xSamples * 0.92f).toInt()
        }

        var bestStart = -1
        var bestEnd = -1
        var currentStart = -1
        var y = 0

        while (y < height) {
            if (rowIsBlack(y)) {
                if (currentStart < 0) currentStart = y
            } else if (currentStart >= 0) {
                val currentEnd = y - 1
                if (currentEnd - currentStart > bestEnd - bestStart) {
                    bestStart = currentStart
                    bestEnd = currentEnd
                }
                currentStart = -1
            }
            y += step
        }

        if (currentStart >= 0) {
            val currentEnd = height - 1
            if (currentEnd - currentStart > bestEnd - bestStart) {
                bestStart = currentStart
                bestEnd = currentEnd
            }
        }

        if (bestStart < 0 || bestEnd < bestStart) return null

        // Ignore black regions touching the very top or bottom. Those are
        // normal letterboxing and are handled by removeBottomLetterbox.
        if (bestStart == 0 || bestEnd == height - 1) return null

        return bestStart to bestEnd
    }

    /**
     * Spotify can expose artwork with a large black letterbox below the
     * actual album cover. The UI hides this with ContentScale.Crop, while
     * the wallpaper renderer receives the complete decoded bitmap.
     *
     * Trim only a clearly detected black tail from a substantially portrait
     * source. Normal square artwork and genuinely dark artwork are untouched.
     */
    private fun removeBottomLetterbox(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        if (width <= 0 || height <= 0) return bitmap
        if (height.toFloat() / width.toFloat() < 1.15f) return bitmap

        val step = (height / 240).coerceAtLeast(1)
        val xSamples = 32

        fun rowIsBlack(y: Int): Boolean {
            var dark = 0
            for (i in 0 until xSamples) {
                val x = ((i + 0.5f) * width / xSamples)
                    .toInt()
                    .coerceIn(0, width - 1)
                val pixel = bitmap.getPixel(x, y)
                val alpha = (pixel ushr 24) and 0xFF
                val red = (pixel ushr 16) and 0xFF
                val green = (pixel ushr 8) and 0xFF
                val blue = pixel and 0xFF

                if (alpha < 20 || (red <= 12 && green <= 12 && blue <= 12)) {
                    dark++
                }
            }
            return dark >= (xSamples * 0.90f).toInt()
        }

        var y = height - 1
        var lastContentY = height - 1

        while (y >= 0 && rowIsBlack(y)) {
            lastContentY = y - 1
            y -= step
        }

        val darkTailHeight = height - 1 - lastContentY
        val minimumTail = (height * 0.12f).toInt()
        if (darkTailHeight < minimumTail) return bitmap

        val minimumContentHeight = (width * 0.75f).toInt()
        if (lastContentY + 1 < minimumContentHeight) return bitmap

        val cropHeight = (lastContentY + 1).coerceAtMost(height)
        if (cropHeight >= height) return bitmap

        return Bitmap.createBitmap(bitmap, 0, 0, width, cropHeight)
    }
}
