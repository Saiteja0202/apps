package com.friendschat.app.messaging

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.friendschat.app.data.Chat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

/**
 * Periodic background check (~every 15 min) that posts local notifications for
 * new messages even when the app has been swiped away. WorkManager survives the
 * app being closed and reboots, so this is the free-plan way to get "app closed"
 * notifications — at the cost of delay (Android's 15-min minimum, more in Doze)
 * and it can't run while the phone is powered off.
 */
class MessageSyncWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return Result.success()
        val snap = try {
            FirebaseFirestore.getInstance().collection("chats")
                .whereArrayContains("members", uid).get().await()
        } catch (e: Exception) {
            return Result.retry()
        }
        for (doc in snap.documents) {
            val chat = runCatching { doc.toObject(Chat::class.java) }.getOrNull() ?: continue
            val msgAt = chat.lastMessageTime?.time ?: continue
            if (chat.lastMessageSenderId.isBlank() || chat.lastMessageSenderId == uid) continue
            if (!chat.hasUnreadFor(uid)) continue
            if (chat.id == MessageNotifier.activeChatId) continue
            if (msgAt <= Notifications.lastNotified(applicationContext, chat.id)) continue
            Notifications.showMessage(applicationContext, chat, uid)
            Notifications.setNotified(applicationContext, chat.id, msgAt)
        }
        return Result.success()
    }

    companion object {
        private const val NAME = "message_sync"

        fun schedule(context: Context) {
            val req = PeriodicWorkRequestBuilder<MessageSyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(
                    Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
                )
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.KEEP, req)
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(NAME)
        }
    }
}
