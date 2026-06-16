package com.arenaclash.game

import android.opengl.GLSurfaceView
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.arenaclash.game.core.GameController
import com.arenaclash.game.core.RenderSnapshot
import com.arenaclash.game.gl.ArenaRenderer
import com.arenaclash.game.net.IpUtil
import com.arenaclash.game.sim.GameWorld
import com.arenaclash.game.ui.Joystick

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { App() }
    }
}

private val BG = Color(0xFF0A0E14)
private val PANEL = Color(0xFF141B28)
private val ACCENT = Color(0xFF36D399)
private val ACCENT2 = Color(0xFF4D8CFF)
private val GOLD = Color(0xFFFFC857)
private val MUTED = Color(0xFF8A95C0)

private val MINI_COLORS = listOf(
    Color(0xFF4D8CFF), Color(0xFFF24452), Color(0xFF4DD17A), Color(0xFFFFC833),
    Color(0xFFC766FF), Color(0xFF33D9D9), Color(0xFFFF8C33), Color(0xFFF272BF),
    Color(0xFF99D94D), Color(0xFFB3B3BF)
)

@Composable
private fun App() {
    val ctx = LocalContext.current
    val controller = remember { GameController() }
    val renderer = remember { ArenaRenderer { controller.snapshot } }
    val glView = remember {
        GLSurfaceView(ctx).apply {
            setEGLContextClientVersion(3); setRenderer(renderer); renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        }
    }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e ->
            when (e) { Lifecycle.Event.ON_RESUME -> glView.onResume(); Lifecycle.Event.ON_PAUSE -> glView.onPause(); else -> {} }
        }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs); controller.stop() }
    }

    var screen by remember { mutableStateOf("menu") }
    var name by remember { mutableStateOf("Player") }
    var ip by remember { mutableStateOf("") }

    when (screen) {
        "menu" -> MenuScreen(name, { name = it }, ip, { ip = it },
            onBots = { controller.startOffline(name.ifBlank { "Player" }, 5); screen = "play" },
            onHost = { controller.startHost(name.ifBlank { "Host" }, 4); screen = "lobbyHost" },
            onJoin = { if (ip.isNotBlank()) { controller.startClient(ip.trim(), name.ifBlank { "Player" }); screen = "lobbyClient" } })
        "lobbyHost" -> LobbyHostScreen(remember { IpUtil.localIp(ctx) }, controller,
            onStart = { controller.hostStartMatch(); screen = "play" }, onBack = { controller.stop(); screen = "menu" })
        "lobbyClient" -> LobbyClientScreen(controller, onPlay = { screen = "play" }, onBack = { controller.stop(); screen = "menu" })
        "play" -> PlayScreen(controller, glView) { controller.stop(); screen = "menu" }
    }
}

@Composable
private fun MenuScreen(name: String, onName: (String) -> Unit, ip: String, onIp: (String) -> Unit,
                       onBots: () -> Unit, onHost: () -> Unit, onJoin: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF0E1626), BG))), contentAlignment = Alignment.Center) {
        Column(
            Modifier.width(560.dp).clip(RoundedCornerShape(24.dp)).background(PANEL).padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("ARENA ", color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.Black)
                Text("CLASH", color = ACCENT, fontSize = 40.sp, fontWeight = FontWeight.Black)
            }
            Text("3D arena battle · up to 10 players · last one standing", color = MUTED, fontSize = 13.sp)
            Spacer(Modifier.height(20.dp))
            OutlinedTextField(name, onName, label = { Text("Your name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(18.dp))

            BigButton("▶  PLAY vs BOTS", ACCENT, Modifier.fillMaxWidth(), onBots)
            Spacer(Modifier.height(10.dp))
            Text("— or play with friends on the same Wi-Fi / hotspot —", color = MUTED, fontSize = 12.sp)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BigButton("HOST", ACCENT2, Modifier.weight(1f), onHost)
                OutlinedTextField(ip, onIp, label = { Text("Host IP") }, singleLine = true, modifier = Modifier.weight(1.4f))
                BigButton("JOIN", GOLD, Modifier.weight(1f), onJoin)
            }

            Spacer(Modifier.height(20.dp))
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color(0xFF0E1422)).padding(14.dp)) {
                Text("HOW TO PLAY", color = ACCENT, fontSize = 12.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(6.dp))
                Info("• Left stick = move,  Right stick = aim & auto-fire")
                Info("• Grab green health packs to heal")
                Info("• Stay inside the shrinking blue ring")
                Info("• Last hunter standing wins")
            }
        }
    }
}

