package com.ayush.vallpaper.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.ayush.vallpaper.ui.navigation.AppNavigation

@Composable
fun VallpaperApp() {

    val navController =
        rememberNavController()

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        AppNavigation(
            navController = navController
        )
    }
}