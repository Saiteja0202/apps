package com.arenaclash.game.sim

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random

class Input(
    var mx: Float = 0f, var mz: Float = 0f,
    var ax: Float = 0f, var az: Float = 0f,
    var fire: Boolean = false
)

class Player(
    val id: Int,
    var name: String,
    var x: Float = 0f,
    var z: Float = 0f,
    var angle: Float = 0f,
    var hp: Float = 100f,
    var alive: Boolean = true,
    val isBot: Boolean = false,
    val colorIndex: Int = 0,
    var kills: Int = 0,
    var cooldown: Float = 0f,
    var moving: Boolean = false
)

class Bullet(var x: Float, var z: Float, var vx: Float, var vz: Float, val owner: Int, var life: Float = 1.6f)

class HealthPack(var x: Float, var z: Float, var active: Boolean = true, var timer: Float = 0f)

class GameWorld(val arenaHalf: Float = 22f, seed: Long = 1L) {
    val players = ArrayList<Player>()
    val bullets = ArrayList<Bullet>()
    val packs = ArrayList<HealthPack>()

    var zoneR = arenaHalf * 1.45f
    var phase = LOBBY
    var winnerId = -1
    var elapsed = 0f
    private val rng = Random(seed)

    companion object {
        const val LOBBY = 0; const val PLAYING = 1; const val OVER = 2
        const val SPEED = 6.5f
        const val BSPEED = 26f
        const val DMG = 20f
        const val HITR = 1.0f
        const val FIRECD = 0.26f
        const val ZONEDMG = 9f
        const val PLAYER_R = 0.7f
        const val ZONE_MIN = 4f
        const val SHRINK = 0.45f
        const val HEAL = 35f
        const val PACK_R = 1.3f
        const val PACK_RESPAWN = 12f
    }

    fun aliveCount() = players.count { it.alive }

    fun start() {
        phase = PLAYING; elapsed = 0f; winnerId = -1
        zoneR = arenaHalf * 1.45f
        val n = players.size.coerceAtLeast(1)
        players.forEachIndexed { i, p ->
            val a = (i.toFloat() / n) * 6.2832f
            p.x = cos(a) * arenaHalf * 0.8f
            p.z = sin(a) * arenaHalf * 0.8f
            p.hp = 100f; p.alive = true; p.kills = 0; p.cooldown = 0f
            p.angle = atan2(-p.z, -p.x) // face center
        }
        bullets.clear()
        packs.clear()
        packs.add(HealthPack(0f, 0f))
        val r = arenaHalf * 0.5f
        for (i in 0 until 4) {
            val a = (i / 4f) * 6.2832f + 0.4f
            packs.add(HealthPack(cos(a) * r, sin(a) * r))
        }
    }

    fun tick(dt: Float, humanInputs: Map<Int, Input>) {
        if (phase != PLAYING) return
        elapsed += dt
        if (zoneR > ZONE_MIN) zoneR = max(ZONE_MIN, zoneR - SHRINK * dt)

        for (p in players) {
            if (!p.alive) continue
            val inp = if (p.isBot) botInput(p) else (humanInputs[p.id] ?: Input())

            var mx = inp.mx; var mz = inp.mz
            val ml = hypot(mx, mz); p.moving = ml > 0.1f
            if (ml > 1f) { mx /= ml; mz /= ml }
            p.x = (p.x + mx * SPEED * dt).coerceIn(-arenaHalf, arenaHalf)
            p.z = (p.z + mz * SPEED * dt).coerceIn(-arenaHalf, arenaHalf)

            val al = hypot(inp.ax, inp.az)
            if (al > 0.2f) p.angle = atan2(inp.az, inp.ax)

            p.cooldown -= dt
            if (inp.fire && p.cooldown <= 0f && al > 0.2f) {
                p.cooldown = FIRECD
                val dx = cos(p.angle); val dz = sin(p.angle)
                bullets.add(Bullet(p.x + dx * PLAYER_R * 1.3f, p.z + dz * PLAYER_R * 1.3f, dx * BSPEED, dz * BSPEED, p.id))
            }

            val d = hypot(p.x, p.z)
            if (d > zoneR) p.hp -= ZONEDMG * dt
            if (p.hp <= 0f) p.alive = false
        }

        // health pickups
        for (pack in packs) {
            if (!pack.active) {
                pack.timer -= dt
                if (pack.timer <= 0f) pack.active = true
                continue
            }
            for (p in players) {
                if (p.alive && p.hp < 100f && hypot(p.x - pack.x, p.z - pack.z) < PACK_R) {
                    p.hp = (p.hp + HEAL).coerceAtMost(100f)
                    pack.active = false; pack.timer = PACK_RESPAWN
                    break
                }
            }
        }

        val bit = bullets.iterator()
        while (bit.hasNext()) {
            val b = bit.next()
            b.life -= dt
            b.x += b.vx * dt; b.z += b.vz * dt
            if (b.life <= 0f || abs(b.x) > arenaHalf + 2 || abs(b.z) > arenaHalf + 2) { bit.remove(); continue }
            var hit = false
            for (p in players) {
                if (!p.alive || p.id == b.owner) continue
                if (hypot(p.x - b.x, p.z - b.z) < HITR + PLAYER_R * 0.3f) {
                    p.hp -= DMG
                    if (p.hp <= 0f) { p.alive = false; players.firstOrNull { it.id == b.owner }?.let { it.kills++ } }
                    hit = true; break
                }
            }
            if (hit) bit.remove()
        }

        if (aliveCount() <= 1) {
            phase = OVER
            winnerId = players.firstOrNull { it.alive }?.id ?: -1
        }
    }

    private fun botInput(bot: Player): Input {
        val inp = Input()
        var target: Player? = null; var best = Float.MAX_VALUE
        for (p in players) {
            if (p.alive && p.id != bot.id) {
                val d = hypot(p.x - bot.x, p.z - bot.z)
                if (d < best) { best = d; target = p }
            }
        }
        // stay inside the safe zone first
        val cd = hypot(bot.x, bot.z)
        if (cd > zoneR - 2f && cd > 0.01f) {
            inp.mx = -bot.x / cd; inp.mz = -bot.z / cd
        }
        val t = target ?: return inp
        val dx = t.x - bot.x; val dz = t.z - bot.z
        val d = hypot(dx, dz).coerceAtLeast(0.001f)
        inp.ax = dx / d; inp.az = dz / d
        if (cd <= zoneR - 2f) {
            val sign = if (d > 9f) 1f else -1f
            inp.mx = (dx / d) * sign * 0.7f + (-dz / d) * 0.5f
            inp.mz = (dz / d) * sign * 0.7f + (dx / d) * 0.5f
        }
        inp.fire = d < 16f && rng.nextFloat() > 0.15f
        return inp
    }
}
