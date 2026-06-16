package com.sololeveling.tasks.ui

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sololeveling.tasks.data.Difficulty
import com.sololeveling.tasks.data.Quest
import com.sololeveling.tasks.data.QuestType

@Composable
fun ManageScreen(vm: MainViewModel, onOpenQuest: (String) -> Unit) {
    var showAdd by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAdd = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) { Icon(Icons.Filled.Add, null); Spacer(Modifier.width(8.dp)); Text("New task", fontWeight = FontWeight.Bold) }
        }
    ) { inner ->
        LazyColumn(Modifier.fillMaxWidth().padding(inner), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            item { Text("ALL TASKS", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge); Spacer(Modifier.height(4.dp)) }
            items(vm.allQuests(), key = { it.id }) { q ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Box(Modifier.weight(1f)) {
                        QuestRow(q, onCheck = { vm.toggleSimple(q) }, onPlus = { vm.increment(q) }, onMinus = { vm.decrement(q) }, onClick = { onOpenQuest(q.id) })
                    }
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    if (showAdd) QuestDialog(null, onDismiss = { showAdd = false }, onSave = { vm.addQuest(it); showAdd = false }, onDelete = null)
}

fun typeLabel(t: QuestType): String = when (t) {
    QuestType.DAILY -> "Every day"
    QuestType.HABIT -> "Some days"
    QuestType.TODO -> "One-time"
}

@Composable
fun QuestDialog(existing: Quest?, onDismiss: () -> Unit, onSave: (Quest) -> Unit, onDelete: (() -> Unit)?) {
    var title by remember { mutableStateOf(existing?.title ?: "") }
    var note by remember { mutableStateOf(existing?.note ?: "") }
    var type by remember { mutableStateOf(existing?.type ?: QuestType.DAILY) }
    var diff by remember { mutableStateOf(existing?.difficulty ?: Difficulty.MEDIUM) }
    var category by remember { mutableStateOf(existing?.category ?: "General") }
    var targetText by remember { mutableStateOf((existing?.targetCount ?: 1).toString()) }
    var weekdays by remember { mutableStateOf(existing?.weekdays ?: (1..7).toSet()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "New task" else "Edit task") },
        text = {
            Column {
                OutlinedTextField(title, { title = it }, label = { Text("Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(note, { note = it }, label = { Text("Note (optional)") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                Text("How often", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    QuestType.values().forEach { tp ->
                        FilterChip(selected = type == tp, onClick = { type = tp }, label = { Text(typeLabel(tp)) })
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("Difficulty (harder = more XP)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Difficulty.values().forEach { df ->
                        FilterChip(selected = diff == df, onClick = { diff = df }, label = { Text("${df.label} ${df.xp}") })
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row {
                    OutlinedTextField(category, { category = it }, label = { Text("Category") }, singleLine = true, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(targetText, { v -> targetText = v.filter { it.isDigit() }.take(4) }, label = { Text("Target") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.width(90.dp))
                }
                if (type == QuestType.HABIT) {
                    Spacer(Modifier.height(8.dp))
                    Text("Repeat on", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        val days = listOf("M" to 1, "T" to 2, "W" to 3, "T" to 4, "F" to 5, "S" to 6, "S" to 7)
                        days.forEach { (lbl, d) ->
                            val on = weekdays.contains(d)
                            AssistChip(
                                onClick = { weekdays = if (on) weekdays - d else weekdays + d },
                                label = { Text(lbl) },
                                colors = androidx.compose.material3.AssistChipDefaults.assistChipColors(
                                    containerColor = if (on) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                )
                            )
                        }
                    }
                }
                if (onDelete != null) {
                    Spacer(Modifier.height(10.dp))
                    TextButton(onClick = onDelete) {
                        Icon(Icons.Filled.DeleteOutline, null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(6.dp)); Text("Delete task", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (title.isBlank()) return@TextButton
                val target = targetText.toIntOrNull()?.coerceAtLeast(1) ?: 1
                val base = existing ?: Quest()
                onSave(base.copy(title = title.trim(), note = note.trim(), type = type, difficulty = diff, category = category.ifBlank { "General" }, targetCount = target, weekdays = if (type == QuestType.HABIT) weekdays.ifEmpty { (1..7).toSet() } else (1..7).toSet()))
            }) { Text("Save", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
