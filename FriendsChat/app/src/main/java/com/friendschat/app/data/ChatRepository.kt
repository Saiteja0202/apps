package com.friendschat.app.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ChatRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    val myUid: String get() = auth.currentUser?.uid ?: ""

    // ---- Users ----
    fun observeUsers(): Flow<List<ChatUser>> = callbackFlow {
        val reg = db.collection("users")
            .addSnapshotListener { snap, err ->
                if (err != null) { trySend(emptyList()); return@addSnapshotListener }
                val users = snap?.documents
                    ?.mapNotNull { it.toObject(ChatUser::class.java) }
                    ?.filter { it.uid != myUid }
                    ?.sortedBy { it.name.lowercase() }
                    ?: emptyList()
                trySend(users)
            }
        awaitClose { reg.remove() }
    }

    /** Privacy-friendly lookup: find a single user by their exact email (not self). */
    suspend fun findUserByEmail(email: String): ChatUser? {
        val e = email.trim()
        if (e.isEmpty()) return null
        val snap = db.collection("users").whereEqualTo("email", e).limit(1).get().await()
        val u = snap.documents.firstOrNull()?.toObject(ChatUser::class.java)
        return u?.takeIf { it.uid != myUid }
    }

    // ---- Chat list ----
    fun observeChats(): Flow<List<Chat>> = callbackFlow {
        val reg = db.collection("chats")
            .whereArrayContains("members", myUid)
            .addSnapshotListener { snap, err ->
                if (err != null) { trySend(emptyList()); return@addSnapshotListener }
                val chats = snap?.documents
                    ?.mapNotNull { it.toObject(Chat::class.java) }
                    ?.sortedByDescending { it.lastMessageTime?.time ?: 0L }
                    ?: emptyList()
                trySend(chats)
            }
        awaitClose { reg.remove() }
    }

    suspend fun getChat(chatId: String): Chat? =
        db.collection("chats").document(chatId).get().await()
            .toObject(Chat::class.java)

    /** Live view of the signed-in user's own profile document. */
    fun observeMe(): Flow<ChatUser?> = callbackFlow {
        val reg = db.collection("users").document(myUid)
            .addSnapshotListener { snap, err ->
                if (err != null) { trySend(null); return@addSnapshotListener }
                trySend(snap?.toObject(ChatUser::class.java))
            }
        awaitClose { reg.remove() }
    }

    /** Uploads a new avatar to Cloudinary and stores its URL on the user doc. */
    suspend fun updateProfilePhoto(context: Context, uri: Uri) {
        val url = CloudinaryUploader.upload(context, uri, "avatar_$myUid")
        db.collection("users").document(myUid)
            .set(mapOf("photoUrl" to url), SetOptions.merge()).await()
    }

    /**
     * Deletes all of the current user's data: their profile document, their
     * direct chats (and the messages in them), and removes them from any group
     * chats. Call this while still authenticated, before deleting the Auth user.
     */
    suspend fun deleteAllMyData() {
        val uid = myUid
        val chats = db.collection("chats")
            .whereArrayContains("members", uid).get().await()
        for (doc in chats.documents) {
            val members = (doc.get("members") as? List<*>)?.size ?: 0
            val chatRef = doc.reference
            if (members <= 2) {
                // Direct (1-on-1) chat: remove the conversation entirely.
                deleteCollection(chatRef.collection("messages"))
                chatRef.delete().await()
            } else {
                // Group chat: just leave it, keep history for the others.
                chatRef.update(
                    mapOf(
                        "members" to FieldValue.arrayRemove(uid),
                        "memberNames.$uid" to FieldValue.delete()
                    )
                ).await()
            }
        }
        db.collection("users").document(uid).delete().await()
    }

    /** Wipes all messages in a chat but keeps the match/conversation itself. */
    suspend fun deleteChatHistory(chatId: String) {
        deleteCollection(db.collection("chats").document(chatId).collection("messages"))
        db.collection("chats").document(chatId).update(
            mapOf("lastMessage" to "", "lastMessageSender" to "")
        ).await()
    }

    private suspend fun deleteCollection(col: CollectionReference) {
        while (true) {
            val snap = col.limit(300).get().await()
            if (snap.isEmpty) break
            val batch = db.batch()
            snap.documents.forEach { batch.delete(it.reference) }
            batch.commit().await()
            if (snap.size() < 300) break
        }
    }

    /** Marks the given messages as seen by the current user (read receipts). */
    suspend fun markRead(chatId: String, messageIds: List<String>) {
        if (messageIds.isEmpty()) return
        val batch = db.batch()
        val col = db.collection("chats").document(chatId).collection("messages")
        messageIds.forEach { id ->
            batch.update(col.document(id), "readBy", FieldValue.arrayUnion(myUid))
        }
        batch.commit().await()
    }

    // ---- Messages ----
    fun observeMessages(chatId: String): Flow<List<Message>> = callbackFlow {
        val reg = db.collection("chats").document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) { trySend(emptyList()); return@addSnapshotListener }
                val msgs = snap?.documents
                    ?.mapNotNull { it.toObject(Message::class.java) }
                    ?: emptyList()
                trySend(msgs)
            }
        awaitClose { reg.remove() }
    }

    /** Live chat document (used for typing indicators + member changes). */
    fun observeChat(chatId: String): Flow<Chat?> = callbackFlow {
        val reg = db.collection("chats").document(chatId)
            .addSnapshotListener { snap, err ->
                if (err != null) { trySend(null); return@addSnapshotListener }
                trySend(snap?.toObject(Chat::class.java))
            }
        awaitClose { reg.remove() }
    }

    /** Live view of another user (used for online / last-seen in a 1-on-1 header). */
    fun observeUser(uid: String): Flow<ChatUser?> = callbackFlow {
        val reg = db.collection("users").document(uid)
            .addSnapshotListener { snap, err ->
                if (err != null) { trySend(null); return@addSnapshotListener }
                trySend(snap?.toObject(ChatUser::class.java))
            }
        awaitClose { reg.remove() }
    }

    /** Whether another user's profile still exists. Emits false once the doc is
     *  gone (account deleted). On error we assume true to avoid false positives. */
    fun observeUserExists(uid: String): Flow<Boolean> = callbackFlow {
        val reg = db.collection("users").document(uid)
            .addSnapshotListener { snap, err ->
                if (err != null) { trySend(true); return@addSnapshotListener }
                trySend(snap?.exists() == true)
            }
        awaitClose { reg.remove() }
    }

    /** Sets/clears my "typing" marker on the chat (a client timestamp others watch). */
    suspend fun setTyping(chatId: String, isTyping: Boolean) {
        val value: Any = if (isTyping) System.currentTimeMillis() else FieldValue.delete()
        runCatching {
            db.collection("chats").document(chatId).update("typing.$myUid", value).await()
        }
    }

    suspend fun editMessage(chatId: String, msgId: String, newText: String) {
        val t = newText.trim()
        if (t.isEmpty()) return
        db.collection("chats").document(chatId).collection("messages").document(msgId)
            .update(mapOf("text" to t, "edited" to true)).await()
    }

    /** Soft-delete: keeps the bubble but shows "deleted" (WhatsApp-style). */
    suspend fun deleteMessage(chatId: String, msgId: String) {
        db.collection("chats").document(chatId).collection("messages").document(msgId)
            .update(
                mapOf(
                    "deleted" to true,
                    "text" to "",
                    "mediaUrl" to "",
                    "mediaName" to ""
                )
            ).await()
    }

    // ---- Create / open chats ----

    /** Deterministic id for a 1-on-1 chat so it is never duplicated. */
    private fun directId(a: String, b: String): String =
        listOf(a, b).sorted().joinToString("_")

    suspend fun openDirectChat(other: ChatUser, myName: String): String {
        val id = directId(myUid, other.uid)
        // Create-or-merge the chat identity fields WITHOUT a pre-read. Reading a
        // not-yet-existing document fails the security rule (resource is null),
        // so we go straight to a merge write. merge() preserves any existing
        // lastMessage / lastMessageTime if the chat is already there.
        val data = mapOf(
            "id" to id,
            "type" to "direct",
            "members" to listOf(myUid, other.uid),
            "memberNames" to mapOf(myUid to myName, other.uid to other.name),
            "createdBy" to myUid
        )
        db.collection("chats").document(id).set(data, SetOptions.merge()).await()
        return id
    }

    suspend fun createGroup(name: String, members: List<ChatUser>, myName: String): String {
        val ref = db.collection("chats").document()
        val allMembers = (members.map { it.uid } + myUid).distinct()
        val names = (members.associate { it.uid to it.name } + (myUid to myName))
        val chat = Chat(
            id = ref.id,
            type = "group",
            name = name.trim(),
            members = allMembers,
            memberNames = names,
            createdBy = myUid,
            lastMessage = "Group created",
            lastMessageSender = myName
        )
        ref.set(chat).await()
        db.collection("chats").document(ref.id)
            .update("lastMessageTime", FieldValue.serverTimestamp())
        return ref.id
    }

    // ---- Group admin ----
    suspend fun renameGroup(chatId: String, name: String) {
        if (name.isBlank()) return
        db.collection("chats").document(chatId).update("name", name.trim()).await()
    }

    suspend fun setGroupPhoto(context: Context, chatId: String, uri: Uri) {
        val url = CloudinaryUploader.upload(context, uri, "group_$chatId")
        db.collection("chats").document(chatId).update("photoUrl", url).await()
    }

    suspend fun addGroupMembers(chatId: String, users: List<ChatUser>) {
        if (users.isEmpty()) return
        val updates = HashMap<String, Any>()
        updates["members"] = FieldValue.arrayUnion(*users.map { it.uid }.toTypedArray())
        users.forEach { updates["memberNames.${it.uid}"] = it.name }
        db.collection("chats").document(chatId).update(updates).await()
    }

    suspend fun removeGroupMember(chatId: String, uid: String) {
        db.collection("chats").document(chatId).update(
            mapOf("members" to FieldValue.arrayRemove(uid), "memberNames.$uid" to FieldValue.delete())
        ).await()
    }

    suspend fun leaveGroup(chatId: String) = removeGroupMember(chatId, myUid)

    // ---- Presence extras: mood + free-to-talk ----
    suspend fun setMood(mood: String) {
        db.collection("users").document(myUid).set(mapOf("mood" to mood), SetOptions.merge()).await()
    }

    suspend fun setFreeToTalk(minutes: Int) {
        val until = if (minutes <= 0) 0L else System.currentTimeMillis() + minutes * 60_000L
        db.collection("users").document(myUid).set(mapOf("freeUntil" to until), SetOptions.merge()).await()
    }

    // ---- Sending ----
    suspend fun sendText(
        chatId: String,
        text: String,
        senderName: String,
        replyToId: String = "",
        replyToText: String = "",
        replyToSender: String = "",
        revealAt: Long = 0L
    ) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        val msg = Message(
            senderId = myUid,
            senderName = senderName,
            type = MessageType.TEXT,
            text = trimmed,
            replyToId = replyToId,
            replyToText = replyToText,
            replyToSender = replyToSender,
            revealAt = revealAt
        )
        val preview = if (revealAt > System.currentTimeMillis()) "🔒 Time capsule" else trimmed
        writeMessage(chatId, msg, preview = preview, senderName = senderName)
    }

    /** Sends a recorded voice note (already a local file Uri) via Cloudinary. */
    suspend fun sendVoice(context: Context, chatId: String, uri: Uri, durationMs: Long, senderName: String) {
        val url = CloudinaryUploader.upload(context, uri, "voice_${System.currentTimeMillis()}.m4a")
        val msg = Message(
            senderId = myUid,
            senderName = senderName,
            type = MessageType.AUDIO,
            mediaUrl = url,
            mediaName = "Voice message",
            mimeType = "audio/mp4",
            durationMs = durationMs
        )
        writeMessage(chatId, msg, preview = "🎙️ Voice message", senderName = senderName)
    }

    /** Forwards an existing message into another chat. */
    suspend fun forwardMessage(toChatId: String, src: Message, senderName: String) {
        val msg = Message(
            senderId = myUid,
            senderName = senderName,
            type = src.type,
            text = src.text,
            mediaUrl = src.mediaUrl,
            mediaName = src.mediaName,
            mimeType = src.mimeType,
            sizeBytes = src.sizeBytes,
            durationMs = src.durationMs,
            forwarded = true
        )
        val preview = when (src.type) {
            MessageType.IMAGE -> "📷 Photo"
            MessageType.VIDEO -> "🎥 Video"
            MessageType.AUDIO -> "🎙️ Voice message"
            MessageType.FILE -> "📎 ${src.mediaName}"
            else -> src.text
        }
        writeMessage(toChatId, msg, preview = preview, senderName = senderName)
    }

    suspend fun react(chatId: String, msgId: String, emoji: String) {
        val ref = db.collection("chats").document(chatId).collection("messages").document(msgId)
        val value: Any = if (emoji.isBlank()) FieldValue.delete() else emoji
        ref.update("reactions.$myUid", value).await()
    }

    /** Live "typing preview": store the current draft text + a freshness stamp. */
    suspend fun setDraft(chatId: String, text: String) {
        val ref = db.collection("chats").document(chatId)
        runCatching {
            if (text.isBlank()) {
                ref.update(mapOf("draft.$myUid" to FieldValue.delete(), "typing.$myUid" to FieldValue.delete())).await()
            } else {
                ref.update(mapOf("draft.$myUid" to text.take(120), "typing.$myUid" to System.currentTimeMillis())).await()
            }
        }
    }

    /**
     * Uploads [uri] to Storage then posts a media message. [type] is one of
     * MessageType.IMAGE / VIDEO / FILE.
     */
    suspend fun sendMedia(
        context: Context,
        chatId: String,
        uri: Uri,
        type: String,
        senderName: String
    ) {
        val (displayName, size) = queryFileMeta(context, uri)
        val mime = context.contentResolver.getType(uri) ?: "application/octet-stream"
        val safeName = displayName.ifBlank { "file_${System.currentTimeMillis()}" }
        val url = CloudinaryUploader.upload(context, uri, safeName)

        val preview = when (type) {
            MessageType.IMAGE -> "📷 Photo"
            MessageType.VIDEO -> "🎥 Video"
            else -> "📎 $safeName"
        }
        val msg = Message(
            senderId = myUid,
            senderName = senderName,
            type = type,
            mediaUrl = url,
            mediaName = safeName,
            mimeType = mime,
            sizeBytes = size
        )
        writeMessage(chatId, msg, preview = preview, senderName = senderName)
    }

    private suspend fun writeMessage(
        chatId: String,
        message: Message,
        preview: String,
        senderName: String
    ) {
        val msgRef = db.collection("chats").document(chatId)
            .collection("messages").document()
        msgRef.set(message.copy(id = msgRef.id)).await()
        db.collection("chats").document(chatId).update(
            mapOf(
                "lastMessage" to preview,
                "lastMessageSender" to senderName,
                "lastMessageSenderId" to message.senderId,
                "lastMessageTime" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    /** Marks the whole chat as read by me right now (drives the unread badge). */
    suspend fun markChatRead(chatId: String) {
        runCatching {
            db.collection("chats").document(chatId)
                .update("lastRead.$myUid", FieldValue.serverTimestamp()).await()
        }
    }

    private fun queryFileMeta(context: Context, uri: Uri): Pair<String, Long> {
        var name = ""
        var size = 0L
        runCatching {
            context.contentResolver.query(uri, null, null, null, null)?.use { c ->
                val nameIdx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIdx = c.getColumnIndex(OpenableColumns.SIZE)
                if (c.moveToFirst()) {
                    if (nameIdx >= 0) name = c.getString(nameIdx) ?: ""
                    if (sizeIdx >= 0) size = c.getLong(sizeIdx)
                }
            }
        }
        return name to size
    }
}
