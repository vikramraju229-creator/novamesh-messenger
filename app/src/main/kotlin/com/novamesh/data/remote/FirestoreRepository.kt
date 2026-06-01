package com.novamesh.data.remote

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * Firestore data models for NovaMesh Messenger.
 *
 * Collections:
 *   /users/{userId}         → User profile data
 *   /chats/{chatId}         → Chat metadata
 *   /chats/{chatId}/messages/{messageId} → Messages
 *   /channels/{channelId}   → Channel data
 *   /channels/{channelId}/posts/{postId} → Channel posts
 *   /stories/{userId}/items/{storyId}    → Story items
 */

data class FirestoreUser(
    val id: String = "",
    val name: String = "",
    val username: String = "",
    val phone: String = "",
    val photoUrl: String = "",
    val bio: String = "",
    val lastSeen: Long = System.currentTimeMillis(),
    val isOnline: Boolean = false,
    val fcmToken: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val snapStreak: Int = 0,
    val subscribedChannels: List<String> = emptyList(),
)

data class FirestoreChat(
    val id: String = "",
    val participants: List<String> = emptyList(),
    val lastMessage: FirestoreLastMessage? = null,
    val unreadCount: Map<String, Int> = emptyMap(),
    val createdAt: Long = System.currentTimeMillis(),
)

data class FirestoreLastMessage(
    val text: String = "",
    val timestamp: Long = 0L,
    val senderId: String = "",
)

data class FirestoreMessage(
    val id: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val text: String = "",
    val type: String = "text", // text, image, video, audio, file
    val mediaUrl: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "sent", // sent, delivered, read
    val replyToId: String? = null,
    val reactions: Map<String, String> = emptyMap(), // userId -> emoji
    val isDeleted: Boolean = false,
)

data class FirestoreChannel(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val category: String = "General",
    val subscriberCount: Int = 0,
    val photoUrl: String = "",
    val isVerified: Boolean = false,
    val ownerId: String = "",
)

data class FirestoreChannelPost(
    val id: String = "",
    val channelId: String = "",
    val content: String = "",
    val mediaUrl: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val likes: Int = 0,
    val comments: Int = 0,
    val authorId: String = "",
)

data class FirestoreStory(
    val id: String = "",
    val userId: String = "",
    val mediaUrl: String = "",
    val type: String = "image", // image, video
    val timestamp: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + 24 * 60 * 60 * 1000L,
    val viewedBy: List<String> = emptyList(),
)

/**
 * Repository for all Firestore operations.
 *
 * Handles user profiles, chats, messages, channels, and stories
 * using Firebase Firestore (free Spark plan).
 */
class FirestoreRepository {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private val currentUserId: String?
        get() = auth.currentUser?.uid

    // ══════════════════════════════════════════════════════════════════
    // User operations
    // ══════════════════════════════════════════════════════════════════

    /** Get a user's profile from Firestore. */
    suspend fun getUser(userId: String): FirestoreUser? {
        return try {
            val doc = firestore.collection("users").document(userId).get().await()
            if (doc.exists()) {
                doc.toObject(FirestoreUser::class.java)?.copy(id = userId)
            } else null
        } catch (_: Exception) { null }
    }

    /** Observe a user's profile in real-time. */
    fun observeUser(userId: String): Flow<FirestoreUser?> {
        return firestore.collection("users").document(userId)
            .snapshots()
            .map { it.toObject(FirestoreUser::class.java)?.copy(id = userId) }
    }

    /** Update user's online status and last seen. */
    suspend fun updatePresence(isOnline: Boolean) {
        val uid = currentUserId ?: return
        try {
            firestore.collection("users").document(uid)
                .update("isOnline", isOnline, "lastSeen", System.currentTimeMillis())
                .await()
        } catch (_: Exception) { }
    }

    /** Update FCM token. */
    suspend fun updateFcmToken(token: String) {
        val uid = currentUserId ?: return
        try {
            firestore.collection("users").document(uid)
                .update("fcmToken", token).await()
        } catch (_: Exception) { }
    }

    /** Search users by name or username. */
    suspend fun searchUsers(query: String): List<FirestoreUser> {
        return try {
            val snapshot = firestore.collection("users")
                .orderBy("name")
                .startAt(query)
                .endAt(query + "\uf8ff")
                .limit(20)
                .get().await()
            snapshot.documents.mapNotNull { it.toObject(FirestoreUser::class.java) }
        } catch (_: Exception) { emptyList() }
    }

    // ══════════════════════════════════════════════════════════════════
    // Chat operations
    // ══════════════════════════════════════════════════════════════════

