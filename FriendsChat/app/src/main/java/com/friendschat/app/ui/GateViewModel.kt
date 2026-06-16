package com.friendschat.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.friendschat.app.data.ChatUser
import com.friendschat.app.data.DatingRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/** Observes the signed-in user's profile so the router can decide whether to
 *  show onboarding or the main app. */
class GateViewModel : ViewModel() {
    private val repo = DatingRepository()
    val me: StateFlow<ChatUser?> =
        repo.observeMyProfile().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
}
