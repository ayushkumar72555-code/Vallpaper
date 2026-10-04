package com.ayush.vallpaper.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ayush.vallpaper.VallpaperApplication
import com.ayush.vallpaper.domain.model.WallpaperStyle
import com.ayush.vallpaper.ui.components.AlbumArtwork
import com.ayush.vallpaper.viewmodel.AppViewModelFactory
import com.ayush.vallpaper.viewmodel.HomeUiState
import com.ayush.vallpaper.viewmodel.HomeViewModel

@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val application = context.applicationContext as VallpaperApplication

    val viewModel: HomeViewModel = viewModel(
        factory = AppViewModelFactory(application)
    )

    val state by viewModel.uiState.collectAsState()
    val track = state.track
    var showSettings by remember { mutableStateOf(false) }

    val displayMetrics = context.resources.displayMetrics
    val wallpaperWidth = displayMetrics.widthPixels.coerceAtLeast(1)
    val wallpaperHeight = displayMetrics.heightPixels.coerceAtLeast(1)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Vallpaper",
                style = MaterialTheme.typography.headlineMedium
            )

            IconButton(onClick = { showSettings = true }) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings"
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = "NOW PLAYING",
            style = MaterialTheme.typography.labelLarge
        )
        Spacer(Modifier.height(12.dp))

        if (track == null) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp)) {
                    Text(
                        text = "No music detected",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Allow Vallpaper to access active media sessions so it can detect the song currently playing on your device.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            context.startActivity(
                                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                            )
                        }
                    ) {
                        Text("Allow Music Access")
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                AlbumArtwork(
                    imageUrl = track.artworkUrl,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 340.dp)
                )
            }

            Spacer(Modifier.height(20.dp))
            Text(
                text = track.title,
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = track.artist,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = track.album,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(Modifier.height(32.dp))

        Text(
            text = "WALLPAPER STYLE",
            style = MaterialTheme.typography.labelLarge
        )
        Spacer(Modifier.height(12.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(WallpaperStyle.entries) { style ->
                StyleCard(
                    style = style,
                    selected = state.selectedStyle == style,
                    onClick = { viewModel.selectStyle(style) }
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        Text(
            text = "PREVIEW",
            style = MaterialTheme.typography.labelLarge
        )
        Spacer(Modifier.height(12.dp))

        when {
            state.isGenerating -> {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(9f / 16f)
                ) {
                    Box(
                        Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }

            state.generatedWallpaper != null -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Image(
                        bitmap = state.generatedWallpaper!!.asImageBitmap(),
                        contentDescription = "Generated wallpaper",
                        modifier = Modifier.fillMaxWidth(),
                        contentScale = ContentScale.FillWidth
                    )
                }
            }

            else -> {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(9f / 16f),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Box(
                        Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Your wallpaper preview will appear here",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        state.generationError?.let { error ->
            Spacer(Modifier.height(8.dp))
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(Modifier.height(20.dp))

        Button(
            modifier = Modifier.fillMaxWidth(),
            enabled = track != null && !state.isGenerating,
            onClick = {
                viewModel.generateWallpaper(
                    width = wallpaperWidth,
                    height = wallpaperHeight
                )
            }
        ) {
            Text(
                if (state.isGenerating) "Generating..." else "Generate Wallpaper"
            )
        }

        Spacer(Modifier.height(32.dp))

        Card(Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Automatic Wallpaper",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = if (state.automaticWallpaper) {
                            "Automatically update wallpaper"
                        } else {
                            "Manual wallpaper generation"
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Switch(
                    checked = state.automaticWallpaper,
                    onCheckedChange = { viewModel.toggleAutomaticWallpaper() }
                )
            }
        }

        Spacer(Modifier.height(32.dp))
    }

    if (showSettings) {
        SettingsOverlay(
            state = state,
            onDismiss = { showSettings = false },
            onAutomaticWallpaperChanged = {
                viewModel.toggleAutomaticWallpaper()
            }
        )
    }
}

@Composable
private fun StyleCard(
    style: WallpaperStyle,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(150.dp)
            .height(100.dp)
            .clickable(onClick = onClick),
        border = if (selected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            null
        },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Box(
            Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = style.displayName,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (selected) "Selected" else "Tap to select",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun SettingsOverlay(
    state: HomeUiState,
    onDismiss: () -> Unit,
    onAutomaticWallpaperChanged: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(Modifier.padding(24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = "Done",
                        modifier = Modifier.clickable { onDismiss() },
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "Automatic Wallpaper",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Automatically update the wallpaper when music changes.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Switch(
                        checked = state.automaticWallpaper,
                        onCheckedChange = { onAutomaticWallpaperChanged() }
                    )
                }
            }
        }
    }
}
