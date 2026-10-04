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

        return removeBottomLetterbox(bitmap)
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
