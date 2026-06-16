package com.sololeveling.tasks.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sololeveling.tasks.data.QuestType
import com.sololeveling.tasks.data.today
import com.sololeveling.tasks.ui.theme.AccentCyan
import com.sololeveling.tasks.ui.theme.AccentGold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestDetailScreen(vm: MainViewModel, questId: String, onBack: () -> Unit) {
    val quest = vm.data.quests.firstOrNull { it.id == questId }
    var editing by remember { mutableStateOf(false) }

    if (quest == null) { onBack(); return }
    val t = today()
    val done = quest.isDoneOn(t)
    val cyan = AccentCyan

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(quest.title, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = { IconButton(onClick = { editing = true }) { Icon(Icons.Filled.Edit, "Edit", tint = MaterialTheme.colorScheme.primary) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, titleContentColor = MaterialTheme.colorScheme.onBackground, navigationIconContentColor = MaterialTheme.colorScheme.onBackground)
            )
        }
    ) { inner ->
        LazyColumn(Modifier.fillMaxSize().padding(inner), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                SystemPanel {
                    Column {
                        Row {
                            Chip(quest.type.name.lowercase().replaceFirstChar { it.uppercase() })
                            Spacer(Modifier.width(6.dp)); Chip(quest.category)
                            Spacer(Modifier.width(6.dp)); Chip("${quest.difficulty.label} · +${quest.difficulty.xp} XP")
                        }
                        if (quest.note.isNotBlank()) { Spacer(Modifier.height(8.dp)); Text(quest.note, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        Spacer(Modifier.height(12.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                            Stat("Streak", "${quest.currentStreak(t)}d")
                            Stat("Today", "${quest.countOn(t)}/${quest.targetCount}")
                            Stat("Done days", "${quest.log.values.count { it >= quest.targetCount }}")
                        }
                    }
                }
            }

            item {
                if (quest.type == QuestType.TODO) {
                    Button(onClick = { vm.toggleSimple(quest) }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.Check, null); Spacer(Modifier.width(8.dp)); Text(if (quest.todoDone) "Mark not done" else "Mark done")
                    }
                } else if (quest.targetCount > 1) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(onClick = { vm.decrement(quest) }, modifier = Modifier.weight(1f)) { Icon(Icons.Filled.Remove, null); Text(" −") }
                        Text("${quest.countOn(t)} / ${quest.targetCount}", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                        Button(onClick = { vm.increment(quest) }, modifier = Modifier.weight(1f)) { Icon(Icons.Filled.Add, null); Text(" +") }
                    }
                } else {
                    Button(onClick = { vm.toggleSimple(quest) }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.Check, null); Spacer(Modifier.width(8.dp)); Text(if (done) "Completed today ✓ (undo)" else "Mark complete")
                    }
                }
            }

            item {
                SystemPanel(glow = AccentCyan) {
                    Column {
                        Text("History — last 5 weeks", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.height(8.dp))
                        Canvas(Modifier.fillMaxWidth().height(120.dp)) {
                            val cols = 7; val rows = 5; val gap = 6.dp.toPx()
                            val cw = (size.width - gap * (cols - 1)) / cols
                            val ch = (size.height - gap * (rows - 1)) / rows
                            for (i in 0 until cols * rows) {
                                val day = t - (cols * rows - 1 - i)
                                val c = quest.countOn(day)
                                val frac = (c.toFloat() / quest.targetCount).coerceIn(0f, 1f)
                                val alpha = if (c == 0) 0.12f else 0.25f + 0.75f * frac
                                val r = i / cols; val col = i % cols
                                drawRoundRect(color = cyan.copy(alpha = alpha), topLeft = Offset(col * (cw + gap), r * (ch + gap)), size = Size(cw, ch), cornerRadius = CornerRadius(6f, 6f))
                            }
                        }
                    }
                }
            }

            item {
                OutlinedButton(onClick = { vm.deleteQuest(quest.id); onBack() }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.DeleteOutline, null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(8.dp)); Text("Delete quest", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    if (editing) {
        QuestDialog(quest, onDismiss = { editing = false }, onSave = { vm.updateQuest(it); editing = false }, onDelete = { vm.deleteQuest(quest.id); editing = false; onBack() })
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = AccentGold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