    /** Get or create a 1:1 chat between current user and another user. */
    suspend fun getOrCreateChat(otherUserId: String): String? {
        val uid = currentUserId ?: return null
        return try {
            // Check if chat already exists
            val snapshot = firestore.collection("chats")
                .whereArrayContains("participants", uid)
                .get().await()
            val existing = snapshot.documents.find { doc ->
                val parts = doc.get("participants") as? List<*> ?: emptyList<Any>()
                parts.contains(otherUserId)
            }
            if (existing != null) {
                existing.id
            } else {
                // Create new chat
                val chatId = UUID.randomUUID().toString()
                firestore.collection("chats").document(chatId).set(
                    hashMapOf(
                        "participants" to listOf(uid, otherUserId),
                        "createdAt" to System.currentTimeMillis(),
                        "unreadCount" to mapOf(uid to 0, otherUserId to 0),
                    )
                ).await()
                chatId
            }
        } catch (_: Exception) { null }
    }

    /** Observe all chats for the current user. */
    fun observeChats(): Flow<List<FirestoreChat>> {
        val uid = currentUserId ?: return kotlinx.coroutines.flow.flowOf(emptyList())
        return firestore.collection("chats")
            .whereArrayContains("participants", uid)
            .orderBy("lastMessage.timestamp", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { it.toObject(FirestoreChat::class.java)?.copy(id = it.id) }
            }
    }

    // ══════════════════════════════════════════════════════════════════
    // Message operations
    // ══════════════════════════════════════════════════════════════════

    /** Send a text message to a chat. */
    suspend fun sendMessage(chatId: String, text: String, type: String = "text", mediaUrl: String = ""): String? {
        val uid = currentUserId ?: return null
        val messageId = UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()

        return try {
            // Add message to subcollection
            firestore.collection("chats").document(chatId)
                .collection("messages").document(messageId)
                .set(
                    FirestoreMessage(
                        id = messageId,
                        chatId = chatId,
                        senderId = uid,
                        text = text,
                        type = type,
                        mediaUrl = mediaUrl,
                        timestamp = timestamp,
                        status = "sent",
                    )
                ).await()

            // Update chat's last message
            firestore.collection("chats").document(chatId)
                .update(
                    "lastMessage.text", text,
                    "lastMessage.timestamp", timestamp,
                    "lastMessage.senderId", uid,
                ).await()

            // Increment unread count for other participants
            val chatDoc = firestore.collection("chats").document(chatId).get().await()
            val participants = chatDoc.get("participants") as? List<*> ?: emptyList<Any>()
            participants.filter { it.toString() != uid }.forEach { participantId ->
                val current = (chatDoc.get("unreadCount.$participantId") as? Long) ?: 0L
                firestore.collection("chats").document(chatId)
                    .update("unreadCount.${participantId.toString()}", current + 1).await()
            }

            messageId
        } catch (_: Exception) { null }
    }

