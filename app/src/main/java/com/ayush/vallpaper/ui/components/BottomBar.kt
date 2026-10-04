package com.ayush.vallpaper.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

data class BottomDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
)

@Composable
fun VallpaperBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {

    val destinations = listOf(
        BottomDestination(
            "home",
            "Home",
            Icons.Default.Home
        ),
        BottomDestination(
            "styles",
            "Styles",
            Icons.Default.Style
        ),
        BottomDestination(
            "history",
            "History",
            Icons.Default.History
        ),
        BottomDestination(
            "settings",
            "Settings",
            Icons.Default.Settings
        )
    )

    NavigationBar {

        destinations.forEach { destination ->

            NavigationBarItem(
                selected =
                    currentRoute == destination.route,
                onClick = {
                    onNavigate(destination.route)
                },
                icon = {
                    Icon(
                        destination.icon,
                        contentDescription =
                            destination.label
                    )
                },
                label = {
                    Text(destination.label)
                }
            )
        }
    }
}