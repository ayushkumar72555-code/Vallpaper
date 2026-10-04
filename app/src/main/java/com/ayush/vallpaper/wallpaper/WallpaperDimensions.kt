package com.ayush.vallpaper.wallpaper

import android.content.Context
import android.os.Build
import android.view.WindowManager

object WallpaperDimensions {

    private const val MAX_WALLPAPER_PIXELS = 20_000_000L

    fun get(context: Context): Pair<Int, Int> {
        val (rawWidth, rawHeight) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = context.getSystemService(WindowManager::class.java)
                ?.maximumWindowMetrics
                ?.bounds

            if (bounds != null) {
                bounds.width() to bounds.height()
            } else {
                context.resources.displayMetrics.widthPixels to
                        context.resources.displayMetrics.heightPixels
            }
        } else {
            context.resources.displayMetrics.widthPixels to
                    context.resources.displayMetrics.heightPixels
        }

        var width = rawWidth.coerceAtLeast(1)
        var height = rawHeight.coerceAtLeast(1)

        val pixels = width.toLong() * height.toLong()
        if (pixels > MAX_WALLPAPER_PIXELS) {
            val scale = kotlin.math.sqrt(
                MAX_WALLPAPER_PIXELS.toDouble() / pixels.toDouble()
            )
            width = (width * scale).toInt().coerceAtLeast(1)
            height = (height * scale).toInt().coerceAtLeast(1)
        }

        return width to height
    }
}
