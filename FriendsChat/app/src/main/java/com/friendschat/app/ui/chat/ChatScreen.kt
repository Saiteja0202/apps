package com.friendschat.app.ui.chat

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Forward
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.friendschat.app.data.Message
import com.friendschat.app.data.MessageType
import com.friendschat.app.ui.components.AudioMessage
import com.friendschat.app.ui.formatClock
import com.friendschat.app.ui.formatLastSeen
import com.friendschat.app.ui.humanSize
import com.friendschat.app.ui.theme.SeenBlue
import com.friendschat.app.ui.theme.brandGradient
import com.friendschat.app.ui.theme.pageGradient
import com.friendschat.app.util.AudioRecorder
import java.util.Date

private val QUICK_EMOJIS = listOf("❤️", "👍", "😂", "😮", "😢", "🙏")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    chatId: String,
    onBack: () -> Unit,
    onOpenGroupInfo: (String) -> Unit,
    vm: ChatViewModel = viewModel()
) {
    LaunchedEffect(chatId) { vm.start(chatId) }
    val context = LocalContext.current
    val recorder = remember { AudioRecorder(context) }

    var input by remember { mutableStateOf("") }
    var attachOpen by remember { mutableStateOf(false) }
    var menuFor by remember { mutableStateOf<Message?>(null) }
    var editing by remember { mutableStateOf<Message?>(null) }
    var replyingTo by remember { mutableStateOf<Message?>(null) }
    var forwardSource by remember { mutableStateOf<Message?>(null) }
    var capsuleReveal by remember { mutableStateOf(0L) }
    var capsuleDialog by remember { mutableStateOf(false) }
    var searchMode by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var recording by remember { mutableStateOf(false) }
    var moreMenu by remember { mutableStateOf(false) }
    var reportDialog by remember { mutableStateOf(false) }
    var deleteHistoryDialog by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { u -> u?.let { vm.sendMedia(it, MessageType.IMAGE) } }
    val pickVideo = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { u -> u?.let { vm.sendMedia(it, MessageType.VIDEO) } }
    val pickFile = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { u -> u?.let { vm.sendMedia(it, MessageType.FILE) } }
    val askMic = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted && recorder.start()) recording = true
    }

    val shown = if (searchMode && searchQuery.isNotBlank())
        vm.messages.filter { it.text.contains(searchQuery, ignoreCase = true) && !it.deleted }
    else vm.messages

    LaunchedEffect(vm.messages.size) {
        if (!searchMode && vm.messages.isNotEmpty()) listState.animateScrollToItem(vm.messages.size - 1)
    }

    val live = vm.liveTypingText()
    val subtitle = when {
        vm.otherDeleted -> "Account deleted"
        vm.otherBlocked -> "Unavailable"
        else -> live ?: if (vm.isGroup) "Group · tap for info" else vm.statusText
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (searchMode) {
                        TextField(
                            value = searchQuery, onValueChange = { searchQuery = it },
                            placeholder = { Text("Search messages") }, singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Column(Modifier.clickable(enabled = vm.isGroup) { onOpenGroupInfo(chatId) }) {
                            Text(vm.title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                            if (subtitle != null) Text(
                                subtitle, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                                style = MaterialTheme.typography.labelSmall, maxLines = 1
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { if (searchMode) { searchMode = false; searchQuery = "" } else onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { searchMode = !searchMode; if (!searchMode) searchQuery = "" }) {
                        Icon(Icons.Filled.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                    Box {
                        IconButton(onClick = { moreMenu = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "More", tint = MaterialTheme.colorScheme.onPrimary)
                        }
                        DropdownMenu(expanded = moreMenu, onDismissRequest = { moreMenu = false }) {
                            if (!vm.isGroup) {
                                DropdownMenuItem(
                                    text = { Text(if (vm.otherBlocked) "Unblock" else "Block") },
                                    onClick = { if (vm.otherBlocked) vm.unblockOther() else vm.blockOther(); moreMenu = false }
                                )
                                DropdownMenuItem(text = { Text("Report") }, onClick = { moreMenu = false; reportDialog = true })
                            }
                            DropdownMenuItem(text = { Text("Delete chat history") }, onClick = { moreMenu = false; deleteHistoryDialog = true })
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
            )
        },
        bottomBar = {
            if (vm.otherDeleted) {
                InfoBar("This account has been deleted — you can no longer send messages.")
                return@Scaffold
            }
            if (vm.otherBlocked) {
                InfoBar("Messaging is unavailable for this conversation.")
                return@Scaffold
            }
            Column {
                if (vm.sending) LinearProgressIndicator(Modifier.fillMaxWidth())
                if (recording) RecordingBar { recorder.cancel(); recording = false }
                if (capsuleReveal > 0) Banner(Icons.Filled.HourglassTop, "Time capsule · unlocks ${formatLastSeen(Date(capsuleReveal))}") { capsuleReveal = 0 }
                if (editing != null) Banner(Icons.Filled.Edit, "Editing message") { editing = null; input = "" }
                replyingTo?.let { r ->
                    Banner(Icons.AutoMirrored.Filled.Reply, "Reply to ${r.senderName}: ${snippetOf(r)}") { replyingTo = null }
                }
                MessageInputBar(
                    value = input,
                    onValueChange = { input = it; vm.onInputChange(it) },
                    attachOpen = attachOpen,
                    editingMode = editing != null,
                    recording = recording,
                    onAttachToggle = { attachOpen = !attachOpen },
                    onPickImage = { attachOpen = false; pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    onPickVideo = { attachOpen = false; pickVideo.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)) },
                    onPickFile = { attachOpen = false; pickFile.launch("*/*") },
                    onCapsule = { attachOpen = false; capsuleDialog = true },
                    onMic = {
                        if (recording) {
                            val res = recorder.stop(); recording = false
                            if (res != null) vm.sendVoice(res.first, res.second)
                        } else {
                            val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                            if (granted) { if (recorder.start()) recording = true } else askMic.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    onSend = {
                        val t = input.trim()
                        when {
                            editing != null && t.isNotEmpty() -> { vm.editMessage(editing!!.id, t); editing = null }
                            t.isNotEmpty() -> { vm.sendText(t, replyingTo, capsuleReveal); replyingTo = null; capsuleReveal = 0 }
                        }
                        input = ""
                    }
                )
            }
        }
    ) { inner ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().background(pageGradient()).padding(inner).padding(horizontal = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item { Spacer(Modifier.size(6.dp)) }
            items(shown, key = { it.id.ifBlank { it.hashCode().toString() } }) { msg ->
                val mine = msg.senderId == vm.myUid
                MessageBubble(
                    msg = msg, mine = mine, showSender = vm.isGroup && !mine,
                    seen = vm.seenState(msg),
                    onOpenMedia = { url -> openExternally(context, url, msg.mimeType) },
                    onLongPress = { if (!msg.deleted) menuFor = msg }
                )
            }
            item { Spacer(Modifier.size(6.dp)) }
        }
    }

    // ---- Long-press action menu ----
    menuFor?.let { m ->
        val mine = m.senderId == vm.myUid
        AlertDialog(
            onDismissRequest = { menuFor = null },
            title = { Text("Message") },
            text = {
                Column {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                        QUICK_EMOJIS.forEach { e ->
                            Text(e, fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable { vm.react(m.id, e); menuFor = null }.padding(4.dp),
                                style = MaterialTheme.typography.titleLarge)
                        }
                    }
                    Spacer(Modifier.size(8.dp))
                    MenuRow(Icons.AutoMirrored.Filled.Reply, "Reply") { replyingTo = m; menuFor = null }
                    MenuRow(Icons.Filled.Forward, "Forward") { forwardSource = m; menuFor = null }
                    if (mine && m.type == MessageType.TEXT && !m.isLocked) MenuRow(Icons.Filled.Edit, "Edit") { input = m.text; editing = m; menuFor = null }
                    if (mine) MenuRow(Icons.Filled.Delete, "Delete", MaterialTheme.colorScheme.error) { vm.deleteMessage(m.id); menuFor = null }
                }
            },
            confirmButton = { TextButton(onClick = { menuFor = null }) { Text("Close") } }
        )
    }

    // ---- Forward target picker ----
    forwardSource?.let { src ->
        val targets by vm.forwardTargets.collectAsState()
        AlertDialog(
            onDismissRequest = { forwardSource = null },
            title = { Text("Forward to") },
            text = {
                Column {
                    targets.forEach { c ->
                        Text(
                            c.titleFor(vm.myUid),
                            modifier = Modifier.fillMaxWidth().clickable { vm.forward(c.id, src); forwardSource = null }.padding(vertical = 12.dp),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { forwardSource = null }) { Text("Cancel") } }
        )
    }

    // ---- Time-capsule preset picker ----
    if (capsuleDialog) {
        AlertDialog(
            onDismissRequest = { capsuleDialog = false },
            title = { Text("Time capsule") },
            text = {
                Column {
                    Text("Lock your next message until:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.size(8.dp))
                    val now = System.currentTimeMillis()
                    MenuRow(Icons.Filled.Schedule, "In 1 hour") { capsuleReveal = now + 3_600_000L; capsuleDialog = false }
                    MenuRow(Icons.Filled.Schedule, "In 1 day") { capsuleReveal = now + 86_400_000L; capsuleDialog = false }
                    MenuRow(Icons.Filled.Schedule, "In 1 week") { capsuleReveal = now + 604_800_000L; capsuleDialog = false }
                }
            },
            confirmButton = { TextButton(onClick = { capsuleDialog = false }) { Text("Cancel") } }
        )
    }

    // ---- Report (Genuine / Fake) ----
    if (reportDialog) {
        AlertDialog(
            onDismissRequest = { reportDialog = false },
            title = { Text("Report ${vm.title.substringBefore(' ')}") },
            text = { Text("Is this a genuine person or a fake/suspicious profile? Genuine votes build trust — 10+ earns a verified star.") },
            confirmButton = { TextButton(onClick = { vm.reportOther("genuine"); reportDialog = false }) { Text("Genuine", fontWeight = FontWeight.Bold) } },
            dismissButton = { TextButton(onClick = { vm.reportOther("fake"); reportDialog = false }) { Text("Fake", color = MaterialTheme.colorScheme.error) } }
        )
    }

    // ---- Delete chat history ----
    if (deleteHistoryDialog) {
        AlertDialog(
            onDismissRequest = { deleteHistoryDialog = false },
            title = { Text("Delete chat history?") },
            text = { Text("This permanently removes all messages in this conversation for everyone. The match stays.") },
            confirmButton = {
                TextButton(onClick = { vm.deleteHistory(); deleteHistoryDialog = false }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { deleteHistoryDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun MenuRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, tint: Color = MaterialTheme.colorScheme.onSurface, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = tint)
        Spacer(Modifier.width(12.dp)); Text(label, color = tint)
    }
}

@Composable
private fun Banner(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, onClose: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(text, Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface, maxLines = 1, style = MaterialTheme.typography.bodyMedium)
            IconButton(onClick = onClose) { Icon(Icons.Filled.Close, contentDescription = "Cancel") }
        }
    }
}

@Composable
private fun InfoBar(message: String) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shadowElevation = 8.dp) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Filled.Block, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun RecordingBar(onCancel: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.errorContainer) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Mic, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            Spacer(Modifier.width(8.dp))
            Text("Recording… tap the stop button to send", Modifier.weight(1f), color = MaterialTheme.colorScheme.onErrorContainer)
            TextButton(onClick = onCancel) { Text("Cancel") }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(
    msg: Message, mine: Boolean, showSender: Boolean, seen: SeenState,
    onOpenMedia: (String) -> Unit, onLongPress: () -> Unit
) {
    val textColor = if (mine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val metaColor = if (mine) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant
    val shape = RoundedCornerShape(16.dp, 16.dp, if (mine) 16.dp else 4.dp, if (mine) 4.dp else 16.dp)
    val locked = msg.isLocked
    val isMedia = msg.type != MessageType.TEXT && !msg.deleted && !locked

    Column(Modifier.fillMaxWidth(), horizontalAlignment = if (mine) Alignment.End else Alignment.Start) {
        Box(
            Modifier.widthIn(max = 300.dp).clip(shape)
                .then(
                    if (mine) Modifier.background(brandGradient())
                    else Modifier
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outline, shape)
                )
                .combinedClickable(onClick = { if (isMedia) onOpenMedia(msg.mediaUrl) }, onLongClick = onLongPress)
        ) {
            Column(Modifier.padding(10.dp)) {
                if (showSender && !msg.deleted) {
                    Text(msg.senderName, color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.size(2.dp))
                }
                if (msg.forwarded && !msg.deleted && !locked) {
                    Text("↪ Forwarded", color = metaColor, fontStyle = FontStyle.Italic, style = MaterialTheme.typography.labelSmall)
                    Spacer(Modifier.size(2.dp))
                }
                if (msg.replyToId.isNotBlank() && !msg.deleted) {
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp))
                            .background(if (mine) Color.White.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant)
                            .padding(6.dp)
                    ) {
                        Text(msg.replyToSender, color = if (mine) Color.White else MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Text(msg.replyToText, color = metaColor, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                    }
                    Spacer(Modifier.size(4.dp))
                }
                when {
                    msg.deleted -> Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Block, contentDescription = null, tint = metaColor, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(5.dp)); Text("This message was deleted", color = metaColor, fontStyle = FontStyle.Italic)
                    }
                    locked -> Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Lock, contentDescription = null, tint = textColor, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Column {
                            Text("Time capsule", color = textColor, fontWeight = FontWeight.Bold)
                            Text("Unlocks ${formatLastSeen(Date(msg.revealAt))}", color = metaColor, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    msg.type == MessageType.IMAGE -> AsyncImage(model = msg.mediaUrl, contentDescription = "image",
                        modifier = Modifier.widthIn(max = 260.dp).heightIn(max = 320.dp).clip(RoundedCornerShape(10.dp)))
                    msg.type == MessageType.VIDEO -> MediaCard(Icons.Filled.PlayCircle, msg.mediaName.ifBlank { "Video" }, "Tap to play · ${humanSize(msg.sizeBytes)}", textColor, metaColor)
                    msg.type == MessageType.AUDIO -> AudioMessage(msg.mediaUrl, msg.durationMs, textColor, metaColor)
                    msg.type == MessageType.FILE -> MediaCard(Icons.Filled.InsertDriveFile, msg.mediaName.ifBlank { "File" }, "Tap to open · ${humanSize(msg.sizeBytes)}", textColor, metaColor)
                    else -> Text(msg.text, color = textColor)
                }
                Spacer(Modifier.size(3.dp))
                Row(Modifier.align(Alignment.End), verticalAlignment = Alignment.CenterVertically) {
                    if (msg.edited && !msg.deleted) { Text("edited", style = MaterialTheme.typography.labelSmall, color = metaColor, fontStyle = FontStyle.Italic); Spacer(Modifier.width(5.dp)) }
                    Text(formatClock(msg.timestamp), style = MaterialTheme.typography.labelSmall, color = metaColor)
                    if (mine && !msg.deleted && seen != SeenState.NONE) {
                        Spacer(Modifier.width(4.dp))
                        when (seen) {
                            SeenState.SENT -> Icon(Icons.Filled.Done, "Sent", tint = metaColor, modifier = Modifier.size(15.dp))
                            SeenState.SEEN_SOME -> Icon(Icons.Filled.DoneAll, "Seen by some", tint = metaColor, modifier = Modifier.size(15.dp))
                            SeenState.SEEN_ALL -> Icon(Icons.Filled.DoneAll, "Seen", tint = SeenBlue, modifier = Modifier.size(15.dp))
                            else -> {}
                        }
                    }
                }
            }
        }
        if (msg.reactions.isNotEmpty() && !msg.deleted) {
            Row(Modifier.padding(top = 2.dp, start = 6.dp, end = 6.dp)) {
                val grouped = msg.reactions.values.groupingBy { it }.eachCount()
                grouped.forEach { (emoji, count) ->
                    Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.surfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.padding(end = 4.dp)) {
                        Text(if (count > 1) "$emoji $count" else emoji, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun MediaCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, tint: Color, subColor: Color) {
    Row(Modifier.widthIn(min = 200.dp, max = 260.dp).padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(40.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, maxLines = 1, color = tint)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = subColor)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MessageInputBar(
    value: String, onValueChange: (String) -> Unit, attachOpen: Boolean, editingMode: Boolean, recording: Boolean,
    onAttachToggle: () -> Unit, onPickImage: () -> Unit, onPickVideo: () -> Unit, onPickFile: () -> Unit,
    onCapsule: () -> Unit, onMic: () -> Unit, onSend: () -> Unit
) {
    val showMic = value.isBlank() && !editingMode
    Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 8.dp) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (!editingMode) {
                Box {
                    IconButton(onClick = onAttachToggle) { Icon(Icons.Filled.AttachFile, contentDescription = "Attach", tint = MaterialTheme.colorScheme.primary) }
                    DropdownMenu(expanded = attachOpen, onDismissRequest = onAttachToggle) {
                        DropdownMenuItem(text = { Text("Photo") }, leadingIcon = { Icon(Icons.Filled.Image, null) }, onClick = onPickImage)
                        DropdownMenuItem(text = { Text("Video") }, leadingIcon = { Icon(Icons.Filled.Videocam, null) }, onClick = onPickVideo)
                        DropdownMenuItem(text = { Text("File") }, leadingIcon = { Icon(Icons.Filled.InsertDriveFile, null) }, onClick = onPickFile)
                        DropdownMenuItem(text = { Text("Time capsule") }, leadingIcon = { Icon(Icons.Filled.HourglassTop, null) }, onClick = onCapsule)
                    }
                }
            } else Spacer(Modifier.width(8.dp))
            TextField(
                value = value, onValueChange = onValueChange,
                placeholder = { Text(if (editingMode) "Edit message" else "Message") },
                modifier = Modifier.weight(1f), maxLines = 5,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant, unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(24.dp)
            )
            Spacer(Modifier.width(6.dp))
            Surface(shape = CircleShape, color = if (recording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp).clickable { if (showMic || recording) onMic() else onSend() }) {
                Box(contentAlignment = Alignment.Center) {
                    val icon = when {
                        recording -> Icons.Filled.Stop
                        editingMode -> Icons.Filled.Done
                        showMic -> Icons.Filled.Mic
                        else -> Icons.Filled.Send
                    }
                    Icon(icon, contentDescription = "Send", tint = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    }
}

private fun snippetOf(m: Message): String = when (m.type) {
    MessageType.IMAGE -> "📷 Photo"; MessageType.VIDEO -> "🎥 Video"
    MessageType.AUDIO -> "🎙️ Voice"; MessageType.FILE -> "📎 ${m.mediaName}"
    else -> m.text
}

private fun openExternally(context: android.content.Context, url: String, mime: String) {
    if (url.isBlank()) return
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(Uri.parse(url), mime.ifBlank { "*/*" }); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    runCatching { context.startActivity(intent) }.onFailure { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } }
}
