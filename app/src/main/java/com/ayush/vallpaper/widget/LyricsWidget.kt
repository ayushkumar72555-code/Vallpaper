package com.ayush.vallpaper.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.text.style.TextAlign
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.dp
import androidx.glance.unit.sp
import com.ayush.vallpaper.MainActivity
import com.ayush.vallpaper.VallpaperApplication

class LyricsWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val initialState = LyricsWidgetStore.read(context)
        val stateFlow = LyricsWidgetStore.stateFlow(context)

        provideContent {
            val state by stateFlow.collectAsState(initial = initialState)
            LyricsWidgetContent(state)
        }
    }

    @androidx.compose.runtime.Composable
    private fun LyricsWidgetContent(state: LyricsWidgetState) {
        val background = ColorProvider(Color(0xFF101010))
        val orange = ColorProvider(Color(0xFFFF7A00))
        val primary = ColorProvider(Color(0xFFF5F5F5))
        val secondary = ColorProvider(Color(0xFF8F8F8F))

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(background)
                .padding(16.dp)
                .clickable(actionStartActivity<MainActivity>()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "VALLPAPER / LYRICS",
                    style = TextStyle(
                        color = orange,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    ),
                    maxLines = 1
                )
                Spacer(GlanceModifier.width(8.dp))
                Text(
                    text = if (state.currentIndex >= 0) "SYNCED" else "WAITING",
                    style = TextStyle(
                        color = secondary,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    ),
                    maxLines = 1
                )
            }

            Spacer(GlanceModifier.height(8.dp))

            if (state.title.isBlank()) {
                Text(
                    text = "PLAY MUSIC TO SHOW LYRICS",
                    style = TextStyle(
                        color = primary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    ),
                    maxLines = 2
                )
            } else {
                Text(
                    text = state.title,
                    style = TextStyle(
                        color = primary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1
                )
                Text(
                    text = state.artist,
                    style = TextStyle(
                        color = secondary,
                        fontSize = 10.sp
                    ),
                    maxLines = 1
                )

                Spacer(GlanceModifier.height(12.dp))

                Text(
                    text = state.previousLine.ifBlank { " " },
                    style = TextStyle(
                        color = secondary,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    ),
                    modifier = GlanceModifier.fillMaxWidth(),
                    maxLines = 1
                )

                Text(
                    text = state.currentLine.ifBlank { "Lyrics unavailable" },
                    style = TextStyle(
                        color = orange,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    ),
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    maxLines = 2
                )

                Text(
                    text = state.nextLine.ifBlank { " " },
                    style = TextStyle(
                        color = secondary,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    ),
                    modifier = GlanceModifier.fillMaxWidth(),
                    maxLines = 1
                )
            }
        }
    }
}

class LyricsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = LyricsWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        (context.applicationContext as? VallpaperApplication)
            ?.startLyricsWidgetSync()
    }

    override fun onDisabled(context: Context) {
        (context.applicationContext as? VallpaperApplication)
            ?.stopLyricsWidgetSync()
        super.onDisabled(context)
    }
}
