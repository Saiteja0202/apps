package com.sololeveling.tasks.data

import java.time.LocalDate

object Game {

    fun xpForLevel(level: Int): Int = 100 + (level - 1) * 40

    fun rankFor(level: Int): String = when {
        level >= 45 -> "S"
        level >= 32 -> "A"
        level >= 22 -> "B"
        level >= 14 -> "C"
        level >= 7 -> "D"
        else -> "E"
    }

    fun titleFor(level: Int): String = when (rankFor(level)) {
        "S" -> "Shadow Monarch"
        "A" -> "National Level Hunter"
        "B" -> "Elite Hunter"
        "C" -> "Skilled Hunter"
        "D" -> "Awakened Hunter"
        else -> "Novice Hunter"
    }

    /** Adds xp to a player, leveling up as needed. Returns new player + levels gained. */
    fun grantXp(player: Player, amount: Int): Pair<Player, Int> {
        var xp = player.xp + amount
        var level = player.level
        var gained = 0
        if (amount >= 0) {
            while (xp >= xpForLevel(level)) { xp -= xpForLevel(level); level++; gained++ }
        } else {
            while (xp < 0 && level > 1) { level--; xp += xpForLevel(level) }
            if (xp < 0) xp = 0
        }
        return player.copy(level = level, xp = xp) to gained
    }
}

data class AchievementDef(val id: String, val title: String, val desc: String, val icon: String)

object Achievements {
    val ALL = listOf(
        AchievementDef("first_blood", "First Blood", "Clear your first quest", "🗡️"),
        AchievementDef("streak_7", "On a Roll", "Reach a 7-day streak", "🔥"),
        AchievementDef("streak_30", "Unbreakable", "Reach a 30-day streak", "💎"),
        AchievementDef("rank_c", "Skilled Hunter", "Reach Rank C", "🎖️"),
        AchievementDef("rank_s", "Sovereign", "Reach Rank S", "👑"),
        AchievementDef("century", "Centurion", "Complete 100 quests total", "💯"),
        AchievementDef("boss_slayer", "Boss Slayer", "Clear a Boss-difficulty quest", "🐉"),
        AchievementDef("focused", "Deep Focus", "Finish 10 focus sessions", "🧠")
    )

    /** Returns the set of achievement ids the player currently qualifies for. */
    fun earned(player: Player, quests: List<Quest>, today: Long): Set<String> {
        val ids = mutableSetOf<String>()
        if (player.totalCompletions >= 1) ids += "first_blood"
        if (player.bestStreak >= 7 || player.streak >= 7) ids += "streak_7"
        if (player.bestStreak >= 30 || player.streak >= 30) ids += "streak_30"
        val rank = Game.rankFor(player.level)
        if (rank in listOf("C", "B", "A", "S")) ids += "rank_c"
        if (rank == "S") ids += "rank_s"
        if (player.totalCompletions >= 100) ids += "century"
        if (quests.any { it.difficulty == Difficulty.BOSS && it.log.values.any { c -> c >= it.targetCount } }) ids += "boss_slayer"
        if (player.focusSessions >= 10) ids += "focused"
        return ids
    }
}

data class ShadowDef(val id: String, val name: String, val emoji: String, val unlockAt: Int)

object Shadows {
    // Unlocked by total completions — your growing shadow army.
    val ALL = listOf(
        ShadowDef("igris", "Igris", "⚔️", 5),
        ShadowDef("iron", "Iron", "🛡️", 15),
        ShadowDef("tank", "Tank", "🐺", 30),
        ShadowDef("tusk", "Tusk", "🪓", 50),
        ShadowDef("kaisel", "Kaisel", "🐲", 80),
        ShadowDef("beru", "Beru", "👹", 120),
        ShadowDef("bellion", "Bellion", "😈", 180),
        ShadowDef("monarch", "Monarch's Legion", "🌑", 260)
    )

    fun earned(totalCompletions: Int): Set<String> =
        ALL.filter { totalCompletions >= it.unlockAt }.map { it.id }.toSet()
}

// ---------- Stats ----------
data class DayStat(val epochDay: Long, val scheduled: Int, val completed: Int) {
    val rate: Float get() = if (scheduled == 0) 0f else completed.toFloat() / scheduled
}

object Stats {
    fun dayStat(quests: List<Quest>, day: Long): DayStat {
        val active = quests.filter { it.type != QuestType.TODO && it.createdEpochDay <= day && it.activeOn(day) }
        val done = active.count { it.isDoneOn(day) }
        return DayStat(day, active.size, done)
    }

    fun lastDays(quests: List<Quest>, today: Long, n: Int): List<DayStat> =
        (0 until n).map { dayStat(quests, today - (n - 1 - it)) }

    fun completionRate(quests: List<Quest>, today: Long, window: Int = 30): Int {
        val days = lastDays(quests, today, window)
        val sched = days.sumOf { it.scheduled }
        val done = days.sumOf { it.completed }
        return if (sched == 0) 0 else (done * 100 / sched)
    }
}

fun today(): Long = LocalDate.now().toEpochDay()
