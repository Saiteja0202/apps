package com.sololeveling.tasks.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sololeveling.tasks.data.Quest
import com.sololeveling.tasks.data.today
import com.sololeveling.tasks.ui.theme.AccentBlue
import com.sololeveling.tasks.ui.theme.AccentCyan
import com.sololeveling.tasks.ui.theme.AccentGold
import com.sololeveling.tasks.ui.theme.AccentViolet
import com.sololeveling.tasks.ui.theme.Danger
import com.sololeveling.tasks.ui.theme.HpGreen

@Composable
fun SystemPanel(glow: Color = AccentBlue, content: @Composable () -> Unit) {
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, glow.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) { content() }
}

@Composable
fun rankColor(rank: String): Color = when (rank) {
    "S" -> AccentGold; "A" -> AccentViolet; "B" -> AccentCyan; "C" -> AccentBlue
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

@Composable
fun PlayerHeaderCard(vm: MainViewModel) {
    val p = vm.player
    SystemPanel(glow = AccentBlue) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(58.dp)) {
                    Box(Modifier.fillMaxSize().clip(CircleShape).background(Brush.linearGradient(listOf(AccentBlue, AccentViolet))))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("LV", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelSmall)
                        Text("${p.level}", color = Color.White, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleLarge)
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(vm.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Paid, null, tint = AccentGold, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(3.dp))
                        Text("${p.gold}", color = AccentGold, style = MaterialTheme.typography.labelSmall)
                        Spacer(Modifier.width(10.dp))
                        Icon(Icons.Filled.LocalFireDepartment, null, tint = AccentGold, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(3.dp))
                        Text("${p.streak}-day streak", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                    }
                }
                RankBadge(vm.rank)
            }
            Spacer(Modifier.height(14.dp))
            BarLabeled("XP", p.xp, vm.xpForNext, listOf(AccentCyan, AccentBlue, AccentViolet))
            Spacer(Modifier.height(8.dp))
            BarLabeled("Energy", p.hp, 100, listOf(HpGreen, AccentCyan), icon = true)
        }
    }
}

@Composable
private fun RankBadge(rank: String) {
    val c = rankColor(rank)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(c.copy(alpha = 0.16f)).border(1.5.dp, c, RoundedCornerShape(12.dp))
    ) { Text(rank, color = c, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleLarge) }
}

@Composable
private fun BarLabeled(label: String, value: Int, max: Int, colors: List<Color>, icon: Boolean = false) {
    val frac = (value.toFloat() / max.coerceAtLeast(1)).coerceIn(0f, 1f)
    val anim by animateFloatAsState(frac, tween(500), label = label)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon) Icon(Icons.Filled.Favorite, null, tint = HpGreen, modifier = Modifier.size(12.dp))
            if (icon) Spacer(Modifier.width(4.dp))
            Text(label, color = colors.first(), style = MaterialTheme.typography.labelSmall)
        }
        Text("$value / $max", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
    }
    Spacer(Modifier.height(4.dp))
    Box(Modifier.fillMaxWidth().height(10.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant)) {
        Box(Modifier.fillMaxWidth(anim).fillMaxSize().clip(CircleShape).background(Brush.horizontalGradient(colors)))
    }
}

@Composable
fun QuestRow(
    quest: Quest,
    onCheck: () -> Unit,
    onPlus: () -> Unit,
    onMinus: () -> Unit,
    onClick: () -> Unit
) {
    val t = today()
    val done = quest.isDoneOn(t)
    val counter = quest.targetCount > 1
    val accent = if (done) AccentCyan else MaterialTheme.colorScheme.outline
    val streak = quest.currentStreak(t)
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, accent.copy(alpha = if (done) 0.9f else 0.4f), RoundedCornerShape(14.dp))
            .clickable { onClick() }.padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(34.dp).clip(RoundedCornerShape(9.dp))
                .background(if (done) AccentCyan else Color.Transparent)
                .border(1.5.dp, if (done) AccentCyan else MaterialTheme.colorScheme.onSurfaceVariant, RoundedCornerShape(9.dp))
                .clickable { onCheck() }
        ) {
            if (done) Icon(Icons.Filled.Check, "done", tint = Color(0xFF03121A), modifier = Modifier.size(20.dp))
            else if (counter) Text("${quest.countOn(t)}", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelSmall)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(quest.title, color = if (done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 3.dp)) {
                Chip(quest.category)
                Spacer(Modifier.width(6.dp))
                Text("+${quest.difficulty.xp} XP", color = AccentGold, style = MaterialTheme.typography.labelSmall)
                if (counter) { Spacer(Modifier.width(6.dp)); Text("${quest.countOn(t)}/${quest.targetCount}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall) }
                if (streak > 0) {
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.Filled.LocalFireDepartment, null, tint = AccentGold, modifier = Modifier.size(12.dp))
                    Text("$streak", color = AccentGold, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        if (counter && !done) {
            CircleBtn(Icons.Filled.Remove) { onMinus() }
            Spacer(Modifier.width(6.dp))
            CircleBtn(Icons.Filled.Add) { onPlus() }
        }
    }
}

@Composable
private fun CircleBtn(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(30.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer).clickable { onClick() }
    ) { Icon(icon, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(18.dp)) }
}

@Composable
fun Chip(text: String) {
    Box(Modifier.clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = 7.dp, vertical = 2.dp)) {
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
    }
}
