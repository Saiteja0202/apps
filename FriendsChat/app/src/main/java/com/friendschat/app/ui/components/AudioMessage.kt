package com.friendschat.app.ui.components

import android.media.MediaPlayer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.clickable
import androidx.compose.ui.unit.dp
import java.util.Locale

@Composable
fun AudioMessage(url: String, durationMs: Long, tint: Color, subColor: Color) {
    var playing by remember { mutableStateOf(false) }
    var prepared by remember { mutableStateOf(false) }
    val player = remember { MediaPlayer() }

    DisposableEffect(Unit) {
        onDispose { runCatching { player.release() } }
    }

    Row(
        Modifier.widthIn(min = 160.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
            contentDescription = if (playing) "Pause" else "Play",
            tint = tint,
            modifier = Modifier
                .size(34.dp)
                .clickable {
                    runCatching {
                        if (playing) {
                            player.pause(); playing = false
                        } else {
                            if (!prepared) {
                                player.setDataSource(url)
                                player.setOnPreparedListener { it.start(); playing = true }
                                player.setOnCompletionListener { playing = false }
                                player.prepareAsync()
                                prepared = true
                            } else {
                                player.start(); playing = true
                            }
                        }
                    }
                }
        )
        Spacer(Modifier.width(8.dp))
        Icon(Icons.Rounded.GraphicEq, contentDescription = null, tint = tint.copy(alpha = 0.7f), modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(8.dp))
        Text(formatDuration(durationMs), color = subColor)
    }
}

private fun formatDuration(ms: Long): String {
    val totalSec = (ms / 1000).toInt()
    return String.format(Locale.US, "%d:%02d", totalSec / 60, totalSec % 60)
}
