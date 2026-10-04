package com.ayush.vallpaper.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
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

private val RetroOrange = Color(0xFFFF7A00)
private val RetroOrangeDark = Color(0xFFFF8A00)

@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val application = context.applicationContext as VallpaperApplication
    val viewModel: HomeViewModel = viewModel(factory = AppViewModelFactory(application))
    val state by viewModel.uiState.collectAsState()
    val track = state.track
    var showSettings by remember { mutableStateOf(false) }

    val metrics = context.resources.displayMetrics
    val wallpaperWidth = metrics.widthPixels.coerceAtLeast(1)
    val wallpaperHeight = metrics.heightPixels.coerceAtLeast(1)
    val accent = if (androidx.compose.foundation.isSystemInDarkTheme()) RetroOrangeDark else RetroOrange

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    "VALLPAPER",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    "VINYL / VISUALS",
                    style = MaterialTheme.typography.labelSmall,
                    color = accent
                )
            }
            IconButton(onClick = { showSettings = true }) {
                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = accent)
            }
        }

        Spacer(Modifier.height(28.dp))
        RetroSectionLabel("NOW PLAYING", accent)
        Spacer(Modifier.height(10.dp))

        if (track == null) {
            RetroPanel(accent) {
                Column(Modifier.padding(18.dp)) {
                    Text("NO MUSIC DETECTED", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Text("Allow Vallpaper to access active media sessions.", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(14.dp))
                    Button(onClick = {
                        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    }) {
                        Text("ALLOW MUSIC ACCESS")
                    }
                }
            }
        } else {
            RetroPanel(accent) {
                Column(Modifier.padding(14.dp)) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        AlbumArtwork(
                            imageUrl = track.artworkUrl,
                            modifier = Modifier.fillMaxWidth().widthIn(max = 340.dp)
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(track.title, style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(3.dp))
                    Text(track.artist, style = MaterialTheme.typography.bodyLarge, color = accent)
                    Text(track.album, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        Spacer(Modifier.height(28.dp))
        RetroSectionLabel("STYLE", accent)
        Spacer(Modifier.height(10.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(WallpaperStyle.entries) { style ->
                RetroStyleCard(
                    style = style,
                    selected = state.selectedStyle == style,
                    accent = accent,
                    onClick = {
                        viewModel.selectStyleAndGenerate(style, wallpaperWidth, wallpaperHeight)
                    }
                )
            }
        }

        Spacer(Modifier.height(28.dp))
        RetroSectionLabel("PREVIEW", accent)
        Spacer(Modifier.height(10.dp))

        when {
            state.isGenerating -> {
                RetroPanel(accent, Modifier.fillMaxWidth().aspectRatio(9f / 16f)) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = accent)
                    }
                }
            }
            state.generatedWallpaper != null -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(3.dp),
                    border = BorderStroke(1.dp, accent),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
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
                RetroPanel(accent, Modifier.fillMaxWidth().aspectRatio(9f / 16f)) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("SELECT A STYLE", color = accent, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }

        state.generationError?.let { error ->
            Spacer(Modifier.height(8.dp))
            Text(error, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(26.dp))
        RetroPanel(accent) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(15.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(Modifier.weight(1f)) {
                    Text("AUTO WALLPAPER", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        if (state.automaticWallpaper) "UPDATES WITH MUSIC" else "MANUAL MODE",
                        style = MaterialTheme.typography.bodySmall,
                        color = accent
                    )
                }
                Switch(
                    checked = state.automaticWallpaper,
                    onCheckedChange = { viewModel.toggleAutomaticWallpaper() }
                )
            }
        }

        Spacer(Modifier.height(24.dp))
    }

    if (showSettings) {
        SettingsOverlay(
            state = state,
            accent = accent,
            onDismiss = { showSettings = false },
            onAutomaticWallpaperChanged = { viewModel.toggleAutomaticWallpaper() }
        )
    }
}

@Composable
private fun RetroSectionLabel(text: String, accent: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(4.dp).height(16.dp).background(accent))
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.labelLarge, color = accent)
    }
}

@Composable
private fun RetroPanel(
    accent: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(3.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.60f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        content()
    }
}

@Composable
private fun RetroStyleCard(
    style: WallpaperStyle,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit
) {
    val background = if (selected) accent else MaterialTheme.colorScheme.surface
    val foreground = if (selected) Color.Black else MaterialTheme.colorScheme.onSurface

    Card(
        modifier = Modifier.width(132.dp).height(78.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(3.dp),
        border = BorderStroke(1.dp, if (selected) accent else accent.copy(alpha = 0.45f)),
        colors = CardDefaults.cardColors(containerColor = background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(style.displayName.uppercase(), style = MaterialTheme.typography.titleSmall, color = foreground)
            Spacer(Modifier.height(3.dp))
            Text(
                if (selected) "ACTIVE" else "SELECT",
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) Color.Black.copy(alpha = 0.70f) else accent
            )
        }
    }
}

@Composable
private fun SettingsOverlay(
    state: HomeUiState,
    accent: Color,
    onDismiss: () -> Unit,
    onAutomaticWallpaperChanged: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        RetroPanel(accent) {
            Column(Modifier.padding(22.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("SETTINGS", style = MaterialTheme.typography.headlineSmall)
                    Text("DONE", modifier = Modifier.clickable { onDismiss() }, color = accent)
                }
                Spacer(Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("AUTO WALLPAPER", style = MaterialTheme.typography.titleMedium)
                        Text("Automatically update when music changes.", style = MaterialTheme.typography.bodySmall)
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
