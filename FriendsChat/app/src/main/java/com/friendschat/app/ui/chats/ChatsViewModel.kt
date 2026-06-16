package com.friendschat.app.ui.chats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.friendschat.app.data.AuthRepository
import com.friendschat.app.data.Chat
import com.friendschat.app.data.ChatRepository
import com.friendschat.app.data.ChatUser
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChatsViewModel : ViewModel() {

    private val repo = ChatRepository()
    private val auth = AuthRepository()

    val myUid: String get() = repo.myUid
    val myName: String get() = auth.currentName ?: "Me"

    val chats: StateFlow<List<Chat>> =
        repo.observeChats().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Existing contacts who tapped "free to talk" and whose window is still open.
     *  Privacy: only people you already chat with appear here — never strangers. */
    val freeUsers: StateFlow<List<ChatUser>> =
        combine(repo.observeChats(), repo.observeUsers()) { chats, users ->
            val contacts = chats.flatMap { it.members }.toSet() - myUid
            users.filter { it.isFreeNow && contacts.contains(it.uid) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun openDirect(user: ChatUser, onReady: (String) -> Unit) {
        viewModelScope.launch {
            runCatching { repo.openDirectChat(user, myName) }.onSuccess { onReady(it) }
        }
    }

    fun logout() = auth.logout()
}
