package com.sololeveling.tasks.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sololeveling.tasks.data.Achievements
import com.sololeveling.tasks.data.Stats
import com.sololeveling.tasks.data.today
import com.sololeveling.tasks.ui.theme.AccentBlue
import com.sololeveling.tasks.ui.theme.AccentCyan
import com.sololeveling.tasks.ui.theme.AccentGold

@Composable
fun StatsScreen(vm: MainViewModel) {
    val t = today()
    val week = Stats.lastDays(vm.data.quests, t, 7)
    val heat = Stats.lastDays(vm.data.quests, t, 35)
    val rate = Stats.completionRate(vm.data.quests, t, 30)
    val earned = vm.earnedAchievements()
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val primary = MaterialTheme.colorScheme.primary

    LazyColumn(Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("YOUR PROGRESS", color = AccentCyan, style = MaterialTheme.typography.labelLarge) }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("Last 30 days", "$rate%", Modifier.weight(1f))
                StatCard("Streak", "${vm.player.streak} days", Modifier.weight(1f))
                StatCard("Best streak", "${vm.player.bestStreak} days", Modifier.weight(1f))
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("Tasks done", "${vm.player.totalCompletions}", Modifier.weight(1f))
                StatCard("Level", "${vm.player.level}", Modifier.weight(1f))
                StatCard("Coins", "${vm.player.gold}", Modifier.weight(1f))
            }
        }
        item {
            SystemPanel(glow = AccentBlue) {
                Column {
                    Text("Last 7 days", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(8.dp))
                    Canvas(Modifier.fillMaxWidth().height(120.dp)) {
                        val n = week.size
                        val gap = 12.dp.toPx()
                        val bw = (size.width - gap * (n - 1)) / n
                        week.forEachIndexed { i, d ->
                            val h = size.height * d.rate.coerceIn(0.04f, 1f)
                            val x = i * (bw + gap)
                            drawRoundRect(color = surfaceVariant, topLeft = Offset(x, 0f), size = Size(bw, size.height), cornerRadius = CornerRadius(8f, 8f))
                            drawRoundRect(color = primary, topLeft = Offset(x, size.height - h), size = Size(bw, h), cornerRadius = CornerRadius(8f, 8f))
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        listOf("6d", "5d", "4d", "3d", "2d", "1d", "Today").forEach {
                            Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
        item {
            SystemPanel(glow = AccentCyan) {
                Column {
                    Text("Activity — last 5 weeks", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(8.dp))
                    Canvas(Modifier.fillMaxWidth().height(120.dp)) {
                        val cols = 7; val rows = 5
                        val gap = 6.dp.toPx()
                        val cw = (size.width - gap * (cols - 1)) / cols
                        val ch = (size.height - gap * (rows - 1)) / rows
                        heat.forEachIndexed { i, d ->
                            val r = i / cols; val c = i % cols
                            val alpha = if (d.scheduled == 0) 0.12f else (0.2f + 0.8f * d.rate)
                            drawRoundRect(color = AccentCyan.copy(alpha = alpha), topLeft = Offset(c * (cw + gap), r * (ch + gap)), size = Size(cw, ch), cornerRadius = CornerRadius(6f, 6f))
                        }
                    }
                }
            }
        }
        item { Text("ACHIEVEMENTS", color = AccentGold, style = MaterialTheme.typography.labelLarge) }
        item {
            LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.fillMaxWidth().height(((Achievements.ALL.size + 1) / 2 * 78).dp), userScrollEnabled = false) {
                items(Achievements.ALL, key = { it.id }) { a ->
                    val unlocked = earned.contains(a.id)
                    Box(Modifier.padding(4.dp)) {
                        SystemPanel(glow = if (unlocked) AccentGold else MaterialTheme.colorScheme.outline) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(if (unlocked) a.icon else "🔒", style = MaterialTheme.typography.titleLarge)
                                Spacer(Modifier.size(8.dp))
                                Column {
                                    Text(a.title, fontWeight = FontWeight.Bold, color = if (unlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
                                    Text(a.desc, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Box(modifier.clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surface).padding(14.dp)) {
        Column {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
