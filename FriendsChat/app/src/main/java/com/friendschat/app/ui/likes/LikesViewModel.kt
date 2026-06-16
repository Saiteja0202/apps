package com.friendschat.app.ui.likes

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.friendschat.app.data.ChatUser
import com.friendschat.app.data.DatingRepository
import com.friendschat.app.data.Like
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LikesViewModel : ViewModel() {

    private val repo = DatingRepository()

    val likers: StateFlow<List<Pair<Like, ChatUser>>> =
        repo.observeLikers().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    var matchedChatId by mutableStateOf("")
        private set
    var working by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    /** Like back → always a match (they already liked you). Returns via [matchedChatId]. */
    fun likeBack(user: ChatUser, onMatched: (String) -> Unit) {
        if (working) return
        working = true; error = null
        viewModelScope.launch {
            runCatching { repo.like(user) }
                .onSuccess { chatId -> if (chatId.isNotEmpty()) { matchedChatId = chatId; onMatched(chatId) } }
                .onFailure { error = it.message ?: "Could not match" }
            working = false
        }
    }

    fun pass(user: ChatUser) {
        if (working) return
        working = true; error = null
        viewModelScope.launch {
            runCatching { repo.pass(user.uid) }.onFailure { error = it.message }
            working = false
        }
    }

    fun clearError() { error = null }
}
