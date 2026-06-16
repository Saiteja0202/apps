package com.friendschat.app.ui.chats

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.friendschat.app.data.AuthRepository
import com.friendschat.app.data.ChatRepository
import com.friendschat.app.data.ChatUser
import kotlinx.coroutines.launch

class NewChatViewModel : ViewModel() {

    private val repo = ChatRepository()
    private val auth = AuthRepository()
    private val myName: String get() = auth.currentName ?: "Me"

    var query by mutableStateOf("")
        private set
    var result by mutableStateOf<ChatUser?>(null)
        private set
    var searching by mutableStateOf(false)
        private set
    var searched by mutableStateOf(false)   // a search has been run at least once
        private set
    var busy by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    /** members picked for a group (built up by searching emails one at a time) */
    var selected by mutableStateOf<List<ChatUser>>(emptyList())
        private set

    fun onQuery(q: String) { query = q; if (q.isBlank()) { result = null; searched = false } }

    fun search() {
        val email = query.trim()
        if (email.isEmpty()) return
        searching = true; error = null; result = null
        viewModelScope.launch {
            runCatching { repo.findUserByEmail(email) }
                .onSuccess { result = it; searched = true; if (it == null) error = "No user found with that email" }
                .onFailure { error = it.message ?: "Search failed" }
            searching = false
        }
    }

    fun addToGroup(user: ChatUser) {
        if (selected.none { it.uid == user.uid }) selected = selected + user
        query = ""; result = null; searched = false
    }

    fun removeFromGroup(uid: String) { selected = selected.filterNot { it.uid == uid } }

    fun startDirect(user: ChatUser, onReady: (String) -> Unit) {
        busy = true; error = null
        viewModelScope.launch {
            runCatching { repo.openDirectChat(user, myName) }
                .onSuccess { onReady(it) }
                .onFailure { error = it.message ?: "Could not open chat" }
            busy = false
        }
    }

    fun createGroup(name: String, onReady: (String) -> Unit) {
        if (name.isBlank()) { error = "Enter a group name"; return }
        if (selected.isEmpty()) { error = "Add at least one member by email"; return }
        busy = true; error = null
        viewModelScope.launch {
            runCatching { repo.createGroup(name, selected, myName) }
                .onSuccess { onReady(it) }
                .onFailure { error = it.message ?: "Could not create group" }
            busy = false
        }
    }
}
