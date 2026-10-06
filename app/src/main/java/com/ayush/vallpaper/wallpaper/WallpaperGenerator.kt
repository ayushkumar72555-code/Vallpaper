package com.ayush.vallpaper.wallpaper

import android.graphics.Bitmap
import kotlinx.coroutines.delay
import com.ayush.vallpaper.domain.model.Track
import com.ayush.vallpaper.domain.model.WallpaperStyle

class WallpaperGenerator(
    private val artworkLoader: ArtworkLoader,
    private val renderer: WallpaperRenderer
) {

    suspend fun generate(
        track: Track,
        style: WallpaperStyle,
        width: Int,
        height: Int
    ): Bitmap {
        // When the phone is locked, a media provider can briefly be
        // unavailable while the player updates its notification/session.
        // Never generate a wallpaper without artwork: doing so would replace
        // the user's valid wallpaper with a blank/dark one.
        var artwork: Bitmap? = null

        repeat(3) { attempt ->
            artwork = artworkLoader.load(track.artworkUrl)
            if (artwork != null) return@repeat

            if (attempt < 2) {
                delay(350L * (attempt + 1))
            }
        }

        val loadedArtwork = artwork
            ?: throw IllegalStateException(
                "Artwork could not be decoded for ${track.title}"
            )

        return renderer.render(
            track = track,
            artwork = loadedArtwork,
            style = style,
            width = width,
            height = height
        )
    }
}
