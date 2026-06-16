package com.sololeveling.tasks.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sololeveling.tasks.ui.theme.AccentCyan
import com.sololeveling.tasks.ui.theme.AccentGold
import com.sololeveling.tasks.ui.theme.Danger
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun TodayScreen(vm: MainViewModel) {
    val quests = vm.todayQuests()
    LazyColumn(
        Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        item {
            val dateLabel = LocalDate.ofEpochDay(vm.currentDay)
                .format(DateTimeFormatter.ofPattern("EEEE, d MMM", Locale.getDefault()))
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text("TODAY", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
                Text(dateLabel, color = AccentCyan, style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(10.dp))
            }
        }
        item { PlayerHeaderCard(vm); Spacer(Modifier.height(10.dp)) }

        if (vm.player.hp <= 40) item {
            SystemPanel(glow = Danger) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.WarningAmber, null, tint = Danger)
                    Spacer(Modifier.height(0.dp))
                    Text("  Your energy is low. Finish today's tasks to bring it back up.", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelSmall)
                }
            }
            Spacer(Modifier.height(10.dp))
        }

        if (vm.clearedToday) item {
            SystemPanel(glow = AccentGold) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Check, null, tint = AccentGold)
                    Spacer(Modifier.height(0.dp))
                    Text("  All done for today! Your streak is safe. 🎉", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelSmall)
                }
            }
            Spacer(Modifier.height(10.dp))
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("TODAY'S TASKS", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
                Text("${quests.count { it.isDoneOn(vm.currentDay) }} of ${quests.size} done", color = AccentCyan, style = MaterialTheme.typography.labelSmall)
            }
            Spacer(Modifier.height(2.dp))
        }

        if (quests.isEmpty()) item {
            Text("Nothing for today yet.\nAdd a task in the Tasks tab.", color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().height(80.dp))
        }

        items(quests, key = { it.id }) { q ->
            QuestRow(
                quest = q,
                onCheck = { vm.toggleSimple(q) },
                onPlus = { vm.increment(q) },
                onMinus = { vm.decrement(q) },
                onClick = { if (q.targetCount > 1) vm.increment(q) else vm.toggleSimple(q) }
            )
        }
        item { Spacer(Modifier.height(12.dp)); Text("A little every day adds up. Keep your streak going!", color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) }
    }
}
