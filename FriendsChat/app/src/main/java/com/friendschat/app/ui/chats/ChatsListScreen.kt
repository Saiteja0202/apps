package com.friendschat.app.ui.chats

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.friendschat.app.data.Chat
import com.friendschat.app.ui.components.Avatar
import com.friendschat.app.ui.formatTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatsListScreen(
    onOpenChat: (String) -> Unit,
    onNewChat: () -> Unit,
    onOpenSettings: () -> Unit,
    vm: ChatsViewModel = viewModel()
) {
    val chats by vm.chats.collectAsState()
    val freeUsers by vm.freeUsers.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("FriendsChat", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                ),
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Person, contentDescription = "Profile & settings")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNewChat,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("New chat", fontWeight = FontWeight.Bold)
            }
        }
    ) { inner ->
        if (chats.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(inner), contentAlignment = Alignment.Center) {
                Text(
                    "No conversations yet.\nTap \"New chat\" to start one.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(inner)) {
                if (freeUsers.isNotEmpty()) {
                    item {
                        Column(Modifier.fillMaxWidth().padding(top = 10.dp)) {
                            Text(
                                "  📡 Free to talk now",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(start = 12.dp, bottom = 6.dp)
                            )
                            androidx.compose.foundation.lazy.LazyRow(
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp)
                            ) {
                                items(freeUsers, key = { it.uid }) { u ->
                                    Column(
                                        Modifier.padding(end = 14.dp).clickable { vm.openDirect(u) { id -> onOpenChat(id) } },
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Avatar(name = u.name, photoUrl = u.photoUrl, size = 56.dp, mood = u.mood)
                                        Spacer(Modifier.size(4.dp))
                                        Text(u.name.substringBefore(' '), style = MaterialTheme.typography.labelSmall, maxLines = 1)
                                    }
                                }
                            }
                            androidx.compose.material3.HorizontalDivider(Modifier.padding(top = 8.dp))
                        }
                    }
                }
                items(chats, key = { it.id }) { chat ->
                    ChatRow(chat = chat, myUid = vm.myUid, onClick = { onOpenChat(chat.id) })
                }
            }
        }
    }
}

@Composable
private fun ChatRow(chat: Chat, myUid: String, onClick: () -> Unit) {
    val title = chat.titleFor(myUid)
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(name = title, photoUrl = "", size = 52.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.size(2.dp))
            Text(
                chat.lastMessage.ifBlank { if (chat.type == "group") "Group · tap to chat" else "Tap to chat" },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            formatTime(chat.lastMessageTime),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelSmall
        )
    }
}
