package com.friendschat.app.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.friendschat.app.R
import kotlinx.coroutines.delay

/** The 4K backdrop art bundled in res/drawable, in display order. */
val animeImages = listOf(
    R.drawable.anime1, R.drawable.anime2, R.drawable.anime3,
    R.drawable.anime4, R.drawable.anime5
)

/**
 * A slideshow of the anime images that gently cross-fades from one to the next
 * while each image slowly zooms and drifts (a "Ken Burns" effect) — so the
 * background always feels alive. Drop a scrim + content on top.
 */
@Composable
fun AnimeCarousel(
    modifier: Modifier = Modifier,
    images: List<Int> = animeImages,
    intervalMs: Long = 5000L
) {
    var index by remember { mutableIntStateOf(0) }
    LaunchedEffect(images) {
        while (true) {
            delay(intervalMs)
            index = (index + 1) % images.size
        }
    }
    Box(modifier.fillMaxSize()) {
        Crossfade(targetState = index, animationSpec = tween(1400), label = "anime") { i ->
            KenBurnsImage(images[i])
        }
    }
}

@Composable
private fun KenBurnsImage(resId: Int) {
    val transition = rememberInfiniteTransition(label = "kenburns")
    // Gentle zoom — the 4K source art stays crisp at any scale.
    val zoom by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(10000, easing = LinearEasing), RepeatMode.Reverse),
        label = "zoom"
    )
    val pan by transition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(tween(12000, easing = LinearEasing), RepeatMode.Reverse),
        label = "pan"
    )
    AsyncImage(
        model = resId,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        filterQuality = FilterQuality.High,
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                scaleX = zoom
                scaleY = zoom
                translationX = pan
            }
    )
}
