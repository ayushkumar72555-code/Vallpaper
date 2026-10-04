package com.ayush.vallpaper.wallpaper

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import com.ayush.vallpaper.domain.model.Track
import com.ayush.vallpaper.domain.model.WallpaperStyle
import kotlin.math.min

class WallpaperRenderer {

    companion object {
        private const val WALLPAPER_WIDTH = 1080
        private const val WALLPAPER_HEIGHT = 2400
        private const val SIDE_PADDING = 72f
    }

    fun render(
        track: Track,
        artwork: Bitmap?,
        style: WallpaperStyle
    ): Bitmap {

        val bitmap =
            Bitmap.createBitmap(
                WALLPAPER_WIDTH,
                WALLPAPER_HEIGHT,
                Bitmap.Config.ARGB_8888
            )

        val canvas =
            Canvas(bitmap)

        // Defensive conversion.
        // Canvas software rendering cannot draw
        // hardware bitmaps.
        val safeArtwork =
            artwork?.let { source ->

                if (source.config == Bitmap.Config.HARDWARE) {
                    source.copy(
                        Bitmap.Config.ARGB_8888,
                        false
                    )
                } else {
                    source
                }
            }

        when (style) {

            WallpaperStyle.AMBIENT -> {
                renderAmbient(
                    canvas = canvas,
                    track = track,
                    artwork = safeArtwork
                )
            }

            else -> {
                renderAmbient(
                    canvas = canvas,
                    track = track,
                    artwork = safeArtwork
                )
            }
        }

        return bitmap
    }

    private fun renderAmbient(
        canvas: Canvas,
        track: Track,
        artwork: Bitmap?
    ) {

        val width =
            canvas.width.toFloat()

        val height =
            canvas.height.toFloat()

        drawBackground(
            canvas = canvas,
            artwork = artwork,
            width = width,
            height = height
        )

        drawArtwork(
            canvas = canvas,
            artwork = artwork,
            width = width,
            height = height
        )

        drawTrackInformation(
            canvas = canvas,
            track = track,
            height = height
        )
    }

    private fun drawBackground(
        canvas: Canvas,
        artwork: Bitmap?,
        width: Float,
        height: Float
    ) {

        if (artwork == null) {

            canvas.drawColor(
                Color.rgb(
                    18,
                    18,
                    24
                )
            )

            return
        }

        val backgroundPaint =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            )

        val scaledArtwork =
            scaleBitmap(
                bitmap = artwork,
                targetWidth = width.toInt(),
                targetHeight = height.toInt()
            )

        val gradient =
            LinearGradient(
                0f,
                0f,
                0f,
                height,
                intArrayOf(
                    Color.argb(
                        230,
                        0,
                        0,
                        0
                    ),
                    Color.argb(
                        120,
                        0,
                        0,
                        0
                    ),
                    Color.argb(
                        245,
                        0,
                        0,
                        0
                    )
                ),
                floatArrayOf(
                    0f,
                    0.5f,
                    1f
                ),
                Shader.TileMode.CLAMP
            )

        backgroundPaint.shader =
            gradient

        val artworkLeft =
            (width - scaledArtwork.width) / 2f

        val artworkTop =
            (height - scaledArtwork.height) / 2f

        canvas.drawBitmap(
            scaledArtwork,
            artworkLeft,
            artworkTop,
            null
        )

        canvas.drawRect(
            0f,
            0f,
            width,
            height,
            backgroundPaint
        )
    }

    private fun drawArtwork(
        canvas: Canvas,
        artwork: Bitmap?,
        width: Float,
        height: Float
    ) {

        if (artwork == null) {
            return
        }

        val artworkSize =
            min(
                width - (SIDE_PADDING * 2),
                720f
            )

        val scaledArtwork =
            scaleBitmap(
                bitmap = artwork,
                targetWidth = artworkSize.toInt(),
                targetHeight = artworkSize.toInt()
            )

        val left =
            (width - scaledArtwork.width) / 2f

        val top =
            height * 0.18f

        val artworkPaint =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {
                isFilterBitmap = true
            }

        canvas.drawBitmap(
            scaledArtwork,
            left,
            top,
            artworkPaint
        )
    }

    private fun drawTrackInformation(
        canvas: Canvas,
        track: Track,
        height: Float
    ) {

        val titlePaint =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {

                color =
                    Color.WHITE

                textSize =
                    58f

                typeface =
                    Typeface.create(
                        Typeface.DEFAULT,
                        Typeface.BOLD
                    )
            }

        val artistPaint =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {

                color =
                    Color.WHITE

                alpha =
                    210

                textSize =
                    38f

                typeface =
                    Typeface.create(
                        Typeface.DEFAULT,
                        Typeface.NORMAL
                    )
            }

        val albumPaint =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {

                color =
                    Color.WHITE

                alpha =
                    160

                textSize =
                    30f
            }

        val textX =
            SIDE_PADDING

        val artworkBottom =
            height * 0.18f + 720f

        val titleY =
            artworkBottom + 150f

        canvas.drawText(
            track.title,
            textX,
            titleY,
            titlePaint
        )

        canvas.drawText(
            track.artist,
            textX,
            titleY + 62f,
            artistPaint
        )

        canvas.drawText(
            track.album,
            textX,
            titleY + 112f,
            albumPaint
        )
    }

    private fun scaleBitmap(
        bitmap: Bitmap,
        targetWidth: Int,
        targetHeight: Int
    ): Bitmap {

        val sourceWidth =
            bitmap.width.toFloat()

        val sourceHeight =
            bitmap.height.toFloat()

        val scale =
            maxOf(
                targetWidth / sourceWidth,
                targetHeight / sourceHeight
            )

        val scaledWidth =
            (sourceWidth * scale).toInt()

        val scaledHeight =
            (sourceHeight * scale).toInt()

        val scaledBitmap =
            Bitmap.createScaledBitmap(
                bitmap,
                scaledWidth,
                scaledHeight,
                true
            )

        val left =
            maxOf(
                0,
                (scaledWidth - targetWidth) / 2
            )

        val top =
            maxOf(
                0,
                (scaledHeight - targetHeight) / 2
            )

        return Bitmap.createBitmap(
            scaledBitmap,
            left,
            top,
            targetWidth,
            targetHeight
        )
    }
}