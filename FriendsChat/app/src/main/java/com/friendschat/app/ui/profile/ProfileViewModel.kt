package com.friendschat.app.ui.profile

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
import com.friendschat.app.data.DatingRepository
import com.friendschat.app.data.Like
import com.friendschat.app.data.Prompt
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProfileViewModel(app: Application) : AndroidViewModel(app) {

    private val dating = DatingRepository()
    private val auth = AuthRepository()
    private val chats = ChatRepository()

    val me: StateFlow<ChatUser?> =
        dating.observeMyProfile().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** Connections lists for the profile sub-tabs. */
    val likedYou: StateFlow<List<Pair<Like, ChatUser>>> =
        dating.observeLikers().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val youLiked: StateFlow<List<ChatUser>> =
        dating.observeLikedByMe().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val blocked: StateFlow<List<ChatUser>> =
        dating.observeBlockedUsers().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun unblock(uid: String) {
        viewModelScope.launch { runCatching { dating.unblock(uid) }.onFailure { error = it.message } }
    }

    var uploading by mutableStateOf(false)
        private set
    var saving by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var deleting by mutableStateOf(false)
        private set

    fun clearError() { error = null }

    fun addPhoto(uri: Uri) {
        uploading = true; error = null
        viewModelScope.launch {
            runCatching { dating.addPhoto(getApplication(), uri) }
                .onFailure { error = it.message ?: "Photo upload failed" }
            uploading = false
        }
    }

    fun removePhoto(url: String) {
        viewModelScope.launch {
            runCatching { dating.removePhoto(url) }.onFailure { error = it.message }
        }
    }

    /**
     * Saves the editable profile fields. When [completeOnboarding] is true, also
     * flips `onboarded` so the gate lets the user into the app. [onDone] runs on
     * success. Requires at least one photo + an age to complete onboarding.
     */
    fun save(
        name: String,
        age: Int,
        gender: String,
        interestedIn: List<String>,
        bio: String,
        jobTitle: String,
        relationshipStatus: String,
        country: String,
        location: String,
        prompts: List<Prompt>,
        completeOnboarding: Boolean,
        photoCount: Int,
        onDone: () -> Unit
    ) {
        if (completeOnboarding) {
            if (photoCount == 0) { error = "Add at least one photo to continue"; return }
            if (age < 18) { error = "You must be 18+ and enter your age"; return }
            if (gender.isBlank()) { error = "Pick how you identify"; return }
            if (interestedIn.isEmpty()) { error = "Pick who you'd like to meet"; return }
            if (location.isBlank()) { error = "Pick your city so we can find people near you"; return }
        }
        saving = true; error = null
        val updates = mutableMapOf<String, Any>(
            "name" to name.trim(),
            "age" to age,
            "gender" to gender,
            "interestedIn" to interestedIn,
            "bio" to bio.trim(),
            "jobTitle" to jobTitle.trim(),
            "relationshipStatus" to relationshipStatus,
            "country" to country.trim(),
            "location" to location.trim(),
            "prompts" to prompts.filter { it.question.isNotBlank() && it.answer.isNotBlank() }
                .map { mapOf("question" to it.question, "answer" to it.answer) }
        )
        if (completeOnboarding) updates["onboarded"] = true
        viewModelScope.launch {
            runCatching {
                dating.saveProfile(updates)
                if (name.isNotBlank()) auth.ensureProfileName()
            }.onSuccess { saving = false; onDone() }
                .onFailure { error = it.message ?: "Could not save profile"; saving = false }
        }
    }

    fun deleteAccount(password: String) {
        if (password.isBlank()) { error = "Enter your password to confirm"; return }
        deleting = true; error = null
        viewModelScope.launch {
            runCatching {
                auth.reauthenticate(password)
                chats.deleteAllMyData()
                auth.deleteAuthAccount()
            }.onFailure {
                error = it.message ?: "Could not delete account"
                deleting = false
            }
        }
    }

    /** "Live Now": broadcast that I'm free to chat for the next 60 minutes (or stop). */
    fun goLive(on: Boolean) {
        viewModelScope.launch {
            runCatching { chats.setFreeToTalk(if (on) 60 else 0) }.onFailure { error = it.message }
        }
    }

    fun logout() = auth.logout()
}