    /** Observe messages in a chat in real-time. */
    fun observeMessages(chatId: String): Flow<List<FirestoreMessage>> {
        return firestore.collection("chats").document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.documents
                    .mapNotNull { it.toObject(FirestoreMessage::class.java)?.copy(id = it.id, chatId = chatId) }
                    .filter { !it.isDeleted }
            }
    }

    /** Update message status (delivered/read). */
    suspend fun updateMessageStatus(messageId: String, chatId: String, status: String) {
        try {
            firestore.collection("chats").document(chatId)
                .collection("messages").document(messageId)
                .update("status", status).await()
        } catch (_: Exception) { }
    }

    /** Soft-delete a message. */
    suspend fun deleteMessage(messageId: String, chatId: String) {
        try {
            firestore.collection("chats").document(chatId)
                .collection("messages").document(messageId)
                .update("isDeleted", true).await()
        } catch (_: Exception) { }
    }

    /** Upload media to Firebase Storage and return download URL. */
    suspend fun uploadMedia(uri: Uri, folder: String = "media"): String? {
        val uid = currentUserId ?: return null
        return try {
            val filename = "${UUID.randomUUID()}.jpg"
            val ref = storage.reference.child("$folder/$uid/$filename")
            ref.putFile(uri).await()
            ref.downloadUrl.await().toString()
        } catch (_: Exception) { null }
    }

    // ══════════════════════════════════════════════════════════════════
    // Channel operations
    // ══════════════════════════════════════════════════════════════════

    /** Observe all channels. */
    fun observeChannels(): Flow<List<FirestoreChannel>> {
        return firestore.collection("channels")
            .orderBy("subscriberCount", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { it.toObject(FirestoreChannel::class.java)?.copy(id = it.id) }
            }
    }

    /** Observe channels by category. */
    fun observeChannelsByCategory(category: String): Flow<List<FirestoreChannel>> {
        return if (category == "All" || category.isBlank()) {
            observeChannels()
        } else {
            firestore.collection("channels")
                .whereEqualTo("category", category)
                .orderBy("subscriberCount", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.documents.mapNotNull { it.toObject(FirestoreChannel::class.java)?.copy(id = it.id) }
                }
        }
    }

    /** Subscribe to a channel. */
    suspend fun subscribeToChannel(channelId: String) {
        val uid = currentUserId ?: return
        try {
            firestore.collection("users").document(uid)
                .update("subscribedChannels", com.google.firebase.firestore.FieldValue.arrayUnion(channelId))
                .await()
            firestore.collection("channels").document(channelId)
                .update("subscriberCount", com.google.firebase.firestore.FieldValue.increment(1))
                .await()
        } catch (_: Exception) { }
    }

    /** Unsubscribe from a channel. */
    suspend fun unsubscribeFromChannel(channelId: String) {
        val uid = currentUserId ?: return
        try {
            firestore.collection("users").document(uid)
                .update("subscribedChannels", com.google.firebase.firestore.FieldValue.arrayRemove(channelId))
                .await()
            firestore.collection("channels").document(channelId)
                .update("subscriberCount", com.google.firebase.firestore.FieldValue.increment(-1))
                .await()
        } catch (_: Exception) { }
    }

    /** Observe posts for a channel. */
    fun observeChannelPosts(channelId: String): Flow<List<FirestoreChannelPost>> {
        return firestore.collection("channels").document(channelId)
            .collection("posts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { it.toObject(FirestoreChannelPost::class.java)?.copy(id = it.id) }
            }
    }

    /** Seed initial channels (call once on first launch). */
    suspend fun seedChannels() {
        val existing = firestore.collection("channels").limit(1).get().await()
        if (!existing.isEmpty) return // Already seeded

        val categories = listOf(
            "Tech" to listOf(
                "TechNews" to "Latest in technology and programming",
                "AIWeekly" to "Artificial intelligence updates",
                "DevLife" to "Developer memes and stories",
                "CyberSec" to "Cybersecurity news and tips",
                "OpenSource" to "Open source project highlights",
            ),
            "News" to listOf(
                "WorldNews" to "Breaking news from around the world",
                "LocalFeed" to "Your local community news",
                "ScienceDaily" to "Scientific discoveries and research",
                "BusinessInsider" to "Business and finance news",
            ),
            "Sports" to listOf(
                "SportsCenter" to "All sports highlights and updates",
                "FootballHub" to "Football (soccer) news and matches",
                "HoopsNation" to "Basketball updates and analysis",
                "F1Daily" to "Formula 1 racing coverage",
            ),
            "Music" to listOf(
                "MusicVibes" to "New releases and music discovery",
                "IndieSpotlight" to "Independent artists showcase",
                "HipHopDaily" to "Hip hop and rap updates",
                "ClassicalHour" to "Classical music appreciation",
            ),
            "Gaming" to listOf(
                "GameZone" to "Video game news and reviews",
                "EsportsArena" to "Esports tournaments and results",
                "RetroGaming" to "Classic gaming nostalgia",
                "StreamCentral" to "Live streaming highlights",
            ),
            "Entertainment" to listOf(
                "MovieTalk" to "Film reviews and discussions",
                "TVSeries" to "TV show updates and theories",
                "CelebNews" to "Celebrity news and gossip",
                "BookClub" to "Book recommendations and reviews",
            ),
        )

        for ((category, channels) in categories) {
            for ((name, desc) in channels) {
                val id = name.lowercase()
                firestore.collection("channels").document(id).set(
                    FirestoreChannel(
                        id = id,
                        name = name,
                        description = desc,
                        category = category,
                        subscriberCount = (100..50000).random(),
                        photoUrl = "",
                        isVerified = listOf(true, false).random(),
                        ownerId = "system",
                    )
                ).await()
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // Story operations
    // ══════════════════════════════════════════════════════════════════

    /** Observe stories from users. */
    fun observeStories(): Flow<List<FirestoreStory>> {
        val now = System.currentTimeMillis()
        return firestore.collectionGroup("items")
            .whereGreaterThan("expiresAt", now)
            .orderBy("expiresAt", Query.Direction.ASCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { it.toObject(FirestoreStory::class.java)?.copy(id = it.id) }
            }
    }

    /** Mark a story as viewed. */
    suspend fun markStoryViewed(storyUserId: String, storyId: String) {
        val uid = currentUserId ?: return
        try {
            firestore.collection("stories").document(storyUserId)
                .collection("items").document(storyId)
                .update("viewedBy", com.google.firebase.firestore.FieldValue.arrayUnion(uid))
                .await()
        } catch (_: Exception) { }
    }

    // ══════════════════════════════════════════════════════════════════
    // Contact matching
    // ══════════════════════════════════════════════════════════════════

    /** Find users by phone numbers (for contact matching). */
    suspend fun findUsersByPhone(phoneNumbers: List<String>): List<FirestoreUser> {
        if (phoneNumbers.isEmpty()) return emptyList()
        return try {
            // Firestore 'in' queries support up to 10 items
            val results = mutableListOf<FirestoreUser>()
            phoneNumbers.chunked(10).forEach { batch ->
                val snapshot = firestore.collection("users")
                    .whereIn("phone", batch)
                    .get().await()
                results.addAll(snapshot.documents.mapNotNull {
                    it.toObject(FirestoreUser::class.java)?.copy(id = it.id)
                })
            }
            results
        } catch (_: Exception) { emptyList() }
    }
}
