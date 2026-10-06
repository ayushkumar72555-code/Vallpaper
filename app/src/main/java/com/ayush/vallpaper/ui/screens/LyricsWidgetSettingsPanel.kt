package com.ayush.vallpaper.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
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

    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("SHOW SONG NAME", style = MaterialTheme.typography.titleSmall)
            Text("Keep the song title above the lyrics.", style = MaterialTheme.typography.bodySmall)
        }
        Switch(
            checked = settings.showSongName,
            onCheckedChange = { onChanged(settings.copy(showSongName = it)) }
        )
    }

    Spacer(Modifier.height(16.dp))
    Text("FONT SIZE", style = MaterialTheme.typography.labelLarge, color = accent)
    Spacer(Modifier.height(8.dp))
    SettingChoices(listOf("16sp", "19sp", "22sp", "26sp"), settings.fontSizeSp.toInt().toString() + "sp", accent) {
        onChanged(settings.copy(fontSizeSp = it.removeSuffix("sp").toFloat()))
    }

    Spacer(Modifier.height(16.dp))
    Text("FONT TYPE", style = MaterialTheme.typography.labelLarge, color = accent)
    Spacer(Modifier.height(8.dp))
    SettingChoices(
        listOf("Sans", "Serif", "Mono"),
        when (settings.fontFamily) {
            LyricsWidgetSettings.FONT_SERIF -> "Serif"
            LyricsWidgetSettings.FONT_MONOSPACE -> "Mono"
            else -> "Sans"
        },
        accent
    ) { value ->
        onChanged(settings.copy(fontFamily = when (value) {
            "Serif" -> LyricsWidgetSettings.FONT_SERIF
            "Mono" -> LyricsWidgetSettings.FONT_MONOSPACE
            else -> LyricsWidgetSettings.FONT_SANS
        }))
    }

    Spacer(Modifier.height(16.dp))
    Text("LYRIC TRANSITION", style = MaterialTheme.typography.labelLarge, color = accent)
    Spacer(Modifier.height(8.dp))
    SettingChoices(
        listOf("None", "Fade", "Slide"),
        when (settings.transition) {
            LyricsWidgetSettings.TRANSITION_SLIDE -> "Slide"
            LyricsWidgetSettings.TRANSITION_NONE -> "None"
            else -> "Fade"
        },
        accent
    ) { value ->
        onChanged(settings.copy(transition = when (value) {
            "Slide" -> LyricsWidgetSettings.TRANSITION_SLIDE
            "None" -> LyricsWidgetSettings.TRANSITION_NONE
            else -> LyricsWidgetSettings.TRANSITION_FADE
        }))
    }

    Spacer(Modifier.height(16.dp))
    Text("LYRIC COLOR", style = MaterialTheme.typography.labelLarge, color = accent)
    Spacer(Modifier.height(8.dp))
    ColorChoices(settings.lyricColorHex, accent) {
        onChanged(settings.copy(lyricColorHex = it))
    }
}

@Composable
private fun SettingChoices(
    options: List<String>,
    selected: String,
    accent: Color,
    onSelected: (String) -> Unit
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEach { option ->
            val selectedNow = option == selected
            Card(
                modifier = Modifier.weight(1f).clickable { onSelected(option) },
                shape = RoundedCornerShape(3.dp),
                border = BorderStroke(1.dp, if (selectedNow) accent else accent.copy(alpha = 0.35f)),
                colors = CardDefaults.cardColors(containerColor = if (selectedNow) accent else Color.Transparent)
            ) {
                Box(Modifier.fillMaxWidth().padding(vertical = 9.dp), contentAlignment = Alignment.Center) {
                    Text(option, style = MaterialTheme.typography.labelMedium, color = if (selectedNow) Color.Black else accent)
                }
            }
        }
    }
}

@Composable
private fun ColorChoices(selected: String, accent: Color, onSelected: (String) -> Unit) {
    val colors = listOf(
        "#FFFFFFFF" to Color.White,
        "#FF000000" to Color.Black,
        "#FFFF5252" to Color(0xFFFF5252),
        "#FFFF9800" to Color(0xFFFF9800),
        "#FFFFEB3B" to Color(0xFFFFEB3B),
        "#FF69F0AE" to Color(0xFF69F0AE),
        "#FF40C4FF" to Color(0xFF40C4FF),
        "#FF7C4DFF" to Color(0xFF7C4DFF),
        "#FFFF4081" to Color(0xFFFF4081)
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        colors.forEach { (hex, color) ->
            val selectedNow = hex.equals(selected, true)
            Card(
                modifier = Modifier.size(32.dp).clickable { onSelected(hex) },
                shape = RoundedCornerShape(50),
                border = BorderStroke(
                    if (selectedNow) 2.dp else 1.dp,
                    if (selectedNow) accent else Color.Gray.copy(alpha = 0.55f)
                ),
                colors = CardDefaults.cardColors(containerColor = color)
            ) {}
        }
    }
}
