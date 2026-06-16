package com.friendschat.app.ui.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.friendschat.app.data.AuthRepository
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {

    private val repo = AuthRepository()

    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var info by mutableStateOf<String?>(null)
        private set

    fun clearError() { error = null; info = null }

    fun sendReset(email: String) {
        if (email.isBlank()) { error = "Enter your email first"; return }
        loading = true; error = null; info = null
        viewModelScope.launch {
            runCatching { repo.sendPasswordReset(email) }
                .onSuccess { info = "Password reset link sent to $email. Check your inbox (and spam)." }
                .onFailure { error = it.message ?: "Could not send reset email" }
            loading = false
        }
    }

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            error = "Enter your email and password"
            return
        }
        loading = true
        error = null
        viewModelScope.launch {
            runCatching { repo.login(email, password) }
                .onFailure { error = it.message ?: "Login failed" }
            loading = false
        }
    }

    fun register(name: String, email: String, password: String) {
        val e = email.trim()
        when {
            name.isBlank() -> { error = "Enter a display name"; return }
            e.isBlank() -> { error = "Enter your email"; return }
            !isValidEmailFormat(e) -> { error = "Enter a valid email address"; return }
            !isGoogleEmail(e) -> { error = "Please sign up with a Google (gmail.com) email address"; return }
            password.length < 6 -> { error = "Password must be at least 6 characters"; return }
        }
        loading = true
        error = null
        viewModelScope.launch {
            runCatching { repo.register(name, e, password) }
                .onFailure { error = it.message ?: "Registration failed" }
            loading = false
        }
    }

    /** Basic RFC-ish format check using the platform email matcher. */
    private fun isValidEmailFormat(email: String): Boolean =
        android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()

    /** We require a Google address; ownership/existence is then proven by the
     *  verification email the user must click before entering the app. */
    private fun isGoogleEmail(email: String): Boolean {
        val domain = email.substringAfterLast('@', "").lowercase()
        return domain == "gmail.com" || domain == "googlemail.com"
    }
}
