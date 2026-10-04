package com.ayush.vallpaper.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ayush.vallpaper.data.AppSettingsRepository
import com.ayush.vallpaper.data.MockTrackRepository
import com.ayush.vallpaper.domain.model.WallpaperStyle
import com.ayush.vallpaper.ui.components.WallpaperStyleCard
import com.ayush.vallpaper.viewmodel.HomeViewModel
import com.ayush.vallpaper.viewmodel.HomeViewModelFactory

@Composable
fun StylesScreen() {

    val trackRepository = remember {
        MockTrackRepository()
    }

    val settingsRepository = remember {
        AppSettingsRepository()
    }

    val viewModel: HomeViewModel = viewModel(
        factory = HomeViewModelFactory(
            trackRepository = trackRepository,
            settingsRepository = settingsRepository
        )
    )

    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {

        Text(
            text = "Styles",
            style = MaterialTheme.typography.headlineMedium
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(top = 24.dp)
        ) {

            items(WallpaperStyle.entries) { style ->

                WallpaperStyleCard(
                    style = style,
                    selected = state.selectedStyle == style,
                    onClick = {
                        viewModel.selectStyle(style)
                    }
                )
            }
        }
    }
}
