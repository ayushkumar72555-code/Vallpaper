package com.ayush.vallpaper.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ayush.vallpaper.widget.LyricsWidgetSettings

@Composable
fun LyricsWidgetSettingsPanel(
    settings: LyricsWidgetSettings,
    accent: Color,
    onChanged: (LyricsWidgetSettings) -> Unit
) {
    Text("LYRICS WIDGET", style = MaterialTheme.typography.labelLarge, color = accent)
    Spacer(Modifier.height(14.dp))

    Text("FONT SIZE", style = MaterialTheme.typography.labelLarge, color = accent)
    Spacer(Modifier.height(8.dp))
    SettingChoices(
        options = listOf("16sp", "19sp", "22sp", "26sp"),
        selected = settings.fontSizeSp.toInt().toString() + "sp",
        accent = accent
    ) { value ->
        onChanged(settings.copy(fontSizeSp = value.removeSuffix("sp").toFloat()))
    }

    Spacer(Modifier.height(16.dp))
    Text("FONT TYPE", style = MaterialTheme.typography.labelLarge, color = accent)
    Spacer(Modifier.height(8.dp))
    SettingChoices(
        options = listOf("Sans", "Serif", "Mono"),
        selected = when (settings.fontFamily) {
            LyricsWidgetSettings.FONT_SERIF -> "Serif"
            LyricsWidgetSettings.FONT_MONOSPACE -> "Mono"
            else -> "Sans"
        },
        accent = accent
    ) { value ->
        val family = when (value) {
            "Serif" -> LyricsWidgetSettings.FONT_SERIF
            "Mono" -> LyricsWidgetSettings.FONT_MONOSPACE
            else -> LyricsWidgetSettings.FONT_SANS
        }
        onChanged(settings.copy(fontFamily = family))
    }

    Spacer(Modifier.height(16.dp))
    Text("LYRIC TRANSITION", style = MaterialTheme.typography.labelLarge, color = accent)
    Spacer(Modifier.height(8.dp))
    SettingChoices(
        options = listOf("None", "Fade", "Slide"),
        selected = when (settings.transition) {
            LyricsWidgetSettings.TRANSITION_SLIDE -> "Slide"
            LyricsWidgetSettings.TRANSITION_NONE -> "None"
            else -> "Fade"
        },
        accent = accent
    ) { value ->
        val transition = when (value) {
            "Slide" -> LyricsWidgetSettings.TRANSITION_SLIDE
            "None" -> LyricsWidgetSettings.TRANSITION_NONE
            else -> LyricsWidgetSettings.TRANSITION_FADE
        }
        onChanged(settings.copy(transition = transition))
    }
}

@Composable
private fun SettingChoices(
    options: List<String>,
    selected: String,
    accent: Color,
    onSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            Card(
                modifier = Modifier.weight(1f).clickable { onSelected(option) },
                shape = RoundedCornerShape(3.dp),
                border = BorderStroke(1.dp, if (isSelected) accent else accent.copy(alpha = 0.35f)),
                colors = CardDefaults.cardColors(containerColor = if (isSelected) accent else Color.Transparent)
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(option, style = MaterialTheme.typography.labelMedium, color = if (isSelected) Color.Black else accent)
                }
            }
        }
    }
}