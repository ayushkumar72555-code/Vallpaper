package com.ayush.vallpaper.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ayush.vallpaper.ui.components.VallpaperBottomBar
import com.ayush.vallpaper.ui.navigation.AppNavigation

@Composable
fun VallpaperApp() {

    val navController =
        rememberNavController()

    val backStackEntry by
    navController.currentBackStackEntryAsState()

    val currentRoute =
        backStackEntry
            ?.destination
            ?.route

    Scaffold(

        bottomBar = {

            VallpaperBottomBar(
                currentRoute = currentRoute,
                onNavigate = { route ->

                    if (route != currentRoute) {

                        navController.navigate(route) {

                            launchSingleTop = true

                            popUpTo(
                                navController.graph.startDestinationId
                            ) {
                                saveState = true
                            }

                            restoreState = true
                        }
                    }
                }
            )
        }

    ) { paddingValues ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

            AppNavigation(
                navController = navController
            )
        }
    }
}