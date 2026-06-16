package com.sololeveling.tasks.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.sololeveling.tasks.data.Achievements
import com.sololeveling.tasks.data.AppData
import com.sololeveling.tasks.data.Game
import com.sololeveling.tasks.data.Quest
import com.sololeveling.tasks.data.QuestType
import com.sololeveling.tasks.data.Shadows
import com.sololeveling.tasks.data.Stats
import com.sololeveling.tasks.data.Store
import com.sololeveling.tasks.data.today
import com.sololeveling.tasks.notify.Reminders
import kotlin.math.max
import kotlin.math.min

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val store = Store(app)

    var data by mutableStateOf(AppData())
        private set

    /** The day the UI is currently showing. Observed so screens refresh when the date changes. */
    var currentDay by mutableStateOf(today())
        private set

    /** transient banner event (level up / achievement) */
    var event by mutableStateOf<String?>(null)
        private set

    val player get() = data.player
    val rank get() = Game.rankFor(player.level)
    val title get() = Game.titleFor(player.level)
    val xpForNext get() = Game.xpForLevel(player.level)

    init {
        var d = store.load()
        d = processRollover(d)
        data = d
        store.save(d)
        Reminders.schedule(getApplication(), d.dailyReminderMinutes)
    }

    /**
     * Re-checks the real date (call on resume / at midnight). If the day rolled
     * over, it processes streak/energy changes and refreshes the Today view.
     */
    fun refreshDay() {
        val t = today()
        if (t != currentDay) {
            val rolled = processRollover(data)
            data = rolled
            store.save(rolled)
            currentDay = t
        }
    }

    // ---------- derived (use currentDay so the UI refreshes when the date changes) ----------
    fun todayQuests(): List<Quest> {
        val t = currentDay
        return data.quests.filter { it.activeOn(t) }.sortedWith(compareBy({ it.isDoneOn(t) }, { it.order }))
    }

    fun allQuests(): List<Quest> = data.quests.sortedBy { it.order }
    fun categories(): List<String> = data.quests.map { it.category }.distinct().sorted()
    val pendingToday: Int get() = currentDay.let { t -> data.quests.count { it.activeOn(t) && !it.isDoneOn(t) } }
    val clearedToday: Boolean get() = currentDay.let { t -> data.quests.any { it.activeOn(t) } && data.quests.filter { it.activeOn(t) }.all { it.isDoneOn(t) } }
    fun earnedAchievements() = Achievements.earned(player, data.quests, currentDay)
    fun earnedShadows() = Shadows.earned(player.totalCompletions)

    // ---------- actions ----------
    fun increment(quest: Quest) = changeProgress(quest, +1)
    fun decrement(quest: Quest) = changeProgress(quest, -1)

    fun toggleSimple(quest: Quest) {
        val t = today()
        if (quest.type == QuestType.TODO) {
            setTodoDone(quest, !quest.todoDone); return
        }
        if (quest.isDoneOn(t)) changeProgress(quest, -quest.countOn(t))
        else changeProgress(quest, quest.targetCount - quest.countOn(t))
    }

    private fun changeProgress(quest: Quest, delta: Int) {
        val t = today()
        val before = quest.isDoneOn(t)
        val newCount = (quest.countOn(t) + delta).coerceIn(0, quest.targetCount)
        val newLog = quest.log.toMutableMap().apply { put(t, newCount) }
        val updated = quest.copy(log = newLog)
        replaceQuest(updated)
        val after = updated.isDoneOn(t)
        if (after && !before) award(quest, +1)
        else if (!after && before) award(quest, -1)
    }

    private fun setTodoDone(quest: Quest, done: Boolean) {
        val before = quest.todoDone
        replaceQuest(quest.copy(todoDone = done))
        if (done && !before) award(quest, +1)
        else if (!done && before) award(quest, -1)
    }

    private fun award(quest: Quest, sign: Int) {
        val xp = quest.difficulty.xp * sign
        val gold = (quest.difficulty.xp / 2) * sign
        val (np, gained) = Game.grantXp(player, xp)
        var p = np.copy(
            gold = max(0, np.gold + gold),
            totalCompletions = max(0, np.totalCompletions + sign),
            hp = if (sign > 0) min(100, np.hp + 2) else np.hp
        )
        // achievements
        val before = p.unlockedAchievements
        val earned = Achievements.earned(p, data.quests, today())
        val newAch = earned - before
        p = p.copy(unlockedAchievements = before + earned, unlockedShadows = p.unlockedShadows + Shadows.earned(p.totalCompletions))
        save(data.copy(player = p))
        if (sign > 0 && gained > 0) event = "⬆ LEVEL UP! You are now Level ${p.level} (${Game.rankFor(p.level)}-Rank)"
        else if (newAch.isNotEmpty()) {
            val a = Achievements.ALL.firstOrNull { it.id == newAch.first() }
            if (a != null) event = "${a.icon} Achievement unlocked: ${a.title}"
        }
    }

    fun addQuest(q: Quest) {
        val order = (data.quests.maxOfOrNull { it.order } ?: 0) + 1
        save(data.copy(quests = data.quests + q.copy(order = order, createdEpochDay = today())))
    }

    fun updateQuest(q: Quest) = replaceQuest(q)

    fun deleteQuest(id: String) = save(data.copy(quests = data.quests.filterNot { it.id == id }))

    private fun replaceQuest(q: Quest) = save(data.copy(quests = data.quests.map { if (it.id == q.id) q else it }))

    fun setTheme(dark: Boolean) = save(data.copy(themeDark = dark))

    fun setReminder(minutes: Int) {
        save(data.copy(dailyReminderMinutes = minutes))
        Reminders.schedule(getApplication(), minutes)
    }

    fun setMood(value: Int) = save(data.copy(moodByDate = data.moodByDate + (today() to value)))

    fun completeFocusSession(minutes: Int) {
        val (np, gained) = Game.grantXp(player, minutes)
        val p = np.copy(gold = np.gold + minutes / 2, focusSessions = np.focusSessions + 1)
        save(data.copy(player = p))
        event = if (gained > 0) "🧠 Focus complete! +$minutes XP · Level ${p.level}" else "🧠 Focus complete! +$minutes XP"
    }

    fun resetAll() {
        val fresh = AppData()
        save(fresh)
        Reminders.schedule(getApplication(), fresh.dailyReminderMinutes)
    }

    fun consumeEvent() { event = null }

    // ---------- internals ----------
    private fun save(newData: AppData) {
        val withStreak = recomputeStreak(newData)
        data = withStreak
        store.save(withStreak)
    }

    private fun recomputeStreak(d: AppData): AppData {
        // keep bestStreak in sync with current streak
        val best = max(d.player.bestStreak, d.player.streak)
        return if (best != d.player.bestStreak) d.copy(player = d.player.copy(bestStreak = best)) else d
    }

    private fun processRollover(d: AppData): AppData {
        val t = today()
        var p = d.player
        if (p.lastActiveEpochDay == 0L) return d.copy(player = p.copy(lastActiveEpochDay = t))
        if (p.lastActiveEpochDay >= t) return d
        var day = p.lastActiveEpochDay
        while (day < t) {
            val st = Stats.dayStat(d.quests, day)
            if (st.scheduled > 0) {
                if (st.completed >= st.scheduled) {
                    p = p.copy(streak = p.streak + 1, hp = min(100, p.hp + 10))
                } else {
                    p = p.copy(streak = 0, hp = max(0, p.hp - 20))
                }
                p = p.copy(bestStreak = max(p.bestStreak, p.streak))
            }
            day++
        }
        // archive finished TODOs older than today is fine to leave; clear nothing else
        return d.copy(player = p.copy(lastActiveEpochDay = t))
    }
}
