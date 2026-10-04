package com.ayush.vallpaper.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen() {

    var automatic by rememberSaveable {
        mutableStateOf(true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {

        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(
            modifier = Modifier.height(32.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.SpaceBetween
        ) {

            Column {

                Text(
                    text = "Automatic Wallpaper",
                    style =
                        MaterialTheme.typography.titleMedium
                )

                Text(
                    text = "Change wallpaper when the track changes",
                    style =
                        MaterialTheme.typography.bodySmall
                )
            }

            Switch(
                checked = automatic,
                onCheckedChange = {
                    automatic = it
                }
            )
        }

        Spacer(
            modifier = Modifier.height(28.dp)
        )

        Text(
            text = "Apply To",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        Text("Home Screen")

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        Text("Lock Screen")

        Spacer(
            modifier = Modifier.height(28.dp)
        )

        Text(
            text = "Vallpaper",
            style = MaterialTheme.typography.titleMedium
        )

        Text(
            text = "Version 0.1.0",
            style = MaterialTheme.typography.bodySmall
        )
    }
}