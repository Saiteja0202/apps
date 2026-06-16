package com.sololeveling.tasks.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sololeveling.tasks.data.Shadows
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun ProfileScreen(vm: MainViewModel) {
    var resetConfirm by remember { mutableStateOf(false) }
    val earnedShadows = vm.earnedShadows()

    LazyColumn(Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("YOUR PROFILE", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge) }
        item { PlayerHeaderCard(vm) }

        // ---- Focus timer ----
        item { FocusTimerCard(vm) }

        // ---- Shadow army ----
        item {
            SystemPanel {
                Column {
                    Text("COLLECTION  (${earnedShadows.size}/${Shadows.ALL.size})", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(10.dp))
                    val rows = Shadows.ALL.chunked(4)
                    rows.forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            row.forEach { s ->
                                val unlocked = earnedShadows.contains(s.id)
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 6.dp)) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(48.dp).clip(CircleShape).background(if (unlocked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)) {
                                        Text(if (unlocked) s.emoji else "🔒", style = MaterialTheme.typography.titleLarge)
                                    }
                                    Text(if (unlocked) s.name else "${s.unlockAt}✓", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                    Text("Unlock these by finishing more tasks.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // ---- Mood ----
        item {
            SystemPanel {
                Column {
                    Text("TODAY'S MOOD", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        listOf("😞" to 1, "😐" to 2, "🙂" to 3, "😄" to 4, "🤩" to 5).forEach { (e, v) ->
                            Text(e, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.clickable { vm.setMood(v) }.padding(4.dp))
                        }
                    }
                }
            }
        }

        // ---- Settings ----
        item {
            SystemPanel {
                Column {
                    Text("SETTINGS", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Dark theme", Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface)
                        Switch(checked = vm.data.themeDark, onCheckedChange = { vm.setTheme(it) })
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Daily reminder", color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("08:00" to 8 * 60, "12:00" to 12 * 60, "20:00" to 20 * 60, "22:00" to 22 * 60, "Off" to -1).forEach { (lbl, m) ->
                            val sel = vm.data.dailyReminderMinutes == m
                            Box(Modifier.clip(RoundedCornerShape(20.dp)).background(if (sel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant).border(1.dp, if (sel) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(20.dp)).clickable { vm.setReminder(m) }.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                Text(lbl, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    TextButton(onClick = { resetConfirm = true }) { Text("Reset all progress", color = MaterialTheme.colorScheme.error) }
                }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }

    if (resetConfirm) {
        AlertDialog(
            onDismissRequest = { resetConfirm = false },
            title = { Text("Reset everything?") },
            text = { Text("This wipes your level, quests, streaks and shadows back to a fresh start. This cannot be undone.") },
            confirmButton = { TextButton(onClick = { vm.resetAll(); resetConfirm = false }) { Text("Reset", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) } },
            dismissButton = { TextButton(onClick = { resetConfirm = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun FocusTimerCard(vm: MainViewModel) {
    var minutes by remember { mutableIntStateOf(25) }
    var running by remember { mutableStateOf(false) }
    var remaining by remember { mutableIntStateOf(25 * 60) }

    LaunchedEffect(running) {
        if (running) {
            while (remaining > 0 && running) { delay(1000); remaining-- }
            if (remaining <= 0) { running = false; vm.completeFocusSession(minutes); remaining = minutes * 60 }
        }
    }

    SystemPanel {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text("FOCUS TIMER", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(6.dp))
            Text(String.format(Locale.US, "%02d:%02d", remaining / 60, remaining % 60), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            if (!running) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(15, 25, 50).forEach { m ->
                        val sel = minutes == m
                        Box(Modifier.clip(RoundedCornerShape(20.dp)).background(if (sel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant).clickable { minutes = m; remaining = m * 60 }.padding(horizontal = 14.dp, vertical = 8.dp)) {
                            Text("$m min", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.primary).clickable {
                if (running) { running = false; remaining = minutes * 60 } else { remaining = minutes * 60; running = true }
            }.padding(horizontal = 18.dp, vertical = 10.dp)) {
                Icon(if (running) Icons.Filled.Stop else Icons.Filled.PlayArrow, null, tint = MaterialTheme.colorScheme.onPrimary)
                Spacer(Modifier.width(8.dp))
                Text(if (running) "Stop" else "Start focus (+$minutes XP)", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
            }
        }
    }
}
