package com.ayush.vallpaper.wallpaper

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader

/**
 * Applies a subtle, device-independent safe area near the top of the
 * generated wallpaper. Android lock-screen clocks and status information
 * can occupy this region, so reducing visual contrast here improves
 * readability without changing the artwork itself.
 */
object LockscreenSafeZoneRenderer {

    private const val SAFE_ZONE_START = 0.0f
    private const val SAFE_ZONE_FULL = 0.18f
    private const val SAFE_ZONE_END = 0.30f

    fun apply(bitmap: Bitmap): Bitmap {
        if (bitmap.width <= 0 || bitmap.height <= 0) {
            return bitmap
        }

        val canvas = Canvas(bitmap)
        val height = bitmap.height.toFloat()

        val safeZoneGradient = LinearGradient(
            0f,
            height * SAFE_ZONE_START,
            0f,
            height * SAFE_ZONE_END,
            intArrayOf(
                Color.argb(115, 0, 0, 0),
                Color.argb(95, 0, 0, 0),
                Color.argb(0, 0, 0, 0)
            ),
            floatArrayOf(
                0f,
                SAFE_ZONE_FULL / SAFE_ZONE_END,
                1f
            ),
            Shader.TileMode.CLAMP
        )

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = safeZoneGradient
        }

        canvas.drawRect(
            0f,
            0f,
            bitmap.width.toFloat(),
            height * SAFE_ZONE_END,
            paint
        )

        return bitmap
    }
}
