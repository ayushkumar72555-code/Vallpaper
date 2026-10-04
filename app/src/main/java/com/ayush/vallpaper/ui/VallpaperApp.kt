package com.ayush.vallpaper.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ayush.vallpaper.ui.components.VallpaperBottomBar
import com.ayush.vallpaper.ui.navigation.AppNavigation

@Composable
fun VallpaperApp() {

    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            VallpaperBottomBar(
                currentRoute =
                    navController
                        .currentBackStackEntryAsState()
                        .value
                        ?.destination
                        ?.route,
                onNavigate = { route ->
                    navController.navigate(route) {
                        launchSingleTop = true
                    }
                }
            )
        }
    ) { padding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

            AppNavigation(
                navController = navController
            )
        }
    }
}