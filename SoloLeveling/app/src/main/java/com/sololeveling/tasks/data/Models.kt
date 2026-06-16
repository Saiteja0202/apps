package com.sololeveling.tasks.data

import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.util.UUID

@Serializable
enum class QuestType { DAILY, HABIT, TODO }

@Serializable
enum class Difficulty(val xp: Int, val label: String) {
    EASY(10, "Easy"),
    MEDIUM(20, "Medium"),
    HARD(35, "Hard"),
    BOSS(60, "Boss")
}

@Serializable
data class Quest(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val note: String = "",
    val type: QuestType = QuestType.DAILY,
    val difficulty: Difficulty = Difficulty.MEDIUM,
    val category: String = "General",
    val targetCount: Int = 1,                 // >1 => counter quest
    val weekdays: Set<Int> = (1..7).toSet(),  // HABIT schedule (1=Mon..7=Sun)
    val reminderMinutes: Int = -1,            // minute-of-day, -1 = none
    val dueEpochDay: Long = -1L,              // TODO due date
    val createdEpochDay: Long = 0L,
    val todoDone: Boolean = false,            // for TODO
    val log: Map<Long, Int> = emptyMap(),     // epochDay -> count done that day
    val order: Int = 0
) {
    fun countOn(day: Long): Int = log[day] ?: 0
    fun isDoneOn(day: Long): Boolean = if (type == QuestType.TODO) todoDone else countOn(day) >= targetCount

    /** Whether this quest is scheduled to appear on [day]. */
    fun activeOn(day: Long): Boolean = when (type) {
        QuestType.DAILY -> true
        QuestType.HABIT -> {
            val wd = LocalDate.ofEpochDay(day).dayOfWeek.value
            weekdays.contains(wd)
        }
        QuestType.TODO -> !todoDone && (dueEpochDay < 0 || dueEpochDay >= day)
    }

    fun currentStreak(today: Long): Int {
        if (type == QuestType.TODO) return 0
        var streak = 0
        var day = today
        // allow today to be incomplete without breaking a streak built up to yesterday
        if (!isDoneOn(day)) day -= 1
        while (activeOn(day) && isDoneOn(day)) { streak++; day -= 1 }
        // skip days the quest wasn't scheduled (for HABIT) without breaking streak
        return streak
    }
}

@Serializable
data class Player(
    val level: Int = 1,
    val xp: Int = 0,
    val gold: Int = 0,
    val streak: Int = 0,
    val bestStreak: Int = 0,
    val hp: Int = 100,
    val totalCompletions: Int = 0,
    val focusSessions: Int = 0,
    val lastActiveEpochDay: Long = 0L,
    val unlockedAchievements: Set<String> = emptySet(),
    val unlockedShadows: Set<String> = emptySet()
)

@Serializable
data class AppData(
    val player: Player = Player(),
    val quests: List<Quest> = emptyList(),
    val themeDark: Boolean = true,
    val dailyReminderMinutes: Int = 20 * 60,   // 8:00 PM
    val moodByDate: Map<Long, Int> = emptyMap(), // epochDay -> 1..5
    val seededDefaults: Boolean = false
)
