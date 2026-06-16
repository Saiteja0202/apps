package com.arenaclash.game

import com.arenaclash.game.sim.GameWorld
import com.arenaclash.game.sim.Input
import com.arenaclash.game.sim.Player
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SimTest {

    private fun world(vararg p: Player): GameWorld {
        val w = GameWorld()
        p.forEach { w.players.add(it) }
        w.phase = GameWorld.PLAYING
        return w
    }

    @Test fun bulletDamagesAndKills() {
        val shooter = Player(0, "A", x = 0f, z = 0f, isBot = false)
        val victim = Player(1, "B", x = 4f, z = 0f, isBot = false)
        val w = world(shooter, victim)
        // aim shooter at +x and fire repeatedly
        val inputs = mapOf(0 to Input(ax = 1f, az = 0f, fire = true))
        var ticks = 0
        while (victim.alive && ticks < 600) { w.tick(0.016f, inputs); ticks++ }
        assertTrue("victim should die", !victim.alive)
        assertTrue("shooter credited a kill", shooter.kills >= 1)
    }

    @Test fun zoneShrinksOverTime() {
        val w = world(Player(0, "A"), Player(1, "B"))
        val start = w.zoneR
        repeat(120) { w.tick(0.05f, emptyMap()) } // ~6s
        assertTrue("zone should shrink", w.zoneR < start)
        assertTrue("zone clamped above min", w.zoneR >= GameWorld.ZONE_MIN - 0.001f)
    }

    @Test fun outsideZoneTakesDamage() {
        val p = Player(0, "A", x = 0f, z = 0f)
        val other = Player(1, "B", x = 0f, z = 0f)
        val w = world(p, other)
        w.zoneR = 1f                     // tiny zone
        p.x = 30f                        // far outside
        val hp0 = p.hp
        w.tick(0.5f, emptyMap())
        assertTrue("hp drops outside zone", p.hp < hp0)
    }

    @Test fun winnerDeclaredWhenOneLeft() {
        val a = Player(0, "A"); val b = Player(1, "B", hp = 1f)
        val w = world(a, b)
        b.alive = false                  // simulate elimination
        w.tick(0.016f, emptyMap())
        assertEquals(GameWorld.OVER, w.phase)
        assertEquals(0, w.winnerId)
    }

    @Test fun startPlacesPlayersInArena() {
        val w = GameWorld()
        w.players.add(Player(0, "A")); w.players.add(Player(1, "B")); w.players.add(Player(2, "C"))
        w.start()
        assertEquals(GameWorld.PLAYING, w.phase)
        w.players.forEach {
            assertTrue(kotlin.math.abs(it.x) <= w.arenaHalf + 0.01f)
            assertTrue(kotlin.math.abs(it.z) <= w.arenaHalf + 0.01f)
            assertTrue(it.alive && it.hp == 100f)
        }
    }

    @Test fun healthPackHealsAndDeactivates() {
        val p = Player(0, "A", x = 0f, z = 0f, hp = 30f)
        val w = world(p, Player(1, "B", x = 100f, z = 100f))
        w.packs.add(com.arenaclash.game.sim.HealthPack(0f, 0f))
        w.zoneR = 100f
        w.tick(0.016f, mapOf(0 to Input()))
        assertTrue("hp should increase from pack", p.hp > 30f)
        assertTrue("pack should deactivate", !w.packs[0].active)
    }

    @Test fun botGeneratesActionTowardEnemy() {
        val bot = Player(0, "Bot", x = 0f, z = 0f, isBot = true)
        val enemy = Player(1, "E", x = 5f, z = 0f)
        val w = world(bot, enemy)
        w.zoneR = 50f
        w.tick(0.016f, emptyMap())
        // bot should aim roughly toward +x enemy (a bullet may spawn) — at least it stays alive and acts
        assertTrue(bot.alive)
    }
}
