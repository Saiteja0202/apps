package com.friendschat.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

private val palette = listOf(
    Color(0xFF5A5CF0), Color(0xFF8B5CF0), Color(0xFFFF6B6B),
    Color(0xFF18B6A6), Color(0xFFF59E0B), Color(0xFF3CC8FF), Color(0xFFE05CC0)
)

/** Mood -> ring color. Empty mood = no ring. */
fun moodColor(mood: String): Color? = when (mood) {
    "celebrating" -> Color(0xFFFFC857)
    "heads_down" -> Color(0xFFFF6B6B)
    "bored" -> Color(0xFF8A95C0)
    "chilling" -> Color(0xFF18B6A6)
    "love" -> Color(0xFFE05CC0)
    else -> null
}

fun moodLabel(mood: String): String = when (mood) {
    "celebrating" -> "🎉 Celebrating"
    "heads_down" -> "🎯 Heads-down"
    "bored" -> "🥱 Bored"
    "chilling" -> "😎 Chilling"
    "love" -> "💖 In love"
    else -> "No mood"
}

/** Green = "live / free to chat right now". */
val LiveGreen = Color(0xFF2FD15B)

@Composable
fun Avatar(
    name: String,
    photoUrl: String,
    size: Dp = 48.dp,
    mood: String = "",
    live: Boolean = false,
    modifier: Modifier = Modifier
) {
    val ring = if (live) LiveGreen else moodColor(mood)
    val ringMod = if (ring != null) Modifier.border(2.5.dp, ring, CircleShape) else Modifier
    val base = modifier.size(size).then(ringMod).clip(CircleShape)

    if (photoUrl.isNotBlank()) {
        AsyncImage(
            model = photoUrl,
            contentDescription = name,
            contentScale = ContentScale.Crop,
            modifier = base
        )
    } else {
        val initial = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
        val c = palette[(name.hashCode().let { if (it < 0) -it else it }) % palette.size]
        Box(
            modifier = base.background(Brush.linearGradient(listOf(c, c.copy(alpha = 0.7f)))),
            contentAlignment = Alignment.Center
        ) {
            Text(initial, color = Color.White, fontWeight = FontWeight.Bold, fontSize = (size.value / 2.3f).sp)
        }
    }
}
