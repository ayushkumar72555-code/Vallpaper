package com.ayush.vallpaper.wallpaper

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import com.ayush.vallpaper.domain.model.Track
import com.ayush.vallpaper.domain.model.WallpaperStyle
import kotlin.math.min

class WallpaperRenderer {

    companion object {
        private const val SIDE_PADDING = 72f
        private const val MAX_PIXELS = 20_000_000L
    }

    fun render(
        track: Track,
        artwork: Bitmap?,
        style: WallpaperStyle,
        width: Int = 1080,
        height: Int = 2400
    ): Bitmap {
        require(width > 0) { "Wallpaper width must be greater than zero" }
        require(height > 0) { "Wallpaper height must be greater than zero" }
        require(width.toLong() * height.toLong() <= MAX_PIXELS) {
            "Requested wallpaper dimensions are too large"
        }

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val safeArtwork = artwork?.let { source ->
            if (source.config == Bitmap.Config.HARDWARE) {
                source.copy(Bitmap.Config.ARGB_8888, false)
            } else {
                source
            }
        }

        when (style) {
            WallpaperStyle.COVER -> renderCover(canvas, track, safeArtwork)
            WallpaperStyle.AMBIENT -> renderAmbient(canvas, track, safeArtwork)
            WallpaperStyle.VINYL -> renderVinyl(canvas, track, safeArtwork)
            WallpaperStyle.MINIMAL -> renderMinimal(canvas, track, safeArtwork)
        }

        return bitmap
    }

    // ================================================================
    // COVER
    // ================================================================

    private fun renderCover(canvas: Canvas, track: Track, artwork: Bitmap?) {
        val width = canvas.width.toFloat()
        val height = canvas.height.toFloat()

        if (artwork == null) {
            canvas.drawColor(Color.rgb(18, 18, 24))
            drawCoverText(canvas, track, width, height)
            return
        }

        val fullScreenArtwork = scaleBitmap(artwork, width.toInt(), height.toInt())
        val artworkLeft = (width - fullScreenArtwork.width) / 2f
        val artworkTop = (height - fullScreenArtwork.height) / 2f

        val artworkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            isFilterBitmap = true
        }
        canvas.drawBitmap(fullScreenArtwork, artworkLeft, artworkTop, artworkPaint)

