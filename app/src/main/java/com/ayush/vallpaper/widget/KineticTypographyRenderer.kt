package com.ayush.vallpaper.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

object KineticTypographyRenderer {

    private const val WIDTH = 900
    private const val HEIGHT = 300
    private const val FRAME_COUNT = 8

    fun renderFrames(
        context: Context,
        text: String,
        settings: LyricsWidgetSettings
    ): List<Bitmap> {
        val color = parseColor(settings.lyricColorHex, Color.WHITE)
        val typeface = when (settings.fontFamily) {
            LyricsWidgetSettings.FONT_SERIF -> Typeface.create(Typeface.SERIF, Typeface.BOLD)
            LyricsWidgetSettings.FONT_MONOSPACE -> Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            else -> Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            this.typeface = typeface
            this.textSize = settings.fontSizeSp * 2f
            this.textAlign = Paint.Align.LEFT
            this.isSubpixelText = true
            this.setShadowLayer(8f, 0f, 0f, 0xBFFFFFFF.toInt())
        }

        val words = layoutWords(text.ifBlank { "Lyrics unavailable" }, paint)
        return List(FRAME_COUNT) { frame ->
            renderFrame(words, paint, frame)
        }
    }

    private data class Word(
        val text: String,
        val x: Float,
        val y: Float
    )

    private fun layoutWords(text: String, paint: Paint): List<Word> {
        val maxWidth = WIDTH - 70f
        val lineHeight = paint.textSize * 1.25f
        val lines = mutableListOf<MutableList<String>>()
        var current = mutableListOf<String>()
        var currentWidth = 0f
        val space = paint.measureText(" ")

        text.trim().split(Regex("""\s+""")).forEach { word ->
            val width = paint.measureText(word)
            if (current.isNotEmpty() && currentWidth + space + width > maxWidth) {
                lines += current
                current = mutableListOf()
                currentWidth = 0f
            }
            current += word
            currentWidth += if (current.size == 1) width else space + width
        }
        if (current.isNotEmpty()) lines += current

        val visibleLines = lines.take(3)
        val totalHeight = visibleLines.size * lineHeight
        var y = (HEIGHT - totalHeight) / 2f - paint.ascent()

        return buildList {
            visibleLines.forEach { line ->
                var x = 35f
                line.forEach { word ->
                    add(Word(word, x, y))
                    x += paint.measureText(word) + space
                }
                y += lineHeight
            }
        }
    }

    private fun renderFrame(
        words: List<Word>,
        paint: Paint,
        frame: Int
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val progress = frame / (FRAME_COUNT - 1f)

        words.forEachIndexed { index, word ->
            val stagger = min(
                1f,
                max(0f, progress * 1.65f - index * 0.055f)
            )
            val eased = stagger * stagger * (3f - 2f * stagger)
            val direction = if (index % 2 == 0) -1f else 1f

            val distance = 72f * (1f - eased)
            val rotation = direction * 8f * (1f - eased)
            val scale = 0.78f + 0.22f * eased
            val alpha = (255f * eased).toInt().coerceIn(0, 255)

            canvas.save()
            canvas.translate(
                word.x + direction * distance,
                word.y - sin(index * 0.9f) * 9f * (1f - eased)
            )
            canvas.rotate(rotation)
            canvas.scale(scale, scale)

            paint.alpha = alpha
            canvas.drawText(word.text, 0f, 0f, paint)
            canvas.restore()
        }

        paint.alpha = 255
        return bitmap
    }

    private fun parseColor(hex: String, fallback: Int): Int =
        try {
            Color.parseColor(hex)
        } catch (_: IllegalArgumentException) {
            fallback
        }
}
