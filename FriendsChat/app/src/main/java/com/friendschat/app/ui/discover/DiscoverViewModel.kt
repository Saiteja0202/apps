package com.friendschat.app.ui.discover

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.friendschat.app.data.ChatUser
import com.friendschat.app.data.DatingRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DiscoverViewModel : ViewModel() {

    private val repo = DatingRepository()

    /** The pool of people we can still act on (likes/passes filtered out live). */
    val candidates: StateFlow<List<ChatUser>> =
        repo.observeCandidates().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** When set, two people just matched — surface the celebration dialog. */
    var matchedWith by mutableStateOf<ChatUser?>(null)
        private set
    var matchedChatId by mutableStateOf("")
        private set
    var working by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    /** When set (via the "Live now" row), Discover shows this person next. */
    var focusedUid by mutableStateOf<String?>(null)
        private set

    fun focus(uid: String) { focusedUid = uid }

    // ---- Filters ----
    var ageMin by mutableIntStateOf(18)
        private set
    var ageMax by mutableIntStateOf(70)
        private set
    var newHereOnly by mutableStateOf(false)
        private set
    val relationshipFilter: SnapshotStateList<String> = mutableStateListOf()

    fun setAgeRange(min: Int, max: Int) { ageMin = min; ageMax = max }
    fun toggleNewHere() { newHereOnly = !newHereOnly }
    fun toggleRelationship(key: String) {
        if (relationshipFilter.contains(key)) relationshipFilter.remove(key) else relationshipFilter.add(key)
    }
    fun clearFilters() { ageMin = 18; ageMax = 70; newHereOnly = false; relationshipFilter.clear() }

    val filtersActive: Boolean
        get() = ageMin != 18 || ageMax != 70 || newHereOnly || relationshipFilter.isNotEmpty()

    /** Apply the active filters to the candidate list. */
    fun applyFilters(list: List<ChatUser>): List<ChatUser> = list.filter { u ->
        (u.age == 0 || u.age in ageMin..ageMax) &&
            (relationshipFilter.isEmpty() || u.relationshipStatus in relationshipFilter) &&
            (!newHereOnly || u.isNew)
    }

    suspend fun reportCounts(uid: String): Pair<Int, Int> = repo.reportCounts(uid)

    fun block(user: ChatUser) {
        viewModelScope.launch { runCatching { repo.block(user.uid) }.onFailure { error = it.message } }
    }

    fun report(user: ChatUser, type: String) {
        viewModelScope.launch { runCatching { repo.report(user.uid, type) }.onFailure { error = it.message } }
    }

    fun like(user: ChatUser, comment: String = "", likedThing: String = "") {
        if (working) return
        working = true; error = null
        viewModelScope.launch {
            runCatching { repo.like(user, comment, likedThing) }
                .onSuccess { chatId -> if (chatId.isNotEmpty()) { matchedWith = user; matchedChatId = chatId } }
                .onFailure { error = it.message ?: "Could not send like" }
            focusedUid = null
            working = false
        }
    }

    fun pass(user: ChatUser) {
        if (working) return
        working = true; error = null
        viewModelScope.launch {
            runCatching { repo.pass(user.uid) }.onFailure { error = it.message }
            focusedUid = null
            working = false
        }
    }

    fun dismissMatch() { matchedWith = null }
    fun clearError() { error = null }
}
