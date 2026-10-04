package com.ayush.vallpaper.wallpaper

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
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

            // Passing null tells WallpaperManager that the complete bitmap is
            // the intended source image. Supplying a crop hint here can make
            // some launchers reinterpret the bitmap bounds and crop it again.
            wallpaperManager.setBitmap(
                bitmap,
                null,
                true,
                flags
            )
        }
    }
}
