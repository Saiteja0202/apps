package com.friendschat.app.ui.chat

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.friendschat.app.data.AuthRepository
import com.friendschat.app.data.Chat
import com.friendschat.app.data.ChatRepository
import com.friendschat.app.data.ChatUser
import com.friendschat.app.data.DatingRepository
import com.friendschat.app.data.Message
import com.friendschat.app.data.MessageType
import com.friendschat.app.ui.formatLastSeen
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SeenState { NONE, SENT, SEEN_SOME, SEEN_ALL }

class ChatViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = ChatRepository()
    private val auth = AuthRepository()
    private val dating = DatingRepository()

    val myUid: String get() = repo.myUid
    val otherUid: String get() = members.firstOrNull { it != myUid } ?: ""
    private val myName: String get() = auth.currentName ?: "Me"

    var title by mutableStateOf("Chat")
        private set
    var isGroup by mutableStateOf(false)
        private set
    var members by mutableStateOf<List<String>>(emptyList())
        private set
    var messages by mutableStateOf<List<Message>>(emptyList())
        private set
    var sending by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    /** True when the other person in a 1-on-1 has deleted their account. */
    var otherDeleted by mutableStateOf(false)
        private set
    /** True when either side has blocked the other. */
    var otherBlocked by mutableStateOf(false)
        private set

    private var memberNamesMap: Map<String, String> = emptyMap()
    private var chatTyping by mutableStateOf<Map<String, Long>>(emptyMap())
    private var chatDraft by mutableStateOf<Map<String, String>>(emptyMap())
    private var otherUser by mutableStateOf<ChatUser?>(null)

    /** Chats this user is in — used as forward targets. */
    val forwardTargets: StateFlow<List<Chat>> =
        repo.observeChats().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var chatId: String = ""
    private var started = false
    private var otherStarted = false
    private var typingClearJob: Job? = null
    private var lastTypingWrite = 0L

    fun start(id: String) {
        if (started) return
        started = true
        chatId = id
        viewModelScope.launch {
            repo.observeChat(id).collect { chat ->
                if (chat != null) {
                    title = chat.titleFor(myUid)
                    isGroup = chat.type == "group"
                    members = chat.members
                    memberNamesMap = chat.memberNames
                    chatTyping = chat.typing
                    chatDraft = chat.draft
                    if (!isGroup && !otherStarted) {
                        val other = chat.members.firstOrNull { it != myUid }
                        if (other != null) {
                            otherStarted = true
                            viewModelScope.launch { repo.observeUser(other).collect { otherUser = it } }
                            viewModelScope.launch { repo.observeUserExists(other).collect { otherDeleted = !it } }
                            viewModelScope.launch { dating.observeBlockRelation(other).collect { otherBlocked = it } }
                        }
                    }
                }
            }
        }
        viewModelScope.launch {
            repo.observeMessages(id).collect { list ->
                messages = list
                val unread = list.filter {
                    it.id.isNotBlank() && it.senderId != myUid && !it.readBy.contains(myUid) && !it.isLocked
                }.map { it.id }
                if (unread.isNotEmpty()) viewModelScope.launch { runCatching { repo.markRead(id, unread) } }
            }
        }
    }

    /** Live "typing…"/draft-preview text for the header, or null. */
    fun liveTypingText(): String? {
        val now = System.currentTimeMillis()
        val typers = chatTyping.filterKeys { it != myUid }.filterValues { now - it < 6000 }.keys
        if (typers.isEmpty()) return null
        val uid = typers.first()
        val who = memberNamesMap[uid]?.substringBefore(' ') ?: "Someone"
        val draft = chatDraft[uid].orEmpty()
        return when {
            draft.isNotBlank() && isGroup -> "$who: $draft"
            draft.isNotBlank() -> draft
            isGroup -> "$who is typing…"
            else -> "typing…"
        }
    }

    val statusText: String?
        get() {
            if (isGroup) return null
            val u = otherUser ?: return null
            return when {
                u.isFreeNow -> "🟢 free to talk now"
                u.online -> "online"
                u.lastSeen != null -> "offline · last seen ${formatLastSeen(u.lastSeen)}"
                else -> "offline"
            }
        }

    val headerPhoto: String get() = otherUser?.photoUrl ?: ""
    val headerMood: String get() = otherUser?.mood ?: ""

    fun seenState(msg: Message): SeenState {
        if (msg.senderId != myUid) return SeenState.NONE
        val others = members.filter { it != myUid }
        if (others.isEmpty()) return SeenState.SENT
        val seenCount = others.count { msg.readBy.contains(it) }
        return when {
            seenCount == 0 -> SeenState.SENT
            seenCount == others.size -> SeenState.SEEN_ALL
            else -> SeenState.SEEN_SOME
        }
    }

    private fun snippet(m: Message): String = when (m.type) {
        MessageType.IMAGE -> "📷 Photo"
        MessageType.VIDEO -> "🎥 Video"
        MessageType.AUDIO -> "🎙️ Voice message"
        MessageType.FILE -> "📎 ${m.mediaName}"
        else -> m.text
    }

    fun onInputChange(text: String) {
        if (text.isNotEmpty()) {
            val now = System.currentTimeMillis()
            if (now - lastTypingWrite > 1200) {
                lastTypingWrite = now
                viewModelScope.launch { repo.setDraft(chatId, text) }
            }
            typingClearJob?.cancel()
            typingClearJob = viewModelScope.launch {
                delay(4000); lastTypingWrite = 0; repo.setDraft(chatId, "")
            }
        } else stopTyping()
    }

    fun stopTyping() {
        typingClearJob?.cancel(); lastTypingWrite = 0
        viewModelScope.launch { repo.setDraft(chatId, "") }
    }

    fun sendText(text: String, replyTo: Message? = null, revealAt: Long = 0L) {
        if (text.isBlank()) return
        if (otherDeleted) { error = "This account has been deleted"; return }
        if (otherBlocked) { error = "Messaging is unavailable"; return }
        stopTyping()
        viewModelScope.launch {
            runCatching {
                repo.sendText(
                    chatId, text, myName,
                    replyToId = replyTo?.id ?: "",
                    replyToText = replyTo?.let { snippet(it) } ?: "",
                    replyToSender = replyTo?.senderName ?: "",
                    revealAt = revealAt
                )
            }.onFailure { error = it.message ?: "Failed to send" }
        }
    }

    fun editMessage(msgId: String, newText: String) {
        viewModelScope.launch { runCatching { repo.editMessage(chatId, msgId, newText) }.onFailure { error = it.message } }
    }

    fun deleteMessage(msgId: String) {
        viewModelScope.launch { runCatching { repo.deleteMessage(chatId, msgId) }.onFailure { error = it.message } }
    }

    fun react(msgId: String, emoji: String) {
        viewModelScope.launch { runCatching { repo.react(chatId, msgId, emoji) }.onFailure { error = it.message } }
    }

    fun forward(toChatId: String, msg: Message) {
        viewModelScope.launch { runCatching { repo.forwardMessage(toChatId, msg, myName) }.onFailure { error = it.message } }
    }

    fun sendVoice(uri: Uri, durationMs: Long) {
        if (otherDeleted) { error = "This account has been deleted"; return }
        if (otherBlocked) { error = "Messaging is unavailable"; return }
        sending = true; error = null
        viewModelScope.launch {
            runCatching { repo.sendVoice(getApplication(), chatId, uri, durationMs, myName) }
                .onFailure { error = it.message ?: "Voice send failed" }
            sending = false
        }
    }

    fun sendMedia(uri: Uri, type: String) {
        if (otherDeleted) { error = "This account has been deleted"; return }
        if (otherBlocked) { error = "Messaging is unavailable"; return }
        sending = true; error = null
        viewModelScope.launch {
            runCatching { repo.sendMedia(getApplication(), chatId, uri, type, myName) }
                .onFailure { error = it.message ?: "Upload failed" }
            sending = false
        }
    }

    fun clearError() { error = null }

    // ---- Safety actions ----
    fun blockOther() {
        val u = otherUid; if (u.isBlank()) return
        viewModelScope.launch { runCatching { dating.block(u) }.onFailure { error = it.message } }
    }

    fun unblockOther() {
        val u = otherUid; if (u.isBlank()) return
        viewModelScope.launch { runCatching { dating.unblock(u) }.onFailure { error = it.message } }
    }

    fun reportOther(type: String) {
        val u = otherUid; if (u.isBlank()) return
        viewModelScope.launch { runCatching { dating.report(u, type) }.onFailure { error = it.message } }
    }

    fun deleteHistory() {
        viewModelScope.launch { runCatching { repo.deleteChatHistory(chatId) }.onFailure { error = it.message } }
    }
}
