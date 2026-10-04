package com.ayush.vallpaper.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ayush.vallpaper.VallpaperApplication
import com.ayush.vallpaper.ui.components.AlbumArtwork

@Composable
fun HistoryScreen() {
    val context = LocalContext.current
    val application = context.applicationContext as VallpaperApplication
    val history by application.trackRepository.history.collectAsState()

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement =
            Arrangement.spacedBy(12.dp),
        verticalArrangement =
            Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {

        items(history) { track ->

            AlbumArtwork(
                imageUrl = track.artworkUrl,
                modifier = Modifier.padding(2.dp)
            )
        }
    }
}
