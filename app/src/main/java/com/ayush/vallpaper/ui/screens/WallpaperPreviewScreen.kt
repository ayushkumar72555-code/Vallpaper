package com.ayush.vallpaper.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ayush.vallpaper.wallpaper.WallpaperApplier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun WallpaperPreviewScreen(
    bitmap: Bitmap,
    onBack: () -> Unit,
    onTargetSelected: (WallpaperTarget) -> Unit = {}
) {
    BackHandler(enabled = true, onBack = onBack)

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val accent = Color(0xFFFF8A00)

    var showTargetDialog by remember { mutableStateOf(false) }
    var isApplying by remember { mutableStateOf(false) }
    var resultMessage by remember { mutableStateOf<String?>(null) }
    var resultIsError by remember { mutableStateOf(false) }

    fun applyWallpaper(target: WallpaperTarget) {
        if (isApplying) return

        showTargetDialog = false
        isApplying = true
        resultMessage = null

        scope.launch {
            val result = withContext(Dispatchers.IO) {
                WallpaperApplier.apply(context, bitmap, target)
            }

            result.fold(
                onSuccess = {
                    isApplying = false
                    resultIsError = false
                    resultMessage = when (target) {
                        WallpaperTarget.HOME -> "HOME WALLPAPER APPLIED"
                        WallpaperTarget.LOCK -> "LOCK SCREEN WALLPAPER APPLIED"
                        WallpaperTarget.BOTH -> "HOME + LOCK WALLPAPER APPLIED"
                    }
                    onTargetSelected(target)
                },
                onFailure = { error ->
                    isApplying = false
                    resultIsError = true
                    resultMessage = when (error) {
                        is SecurityException -> "PERMISSION DENIED. COULD NOT APPLY WALLPAPER."
                        is java.io.IOException -> "COULD NOT WRITE WALLPAPER. TRY AGAIN."
                        else -> "COULD NOT APPLY WALLPAPER. TRY AGAIN."
                    }
                }
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 18.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, enabled = !isApplying) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Text(
                    text = "PREVIEW",
                    style = MaterialTheme.typography.titleMedium,
                    color = accent
                )
            }

            Spacer(Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Full screen wallpaper preview",
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(9f / 16f),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(Modifier.height(14.dp))

            if (isApplying) {
                CircularProgressIndicator(color = accent)
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "APPLYING WALLPAPER...",
                    style = MaterialTheme.typography.labelLarge,
                    color = accent
                )
            } else {
                Button(onClick = { showTargetDialog = true }) {
                    Text("APPLY WALLPAPER")
                }
            }

            Spacer(Modifier.height(8.dp))

            resultMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (resultIsError) MaterialTheme.colorScheme.error else accent
                )
                Spacer(Modifier.height(4.dp))
            }

            Text(
                text = if (isApplying) "PLEASE WAIT" else "CHOOSE HOME, LOCK SCREEN, OR BOTH",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.65f)
            )

            Spacer(Modifier.height(12.dp))
        }
    }

    if (showTargetDialog && !isApplying) {
        WallpaperTargetDialog(
            onDismiss = { showTargetDialog = false },
            onTargetSelected = ::applyWallpaper
        )
    }
}
