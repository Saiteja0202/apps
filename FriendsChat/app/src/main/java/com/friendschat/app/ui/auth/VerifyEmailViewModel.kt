package com.friendschat.app.ui.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.friendschat.app.data.AuthRepository
import kotlinx.coroutines.launch

class VerifyEmailViewModel : ViewModel() {

    private val repo = AuthRepository()

    val email: String get() = repo.currentEmail ?: ""

    var checking by mutableStateOf(false)
        private set
    var resending by mutableStateOf(false)
        private set
    var message by mutableStateOf<String?>(null)
        private set

    /** Reloads the user from the server; if the email is now verified, [onVerified] runs. */
    fun recheck(onVerified: () -> Unit) {
        if (checking) return
        checking = true; message = null
        viewModelScope.launch {
            runCatching { repo.reloadUser() }
            checking = false
            if (repo.isEmailVerified()) onVerified()
            else message = "Not verified yet. Open the link in your email, then tap \"I've verified\"."
        }
    }

    fun resend() {
        if (resending) return
        resending = true; message = null
        viewModelScope.launch {
            runCatching { repo.sendEmailVerification() }
                .onSuccess { message = "Verification email re-sent to $email." }
                .onFailure { message = it.message ?: "Could not resend the email" }
            resending = false
        }
    }

    fun logout() = repo.logout()
}
