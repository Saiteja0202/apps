package com.friendschat.app.ui.group

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.friendschat.app.data.Chat
import com.friendschat.app.data.ChatRepository
import com.friendschat.app.data.ChatUser
import kotlinx.coroutines.launch

class GroupInfoViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = ChatRepository()
    val myUid: String get() = repo.myUid

    var chat by mutableStateOf<Chat?>(null)
        private set
    var busy by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    // privacy-friendly add-by-email
    var query by mutableStateOf("")
        private set
    var result by mutableStateOf<ChatUser?>(null)
        private set
    var searching by mutableStateOf(false)
        private set

    private var chatId = ""
    private var started = false

    val isAdmin: Boolean get() = chat?.createdBy == myUid

    fun start(id: String) {
        if (started) return
        started = true; chatId = id
        viewModelScope.launch { repo.observeChat(id).collect { chat = it } }
    }

    fun rename(name: String) {
        viewModelScope.launch { runCatching { repo.renameGroup(chatId, name) }.onFailure { error = it.message } }
    }

    fun setPhoto(uri: Uri) {
        busy = true; error = null
        viewModelScope.launch {
            runCatching { repo.setGroupPhoto(getApplication(), chatId, uri) }.onFailure { error = it.message }
            busy = false
        }
    }

    fun onQuery(q: String) { query = q; if (q.isBlank()) result = null }

    fun search() {
        val email = query.trim()
        if (email.isEmpty()) return
        searching = true; error = null; result = null
        viewModelScope.launch {
            runCatching { repo.findUserByEmail(email) }
                .onSuccess { result = it; if (it == null) error = "No user found with that email" }
                .onFailure { error = it.message }
            searching = false
        }
    }

    fun addFound() {
        val u = result ?: return
        viewModelScope.launch {
            runCatching { repo.addGroupMembers(chatId, listOf(u)) }
                .onSuccess { result = null; query = "" }
                .onFailure { error = it.message }
        }
    }

    fun removeMember(uid: String) {
        viewModelScope.launch { runCatching { repo.removeGroupMember(chatId, uid) }.onFailure { error = it.message } }
    }

    fun leave(onDone: () -> Unit) {
        viewModelScope.launch {
            runCatching { repo.leaveGroup(chatId) }.onSuccess { onDone() }.onFailure { error = it.message }
        }
    }
}
