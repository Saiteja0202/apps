package com.sololeveling.tasks

import com.sololeveling.tasks.data.Achievements
import com.sololeveling.tasks.data.Difficulty
import com.sololeveling.tasks.data.Game
import com.sololeveling.tasks.data.Player
import com.sololeveling.tasks.data.Quest
import com.sololeveling.tasks.data.QuestType
import com.sololeveling.tasks.data.Shadows
import com.sololeveling.tasks.data.Stats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class GameTest {

    private val MON = LocalDate.of(2024, 1, 1).toEpochDay() // Monday

    @Test fun xpCurveIncreases() {
        assertEquals(100, Game.xpForLevel(1))
        assertEquals(140, Game.xpForLevel(2))
        assertTrue(Game.xpForLevel(10) > Game.xpForLevel(5))
    }

    @Test fun grantXpLevelsUp() {
        val (p1, g1) = Game.grantXp(Player(), 100)
        assertEquals(2, p1.level); assertEquals(0, p1.xp); assertEquals(1, g1)

        val (p2, g2) = Game.grantXp(Player(), 250) // 100 -> L2, 140 -> L3, rem 10
        assertEquals(3, p2.level); assertEquals(10, p2.xp); assertEquals(2, g2)
    }

    @Test fun grantNegativeXpCanDeLevel() {
        val start = Player(level = 2, xp = 0)
        val (p, _) = Game.grantXp(start, -10)
        assertEquals(1, p.level)
        assertTrue(p.xp >= 0)
    }

    @Test fun rankThresholds() {
        assertEquals("E", Game.rankFor(1))
        assertEquals("D", Game.rankFor(7))
        assertEquals("C", Game.rankFor(14))
        assertEquals("B", Game.rankFor(22))
        assertEquals("A", Game.rankFor(32))
        assertEquals("S", Game.rankFor(45))
    }

    @Test fun counterCompletion() {
        val q = Quest(targetCount = 3, log = mapOf(MON to 2))
        assertFalse(q.isDoneOn(MON))
        assertTrue(q.copy(log = mapOf(MON to 3)).isDoneOn(MON))
    }

    @Test fun habitScheduling() {
        val q = Quest(type = QuestType.HABIT, weekdays = setOf(1)) // Mondays only
        assertTrue(q.activeOn(MON))
        assertFalse(q.activeOn(MON + 1)) // Tuesday
    }

    @Test fun todoActiveUntilDone() {
        val q = Quest(type = QuestType.TODO)
        assertTrue(q.activeOn(MON))
        assertFalse(q.copy(todoDone = true).activeOn(MON))
    }

    @Test fun streakCountsConsecutiveDays() {
        val q = Quest(type = QuestType.DAILY, targetCount = 1,
            log = mapOf(MON to 1, (MON - 1) to 1, (MON - 2) to 1))
        assertEquals(3, q.currentStreak(MON))
        // today not done but previous two done => streak 2
        val q2 = Quest(type = QuestType.DAILY, targetCount = 1,
            log = mapOf((MON - 1) to 1, (MON - 2) to 1))
        assertEquals(2, q2.currentStreak(MON))
    }

    @Test fun statsRespectCreationDate() {
        val q = Quest(type = QuestType.DAILY, createdEpochDay = MON, targetCount = 1)
        assertEquals(0, Stats.dayStat(listOf(q), MON - 1).scheduled) // before it existed
        assertEquals(1, Stats.dayStat(listOf(q), MON).scheduled)
    }

    @Test fun completionRateComputed() {
        val q = Quest(type = QuestType.DAILY, createdEpochDay = MON - 6, targetCount = 1,
            log = (0..6).associate { (MON - it) to 1 })
        val rate = Stats.completionRate(listOf(q), MON, 7)
        assertEquals(100, rate)
    }

    @Test fun achievementsEarned() {
        assertTrue(Achievements.earned(Player(totalCompletions = 1), emptyList(), MON).contains("first_blood"))
        assertTrue(Achievements.earned(Player(totalCompletions = 100), emptyList(), MON).contains("century"))
        assertTrue(Achievements.earned(Player(level = 45), emptyList(), MON).contains("rank_s"))
        assertTrue(Achievements.earned(Player(focusSessions = 10), emptyList(), MON).contains("focused"))
        assertFalse(Achievements.earned(Player(), emptyList(), MON).contains("century"))
    }

    @Test fun shadowsUnlock() {
        assertTrue(Shadows.earned(5).contains("igris"))
        assertTrue(Shadows.earned(4).isEmpty())
        assertEquals(Shadows.ALL.size, Shadows.earned(1000).size)
    }

    @Test fun difficultyXpValues() {
        assertEquals(10, Difficulty.EASY.xp)
        assertEquals(60, Difficulty.BOSS.xp)
    }
}
