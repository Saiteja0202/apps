package com.friendschat.app.messaging

import android.content.Context
import com.friendschat.app.data.Chat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

/**
 * App-level (local) message notifications — WhatsApp-style "X messaged you".
 *
 * While the app process is alive (foreground OR background) this watches the
 * signed-in user's chats. When a chat receives a newer message from someone
 * else — i.e. the same condition that lights the unread badge — it posts a
 * local notification. No server / Cloud Function needed, so it works on the
 * free Firebase plan.
 *
 * Limitation: a listener only runs while the process exists. If the app is
 * fully closed (swiped away / killed) or the phone is off, nothing fires —
 * that case needs FCM push (Blaze plan + the Cloud Function in /functions).
 */
object MessageNotifier {

    /** Chat currently open on screen — suppress its notifications (you're reading it). */
    @Volatile var activeChatId: String? = null

    private var reg: ListenerRegistration? = null
    private var primed = false

    /** Begin watching once the user is signed in. Safe to call repeatedly. */
    fun start(context: Context) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        if (reg != null) return
        val app = context.applicationContext
        reg = FirebaseFirestore.getInstance().collection("chats")
            .whereArrayContains("members", uid)
            .addSnapshotListener { snap, err ->
                if (err != null || snap == null) return@addSnapshotListener
                val firstLoad = !primed
                for (doc in snap.documents) {
                    val chat = runCatching { doc.toObject(Chat::class.java) }.getOrNull() ?: continue
                    val msgAt = chat.lastMessageTime?.time ?: continue

                    if (firstLoad) {
                        // Baseline: record where each chat stands without notifying,
                        // so the background worker won't re-announce existing history.
                        Notifications.setNotified(app, chat.id, msgAt)
                        continue
                    }
                    if (msgAt <= Notifications.lastNotified(app, chat.id)) continue
                    if (chat.lastMessageSenderId.isBlank() || chat.lastMessageSenderId == uid) continue
                    if (!chat.hasUnreadFor(uid)) continue         // already read (e.g. on another device)
                    if (chat.id == activeChatId) continue         // you're looking at this chat right now
                    Notifications.showMessage(app, chat, uid)
                    Notifications.setNotified(app, chat.id, msgAt)
                }
                primed = true
            }
    }

    fun stop() {
        reg?.remove(); reg = null; primed = false
    }
}
