package com.friendschat.app.data

import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

/** Handles email/password authentication and the user profile document. */
class AuthRepository {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    val currentUid: String? get() = auth.currentUser?.uid
    val currentName: String? get() = auth.currentUser?.displayName
    val currentEmail: String? get() = auth.currentUser?.email

    fun isLoggedIn(): Boolean = auth.currentUser != null

    /** True once the user has clicked the verification link in their email. */
    fun isEmailVerified(): Boolean = auth.currentUser?.isEmailVerified == true

    /** Re-fetches the user from the server so [isEmailVerified] reflects a link
     *  the user may have just clicked in their inbox. */
    suspend fun reloadUser() {
        auth.currentUser?.reload()?.await()
    }

    /** (Re)sends the verification email via the project's configured SMTP. */
    suspend fun sendEmailVerification() {
        auth.currentUser?.sendEmailVerification()?.await()
    }

    suspend fun register(name: String, email: String, password: String) {
        val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
        val uid = result.user?.uid ?: error("Registration failed")
        val profile = mapOf(
            "uid" to uid,
            "name" to name.trim(),
            "email" to email.trim(),
            "photoUrl" to "",
            "onboarded" to false,   // gate into profile setup until the basics are filled in
            "joinedAt" to System.currentTimeMillis(),
            "fcmToken" to runCatching { FirebaseMessaging.getInstance().token.await() }.getOrDefault("")
        )
        db.collection("users").document(uid).set(profile, SetOptions.merge()).await()
        // IMPORTANT: also set the Auth display name, otherwise senderName falls back to "Me".
        runCatching {
            result.user?.updateProfile(
                UserProfileChangeRequest.Builder().setDisplayName(name.trim()).build()
            )?.await()
        }
        // Send the verification email (delivered via the project's configured SMTP).
        // This is also what proves the address actually exists and is owned by them.
        runCatching { result.user?.sendEmailVerification()?.await() }
    }

    /**
     * Ensures the Auth display name matches the Firestore profile name, and
     * propagates it into every chat's memberNames so others see the correct
     * name (fixes legacy data where the name showed up as "Me").
     */
    suspend fun ensureProfileName() {
        val user = auth.currentUser ?: return
        val docName = runCatching {
            db.collection("users").document(user.uid).get().await().getString("name")
        }.getOrNull()?.takeIf { it.isNotBlank() }
        val name = docName ?: user.displayName?.takeIf { it.isNotBlank() } ?: return

        if (user.displayName != name) {
            runCatching {
                user.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(name).build()).await()
            }
        }
        runCatching {
            val chats = db.collection("chats").whereArrayContains("members", user.uid).get().await()
            if (!chats.isEmpty) {
                val batch = db.batch()
                chats.documents.forEach { batch.update(it.reference, "memberNames.${user.uid}", name) }
                batch.commit().await()
            }
        }
    }

    suspend fun login(email: String, password: String) {
        auth.signInWithEmailAndPassword(email.trim(), password).await()
        // refresh token in case it changed
        currentUid?.let { uid ->
            runCatching {
                val token = FirebaseMessaging.getInstance().token.await()
                db.collection("users").document(uid)
                    .set(mapOf("fcmToken" to token), SetOptions.merge()).await()
            }
        }
    }

    fun logout() = auth.signOut()

    /** Sends a password-reset email with a secure link to set a new password. */
    suspend fun sendPasswordReset(email: String) {
        auth.sendPasswordResetEmail(email.trim()).await()
    }

    /** Re-confirms the user's password (required by Firebase before deleting an account). */
    suspend fun reauthenticate(password: String) {
        val user = auth.currentUser ?: error("Not signed in")
        val email = user.email ?: error("This account has no email")
        user.reauthenticate(EmailAuthProvider.getCredential(email, password)).await()
    }

    /** Permanently deletes the Firebase Auth account (user can never sign in again). */
    suspend fun deleteAuthAccount() {
        auth.currentUser?.delete()?.await()
    }

    fun addAuthStateListener(block: (Boolean) -> Unit): FirebaseAuth.AuthStateListener {
        val listener = FirebaseAuth.AuthStateListener { block(it.currentUser != null) }
        auth.addAuthStateListener(listener)
        return listener
    }

    fun removeAuthStateListener(listener: FirebaseAuth.AuthStateListener) {
        auth.removeAuthStateListener(listener)
    }
}
