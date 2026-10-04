package com.ayush.vallpaper.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ayush.vallpaper.domain.model.WallpaperStyle

@Composable
fun WallpaperStyleCard(
    style: WallpaperStyle,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        border = if (selected) {
            BorderStroke(
                1.dp,
                androidx.compose.material3.MaterialTheme.colorScheme.primary
            )
        } else {
            null
        },
        colors = CardDefaults.cardColors(
            containerColor =
                androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant
        )
    ) {

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            Text(
                text = style.displayName,
                style =
                    androidx.compose.material3.MaterialTheme.typography.titleMedium
            )

            Text(
                text = when (style) {
                    WallpaperStyle.COVER ->
                        "Album artwork focused"

                    WallpaperStyle.AMBIENT ->
                        "Blurred atmospheric background"

                    WallpaperStyle.VINYL ->
                        "Vinyl inspired composition"

                    WallpaperStyle.MINIMAL ->
                        "Clean artwork and typography"
                },
                style =
                    androidx.compose.material3.MaterialTheme.typography.bodySmall
            )
        }
    }
}