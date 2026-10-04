package com.ayush.vallpaper.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ayush.vallpaper.VallpaperApplication
import com.ayush.vallpaper.viewmodel.WallpaperViewModel
import com.ayush.vallpaper.viewmodel.WallpaperViewModelFactory
import com.ayush.vallpaper.wallpaper.ArtworkLoader
import com.ayush.vallpaper.wallpaper.WallpaperGenerator
import com.ayush.vallpaper.wallpaper.WallpaperRenderer

@Composable
fun PreviewScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current

    val application =
        context.applicationContext as VallpaperApplication

    val track by
    application.trackRepository.currentTrack
        .collectAsState()

    val settings by
    application.settingsRepository.settings
        .collectAsState(
            initial = null
        )

    val generator =
        remember {
            WallpaperGenerator(
                artworkLoader = ArtworkLoader(
                    context = context
                ),
                renderer = WallpaperRenderer()
            )
        }

    val viewModel: WallpaperViewModel =
        viewModel(
            factory = WallpaperViewModelFactory(
                generator = generator
            )
        )

    val previewState by
    viewModel.previewState.collectAsState()

    val currentTrack = track
    val currentSettings = settings

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(
                rememberScrollState()
            )
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = "Wallpaper Preview",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        if (currentTrack == null) {

            Text(
                text = "No music detected",
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            Button(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Back")
            }

            return@Column
        }

        if (previewState.isGenerating) {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(500.dp),
                contentAlignment = Alignment.Center
            ) {

                CircularProgressIndicator()
            }

        } else if (previewState.bitmap != null) {

            Image(
                bitmap =
                    previewState.bitmap!!.asImageBitmap(),
                contentDescription =
                    "Generated wallpaper preview",
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(),
                contentScale =
                    ContentScale.Fit
            )

        } else {

            Text(
                text = "No wallpaper generated yet",
                style = MaterialTheme.typography.bodyLarge
            )
        }

        if (previewState.error != null) {

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            Text(
                text = previewState.error!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        Text(
            text = currentTrack.title,
            style = MaterialTheme.typography.titleLarge
        )

        Text(
            text = currentTrack.artist,
            style = MaterialTheme.typography.bodyMedium
        )

        if (currentSettings != null) {

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text =
                    "Style: ${currentSettings.selectedStyle.displayName}",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        Button(
            modifier = Modifier.fillMaxWidth(),
            enabled =
                currentSettings != null &&
                        !previewState.isGenerating,
            onClick = {

                val style =
                    currentSettings?.selectedStyle
                        ?: return@Button

                viewModel.generateWallpaper(
                    track = currentTrack,
                    style = style
                )
            }
        ) {
            Text("Generate Wallpaper")
        }

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back")
        }

        Spacer(
            modifier = Modifier.height(24.dp)
        )
    }
}