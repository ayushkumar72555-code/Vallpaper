package com.ayush.vallpaper.ui.components

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.request.ImageRequest
import coil3.request.crossfade
import androidx.compose.ui.platform.LocalContext

private const val TAG = "VallpaperArtwork"

@Composable
fun AlbumArtwork(
    imageUrl: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(20.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (imageUrl.isBlank()) {
            Text("No artwork available")
            return@Box
        }

        val request = ImageRequest.Builder(context)
            .data(imageUrl)
            .crossfade(true)
            .build()

        AsyncImage(
            model = request,
            contentDescription = "Album artwork",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            onState = { state ->
                when (state) {
                    is AsyncImagePainter.State.Loading -> {
                        Log.d(TAG, "Loading artwork: $imageUrl")
                    }

                    is AsyncImagePainter.State.Success -> {
                        Log.d(TAG, "Artwork loaded successfully: $imageUrl")
                    }

                    is AsyncImagePainter.State.Error -> {
                        Log.e(
                            TAG,
                            "Artwork load failed: $imageUrl",
                            state.result.throwable
                        )
                    }

                    is AsyncImagePainter.State.Empty -> Unit
                }
            }
        )
    }
}
