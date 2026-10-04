package com.ayush.vallpaper.wallpaper

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import com.ayush.vallpaper.ui.screens.WallpaperTarget

object WallpaperApplier {

    fun apply(
        context: Context,
        bitmap: Bitmap,
        target: WallpaperTarget
    ): Result<Unit> {
        return runCatching {
            val wallpaperManager = WallpaperManager.getInstance(context)

            val flags = when (target) {
                WallpaperTarget.HOME -> WallpaperManager.FLAG_SYSTEM
                WallpaperTarget.LOCK -> WallpaperManager.FLAG_LOCK
                WallpaperTarget.BOTH -> WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
            }

            val fullImageRect = Rect(
                0,
                0,
                bitmap.width,
                bitmap.height
            )

            wallpaperManager.setBitmap(
                bitmap,
                fullImageRect,
                true,
                flags
            )
        }
    }
}
