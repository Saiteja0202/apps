package com.friendschat.app.ui.chats

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GroupAdd
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.friendschat.app.data.ChatUser
import com.friendschat.app.ui.components.Avatar

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NewChatScreen(
    onBack: () -> Unit,
    onChatReady: (String) -> Unit,
    vm: NewChatViewModel = viewModel()
) {
    var groupMode by remember { mutableStateOf(false) }
    var groupName by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (groupMode) "New group" else "New chat") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") } },
                actions = {
                    TextButton(onClick = { groupMode = !groupMode }) {
                        Icon(Icons.Rounded.GroupAdd, null, tint = MaterialTheme.colorScheme.onPrimary)
                        Spacer(Modifier.width(6.dp))
                        Text(if (groupMode) "DM" else "Group", color = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { inner ->
        Column(Modifier.fillMaxSize().padding(inner).padding(16.dp)) {
            if (groupMode) {
                OutlinedTextField(
                    value = groupName, onValueChange = { groupName = it },
                    label = { Text("Group name") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                if (vm.selected.isNotEmpty()) {
                    Spacer(Modifier.size(8.dp))
                    Text("Members (${vm.selected.size})", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.size(4.dp))
                    androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        vm.selected.forEach { u ->
                            AssistChip(onClick = { vm.removeFromGroup(u.uid) },
                                label = { Text(u.name) },
                                trailingIcon = { Icon(Icons.Rounded.Close, "remove", modifier = Modifier.size(16.dp)) })
                        }
                    }
                }
                Spacer(Modifier.size(12.dp))
            }

            // Privacy: find a specific person by their exact email
            Text("Find a person by their email", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.size(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = vm.query, onValueChange = { vm.onQuery(it) },
                    label = { Text("Email address") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Surface(
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(52.dp).clickable { vm.search() }
                ) {
                    androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                        if (vm.searching) CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(22.dp))
                        else Icon(Icons.Rounded.Search, "Search", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }

            vm.error?.let {
                Spacer(Modifier.size(10.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            vm.result?.let { user ->
                Spacer(Modifier.size(14.dp))
                ResultCard(
                    user = user,
                    groupMode = groupMode,
                    onClick = {
                        if (groupMode) vm.addToGroup(user)
                        else vm.startDirect(user, onChatReady)
                    }
                )
            }

            if (!vm.searched && vm.result == null) {
                Spacer(Modifier.size(40.dp))
                Text(
                    "🔒 For privacy, members aren't listed.\nEnter someone's exact email to find them.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (groupMode) {
                Spacer(Modifier.weight(1f))
                TextButton(
                    onClick = { vm.createGroup(groupName) { onChatReady(it) } },
                    enabled = !vm.busy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.Check, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Create group (${vm.selected.size} members)", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ResultCard(user: ChatUser, groupMode: Boolean, onClick: () -> Unit) {
    Surface(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Avatar(name = user.name, photoUrl = user.photoUrl, size = 46.dp, mood = user.mood)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(user.name, fontWeight = FontWeight.SemiBold)
                Text(
                    if (user.isFreeNow) "🟢 free to talk now" else user.email,
                    color = if (user.isFreeNow) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Icon(
                if (groupMode) Icons.Rounded.Add else Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}
