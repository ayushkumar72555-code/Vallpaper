package com.ayush.vallpaper.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.ayush.vallpaper.VallpaperApplication
import com.ayush.vallpaper.domain.model.WallpaperStyle
import com.ayush.vallpaper.ui.components.AlbumArtwork
import com.ayush.vallpaper.viewmodel.AppViewModelFactory
import com.ayush.vallpaper.viewmodel.HomeUiState
import com.ayush.vallpaper.viewmodel.HomeViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

private val FrostBlue = Color(0xFFBFE8FF)

@Composable
private fun FrostedCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val shape = RoundedCornerShape(20.dp)

    val surfaceColor = if (isDark) {
        Color.White.copy(alpha = 0.045f)
    } else {
        Color.White.copy(alpha = 0.62f)
    }

    val borderColor = if (isDark) {
        FrostBlue.copy(alpha = 0.20f)
    } else {
        FrostBlue.copy(alpha = 0.48f)
    }

    Box(
        modifier = modifier
            .shadow(
                elevation = if (isDark) 0.dp else 5.dp,
                shape = shape,
                clip = false
            )
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(shape)
        ) {
            Box(
                modifier = Modifier
                    .width(130.dp)
                    .height(130.dp)
                    .blur(34.dp)
                    .align(Alignment.TopStart)
                    .background(
                        FrostBlue.copy(alpha = if (isDark) 0.13f else 0.20f)
                    )
            )

            Box(
                modifier = Modifier
                    .width(100.dp)
                    .height(100.dp)
                    .blur(30.dp)
                    .align(Alignment.BottomEnd)
                    .background(
                        FrostBlue.copy(alpha = if (isDark) 0.07f else 0.14f)
                    )
            )

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(surfaceColor)
            )
        }

        Card(
            modifier = Modifier.fillMaxSize(),
            shape = shape,
            border = BorderStroke(1.dp, borderColor),
            colors = CardDefaults.cardColors(
                containerColor = Color.Transparent
            )
        ) {
            content()
        }
    }
}

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

            FrostedCard(modifier = Modifier.height(48.dp).width(48.dp)) {
                IconButton(onClick = { showSettings = true }) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings"
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = "NOW PLAYING",
            style = MaterialTheme.typography.labelLarge
        )
        Spacer(Modifier.height(12.dp))

        if (track == null) {
            FrostedCard(Modifier.fillMaxWidth()) {
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
            FrostedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
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

                    Spacer(Modifier.height(16.dp))
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
            }
        }

        Spacer(Modifier.height(32.dp))

        Text(
            text = "WALLPAPER STYLE",
            style = MaterialTheme.typography.labelLarge
        )
        Spacer(Modifier.height(12.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(WallpaperStyle.entries) { style ->
                StyleCard(
                    style = style,
                    selected = state.selectedStyle == style,
                    onClick = {
                        viewModel.selectStyleAndGenerate(
                            style = style,
                            width = wallpaperWidth,
                            height = wallpaperHeight
                        )
                    }
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
                FrostedCard(
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
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(
                        1.dp,
                        if (isSystemInDarkTheme()) {
                            FrostBlue.copy(alpha = 0.20f)
                        } else {
                            FrostBlue.copy(alpha = 0.48f)
                        }
                    )
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
                FrostedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(9f / 16f)
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

        Spacer(Modifier.height(32.dp))

        FrostedCard(Modifier.fillMaxWidth()) {
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
    val isDark = isSystemInDarkTheme()

    val glassColor = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.20f else 0.14f)
    } else if (isDark) {
        Color.White.copy(alpha = 0.045f)
    } else {
        Color.White.copy(alpha = 0.62f)
    }

    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.80f)
    } else {
        FrostBlue.copy(alpha = if (isDark) 0.20f else 0.48f)
    }

    Box(
        modifier = Modifier
            .width(150.dp)
            .height(100.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(18.dp))
        ) {
            Box(
                modifier = Modifier
                    .width(100.dp)
                    .height(100.dp)
                    .blur(30.dp)
                    .background(FrostBlue.copy(alpha = if (isDark) 0.10f else 0.17f))
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(glassColor)
            )
        }

        Card(
            modifier = Modifier.fillMaxSize(),
            border = BorderStroke(
                if (selected) 2.dp else 1.dp,
                borderColor
            ),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
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
}

@Composable
private fun SettingsOverlay(
    state: HomeUiState,
    onDismiss: () -> Unit,
    onAutomaticWallpaperChanged: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        FrostedCard(Modifier.fillMaxWidth()) {
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
