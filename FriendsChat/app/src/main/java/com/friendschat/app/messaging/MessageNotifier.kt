package com.friendschat.app.messaging

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.friendschat.app.MainActivity
import com.friendschat.app.R
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
    private val lastSeen = HashMap<String, Long>()  // chatId -> last message time we've handled

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
                    val chat = doc.toObject(Chat::class.java) ?: continue
                    val msgAt = chat.lastMessageTime?.time ?: continue
                    val prev = lastSeen[chat.id] ?: 0L
                    lastSeen[chat.id] = maxOf(prev, msgAt)

                    if (firstLoad) continue                       // baseline only — don't notify for history
                    if (msgAt <= prev) continue                   // nothing new in this chat
                    if (chat.lastMessageSenderId.isBlank() || chat.lastMessageSenderId == uid) continue
                    if (!chat.hasUnreadFor(uid)) continue         // already read (e.g. on another device)
                    if (chat.id == activeChatId) continue         // you're looking at this chat right now
                    show(app, chat, uid)
                }
                primed = true
            }
    }

    fun stop() {
        reg?.remove(); reg = null; primed = false; lastSeen.clear()
    }

    private fun show(context: Context, chat: Chat, uid: String) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("openChatId", chat.id)
        }
        val pending = PendingIntent.getActivity(
            context, chat.id.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val title = chat.titleFor(uid)
        val body = chat.lastMessage.ifBlank { "New message" }
        val n = NotificationCompat.Builder(context, "messages")
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()
        // One notification per chat (id = chat). A newer message replaces the old one.
        NotificationManagerCompat.from(context).notify(chat.id.hashCode(), n)
    }
}
