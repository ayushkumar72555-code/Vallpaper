package com.ayush.vallpaper.wallpaper

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import com.ayush.vallpaper.domain.model.WallpaperStyle

/**
 * Protects the upper part of generated wallpapers from lock-screen clocks.
 *
 * The previous implementation only darkened this area with a translucent
 * overlay. That did not actually remove artwork from behind the clock.
 * For Ambient, Vinyl and Minimal, the clock area is now kept completely
 * clear with an opaque black region followed by a gradual transition.
 */
object LockscreenSafeZoneRenderer {

    private const val SAFE_ZONE_SOLID_END = 0.24f
    private const val SAFE_ZONE_FADE_END = 0.34f

    fun apply(
        bitmap: Bitmap,
        style: WallpaperStyle
    ): Bitmap {
        if (bitmap.width <= 0 || bitmap.height <= 0) {
            return bitmap
        }

        // Cover is intentionally left unchanged because its visual identity
        // depends on full-screen artwork. The other three styles have their
        // visual content positioned below the clock-safe area.
        if (style == WallpaperStyle.COVER) {
            return bitmap
        }

        val canvas = Canvas(bitmap)
        val width = bitmap.width.toFloat()
        val height = bitmap.height.toFloat()

        // Completely clear the region where the lock-screen clock normally
        // appears. This is intentionally opaque, not translucent.
        val solidPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
        }

        canvas.drawRect(
            0f,
            0f,
            width,
            height * SAFE_ZONE_SOLID_END,
            solidPaint
        )

        // Gradually blend back into the generated artwork so there is no
        // harsh horizontal edge below the clock-safe region.
        val fadeGradient = LinearGradient(
            0f,
            height * SAFE_ZONE_SOLID_END,
            0f,
            height * SAFE_ZONE_FADE_END,
            Color.BLACK,
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP
        )

        val fadePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = fadeGradient
        }

        canvas.drawRect(
            0f,
            height * SAFE_ZONE_SOLID_END,
            width,
            height * SAFE_ZONE_FADE_END,
            fadePaint
        )

        return bitmap
    }
}
