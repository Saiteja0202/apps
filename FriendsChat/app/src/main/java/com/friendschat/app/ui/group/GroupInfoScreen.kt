package com.friendschat.app.ui.group

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.PersonRemove
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.friendschat.app.data.ChatUser
import com.friendschat.app.ui.components.Avatar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupInfoScreen(
    chatId: String,
    onBack: () -> Unit,
    onLeft: () -> Unit,
    vm: GroupInfoViewModel = viewModel()
) {
    LaunchedEffect(chatId) { vm.start(chatId) }
    val chat = vm.chat
    var renameDialog by remember { mutableStateOf(false) }
    var addDialog by remember { mutableStateOf(false) }
    var nameField by remember { mutableStateOf("") }

    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { u -> u?.let { vm.setPhoto(it) } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Group info", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { inner ->
        if (chat == null) {
            Box(Modifier.fillMaxSize().padding(inner), contentAlignment = Alignment.Center) { Text("Loading…") }
            return@Scaffold
        }
        val members = chat.members
        LazyColumn(Modifier.fillMaxSize().padding(inner)) {
            item {
                Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        Avatar(name = chat.name.ifBlank { "Group" }, photoUrl = chat.photoUrl, size = 100.dp)
                        if (vm.isAdmin) Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp).clickable { pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                            Box(contentAlignment = Alignment.Center) { Icon(Icons.Rounded.PhotoCamera, "Change photo", tint = Color.White, modifier = Modifier.size(18.dp)) }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(chat.name.ifBlank { "Group" }, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        if (vm.isAdmin) {
                            Spacer(Modifier.width(6.dp))
                            IconButton(onClick = { nameField = chat.name; renameDialog = true }) { Icon(Icons.Rounded.Edit, "Rename", tint = MaterialTheme.colorScheme.primary) }
                        }
                    }
                    Text("${members.size} members", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    vm.error?.let { Spacer(Modifier.height(6.dp)); Text(it, color = MaterialTheme.colorScheme.error) }
                }
            }
            if (vm.isAdmin) item {
                Row(Modifier.fillMaxWidth().clickable { addDialog = true }.padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.PersonAdd, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(14.dp)); Text("Add members", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                }
            }
            items(members, key = { it }) { uid ->
                val name = chat.memberNames[uid] ?: "Member"
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Avatar(name = name, photoUrl = "", size = 42.dp)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(if (uid == vm.myUid) "$name (you)" else name, fontWeight = FontWeight.SemiBold)
                        if (uid == chat.createdBy) Text("Admin", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                    }
                    if (vm.isAdmin && uid != vm.myUid) {
                        IconButton(onClick = { vm.removeMember(uid) }) { Icon(Icons.Rounded.PersonRemove, "Remove", tint = MaterialTheme.colorScheme.error) }
                    }
                }
            }
            item {
                OutlinedButton(onClick = { vm.leave(onLeft) }, modifier = Modifier.fillMaxWidth().padding(20.dp).height(50.dp)) {
                    Icon(Icons.AutoMirrored.Rounded.Logout, null); Spacer(Modifier.width(8.dp)); Text("Leave group", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (renameDialog) {
        AlertDialog(
            onDismissRequest = { renameDialog = false },
            title = { Text("Rename group") },
            text = { OutlinedTextField(value = nameField, onValueChange = { nameField = it }, singleLine = true, modifier = Modifier.fillMaxWidth()) },
            confirmButton = { TextButton(onClick = { vm.rename(nameField); renameDialog = false }) { Text("Save") } },
            dismissButton = { TextButton(onClick = { renameDialog = false }) { Text("Cancel") } }
        )
    }

    if (addDialog) {
        val already = (vm.chat?.members ?: emptyList())
        AlertDialog(
            onDismissRequest = { addDialog = false },
            title = { Text("Add member by email") },
            text = {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = vm.query, onValueChange = { vm.onQuery(it) },
                            label = { Text("Email") }, singleLine = true, modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        IconButton(onClick = { vm.search() }) { Icon(Icons.Rounded.Search, "Search", tint = MaterialTheme.colorScheme.primary) }
                    }
                    vm.error?.let { Spacer(Modifier.height(6.dp)); Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                    vm.result?.let { u ->
                        Spacer(Modifier.height(10.dp))
                        val inGroup = already.contains(u.uid)
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Avatar(name = u.name, photoUrl = u.photoUrl, size = 38.dp, mood = u.mood)
                            Spacer(Modifier.width(12.dp)); Text(u.name, Modifier.weight(1f))
                            if (inGroup) Text("Already in", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                            else TextButton(onClick = { vm.addFound(); addDialog = false }) { Icon(Icons.Rounded.Check, null); Spacer(Modifier.width(4.dp)); Text("Add") }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { addDialog = false }) { Text("Done") } }
        )
    }
}
