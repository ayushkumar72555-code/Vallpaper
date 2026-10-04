package com.ayush.vallpaper.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ayush.vallpaper.ui.components.AlbumArtwork
import com.ayush.vallpaper.viewmodel.HomeViewModel

@Composable
fun HomeScreen(
    onPreview: () -> Unit,
    viewModel: HomeViewModel = viewModel()
) {

    val state by viewModel.uiState.collectAsState()
    val track = state.track ?: return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {

        Text(
            text = "Vallpaper",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(
            modifier = Modifier.height(32.dp)
        )

        AlbumArtwork(
            imageUrl = track.artworkUrl,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        Text(
            text = track.title,
            style = MaterialTheme.typography.headlineSmall
        )

        Text(
            text = track.artist,
            style = MaterialTheme.typography.bodyLarge
        )

        Text(
            text = track.album,
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            Column {
                Text("Automatic Wallpaper")

                Text(
                    text =
                        if (state.automaticWallpaper)
                            "Enabled"
                        else
                            "Disabled",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Switch(
                checked = state.automaticWallpaper,
                onCheckedChange = {
                    viewModel.toggleAutomaticWallpaper()
                }
            )
        }

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = onPreview
        ) {
            Text("Preview Wallpaper")
        }
    }
}