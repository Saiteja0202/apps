package com.friendschat.app.ui.onboarding

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.friendschat.app.ui.theme.auroraGradient

/**
 * A calm, minimal animated backdrop: a soft aurora gradient with a few large,
 * translucent colour "orbs" that drift slowly. No emoji, no clutter — just gentle
 * movement that keeps the onboarding flow feeling alive while staying readable.
 */
@Composable
fun AnimatedBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val c = MaterialTheme.colorScheme
    BoxWithConstraints(modifier.fillMaxSize().background(auroraGradient())) {
        val w = maxWidth
        val h = maxHeight
        Orb(c.primary, 260.dp, w * 0.05f, h * 0.06f, 7000, 30f)
        Orb(c.secondary, 200.dp, w * 0.62f, h * 0.16f, 9000, 26f)
        Orb(c.tertiary, 240.dp, w * 0.18f, h * 0.58f, 8200, 34f)
        Orb(c.primary, 180.dp, w * 0.66f, h * 0.72f, 10000, 24f)
        content()
    }
}

@Composable
private fun Orb(color: Color, size: Dp, baseX: Dp, baseY: Dp, durationMs: Int, drift: Float) {
    val t = rememberInfiniteTransition(label = "orb")
    val dx by t.animateFloat(
        initialValue = -1f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMs), RepeatMode.Reverse), label = "dx"
    )
    val dy by t.animateFloat(
        initialValue = 1f, targetValue = -1f,
        animationSpec = infiniteRepeatable(tween((durationMs * 1.3f).toInt()), RepeatMode.Reverse), label = "dy"
    )
    androidx.compose.foundation.layout.Box(
        Modifier
            .offset(x = baseX + (dx * drift).dp, y = baseY + (dy * drift).dp)
            .size(size)
            .background(
                Brush.radialGradient(listOf(color.copy(alpha = 0.26f), Color.Transparent)),
                CircleShape
            )
    )
}
