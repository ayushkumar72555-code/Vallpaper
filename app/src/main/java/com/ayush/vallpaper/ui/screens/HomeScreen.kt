package com.ayush.vallpaper.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ayush.vallpaper.VallpaperApplication
import com.ayush.vallpaper.domain.model.WallpaperStyle
import com.ayush.vallpaper.ui.components.AlbumArtwork
import com.ayush.vallpaper.viewmodel.AppViewModelFactory
import com.ayush.vallpaper.viewmodel.HomeViewModel

@Composable
fun HomeScreen(
    onPreview: () -> Unit
) {

    val context = LocalContext.current

    val application =
        context.applicationContext
                as VallpaperApplication

    val viewModel: HomeViewModel =
        viewModel(
            factory =
                AppViewModelFactory(
                    application
                )
        )

    val state by
    viewModel.uiState
        .collectAsState()

    val track =
        state.track

    var showSettings by
    remember {
        mutableStateOf(false)
    }

    val displayMetrics =
        context.resources.displayMetrics

    val wallpaperWidth =
        displayMetrics.widthPixels
            .coerceAtLeast(1)

    val wallpaperHeight =
        displayMetrics.heightPixels
            .coerceAtLeast(1)

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(20.dp)
    ) {

        /*
         * --------------------------------------------------------
         * HEADER
         * --------------------------------------------------------
         */

        Row(
            modifier =
                Modifier.fillMaxWidth(),
            verticalAlignment =
                Alignment.CenterVertically,
            horizontalArrangement =
                Arrangement.SpaceBetween
        ) {

            Text(
                text = "Vallpaper",
                style =
                    MaterialTheme
                        .typography
                        .headlineMedium
            )

            IconButton(
                onClick = {
                    showSettings = true
                }
            ) {

                Icon(
                    imageVector =
                        Icons.Default.Settings,
                    contentDescription =
                        "Settings"
                )
            }
        }

        Spacer(
            modifier =
                Modifier.height(24.dp)
        )

        /*
         * --------------------------------------------------------
         * CURRENTLY PLAYING
         * --------------------------------------------------------
         */

        Text(
            text = "NOW PLAYING",
            style =
                MaterialTheme
                    .typography
                    .labelLarge
        )

        Spacer(
            modifier =
                Modifier.height(12.dp)
        )

        if (track == null) {

            Card(
                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Column(
                    modifier =
                        Modifier.padding(20.dp)
                ) {

                    Text(
                        text =
                            "No music detected",
                        style =
                            MaterialTheme
                                .typography
                                .headlineSmall
                    )

                    Spacer(
                        modifier =
                            Modifier.height(8.dp)
                    )

                    Text(
                        text =
                            "Allow Vallpaper to access active media sessions so it can detect the song currently playing on your device.",
                        style =
                            MaterialTheme
                                .typography
                                .bodyMedium
                    )

                    Spacer(
                        modifier =
                            Modifier.height(16.dp)
                    )

                    Button(
                        modifier =
                            Modifier.fillMaxWidth(),
                        onClick = {

                            context.startActivity(
                                Intent(
                                    Settings
                                        .ACTION_NOTIFICATION_LISTENER_SETTINGS
                                )
                            )
                        }
                    ) {

                        Text(
                            text =
                                "Allow Music Access"
                        )
                    }
                }
            }

        } else {

            Box(
                modifier =
                    Modifier.fillMaxWidth(),
                contentAlignment =
                    Alignment.Center
            ) {

                AlbumArtwork(
                    imageUrl =
                        track.artworkUrl,

                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .widthIn(
                                max = 340.dp
                            )
                )
            }

            Spacer(
                modifier =
                    Modifier.height(20.dp)
            )

            Text(
                text =
                    track.title,
                style =
                    MaterialTheme
                        .typography
                        .headlineSmall
            )

            Spacer(
                modifier =
                    Modifier.height(4.dp)
            )

            Text(
                text =
                    track.artist,
                style =
                    MaterialTheme
                        .typography
                        .bodyLarge
            )

            Text(
                text =
                    track.album,
                style =
                    MaterialTheme
                        .typography
                        .bodyMedium
            )
        }

        Spacer(
            modifier =
                Modifier.height(32.dp)
        )

        /*
         * --------------------------------------------------------
         * WALLPAPER STYLE
         * --------------------------------------------------------
         */

        Text(
            text =
                "WALLPAPER STYLE",
            style =
                MaterialTheme
                    .typography
                    .labelLarge
        )

        Spacer(
            modifier =
                Modifier.height(12.dp)
        )

        LazyRow(
            horizontalArrangement =
                Arrangement.spacedBy(12.dp)
        ) {

            items(
                WallpaperStyle.entries
            ) { style ->

                StyleCard(
                    style = style,
                    selected =
                        state.selectedStyle == style,
                    onClick = {

                        viewModel.selectStyle(
                            style
                        )
                    }
                )
            }
        }

        Spacer(
            modifier =
                Modifier.height(32.dp)
        )

        /*
         * --------------------------------------------------------
         * PREVIEW
         * --------------------------------------------------------
         */

        Text(
            text =
                "PREVIEW",
            style =
                MaterialTheme
                    .typography
                    .labelLarge
        )

        Spacer(
            modifier =
                Modifier.height(12.dp)
        )

        if (
            state.isGenerating
        ) {

            Card(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(
                            9f / 16f
                        )
            ) {

                Box(
                    modifier =
                        Modifier.fillMaxSize(),
                    contentAlignment =
                        Alignment.Center
                ) {

                    CircularProgressIndicator()
                }
            }

        } else if (
            state.generatedWallpaper != null
        ) {

            Card(
                modifier =
                    Modifier.fillMaxWidth(),
                shape =
                    RoundedCornerShape(20.dp)
            ) {

                Image(
                    bitmap =
                        state
                            .generatedWallpaper!!
                            .asImageBitmap(),

                    contentDescription =
                        "Generated wallpaper",

                    modifier =
                        Modifier
                            .fillMaxWidth(),

                    contentScale =
                        ContentScale.FillWidth
                )
            }

        } else {

            Card(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(
                            9f / 16f
                        ),
                colors =
                    CardDefaults
                        .cardColors(
                            containerColor =
                                MaterialTheme
                                    .colorScheme
                                    .surfaceVariant
                        )
            ) {

                Box(
                    modifier =
                        Modifier.fillMaxSize(),
                    contentAlignment =
                        Alignment.Center
                ) {

                    Text(
                        text =
                            "Your wallpaper preview will appear here",
                        style =
                            MaterialTheme
                                .typography
                                .bodyMedium
                    )
                }
            }
        }

        if (
            state.generationError != null
        ) {

            Spacer(
                modifier =
                    Modifier.height(8.dp)
            )

            Text(
                text =
                    state.generationError!!,
                color =
                    MaterialTheme
                        .colorScheme
                        .error,
                style =
                    MaterialTheme
                        .typography
                        .bodyMedium
            )
        }

        Spacer(
            modifier =
                Modifier.height(20.dp)
        )

        /*
         * --------------------------------------------------------
         * GENERATE BUTTON
         * --------------------------------------------------------
         */

        Button(
            modifier =
                Modifier.fillMaxWidth(),

            enabled =
                track != null &&
                        !state.isGenerating,

            onClick = {

                viewModel.generateWallpaper(
                    width =
                        wallpaperWidth,
                    height =
                        wallpaperHeight
                )
            }
        ) {

            Text(
                text =
                    if (state.isGenerating) {
                        "Generating..."
                    } else {
                        "Generate Wallpaper"
                    }
            )
        }

        Spacer(
            modifier =
                Modifier.height(12.dp)
        )

        /*
         * --------------------------------------------------------
         * PREVIEW SCREEN
         * --------------------------------------------------------
         *
         * Kept temporarily while we migrate the old navigation.
         */

        Button(
            modifier =
                Modifier.fillMaxWidth(),

            enabled =
                track != null,

            onClick =
                onPreview
        ) {

            Text(
                text =
                    "Open Full Preview"
            )
        }

        Spacer(
            modifier =
                Modifier.height(32.dp)
        )

        /*
         * --------------------------------------------------------
         * AUTOMATIC WALLPAPER
         * --------------------------------------------------------
         */

        Card(
            modifier =
                Modifier.fillMaxWidth()
        ) {

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),

                verticalAlignment =
                    Alignment.CenterVertically,

                horizontalArrangement =
                    Arrangement.SpaceBetween
            ) {

                Column(
                    modifier =
                        Modifier.weight(1f)
                ) {

                    Text(
                        text =
                            "Automatic Wallpaper",
                        style =
                            MaterialTheme
                                .typography
                                .titleMedium
                    )

                    Spacer(
                        modifier =
                            Modifier.height(2.dp)
                    )

                    Text(
                        text =
                            if (
                                state.automaticWallpaper
                            ) {
                                "Automatically update wallpaper"
                            } else {
                                "Manual wallpaper generation"
                            },

                        style =
                            MaterialTheme
                                .typography
                                .bodySmall
                    )
                }

                Switch(
                    checked =
                        state.automaticWallpaper,

                    onCheckedChange = {
                        viewModel
                            .toggleAutomaticWallpaper()
                    }
                )
            }
        }

        Spacer(
            modifier =
                Modifier.height(32.dp)
        )
    }

    /*
     * ------------------------------------------------------------
     * SETTINGS OVERLAY
     * ------------------------------------------------------------
     */

    if (showSettings) {

        SettingsOverlay(
            state = state,
            onDismiss = {
                showSettings = false
            },
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

    val border =
        if (selected) {
            BorderStroke(
                width = 2.dp,
                color =
                    MaterialTheme
                        .colorScheme
                        .primary
            )
        } else {
            null
        }

    Card(
        modifier =
            Modifier
                .width(150.dp)
                .height(100.dp)
                .clickable(
                    onClick = onClick
                ),

        border =
            border,

        shape =
            RoundedCornerShape(16.dp),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (selected) {
                        MaterialTheme
                            .colorScheme
                            .primaryContainer
                    } else {
                        MaterialTheme
                            .colorScheme
                            .surfaceVariant
                    }
            )
    ) {

        Box(
            modifier =
                Modifier.fillMaxSize(),
            contentAlignment =
                Alignment.Center
        ) {

            Column(
                horizontalAlignment =
                    Alignment.CenterHorizontally
            ) {

                Text(
                    text =
                        style.displayName,

                    style =
                        MaterialTheme
                            .typography
                            .titleMedium
                )

                Spacer(
                    modifier =
                        Modifier.height(4.dp)
                )

                Text(
                    text =
                        if (selected) {
                            "Selected"
                        } else {
                            "Tap to select"
                        },

                    style =
                        MaterialTheme
                            .typography
                            .bodySmall
                )
            }
        }
    }
}


