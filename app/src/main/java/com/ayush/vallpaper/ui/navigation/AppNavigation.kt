package com.ayush.vallpaper.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.ayush.vallpaper.ui.screens.HistoryScreen
import com.ayush.vallpaper.ui.screens.HomeScreen
import com.ayush.vallpaper.ui.screens.PreviewScreen
import com.ayush.vallpaper.ui.screens.SettingsScreen
import com.ayush.vallpaper.ui.screens.StylesScreen

object Routes {

    const val HOME = "home"
    const val STYLES = "styles"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
    const val PREVIEW = "preview"
}

@Composable
fun AppNavigation(
    navController: NavHostController
) {

    NavHost(
        navController = navController,
        startDestination = Routes.HOME
    ) {

        composable(Routes.HOME) {

            HomeScreen(
                onPreview = {
                    navController.navigate(
                        Routes.PREVIEW
                    )
                }
            )
        }

        composable(Routes.STYLES) {

            StylesScreen()
        }

        composable(Routes.HISTORY) {

            HistoryScreen()
        }

        composable(Routes.SETTINGS) {

            SettingsScreen()
        }

        composable(Routes.PREVIEW) {

            PreviewScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}