package com.friendschat.app.ui.matches

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.friendschat.app.data.Chat
import com.friendschat.app.data.ChatRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

/** A match row = the chat plus the matched person's photo, resolved live. */
data class MatchRow(
    val chat: Chat,
    val title: String,
    val photoUrl: String,
    val deleted: Boolean = false,
    val unread: Boolean = false,
    val online: Boolean = false
)

class MatchesViewModel : ViewModel() {

    private val repo = ChatRepository()
    val myUid: String get() = repo.myUid

    /** Ticks periodically so online-status freshness is re-evaluated even when no
     *  Firestore document changes (e.g. someone's app was killed and just goes stale). */
    private fun ticker(): Flow<Unit> = flow {
        while (true) { emit(Unit); delay(30_000) }
    }

    val matches: StateFlow<List<MatchRow>> =
        combine(repo.observeChats(), repo.observeUsers(), ticker()) { chats, users, _ ->
            val byId = users.associateBy { it.uid }
            chats.filter { it.type == "direct" }.map { chat ->
                val otherUid = chat.otherUid(myUid)
                val other = byId[otherUid]
                // Once the user directory has loaded, a missing other-user means
                // they deleted their account.
                val deleted = users.isNotEmpty() && otherUid.isNotBlank() && other == null
                MatchRow(
                    chat = chat,
                    title = if (deleted) "Account deleted" else chat.titleFor(myUid),
                    photoUrl = other?.photoUrl ?: chat.photoUrl,
                    deleted = deleted,
                    unread = chat.hasUnreadFor(myUid),
                    online = other?.isOnline == true   // true presence only (fresh heartbeat)
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
