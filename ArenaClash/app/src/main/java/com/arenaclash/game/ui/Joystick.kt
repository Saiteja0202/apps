package com.arenaclash.game.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/** A draggable virtual joystick. Reports a normalized vector (x right+, y down+). */
@Composable
fun Joystick(accent: Color, onVector: (Float, Float) -> Unit, modifier: Modifier = Modifier) {
    val sizeDp = 160.dp
    val radiusPx = with(LocalDensity.current) { (sizeDp / 2).toPx() }
    var knob by remember { mutableStateOf(Offset.Zero) }

    Canvas(
        modifier
            .size(sizeDp)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = { knob = Offset.Zero; onVector(0f, 0f) },
                    onDragCancel = { knob = Offset.Zero; onVector(0f, 0f) },
                    onDrag = { change, amount ->
                        change.consume()
                        var n = knob + amount
                        val m = n.getDistance()
                        if (m > radiusPx) n *= (radiusPx / m)
                        knob = n
                        onVector(n.x / radiusPx, n.y / radiusPx)
                    }
                )
            }
    ) {
        drawCircle(Color(0x22FFFFFF), radius = size.minDimension / 2f)
        drawCircle(accent.copy(alpha = 0.35f), radius = size.minDimension / 2f, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f))
        drawCircle(accent.copy(alpha = 0.85f), radius = size.minDimension / 5.5f, center = center + knob)
    }
}
