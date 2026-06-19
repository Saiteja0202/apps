package com.friendschat.app.ui.discover

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.friendschat.app.data.ChatUser
import com.friendschat.app.data.DatingRepository
import com.friendschat.app.data.RelationshipStatus
import com.friendschat.app.ui.components.Avatar
import com.friendschat.app.ui.components.LiveGreen
import com.friendschat.app.ui.components.LivePill
import com.friendschat.app.ui.components.ProfileCard
import com.friendschat.app.ui.components.PulsingDot

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun DiscoverScreen(
    onOpenChat: (String) -> Unit,
    vm: DiscoverViewModel = viewModel()
) {
    val candidates by vm.candidates.collectAsState()
    val filtered = vm.applyFilters(candidates)
    val liveUsers = filtered.filter { it.isFreeNow }
    val current: ChatUser? = filtered.firstOrNull { it.uid == vm.focusedUid } ?: filtered.firstOrNull()

    // Like-with-comment dialog state
    var likeTarget by remember { mutableStateOf<ChatUser?>(null) }
    var likedThing by remember { mutableStateOf("") }
    var comment by remember { mutableStateOf("") }
    var showFilters by remember { mutableStateOf(false) }
    var reportTarget by remember { mutableStateOf<ChatUser?>(null) }
    var blockTarget by remember { mutableStateOf<ChatUser?>(null) }

    // Trusted-star status for the currently shown profile.
    var starred by remember { mutableStateOf(false) }
    LaunchedEffect(current?.uid) {
        starred = false
        val uid = current?.uid
        if (uid != null) {
            val (genuine, _) = runCatching { vm.reportCounts(uid) }.getOrDefault(0 to 0)
            starred = genuine >= DatingRepository.STAR_THRESHOLD
        }
    }

    fun openLike(user: ChatUser, thing: String) {
        likeTarget = user; likedThing = thing; comment = ""
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Surface(color = MaterialTheme.colorScheme.surface) {
                Column {
                    Row(
                        Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 14.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "GENZ",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 3.sp
                            )
                            Text("Discover", style = MaterialTheme.typography.displaySmall)
                        }
                        IconButton(onClick = { showFilters = true }) {
                            Icon(
                                Icons.Filled.Tune,
                                contentDescription = "Filters",
                                tint = if (vm.filtersActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                }
            }
        }
    ) { inner ->
        Box(Modifier.fillMaxSize().padding(inner)) {
            if (current == null) {
                Column(
                    Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("✨", style = MaterialTheme.typography.displaySmall)
                    Spacer(Modifier.height(12.dp))
                    Text("You're all caught up", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "No new people right now. Check back soon — or refine your profile to meet more matches.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                ) {
                    Spacer(Modifier.height(8.dp))

                    // ---- Live Now row (real-time availability) ----
                    if (liveUsers.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            PulsingDot(9.dp)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "LIVE NOW · ${liveUsers.size} FREE TO CHAT",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = LiveGreen
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(liveUsers, key = { it.uid }) { u ->
                                Column(
                                    Modifier.clickable { vm.focus(u.uid) },
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Avatar(name = u.name, photoUrl = u.gallery.firstOrNull() ?: "", size = 60.dp, live = true)
                                    Spacer(Modifier.height(4.dp))
                                    Text(u.name.substringBefore(' '), style = MaterialTheme.typography.labelSmall, maxLines = 1)
                                }
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                    }

                    if (current.isFreeNow) {
                        LivePill("Live now")
                        Spacer(Modifier.height(8.dp))
                    }
                    ProfileCard(
                        user = current,
                        starred = starred,
                        onLikeItem = { thing -> openLike(current, thing) }
                    )
                    Spacer(Modifier.height(20.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        OutlinedButton(
                            onClick = { vm.pass(current) },
                            modifier = Modifier.weight(1f).height(56.dp),
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Icon(Icons.Filled.Close, "Pass", modifier = Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.width(8.dp))
                            Text("Pass", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Button(
                            onClick = { openLike(current, "") },
                            modifier = Modifier.weight(1f).height(56.dp),
                            shape = RoundedCornerShape(50)
                        ) {
                            Icon(Icons.Filled.Favorite, "Like", modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Like", fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        TextButton(onClick = { reportTarget = current }) {
                            Icon(Icons.Filled.Flag, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp)); Text("Report")
                        }
                        Spacer(Modifier.width(8.dp))
                        TextButton(onClick = { blockTarget = current }) {
                            Icon(Icons.Filled.Block, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.width(4.dp)); Text("Block", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    Spacer(Modifier.height(28.dp))
                }
            }

            vm.error?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
                )
            }
        }
    }

    // ---- Like-with-optional-comment dialog ----
    likeTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { likeTarget = null },
            title = { Text("Send a like to ${target.name.substringBefore(' ')}") },
            text = {
                Column {
                    if (likedThing.isNotBlank()) {
                        Text("Liking $likedThing", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                    }
                    OutlinedTextField(
                        value = comment,
                        onValueChange = { comment = it.take(200) },
                        label = { Text("Add a comment (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.like(target, comment, likedThing)
                    likeTarget = null
                }) { Text("Send like", fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { likeTarget = null }) { Text("Cancel") } }
        )
    }

    // ---- Filters ----
    if (showFilters) {
        AlertDialog(
            onDismissRequest = { showFilters = false },
            title = { Text("Filters") },
            text = {
                Column {
                    Text("Age: ${vm.ageMin} – ${vm.ageMax}", fontWeight = FontWeight.SemiBold)
                    RangeSlider(
                        value = vm.ageMin.toFloat()..vm.ageMax.toFloat(),
                        onValueChange = { r -> vm.setAgeRange(r.start.toInt(), r.endInclusive.toInt()) },
                        valueRange = 18f..80f
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("Relationship status", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RelationshipStatus.options.forEach { (key, label) ->
                            FilterChip(
                                selected = vm.relationshipFilter.contains(key),
                                onClick = { vm.toggleRelationship(key) },
                                label = { Text(label) }
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("New here only", Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                        Switch(checked = vm.newHereOnly, onCheckedChange = { vm.toggleNewHere() })
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showFilters = false }) { Text("Done", fontWeight = FontWeight.Bold) } },
            dismissButton = { TextButton(onClick = { vm.clearFilters() }) { Text("Reset") } }
        )
    }

    // ---- Report (Genuine vouch / Fake flag) ----
    reportTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { reportTarget = null },
            title = { Text("Report ${target.name.substringBefore(' ')}") },
            text = { Text("Is this a genuine person, or a fake/suspicious profile? Genuine votes build trust — 10+ earns a verified star.") },
            confirmButton = {
                Button(onClick = { vm.report(target, "genuine"); reportTarget = null }) { Text("Genuine") }
            },
            dismissButton = {
                OutlinedButton(onClick = { vm.report(target, "fake"); reportTarget = null }) {
                    Text("Fake", color = MaterialTheme.colorScheme.error)
                }
            }
        )
    }

    // ---- Block confirm ----
    blockTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { blockTarget = null },
            title = { Text("Block ${target.name.substringBefore(' ')}?") },
            text = { Text("They'll be removed from your discovery and can't appear or message you. You can unblock later from your profile.") },
            confirmButton = {
                TextButton(onClick = { vm.block(target); blockTarget = null }) {
                    Text("Block", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { blockTarget = null }) { Text("Cancel") } }
        )
    }

    // ---- It's a match! ----
    vm.matchedWith?.let { match ->
        val chatId = vm.matchedChatId
        AlertDialog(
            onDismissRequest = { vm.dismissMatch() },
            title = { Text("It's a match! 🔥", fontWeight = FontWeight.Bold) },
            text = { Text("You and ${match.name.substringBefore(' ')} liked each other. Say hello!") },
            confirmButton = {
                TextButton(onClick = {
                    vm.dismissMatch()
                    if (chatId.isNotEmpty()) onOpenChat(chatId)
                }) { Text("Send a message", fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { vm.dismissMatch() }) { Text("Keep exploring") } }
        )
    }
}
