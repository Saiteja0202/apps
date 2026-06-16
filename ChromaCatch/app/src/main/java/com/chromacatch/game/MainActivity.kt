package com.chromacatch.game

import android.opengl.GLSurfaceView
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { GameScreen() }
    }
}

@Composable
private fun GameScreen() {
    val activityContext = androidx.compose.ui.platform.LocalContext.current

    val renderer = remember { GameRenderer(activityContext) }
    val glView = remember {
        GLSurfaceView(activityContext).apply {
            setEGLContextClientVersion(3)
            setRenderer(renderer)
            renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        }
    }

    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e ->
            when (e) {
                Lifecycle.Event.ON_RESUME -> glView.onResume()
                Lifecycle.Event.ON_PAUSE -> glView.onPause()
                else -> {}
            }
        }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }

    var phase by remember { mutableIntStateOf(GameRenderer.START) }
    var score by remember { mutableIntStateOf(0) }
    var combo by remember { mutableIntStateOf(0) }
    var best by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos { }
            phase = renderer.phase; score = renderer.score; combo = renderer.combo; best = renderer.best
        }
    }

    var widthPx by remember { mutableIntStateOf(1) }
    Box(
        Modifier
            .fillMaxSize()
            .onSizeChanged { widthPx = it.width }
            .pointerInput(Unit) {
                detectTapGestures { o ->
                    val side = if (o.x < widthPx / 2f) -1 else 1
                    glView.queueEvent { renderer.onTap(side) }
                }
            }
    ) {
        AndroidView(factory = { glView }, modifier = Modifier.fillMaxSize())
        Hud(phase, score, combo, best)
    }
}

@Composable
private fun Hud(phase: Int, score: Int, combo: Int, best: Int) {
    Box(Modifier.fillMaxSize().padding(20.dp)) {
        // Best (always, top-left)
        Text("BEST  $best", color = Color(0xFF8A95C0), fontSize = 14.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.TopStart).padding(top = 8.dp))

        if (phase == GameRenderer.PLAY) {
            Column(Modifier.align(Alignment.TopCenter).padding(top = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$score", color = Color.White, fontSize = 64.sp, fontWeight = FontWeight.Black)
                if (combo >= 4) Text("x${1 + combo / 5}  ·  $combo combo", color = Color(0xFFFFC857), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            // left / right hint
            Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                HintPill("◀  prev color")
                HintPill("next color  ▶")
            }
        }

        if (phase == GameRenderer.START) {
            Overlay {
                Text("CHROMA", color = Color.White, fontSize = 52.sp, fontWeight = FontWeight.Black)
                Text("CATCH", color = Color(0xFF4D7CFF), fontSize = 52.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(18.dp))
                Dots()
                Spacer(Modifier.height(18.dp))
                Text("Match the cube's color to the falling gem\nbefore it lands.",
                    color = Color(0xFFB9C2E0), fontSize = 16.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Text("Tap LEFT or RIGHT to change color.",
                    color = Color(0xFFB9C2E0), fontSize = 16.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.height(28.dp))
                Text("TAP TO START", color = Color(0xFF19D3FF), fontSize = 22.sp, fontWeight = FontWeight.Black)
                if (best > 0) { Spacer(Modifier.height(10.dp)); Text("Best  $best", color = Color(0xFF8A95C0), fontSize = 15.sp) }
            }
        }

        if (phase == GameRenderer.OVER) {
            Overlay {
                Text("GAME OVER", color = Color(0xFFFF5C7A), fontSize = 40.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(20.dp))
                Text("$score", color = Color.White, fontSize = 72.sp, fontWeight = FontWeight.Black)
                Text("SCORE", color = Color(0xFF8A95C0), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                Text("Best  $best", color = Color(0xFFFFC857), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(28.dp))
                Text("TAP TO PLAY AGAIN", color = Color(0xFF19D3FF), fontSize = 20.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun Overlay(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Box(Modifier.fillMaxSize().background(Color(0xCC05060C)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, content = content)
    }
}

@Composable
private fun Dots() {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        listOf(Color(0xFFF2434F), Color(0xFFFFC833), Color(0xFF4DD17A), Color(0xFF4D8CFF)).forEach {
            Box(Modifier.size(22.dp).clip(CircleShape).background(it))
        }
    }
}

@Composable
private fun HintPill(text: String) {
    Box(Modifier.clip(RoundedCornerShape(20.dp)).background(Color(0x33FFFFFF)).padding(horizontal = 14.dp, vertical = 8.dp)) {
        Text(text, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}