@Composable
private fun SettingsOverlay(
    state: com.ayush.vallpaper.viewmodel.HomeUiState,
    onDismiss: () -> Unit,
    onAutomaticWallpaperChanged: () -> Unit
) {

    androidx.compose.ui.window.Dialog(
        onDismissRequest =
            onDismiss
    ) {

        Card(
            modifier =
                Modifier.fillMaxWidth(),
            shape =
                RoundedCornerShape(24.dp)
        ) {

            Column(
                modifier =
                    Modifier.padding(24.dp)
            ) {

                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.SpaceBetween,
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Text(
                        text =
                            "Settings",

                        style =
                            MaterialTheme
                                .typography
                                .headlineSmall
                    )

                    Text(
                        text =
                            "Done",

                        modifier =
                            Modifier.clickable {
                                onDismiss()
                            },

                        color =
                            MaterialTheme
                                .colorScheme
                                .primary
                    )
                }

                Spacer(
                    modifier =
                        Modifier.height(24.dp)
                )

                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.SpaceBetween,
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Column(
                        modifier =
                            Modifier.weight(1f)
                    ) {

                        Text(
                            text =
                                "Automatic Wallpaper",

                            style =
                                MaterialTheme
                                    .typography
                                    .titleMedium
                        )

                        Text(
                            text =
                                "Automatically update the wallpaper when music changes.",

                            style =
                                MaterialTheme
                                    .typography
                                    .bodySmall
                        )
                    }

                    Switch(
                        checked =
                            state.automaticWallpaper,

                        onCheckedChange = {
                            onAutomaticWallpaperChanged()
                        }
                    )
                }

                Spacer(
                    modifier =
                        Modifier.height(20.dp)
                )

                Text(
                    text =
                        "Selected style: ${state.selectedStyle.displayName}",

                    style =
                        MaterialTheme
                            .typography
                            .bodyMedium
                )
            }
        }
    }
}