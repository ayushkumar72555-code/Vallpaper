package com.ayush.vallpaper.wallpaper

import android.graphics.Bitmap
import com.ayush.vallpaper.domain.model.Track
import com.ayush.vallpaper.domain.model.WallpaperStyle

class WallpaperGenerator(
    private val artworkLoader: ArtworkLoader,
    private val renderer: WallpaperRenderer
) {

    suspend fun generate(
        track: Track,
        style: WallpaperStyle
    ): Bitmap {

        val artwork =
            artworkLoader.load(
                track.artworkUrl
            )

        return renderer.render(
            track = track,
            artwork = artwork,
            style = style
        )
    }
}