        // Keep the artwork visible across the entire screen. This is deliberately
        // a subtle readability overlay, not the previous near-black bottom fade.
        val overallOverlay = LinearGradient(
            0f,
            0f,
            0f,
            height,
            intArrayOf(
                Color.argb(18, 0, 0, 0),
                Color.argb(8, 0, 0, 0),
                Color.argb(45, 0, 0, 0)
            ),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP
        )
        val overlayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = overallOverlay
        }
        canvas.drawRect(0f, 0f, width, height, overlayPaint)

        // A restrained gradient behind the text keeps it readable while preserving
        // the artwork. It never becomes an opaque black panel.
        val textGradient = LinearGradient(
            0f,
            height * 0.68f,
            0f,
            height,
            intArrayOf(
                Color.argb(0, 0, 0, 0),
                Color.argb(18, 0, 0, 0),
                Color.argb(75, 0, 0, 0)
            ),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP
        )
        val textGradientPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = textGradient
        }
        canvas.drawRect(0f, height * 0.62f, width, height, textGradientPaint)

        drawCoverText(canvas, track, width, height)
    }

    private fun drawCoverText(canvas: Canvas, track: Track, width: Float, height: Float) {
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 62f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isSubpixelText = true
            setShadowLayer(8f, 0f, 2f, Color.argb(120, 0, 0, 0))
        }
        val artistPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            alpha = 235
            textSize = 40f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isSubpixelText = true
            setShadowLayer(6f, 0f, 2f, Color.argb(110, 0, 0, 0))
        }
        val albumPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            alpha = 190
            textSize = 30f
            setShadowLayer(5f, 0f, 2f, Color.argb(100, 0, 0, 0))
        }

        val textX = SIDE_PADDING
        val titleY = height - 310f
        val artistY = titleY + 62f
        val albumY = artistY + 50f
        val maxTextWidth = width - (SIDE_PADDING * 2)

        canvas.drawText(fitText(track.title, titlePaint, maxTextWidth), textX, titleY, titlePaint)
        canvas.drawText(fitText(track.artist, artistPaint, maxTextWidth), textX, artistY, artistPaint)
        canvas.drawText(fitText(track.album, albumPaint, maxTextWidth), textX, albumY, albumPaint)
    }

    // ================================================================
    // VINYL STYLE
    // ================================================================

    private fun renderVinyl(canvas: Canvas, track: Track, artwork: Bitmap?) {
        val width = canvas.width.toFloat()
        val height = canvas.height.toFloat()

        val backgroundGradient = LinearGradient(
            0f,
            0f,
            0f,
            height,
            intArrayOf(Color.rgb(12, 12, 16), Color.rgb(28, 25, 31), Color.rgb(8, 8, 10)),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = backgroundGradient }
        canvas.drawRect(0f, 0f, width, height, backgroundPaint)

        val centerX = width / 2f
        val centerY = height / 2f
        val recordRadius = min(width * 0.43f, 465f)

        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(120, 0, 0, 0) }
        canvas.drawCircle(centerX, centerY + 24f, recordRadius + 14f, shadowPaint)

        val vinylPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                centerX,
                centerY,
                recordRadius,
                intArrayOf(Color.rgb(50, 50, 55), Color.rgb(18, 18, 21), Color.rgb(5, 5, 7)),
                floatArrayOf(0f, 0.45f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(centerX, centerY, recordRadius, vinylPaint)

        val groovePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = Color.argb(55, 190, 190, 195)
            strokeWidth = 2f
        }
        val grooveCount = 13
        for (index in 1..grooveCount) {
            val radius = recordRadius * (0.25f + (index.toFloat() / grooveCount.toFloat()) * 0.72f)
            canvas.drawCircle(centerX, centerY, radius, groovePaint)
        }

        val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                centerX - recordRadius,
                centerY - recordRadius,
                centerX + recordRadius,
                centerY + recordRadius,
                Color.argb(55, 255, 255, 255),
                Color.argb(0, 255, 255, 255),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(centerX, centerY, recordRadius, highlightPaint)

        val labelRadius = recordRadius * 0.36f
        if (artwork != null) {
            val labelSize = (labelRadius * 2f).toInt()
            val labelArtwork = scaleBitmap(artwork, labelSize, labelSize)

            canvas.save()
            val clipPath = android.graphics.Path()
            clipPath.addCircle(centerX, centerY, labelRadius, android.graphics.Path.Direction.CW)
            canvas.clipPath(clipPath)

            val artworkLeft = centerX - labelArtwork.width / 2f
            val artworkTop = centerY - labelArtwork.height / 2f
            val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = true }
            canvas.drawBitmap(labelArtwork, artworkLeft, artworkTop, labelPaint)
            canvas.restore()
        } else {
            val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(70, 70, 75) }
            canvas.drawCircle(centerX, centerY, labelRadius, labelPaint)
        }

        val holePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(12, 12, 14) }
        canvas.drawCircle(centerX, centerY, 15f, holePaint)

        val holeRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f
            color = Color.argb(150, 210, 210, 215)
        }
        canvas.drawCircle(centerX, centerY, 20f, holeRingPaint)

        drawVinylText(canvas, track, width, height)
    }

    private fun drawVinylText(canvas: Canvas, track: Track, width: Float, height: Float) {
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 54f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val artistPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            alpha = 210
            textSize = 36f
            textAlign = Paint.Align.CENTER
        }
        val albumPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            alpha = 150
            textSize = 28f
            textAlign = Paint.Align.CENTER
        }

        val centerX = width / 2f
        val titleY = height * 0.76f
        val artistY = titleY + 58f
        val albumY = artistY + 44f

        canvas.drawText(fitText(track.title, titlePaint, width - 120f), centerX, titleY, titlePaint)
        canvas.drawText(fitText(track.artist, artistPaint, width - 160f), centerX, artistY, artistPaint)
        canvas.drawText(fitText(track.album, albumPaint, width - 180f), centerX, albumY, albumPaint)
    }

    // ================================================================
    // MINIMAL STYLE
    // ================================================================

    private fun renderMinimal(canvas: Canvas, track: Track, artwork: Bitmap?) {
        val width = canvas.width.toFloat()
        val height = canvas.height.toFloat()

        val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        backgroundPaint.shader = LinearGradient(
            0f,
            0f,
            0f,
            height,
            Color.rgb(16, 16, 20),
            Color.rgb(30, 30, 34),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, width, height, backgroundPaint)

        var artworkSize = 0f
        if (artwork != null) {
            artworkSize = min(width - 180f, 760f)
            val scaledArtwork = scaleBitmap(artwork, artworkSize.toInt(), artworkSize.toInt())
            val artworkLeft = (width - scaledArtwork.width) / 2f
            val artworkTop = height / 2f - scaledArtwork.height / 2f

            val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(110, 0, 0, 0) }
            canvas.drawRoundRect(
                RectF(
                    artworkLeft + 12f,
                    artworkTop + 18f,
                    artworkLeft + scaledArtwork.width + 12f,
                    artworkTop + scaledArtwork.height + 18f
                ),
                28f,
                28f,
                shadowPaint
            )

            canvas.save()
            val artworkPath = android.graphics.Path()
            artworkPath.addRoundRect(
                RectF(
                    artworkLeft,
                    artworkTop,
                    artworkLeft + scaledArtwork.width,
                    artworkTop + scaledArtwork.height
                ),
                28f,
                28f,
                android.graphics.Path.Direction.CW
            )
            canvas.clipPath(artworkPath)

            val artworkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = true }
            canvas.drawBitmap(scaledArtwork, artworkLeft, artworkTop, artworkPaint)
            canvas.restore()
        }

        drawMinimalText(canvas, track, width, height, artwork != null, artworkSize)
    }

    private fun drawMinimalText(
        canvas: Canvas,
        track: Track,
        width: Float,
        height: Float,
        artworkPresent: Boolean,
        artworkSize: Float
    ) {
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 52f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isSubpixelText = true
        }
        val artistPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            alpha = 210
            textSize = 34f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
            isSubpixelText = true
        }
        val albumPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            alpha = 145
            textSize = 27f
            textAlign = Paint.Align.CENTER
        }

        val centerX = width / 2f
        val titleY = if (artworkPresent) {
            height / 2f + artworkSize / 2f + 62f
        } else {
            height * 0.52f
        }
        val artistY = titleY + 56f
        val albumY = artistY + 42f

        canvas.drawText(fitText(track.title, titlePaint, width - 120f), centerX, titleY, titlePaint)
        canvas.drawText(fitText(track.artist, artistPaint, width - 160f), centerX, artistY, artistPaint)
        canvas.drawText(fitText(track.album, albumPaint, width - 180f), centerX, albumY, albumPaint)
    }

    // ================================================================
    // AMBIENT STYLE
    // ================================================================

    private fun renderAmbient(canvas: Canvas, track: Track, artwork: Bitmap?) {
        val width = canvas.width.toFloat()
        val height = canvas.height.toFloat()

        drawAmbientBackground(canvas, artwork, width, height)
        drawAmbientArtwork(canvas, artwork, width, height)
        drawAmbientTrackInformation(canvas, track, height)
    }

    private fun drawAmbientBackground(canvas: Canvas, artwork: Bitmap?, width: Float, height: Float) {
        if (artwork == null) {
            canvas.drawColor(Color.rgb(18, 18, 24))
            return
        }

        val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val scaledArtwork = scaleBitmap(artwork, width.toInt(), height.toInt())
        val gradient = LinearGradient(
            0f,
            0f,
            0f,
            height,
            intArrayOf(Color.argb(230, 0, 0, 0), Color.argb(120, 0, 0, 0), Color.argb(245, 0, 0, 0)),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        backgroundPaint.shader = gradient

        val artworkLeft = (width - scaledArtwork.width) / 2f
        val artworkTop = (height - scaledArtwork.height) / 2f
        canvas.drawBitmap(scaledArtwork, artworkLeft, artworkTop, null)
        canvas.drawRect(0f, 0f, width, height, backgroundPaint)
    }

    private fun drawAmbientArtwork(canvas: Canvas, artwork: Bitmap?, width: Float, height: Float) {
        if (artwork == null) return

        val artworkSize = min(width - (SIDE_PADDING * 2), 720f)
        val scaledArtwork = scaleBitmap(artwork, artworkSize.toInt(), artworkSize.toInt())
        val left = (width - scaledArtwork.width) / 2f
        val top = height / 2f - scaledArtwork.height / 2f

        val artworkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = true }
        canvas.drawBitmap(scaledArtwork, left, top, artworkPaint)
    }

    private fun drawAmbientTrackInformation(canvas: Canvas, track: Track, height: Float) {
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 58f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val artistPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            alpha = 210
            textSize = 38f
        }
        val albumPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            alpha = 160
            textSize = 30f
        }

        val textX = SIDE_PADDING
        val artworkBottom = height / 2f + min(canvas.width.toFloat() - (SIDE_PADDING * 2), 720f) / 2f
        val titleY = artworkBottom + 90f
        val maxTextWidth = canvas.width.toFloat() - (SIDE_PADDING * 2)

        canvas.drawText(fitText(track.title, titlePaint, maxTextWidth), textX, titleY, titlePaint)
        canvas.drawText(fitText(track.artist, artistPaint, maxTextWidth), textX, titleY + 62f, artistPaint)
        canvas.drawText(fitText(track.album, albumPaint, maxTextWidth), textX, titleY + 112f, albumPaint)
    }

    // ================================================================
    // TEXT HELPER
    // ================================================================

    private fun fitText(text: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text

        val ellipsis = "..."
        val availableWidth = maxWidth - paint.measureText(ellipsis)
        var end = text.length

        while (end > 0 && paint.measureText(text.substring(0, end)) > availableWidth) {
            end--
        }

        if (end <= 0) return ellipsis
        return text.substring(0, end).trimEnd() + ellipsis
    }

    // ================================================================
    // BITMAP SCALING
    // ================================================================

    private fun scaleBitmap(bitmap: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap {
        require(targetWidth > 0) { "Target width must be greater than zero" }
        require(targetHeight > 0) { "Target height must be greater than zero" }

        val targetPixels = targetWidth.toLong() * targetHeight.toLong()
        require(targetPixels <= MAX_PIXELS) { "Requested wallpaper size is too large" }

        if (bitmap.width == targetWidth && bitmap.height == targetHeight) return bitmap

        val sourceWidth = bitmap.width.toFloat()
        val sourceHeight = bitmap.height.toFloat()
        if (sourceWidth <= 0f || sourceHeight <= 0f) {
            throw IllegalArgumentException("Invalid source bitmap dimensions")
        }

        val scale = maxOf(targetWidth / sourceWidth, targetHeight / sourceHeight)
        val scaledWidth = (sourceWidth * scale).toInt().coerceAtLeast(targetWidth)
        val scaledHeight = (sourceHeight * scale).toInt().coerceAtLeast(targetHeight)
        val scaledPixels = scaledWidth.toLong() * scaledHeight.toLong()
        require(scaledPixels <= MAX_PIXELS) { "Scaled artwork is too large" }

        val scaledBitmap = Bitmap.createBitmap(scaledWidth, scaledHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(scaledBitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = true }
        canvas.drawBitmap(
            bitmap,
            null,
            RectF(0f, 0f, scaledWidth.toFloat(), scaledHeight.toFloat()),
            paint
        )

        val cropLeft = ((scaledWidth - targetWidth) / 2f).coerceAtLeast(0f)
        val cropTop = ((scaledHeight - targetHeight) / 2f).coerceAtLeast(0f)
        return Bitmap.createBitmap(
            scaledBitmap,
            cropLeft.toInt(),
            cropTop.toInt(),
            targetWidth,
            targetHeight
        )
    }
}