@Composable
private fun Info(t: String) = Text(t, color = Color(0xFFC9D2EA), fontSize = 13.sp)

@Composable
private fun LobbyHostScreen(ip: String, controller: GameController, onStart: () -> Unit, onBack: () -> Unit) {
    var names by remember { mutableStateOf(listOf<String>()) }
    LaunchedEffect(Unit) { while (true) { withFrameNanos { }; names = controller.connectedNames } }
    Box(Modifier.fillMaxSize().background(BG), contentAlignment = Alignment.Center) {
        Column(Modifier.width(460.dp).clip(RoundedCornerShape(20.dp)).background(PANEL).padding(26.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("HOSTING A MATCH", color = ACCENT2, fontSize = 24.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(14.dp))
            Text("Friends open the app → JOIN → enter:", color = MUTED, fontSize = 13.sp)
            Box(Modifier.clip(RoundedCornerShape(12.dp)).background(Color(0xFF0E1422)).padding(horizontal = 20.dp, vertical = 10.dp)) {
                Text(ip, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.height(16.dp))
            Text("In lobby (${names.size}):", color = MUTED, fontSize = 13.sp)
            names.forEach { Text("• $it", color = Color.White, fontSize = 16.sp) }
            Text("Empty slots fill with bots.", color = MUTED, fontSize = 12.sp)
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BigButton("START MATCH", ACCENT, Modifier, onStart)
                BigButton("BACK", Color(0xFF55607F), Modifier, onBack)
            }
        }
    }
}

@Composable
private fun LobbyClientScreen(controller: GameController, onPlay: () -> Unit, onBack: () -> Unit) {
    var err by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos { }
            err = controller.netError
            if ((controller.snapshot?.phase ?: GameWorld.LOBBY) == GameWorld.PLAYING) { onPlay(); break }
        }
    }
    Box(Modifier.fillMaxSize().background(BG), contentAlignment = Alignment.Center) {
        Column(Modifier.width(420.dp).clip(RoundedCornerShape(20.dp)).background(PANEL).padding(26.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            if (err == null) {
                Text("Connecting…", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp)); Text("Waiting for the host to start.", color = MUTED)
            } else {
                Text("Couldn't connect", color = Color(0xFFFF5C7A), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp)); Text(err ?: "", color = MUTED, textAlign = TextAlign.Center)
            }
            Spacer(Modifier.height(20.dp)); BigButton("BACK", Color(0xFF55607F), Modifier, onBack)
        }
    }
}

