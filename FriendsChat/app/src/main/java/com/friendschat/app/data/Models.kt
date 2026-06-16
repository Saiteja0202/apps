package com.friendschat.app.data

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * A user / dating profile. The chat-era fields (online, mood, freeUntil) are kept
 * for the messaging layer; the dating fields drive discovery + profiles.
 */
data class ChatUser(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val photoUrl: String = "",                 // primary photo (first of [photos])
    val fcmToken: String = "",
    val online: Boolean = false,
    val lastSeen: Date? = null,
    val mood: String = "",
    val freeUntil: Long = 0L,
    // ---- Dating profile ----
    val age: Int = 0,
    val gender: String = "",                   // "woman" | "man" | "nonbinary"
    val interestedIn: List<String> = emptyList(),  // genders this user wants to see
    val bio: String = "",
    val jobTitle: String = "",
    val relationshipStatus: String = "",       // single | divorced | separated | widowed | complicated
    val country: String = "",                  // e.g. "India"
    val location: String = "",                 // city, e.g. "Kolkata" — used to match nearby people
    val photos: List<String> = emptyList(),    // gallery (Cloudinary URLs), up to 6
    val prompts: List<Prompt> = emptyList(),   // up to 3 prompt + answer pairs
    val joinedAt: Long = 0L,                   // epoch millis the account was created
    val onboarded: Boolean = false             // profile complete enough to be shown
) {
    val isFreeNow: Boolean get() = freeUntil > System.currentTimeMillis()

    /** "New here" = joined within the last 7 days. */
    val isNew: Boolean get() = joinedAt > 0L && joinedAt > System.currentTimeMillis() - 7L * 86_400_000L

    /** Photos to render, falling back to the legacy single avatar if needed. */
    val gallery: List<String>
        get() = photos.ifEmpty { if (photoUrl.isNotBlank()) listOf(photoUrl) else emptyList() }

    /** Answered prompts only. */
    val answeredPrompts: List<Prompt>
        get() = prompts.filter { it.question.isNotBlank() && it.answer.isNotBlank() }
}

/** A dating prompt the user answered, e.g. "A perfect first date…" → "…". */
data class Prompt(
    val question: String = "",
    val answer: String = ""
)

/** A one-directional like. A mutual pair of likes becomes a match (a chat). */
data class Like(
    val id: String = "",
    val fromUid: String = "",
    val fromName: String = "",
    val toUid: String = "",
    val comment: String = "",       // optional note attached to the like
    val likedThing: String = "",    // which photo/prompt was liked (for context)
    @ServerTimestamp val timestamp: Date? = null
)

/** Relationship-status options (key to display label). */
object RelationshipStatus {
    val options: List<Pair<String, String>> = listOf(
        "single" to "Single",
        "divorced" to "Divorced",
        "separated" to "Separated",
        "widowed" to "Widowed",
        "complicated" to "It's complicated"
    )
    fun label(key: String): String = options.firstOrNull { it.first == key }?.second ?: ""
}

/** Curated prompt questions users pick from when building a profile. */
object PromptLibrary {
    val questions: List<String> = listOf(
        "A perfect first date looks like…",
        "The way to win me over is…",
        "I'm weirdly attracted to…",
        "My simple pleasures…",
        "Two truths and a lie…",
        "I geek out about…",
        "We'll get along if…",
        "My most irrational fear…",
        "The hallmark of a good relationship is…",
        "I'll fall for you if…",
        "My ideal Sunday…",
        "Together we could…"
    )
}

data class Chat(
    val id: String = "",
    val type: String = "direct",            // "direct" | "group"
    val name: String = "",
    val photoUrl: String = "",
    val members: List<String> = emptyList(),
    val memberNames: Map<String, String> = emptyMap(),
    val createdBy: String = "",
    val lastMessage: String = "",
    val lastMessageSender: String = "",
    val typing: Map<String, Long> = emptyMap(),
    val draft: Map<String, String> = emptyMap(),   // live typing preview
    @ServerTimestamp val lastMessageTime: Date? = null
) {
    fun titleFor(currentUid: String): String {
        if (type == "group") return name.ifBlank { "Group" }
        val otherUid = members.firstOrNull { it != currentUid }
        return memberNames[otherUid] ?: "Match"
    }

    fun otherUid(currentUid: String): String =
        members.firstOrNull { it != currentUid } ?: ""
}

object MessageType {
    const val TEXT = "text"
    const val IMAGE = "image"
    const val VIDEO = "video"
    const val FILE = "file"
    const val AUDIO = "audio"
}

data class Message(
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val type: String = MessageType.TEXT,
    val text: String = "",
    val mediaUrl: String = "",
    val mediaName: String = "",
    val mimeType: String = "",
    val sizeBytes: Long = 0L,
    val durationMs: Long = 0L,                       // voice messages
    val readBy: List<String> = emptyList(),
    val reactions: Map<String, String> = emptyMap(), // uid -> emoji
    val replyToId: String = "",
    val replyToText: String = "",
    val replyToSender: String = "",
    val forwarded: Boolean = false,
    val revealAt: Long = 0L,                          // time-capsule: hidden until this time
    val edited: Boolean = false,
    val deleted: Boolean = false,
    @ServerTimestamp val timestamp: Date? = null
) {
    val isLocked: Boolean get() = revealAt > System.currentTimeMillis()
}
