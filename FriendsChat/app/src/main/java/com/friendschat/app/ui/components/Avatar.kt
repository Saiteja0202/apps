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
    Color(0xFFC2603F), Color(0xFF6F8061), Color(0xFFCC9A3D),
    Color(0xFFB07A56), Color(0xFF9A6A4E), Color(0xFF7E8A66), Color(0xFFC78A5A)
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

/** Leaf-green = "live / free to chat right now" (warmed to fit the paper theme). */
val LiveGreen = Color(0xFF5C9A4F)

@Composable
fun Avatar(
    name: String,
    photoUrl: String,
    size: Dp = 48.dp,
    mood: String = "",
    live: Boolean = false,
    online: Boolean = false,
    modifier: Modifier = Modifier
) {
    val ring = if (live) LiveGreen else moodColor(mood)
    val ringMod = if (ring != null) Modifier.border(2.5.dp, ring, CircleShape) else Modifier
    val base = Modifier.size(size).then(ringMod).clip(CircleShape)

    Box(modifier) {
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
        if (online) OnlineDot(size, Modifier.align(Alignment.BottomEnd))
    }
}

/** WhatsApp-style online status: a green dot with a soft glow halo + white cutout. */
@Composable
private fun OnlineDot(avatarSize: Dp, modifier: Modifier = Modifier) {
    val dot = (avatarSize.value * 0.30f).dp
    val halo = (avatarSize.value * 0.50f).dp
    Box(modifier.size(halo), contentAlignment = Alignment.Center) {
        Box(Modifier.size(halo).clip(CircleShape).background(LiveGreen.copy(alpha = 0.30f)))
        Box(
            Modifier.size(dot).clip(CircleShape)
                .background(Color.White)
                .border((avatarSize.value * 0.02f).dp, Color.White, CircleShape)
        )
        Box(Modifier.size(dot * 0.74f).clip(CircleShape).background(LiveGreen))
    }
}
