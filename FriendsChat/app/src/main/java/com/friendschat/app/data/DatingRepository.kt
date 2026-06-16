package com.friendschat.app.data

import android.content.Context
import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.tasks.await

/**
 * Dating layer: profile editing, photo gallery, the discovery feed, and the
 * like / pass / match flow. Matches reuse the existing 1-on-1 chat plumbing
 * ([ChatRepository.openDirectChat]) so messaging "just works" once two people
 * like each other. Everything lives in Firestore + Cloudinary — no billing.
 */
class DatingRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val chats = ChatRepository()

    val myUid: String get() = auth.currentUser?.uid ?: ""
    private val myName: String get() = auth.currentUser?.displayName ?: "Someone"

    // ---------------------------------------------------------------- Profile
    fun observeMyProfile(): Flow<ChatUser?> = callbackFlow {
        val reg = db.collection("users").document(myUid)
            .addSnapshotListener { snap, err ->
                if (err != null) { trySend(null); return@addSnapshotListener }
                trySend(snap?.toObject(ChatUser::class.java))
            }
        awaitClose { reg.remove() }
    }

    /** Merges the given fields into the current user's profile document. */
    suspend fun saveProfile(updates: Map<String, Any>) {
        if (updates.isEmpty()) return
        db.collection("users").document(myUid).set(updates, SetOptions.merge()).await()
    }

    /** Uploads a photo to Cloudinary, appends it to the gallery, and (if it's the
     *  first one) sets it as the primary avatar photo. Returns the hosted URL. */
    suspend fun addPhoto(context: Context, uri: Uri): String {
        val url = CloudinaryUploader.upload(context, uri, "photo_${myUid}_${System.nanoTime()}")
        val ref = db.collection("users").document(myUid)
        val current = ref.get().await().toObject(ChatUser::class.java)
        val updates = HashMap<String, Any>()
        updates["photos"] = FieldValue.arrayUnion(url)
        if (current?.photoUrl.isNullOrBlank()) updates["photoUrl"] = url
        ref.set(updates, SetOptions.merge()).await()
        return url
    }

    suspend fun removePhoto(url: String) {
        val ref = db.collection("users").document(myUid)
        val current = ref.get().await().toObject(ChatUser::class.java)
        val updates = HashMap<String, Any>()
        updates["photos"] = FieldValue.arrayRemove(url)
        // If we removed the primary, promote the next remaining photo.
        if (current?.photoUrl == url) {
            val next = current.photos.firstOrNull { it != url } ?: ""
            updates["photoUrl"] = next
        }
        ref.set(updates, SetOptions.merge()).await()
    }

    // -------------------------------------------------------------- Discovery
    /** People available to discover, filtered by mutual gender preference and by
     *  everyone the current user has already liked or passed. Client-side filter
     *  is fine at free-tier scale and keeps the security rules trivial. */
    fun observeCandidates(): Flow<List<ChatUser>> =
        combine(chats.observeUsers(), excludedUids(), observeMyProfile()) { users, excluded, me ->
            users.filter { u ->
                u.onboarded &&
                    u.gallery.isNotEmpty() &&
                    u.uid !in excluded &&
                    wantsEachOther(me, u) &&
                    sameCity(me, u)
            }.sortedByDescending { it.isFreeNow }   // people who are "live now" surface first
        }

    /** Everyone to hide from the feed: people I liked, passed, blocked, or who blocked me. */
    private fun excludedUids(): Flow<Set<String>> =
        combine(outgoingIds("likes"), outgoingIds("passes"), blockedByMe(), blockedMe()) { l, p, b1, b2 ->
            l + p + b1 + b2
        }

    /** Only surface people in the same city (and country, when both set). If the
     *  current user hasn't set a city yet, don't filter — avoids an empty feed. */
    private fun sameCity(me: ChatUser?, other: ChatUser): Boolean {
        val myCity = me?.location?.trim().orEmpty()
        if (myCity.isBlank()) return true
        if (!other.location.trim().equals(myCity, ignoreCase = true)) return false
        val myCountry = me?.country?.trim().orEmpty()
        val theirCountry = other.country.trim()
        return myCountry.isBlank() || theirCountry.isBlank() || myCountry.equals(theirCountry, ignoreCase = true)
    }

    /** Mutual interest check. Falls back to "show everyone" when a preference is
     *  unset so new accounts never see an empty feed. */
    private fun wantsEachOther(me: ChatUser?, other: ChatUser): Boolean {
        val iWantThem = me?.interestedIn.isNullOrEmpty() ||
            other.gender.isBlank() || me!!.interestedIn.contains(other.gender)
        val theyWantMe = other.interestedIn.isEmpty() ||
            me?.gender.isNullOrBlank() || other.interestedIn.contains(me!!.gender)
        return iWantThem && theyWantMe
    }

    /** Set of toUids the current user has acted on in [collection] ("likes"/"passes"). */
    private fun outgoingIds(collection: String): Flow<Set<String>> = callbackFlow {
        val reg = db.collection(collection)
            .whereEqualTo("fromUid", myUid)
            .addSnapshotListener { snap, err ->
                if (err != null) { trySend(emptySet()); return@addSnapshotListener }
                trySend(snap?.documents?.mapNotNull { it.getString("toUid") }?.toSet() ?: emptySet())
            }
        awaitClose { reg.remove() }
    }

    private fun pairId(a: String, b: String) = "${a}_$b"

    /**
     * Records a like from me → [target]. If [target] already liked me, it's a
     * match: a 1-on-1 chat is opened and its id is returned. Otherwise returns
     * an empty string.
     */
    suspend fun like(target: ChatUser, comment: String = "", likedThing: String = ""): String {
        val id = pairId(myUid, target.uid)
        val like = Like(
            id = id,
            fromUid = myUid,
            fromName = myName,
            toUid = target.uid,
            comment = comment.trim(),
            likedThing = likedThing
        )
        db.collection("likes").document(id).set(like).await()

        val reciprocal = db.collection("likes").document(pairId(target.uid, myUid)).get().await()
        if (reciprocal.exists()) {
            // It's a match — create/open the conversation and seed an opener.
            val chatId = chats.openDirectChat(target, myName)
            val opener = comment.ifBlank { "You matched! Say hi 👋" }
            runCatching { chats.sendText(chatId, opener, myName) }
            return chatId
        }
        return ""
    }

    suspend fun pass(targetUid: String) {
        val id = pairId(myUid, targetUid)
        db.collection("passes").document(id)
            .set(mapOf("fromUid" to myUid, "toUid" to targetUid)).await()
    }

    // -------------------------------------------------------------- Likes you
    /** Live list of people who liked me and whom I haven't yet liked or passed
     *  (an open incoming like I can turn into a match). */
    fun observeIncomingLikes(): Flow<List<Like>> =
        combine(incomingLikes(), outgoingIds("likes"), outgoingIds("passes")) { incoming, liked, passed ->
            incoming.filter { it.fromUid !in liked && it.fromUid !in passed }
                .sortedByDescending { it.timestamp?.time ?: 0L }
        }

    /** Incoming likes joined with the liker's full profile, ready to render. */
    fun observeLikers(): Flow<List<Pair<Like, ChatUser>>> =
        combine(observeIncomingLikes(), chats.observeUsers()) { likes, users ->
            val byId = users.associateBy { it.uid }
            likes.mapNotNull { like -> byId[like.fromUid]?.let { like to it } }
        }

    private fun incomingLikes(): Flow<List<Like>> = callbackFlow {
        val reg = db.collection("likes")
            .whereEqualTo("toUid", myUid)
            .addSnapshotListener { snap, err ->
                if (err != null) { trySend(emptyList()); return@addSnapshotListener }
                trySend(snap?.documents?.mapNotNull { it.toObject(Like::class.java) } ?: emptyList())
            }
        awaitClose { reg.remove() }
    }

    suspend fun getUser(uid: String): ChatUser? =
        db.collection("users").document(uid).get().await().toObject(ChatUser::class.java)

    // -------------------------------------------------------------- Liked lists
    /** People I have liked (my "Interested" list), joined with their profiles. */
    fun observeLikedByMe(): Flow<List<ChatUser>> =
        combine(outgoingIds("likes"), chats.observeUsers()) { likedIds, users ->
            users.filter { it.uid in likedIds }
        }

    // -------------------------------------------------------------- Blocking
    private fun blockPairId(blocker: String, blocked: String) = "${blocker}_$blocked"

    suspend fun block(uid: String) {
        db.collection("blocks").document(blockPairId(myUid, uid))
            .set(mapOf("blockerUid" to myUid, "blockedUid" to uid)).await()
    }

    suspend fun unblock(uid: String) {
        db.collection("blocks").document(blockPairId(myUid, uid)).delete().await()
    }

    /** uids I've blocked. */
    private fun blockedByMe(): Flow<Set<String>> = callbackFlow {
        val reg = db.collection("blocks").whereEqualTo("blockerUid", myUid)
            .addSnapshotListener { snap, err ->
                if (err != null) { trySend(emptySet()); return@addSnapshotListener }
                trySend(snap?.documents?.mapNotNull { it.getString("blockedUid") }?.toSet() ?: emptySet())
            }
        awaitClose { reg.remove() }
    }

    /** uids who have blocked me. */
    private fun blockedMe(): Flow<Set<String>> = callbackFlow {
        val reg = db.collection("blocks").whereEqualTo("blockedUid", myUid)
            .addSnapshotListener { snap, err ->
                if (err != null) { trySend(emptySet()); return@addSnapshotListener }
                trySend(snap?.documents?.mapNotNull { it.getString("blockerUid") }?.toSet() ?: emptySet())
            }
        awaitClose { reg.remove() }
    }

    /** Either direction: true if I blocked them or they blocked me. */
    fun observeBlockRelation(uid: String): Flow<Boolean> =
        combine(blockedByMe(), blockedMe()) { mine, theirs -> uid in mine || uid in theirs }

    /** My blocked users, joined with their profiles (for the Blocked list). */
    fun observeBlockedUsers(): Flow<List<ChatUser>> =
        combine(blockedByMe(), chats.observeUsers()) { ids, users -> users.filter { it.uid in ids } }

    // -------------------------------------------------------------- Reports / vouches
    private fun reportPairId(target: String, reporter: String) = "${target}_$reporter"

    /** [type] is "genuine" (a vouch) or "fake" (a flag). One per reporter per target. */
    suspend fun report(uid: String, type: String) {
        db.collection("reports").document(reportPairId(uid, myUid))
            .set(mapOf("targetUid" to uid, "reporterUid" to myUid, "type" to type)).await()
    }

    /** (genuineCount, fakeCount) for a user. */
    suspend fun reportCounts(uid: String): Pair<Int, Int> {
        val snap = db.collection("reports").whereEqualTo("targetUid", uid).get().await()
        val genuine = snap.documents.count { it.getString("type") == "genuine" }
        val fake = snap.documents.count { it.getString("type") == "fake" }
        return genuine to fake
    }

    companion object {
        /** Genuine vouches needed for the trusted "star" badge. */
        const val STAR_THRESHOLD = 10
    }
}
