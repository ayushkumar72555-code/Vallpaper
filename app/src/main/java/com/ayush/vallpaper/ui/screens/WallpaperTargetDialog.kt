package com.ayush.vallpaper.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val RetroOrange = Color(0xFFFF8A00)

enum class WallpaperTarget {
    HOME,
    LOCK,
    BOTH
}

@androidx.compose.runtime.Composable
fun WallpaperTargetDialog(
    onDismiss: () -> Unit,
    onTargetSelected: (WallpaperTarget) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("APPLY WALLPAPER", color = MaterialTheme.colorScheme.onSurface)
        },
        text = {
            Column {
                Text(
                    "Choose where Vallpaper should apply the current wallpaper.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.padding(top = 8.dp))
                WallpaperTargetOption("HOME SCREEN", WallpaperTarget.HOME, onTargetSelected)
                WallpaperTargetOption("LOCK SCREEN", WallpaperTarget.LOCK, onTargetSelected)
                WallpaperTargetOption("BOTH", WallpaperTarget.BOTH, onTargetSelected)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = RetroOrange)
            }
        },
        shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@androidx.compose.runtime.Composable
private fun WallpaperTargetOption(
    label: String,
    target: WallpaperTarget,
    onTargetSelected: (WallpaperTarget) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onTargetSelected(target) }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        RadioButton(
            selected = false,
            onClick = { onTargetSelected(target) }
        )
        Spacer(Modifier.width(8.dp))
        Text(label, modifier = Modifier.padding(top = 12.dp))
    }
}