@Composable
private fun PlayScreen(controller: GameController, glView: GLSurfaceView, onExit: () -> Unit) {
    var snap by remember { mutableStateOf<RenderSnapshot?>(null) }
    LaunchedEffect(Unit) { while (true) { withFrameNanos { }; snap = controller.snapshot } }
    val hp = snap?.localHp ?: 100f
    val alive = snap?.aliveCount ?: 1
    val total = snap?.totalPlayers ?: 1
    val phase = snap?.phase ?: GameWorld.PLAYING

    Box(Modifier.fillMaxSize().background(BG)) {
        AndroidView(factory = { glView }, modifier = Modifier.fillMaxSize())

        // top HUD
        Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Box(Modifier.clip(RoundedCornerShape(12.dp)).background(Color(0x88000000)).padding(horizontal = 12.dp, vertical = 6.dp)) {
                Text("ALIVE  $alive / $total", color = Color.White, fontWeight = FontWeight.Black, fontSize = 15.sp)
            }
            Minimap(snap, Modifier.size(110.dp))
        }

        // health bar bottom-center
        Column(Modifier.align(Alignment.TopCenter).padding(top = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("HP ${hp.toInt()}", color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp)
            Box(Modifier.width(200.dp).height(16.dp).clip(RoundedCornerShape(8.dp)).background(Color(0x66000000))) {
                Box(Modifier.fillMaxWidth((hp / 100f).coerceIn(0f, 1f)).height(16.dp).clip(RoundedCornerShape(8.dp))
                    .background(if (hp > 35f) ACCENT else Color(0xFFFF5C7A)))
            }
        }

        // joysticks
        Box(Modifier.align(Alignment.BottomStart).padding(20.dp)) {
            Joystick(ACCENT2, { x, y -> controller.localInput.mx = x; controller.localInput.mz = y })
        }
        Box(Modifier.align(Alignment.BottomEnd).padding(20.dp)) {
            Joystick(Color(0xFFFF7A7A), { x, y ->
                controller.localInput.ax = x; controller.localInput.az = y
                controller.localInput.fire = (x * x + y * y) > 0.06f
            })
        }
        Text("MOVE", color = Color(0x77FFFFFF), fontSize = 11.sp, modifier = Modifier.align(Alignment.BottomStart).padding(start = 84.dp, bottom = 6.dp))
        Text("AIM / FIRE", color = Color(0x77FFFFFF), fontSize = 11.sp, modifier = Modifier.align(Alignment.BottomEnd).padding(end = 66.dp, bottom = 6.dp))

        if (phase == GameWorld.OVER) {
            val youAlive = snap?.players?.firstOrNull { it.isLocal }?.alive == true
            Column(Modifier.fillMaxSize().background(Color(0xDD05060C)), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (youAlive) "VICTORY!" else "ELIMINATED", color = if (youAlive) ACCENT else Color(0xFFFF5C7A), fontSize = 46.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(10.dp))
                Text(snap?.winnerName?.let { "Winner: $it" } ?: "No survivors", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(28.dp))
                BigButton("BACK TO MENU", ACCENT, Modifier, onExit)
            }
        }
    }
}

@Composable
private fun Minimap(snap: RenderSnapshot?, modifier: Modifier) {
    Box(modifier.clip(RoundedCornerShape(10.dp)).background(Color(0xAA0A0E18))) {
        Canvas(Modifier.fillMaxSize().padding(6.dp)) {
            val s = snap ?: return@Canvas
            val h = s.arenaHalf
            val sc = size.minDimension / (2f * h)
            fun mapX(x: Float) = (x + h) * sc
            fun mapZ(z: Float) = (z + h) * sc
            // arena border
            drawRect(Color(0x33FFFFFF), size = Size(size.minDimension, size.minDimension), style = Stroke(2f))
            // zone
            drawCircle(Color(0xFF33D9E6), radius = s.zoneR * sc, center = Offset(mapX(0f), mapZ(0f)), style = Stroke(2f))
            // packs
            for (i in s.packX.indices) drawCircle(Color(0xFF4DD17A), radius = 3f, center = Offset(mapX(s.packX[i]), mapZ(s.packZ[i])))
            // players
            for (p in s.players) {
                if (!p.alive) continue
                val c = if (p.isLocal) Color.White else MINI_COLORS[p.colorIndex % MINI_COLORS.size]
                drawCircle(c, radius = if (p.isLocal) 5f else 4f, center = Offset(mapX(p.x), mapZ(p.z)))
            }
        }
    }
}

@Composable
private fun BigButton(text: String, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = modifier.height(50.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = Color(0xFF05060C)),
        shape = RoundedCornerShape(14.dp)) {
        Text(text, fontWeight = FontWeight.Black, fontSize = 15.sp)
    }
}
