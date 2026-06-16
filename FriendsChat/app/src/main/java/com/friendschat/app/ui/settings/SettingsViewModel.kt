package com.friendschat.app.ui.settings

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.friendschat.app.data.AuthRepository
import com.friendschat.app.data.ChatRepository
import com.friendschat.app.data.ChatUser
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = ChatRepository()
    private val auth = AuthRepository()

    val me: StateFlow<ChatUser?> =
        repo.observeMe().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    var uploading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var deleting by mutableStateOf(false)
        private set

    fun clearError() { error = null }

    /**
     * Permanently deletes the account: re-auth with the password, wipe Firestore
     * data, then delete the Auth user. On success the auth-state listener routes
     * back to the login screen automatically.
     */
    fun deleteAccount(password: String) {
        if (password.isBlank()) { error = "Enter your password to confirm"; return }
        deleting = true; error = null
        viewModelScope.launch {
            runCatching {
                auth.reauthenticate(password)
                repo.deleteAllMyData()
                auth.deleteAuthAccount()
            }.onFailure {
                error = it.message ?: "Could not delete account"
                deleting = false
            }
        }
    }

    fun uploadPhoto(uri: Uri) {
        uploading = true; error = null
        viewModelScope.launch {
            runCatching { repo.updateProfilePhoto(getApplication(), uri) }
                .onFailure { error = it.message ?: "Upload failed" }
            uploading = false
        }
    }

    fun setMood(mood: String) {
        viewModelScope.launch { runCatching { repo.setMood(mood) }.onFailure { error = it.message } }
    }

    fun setFreeToTalk(minutes: Int) {
        viewModelScope.launch { runCatching { repo.setFreeToTalk(minutes) }.onFailure { error = it.message } }
    }

    fun logout() = auth.logout()
}
