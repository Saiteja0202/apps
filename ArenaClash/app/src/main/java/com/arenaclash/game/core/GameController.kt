package com.arenaclash.game.core

import com.arenaclash.game.net.GameClient
import com.arenaclash.game.net.HostServer
import com.arenaclash.game.net.InputDTO
import com.arenaclash.game.net.PlayerDTO
import com.arenaclash.game.net.SnapshotDTO
import com.arenaclash.game.sim.GameWorld
import com.arenaclash.game.sim.Input
import com.arenaclash.game.sim.Player
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread

class GameController {
    enum class Mode { OFFLINE, HOST, CLIENT }

    @Volatile var snapshot: RenderSnapshot? = null
    val localInput = Input()
    @Volatile var localId = 0
    @Volatile var mode = Mode.OFFLINE
    @Volatile var connectedNames: List<String> = emptyList()
    @Volatile var started = false
    @Volatile var netError: String? = null

    private var world: GameWorld? = null
    private val clientInputs = ConcurrentHashMap<Int, Input>()
    private var host: HostServer? = null
    private var client: GameClient? = null
    private var pendingBots = 0
    @Volatile private var loopRunning = false

    // ---------------- OFFLINE ----------------
    fun startOffline(name: String, botCount: Int) {
        mode = Mode.OFFLINE
        val w = GameWorld(); world = w; localId = 0
        w.players.add(Player(0, name, isBot = false, colorIndex = 0))
        for (i in 1..botCount) w.players.add(Player(100 + i, "Bot $i", isBot = true, colorIndex = i % 10))
        w.start(); started = true
        startLoop()
    }

    // ---------------- HOST ----------------
    fun startHost(name: String, fillBots: Int) {
        mode = Mode.HOST
        val w = GameWorld(); world = w; localId = 0; pendingBots = fillBots
        w.players.add(Player(0, name, isBot = false, colorIndex = 0))
        host = HostServer().apply {
            onJoin = { id, nm ->
                synchronized(w) { if (!started && w.players.none { it.id == id }) w.players.add(Player(id, nm, isBot = false, colorIndex = id % 10)) }
                clientInputs[id] = Input(); refreshNames(w)
            }
            onLeave = { id -> clientInputs.remove(id); synchronized(w) { w.players.removeAll { it.id == id } }; refreshNames(w) }
            onInput = { id, dto -> clientInputs[id] = dto.toInput() }
            start()
        }
        refreshNames(w)
        startLoop() // broadcasts lobby snapshots until match starts
    }

    fun hostStartMatch() {
        val w = world ?: return
        synchronized(w) {
            for (i in 1..pendingBots) w.players.add(Player(200 + i, "Bot $i", isBot = true, colorIndex = (w.players.size) % 10))
            w.start()
        }
        started = true
    }

    fun hostClientCount() = host?.clientCount() ?: 0

    // ---------------- CLIENT ----------------
    fun startClient(hostIp: String, name: String) {
        mode = Mode.CLIENT
        client = GameClient(hostIp).apply {
            onWelcome = { id -> localId = id }
            onSnapshot = { dto -> snapshot = dto.toRender(localId) }
            onError = { e -> netError = e }
            connect(name)
        }
        startLoop()
    }

    val clientPhase: Int get() = snapshot?.phase ?: GameWorld.LOBBY

    // ---------------- loop ----------------
    private fun startLoop() {
        loopRunning = true
        thread(name = "game-loop") {
            var last = System.nanoTime()
            while (loopRunning) {
                val now = System.nanoTime()
                var dt = (now - last) / 1_000_000_000f; last = now
                if (dt > 0.05f) dt = 0.05f
                when (mode) {
                    Mode.OFFLINE -> { val w = world!!; synchronized(w) { w.tick(dt, mapOf(localId to localInput)) }; snapshot = buildSnapshot(w) }
                    Mode.HOST -> {
                        val w = world!!
                        synchronized(w) {
                            val inputs = HashMap<Int, Input>(); inputs[localId] = localInput; inputs.putAll(clientInputs)
                            w.tick(dt, inputs)
                        }
                        snapshot = buildSnapshot(w)
                        host?.broadcast(buildDto(w))
                    }
                    Mode.CLIENT -> client?.sendInput(localInput.toDto())
                }
                try { Thread.sleep(28) } catch (_: InterruptedException) {}
            }
        }
    }

    private fun refreshNames(w: GameWorld) {
        connectedNames = synchronized(w) { w.players.filter { !it.isBot }.map { it.name } }
    }

    private fun buildSnapshot(w: GameWorld): RenderSnapshot = synchronized(w) {
        val rps = w.players.map { RP(it.x, it.z, it.angle, it.colorIndex, it.alive, it.id == localId, it.name, it.hp, it.kills, it.moving) }
        val bx = FloatArray(w.bullets.size) { w.bullets[it].x }
        val bz = FloatArray(w.bullets.size) { w.bullets[it].z }
        val active = w.packs.filter { it.active }
        val px = FloatArray(active.size) { active[it].x }
        val pz = FloatArray(active.size) { active[it].z }
        val localHp = w.players.firstOrNull { it.id == localId }?.hp ?: 0f
        val winner = if (w.winnerId >= 0) w.players.firstOrNull { it.id == w.winnerId }?.name else null
        RenderSnapshot(rps, bx, bz, px, pz, w.zoneR, w.arenaHalf, w.phase, localHp, w.aliveCount(), w.players.size, winner)
    }

    private fun buildDto(w: GameWorld): SnapshotDTO = synchronized(w) {
        val active = w.packs.filter { it.active }
        SnapshotDTO(
            w.players.map { PlayerDTO(it.id, it.name, it.x, it.z, it.angle, it.hp, it.alive, it.colorIndex, it.kills, it.isBot, it.moving) },
            w.bullets.map { it.x }, w.bullets.map { it.z },
            active.map { it.x }, active.map { it.z },
            w.zoneR, w.arenaHalf, w.phase,
            if (w.winnerId >= 0) w.players.firstOrNull { it.id == w.winnerId }?.name ?: "" else ""
        )
    }

    fun stop() {
        loopRunning = false
        host?.stop(); client?.close()
        host = null; client = null
    }
}

private fun Input.toDto() = InputDTO(mx, mz, ax, az, fire)
private fun InputDTO.toInput() = Input(mx, mz, ax, az, fire)

private fun SnapshotDTO.toRender(localId: Int): RenderSnapshot {
    val rps = players.map { RP(it.x, it.z, it.angle, it.color, it.alive, it.id == localId, it.name, it.hp, it.kills, it.moving) }
    val localHp = players.firstOrNull { it.id == localId }?.hp ?: 0f
    return RenderSnapshot(rps, bx.toFloatArray(), bz.toFloatArray(), px.toFloatArray(), pz.toFloatArray(), zoneR, arenaHalf, phase, localHp, players.count { it.alive }, players.size, winner.ifEmpty { null })
}
