package com.sololeveling.tasks.data

import android.content.Context
import kotlinx.serialization.json.Json

class Store(context: Context) {
    private val prefs = context.getSharedPreferences("solo_v2", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun load(): AppData {
        val raw = prefs.getString(KEY, null) ?: return seed()
        return runCatching { json.decodeFromString(AppData.serializer(), raw) }.getOrElse { seed() }
    }

    fun save(data: AppData) {
        prefs.edit().putString(KEY, json.encodeToString(AppData.serializer(), data)).apply()
    }

    private fun seed(): AppData {
        val t = today()
        val q = listOf(
            Quest(title = "100 Push-ups", type = QuestType.DAILY, difficulty = Difficulty.HARD, category = "Fitness", targetCount = 100, createdEpochDay = t, order = 0),
            Quest(title = "100 Sit-ups", type = QuestType.DAILY, difficulty = Difficulty.HARD, category = "Fitness", targetCount = 100, createdEpochDay = t, order = 1),
            Quest(title = "100 Squats", type = QuestType.DAILY, difficulty = Difficulty.HARD, category = "Fitness", targetCount = 100, createdEpochDay = t, order = 2),
            Quest(title = "10 km Run", type = QuestType.DAILY, difficulty = Difficulty.BOSS, category = "Fitness", targetCount = 1, createdEpochDay = t, order = 3),
            Quest(title = "Drink water", type = QuestType.DAILY, difficulty = Difficulty.EASY, category = "Health", targetCount = 8, createdEpochDay = t, order = 4),
            Quest(title = "Read 20 minutes", type = QuestType.HABIT, difficulty = Difficulty.MEDIUM, category = "Study", targetCount = 1, weekdays = (1..5).toSet(), createdEpochDay = t, order = 5)
        )
        return AppData(quests = q, seededDefaults = true)
    }

    companion object { private const val KEY = "appdata" }
}
