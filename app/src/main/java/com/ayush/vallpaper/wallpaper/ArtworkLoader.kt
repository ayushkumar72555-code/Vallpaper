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

    private val imageLoader =
        ImageLoader(context)

    suspend fun load(
        artworkUrl: String
    ): Bitmap? {

        if (artworkUrl.isBlank()) {
            return null
        }

        val request =
            ImageRequest.Builder(context)
                .data(artworkUrl)

                // Hardware bitmaps cannot be drawn
                // by the software Canvas used by
                // WallpaperRenderer.
                .allowHardware(false)

                // Explicitly request a normal software bitmap.
                .bitmapConfig(
                    Bitmap.Config.ARGB_8888
                )

                .build()

        val result =
            imageLoader.execute(request)

        if (result !is SuccessResult) {
            return null
        }

        return result.image.toBitmap(
            result.image.width,
            result.image.height,
            Bitmap.Config.ARGB_8888
        )
    }
}