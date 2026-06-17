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

/**
 * Builds + posts a "new message" notification, and remembers which message we
 * last notified per chat. Shared by [MessageNotifier] (live, app-running) and
 * [MessageSyncWorker] (periodic, app-closed) so the same message is never
 * notified twice.
 */
object Notifications {
    private const val PREFS = "msg_notify"

    fun lastNotified(context: Context, chatId: String): Long =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong(chatId, 0L)

    fun setNotified(context: Context, chatId: String, atMillis: Long) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putLong(chatId, atMillis).apply()
    }

    /** Posts (or refreshes) the message notification for [chat] addressed to [uid]. */
    fun showMessage(context: Context, chat: Chat, uid: String) {
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
        val body = chat.lastMessage.ifBlank { "New message" }
        val n = NotificationCompat.Builder(context, "messages")
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle(chat.titleFor(uid))
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()
        NotificationManagerCompat.from(context).notify(chat.id.hashCode(), n)
    }
}
