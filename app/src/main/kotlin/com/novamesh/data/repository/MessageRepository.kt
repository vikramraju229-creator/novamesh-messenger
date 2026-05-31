package com.novamesh.data.repository

import com.novamesh.data.local.dao.ChatDao
import com.novamesh.data.local.dao.MessageDao
import com.novamesh.data.local.entity.ChatEntity
import com.novamesh.data.local.entity.MessageEntity
import com.novamesh.data.local.entity.UserEntity
import com.novamesh.data.remote.MatrixRepository
import com.novamesh.domain.model.Message
import com.novamesh.domain.model.MessageContent
import com.novamesh.domain.model.MessageStatus
import com.novamesh.domain.model.MessageType
import com.novamesh.domain.model.User
import com.novamesh.domain.model.UserPresence
import com.novamesh.security.SignalProtocolManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

// ---------------------------------------------------------------------------
// Domain models used by the repository (placeholders — define in domain layer)
// ---------------------------------------------------------------------------
// These are inlined here for clarity; the actual domain models live in
// com.novamesh.domain.model. Adjust imports as needed.
// ---------------------------------------------------------------------------

/**
 * Repository that bridges the local Room database and the remote Matrix SDK.
 *
 * Responsibilities:
 *   - Provides a unified API for message operations
 *   - Maps between [MessageEntity] (Room) and [Message] (domain)
 *   - Encrypts outgoing messages via [SignalProtocolManager] before sending
 *   - Decrypts incoming messages from [MatrixRepository]
 *   - Syncs remote state into the local cache
 *
 * All write operations update the local database optimistically and then
 * attempt remote delivery, updating status on failure.
 */
class MessageRepository(
    private val messageDao: MessageDao,
    private val chatDao: ChatDao,
    private val matrixRepository: MatrixRepository,
    private val signalProtocolManager: SignalProtocolManager,
    private val userId: String, // current logged-in user ID
) {

    /**
     * Observe messages for a chat, mapped from entities to domain models.
     */
    fun getMessages(chatId: String): Flow<List<Message>> {
        return messageDao.getMessagesForChat(chatId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    /**
     * Send a message: encrypt locally, store optimistically, deliver via Matrix.
     *
     * @param chatId The target chat/room ID
     * @param content The message content (text, image, etc.)
     * @return Result containing the sent [Message], or failure
     */
    suspend fun sendMessage(chatId: String, content: MessageContent): Result<Message> {
        return try {
            // 1. Create the entity with SENDING status
            val messageId = UUID.randomUUID().toString()
            val timestamp = System.currentTimeMillis()
            val type = content.type

            // 2. Encrypt content using Signal protocol
            val encryptedContent = signalProtocolManager.encrypt(
                content = content.toJson(),
                chatId = chatId,
            )

            val entity = MessageEntity(
                id = messageId,
                chatId = chatId,
                senderId = userId,
                content = encryptedContent,
                type = type.name,
                timestamp = timestamp,
                status = MessageStatus.SENDING.name,
                encryptionType = "OLM",
            )

            // 3. Store locally (optimistic insert)
            messageDao.insertMessage(entity)

            // 4. Send via Matrix
            val domainMessage = entity.toDomain()
            val sendResult = matrixRepository.sendMessage(chatId, domainMessage)

            // 5. Update local status based on result
            if (sendResult.isSuccess) {
                messageDao.updateMessageStatus(messageId, MessageStatus.SENT.name)
            } else {
                messageDao.updateMessageStatus(messageId, MessageStatus.FAILED.name)
            }

            // 6. Update chat's last message preview
            updateChatPreview(chatId, content, userId, timestamp)

            // Return the latest state
            val updated = messageDao.getMessageById(messageId)
            Result.success(updated?.toDomain() ?: domainMessage)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Delete a message locally and from the remote server.
     */
    suspend fun deleteMessage(messageId: String): Result<Unit> {
        return try {
            val entity = messageDao.getMessageById(messageId)
                ?: return Result.failure(IllegalArgumentException("Message not found: $messageId"))

            // Attempt remote deletion
            matrixRepository.sendMessageDelete(entity.chatId, entity.id)

            // Delete locally
            messageDao.deleteMessage(entity)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Toggle a reaction (emoji) on a message.
     *
     * Reads the current reactions JSON, adds or removes the user's reaction,
     * persists locally, and syncs to the remote server.
     */
    suspend fun toggleReaction(messageId: String, emoji: String): Result<Unit> {
        return try {
            val entity = messageDao.getMessageById(messageId)
                ?: return Result.failure(IllegalArgumentException("Message not found: $messageId"))

            val reactions = entity.reactionsJson?.let { parseReactions(it) } ?: mutableMapOf()
            val currentUsers = reactions[emoji]?.toMutableList() ?: mutableListOf()

            if (userId in currentUsers) {
                currentUsers.remove(userId)
            } else {
                currentUsers.add(userId)
            }

            reactions[emoji] = currentUsers
            if (currentUsers.isEmpty()) reactions.remove(emoji)

            val updatedJson = serializeReactions(reactions)
            messageDao.updateReactions(messageId, updatedJson)

            // Sync to Matrix
            matrixRepository.sendReaction(entity.chatId, messageId, emoji)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Search messages within a chat by content.
     */
    fun searchMessages(chatId: String, query: String): Flow<List<Message>> {
        return messageDao.searchMessages(chatId, query).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    /**
     * Full-text search across all messages.
     */
    fun searchAllMessages(query: String): Flow<List<Message>> {
        // Delegate to a broader query; for simplicity we search each chat.
        // A real implementation would use FTS4/5 tables.
        return messageDao.searchMessages("%", query).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    /**
     * Sync messages from the remote server for a given chat.
     *
     * Fetches pending messages from Matrix and merges them into the local
     * database, decrypting each one using the Signal protocol manager.
     */
    suspend fun syncMessages(chatId: String): Result<Unit> {
        return try {
            val remoteMessages = matrixRepository.getMessages(chatId)
            // Collect the flow once
            val messages = mutableListOf<Message>()
            remoteMessages.collect { batch ->
                messages.addAll(batch)
                return@collect // only take first emission for sync
            }

            for (msg in messages) {
                val entity = msg.toEntity()
                // Decrypt if needed
                val decryptedContent = if (entity.encryptionType != "NONE") {
                    signalProtocolManager.decrypt(entity.content, chatId)
                } else {
                    entity.content
                }
                val decryptedEntity = entity.copy(content = decryptedContent)
                messageDao.insertMessage(decryptedEntity)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Mark a message as read and notify the remote server.
     */
    suspend fun markAsRead(chatId: String, messageId: String): Result<Unit> {
        return try {
            messageDao.updateMessageStatus(messageId, MessageStatus.READ.name)
            matrixRepository.sendReadReceipt(chatId, messageId)
            chatDao.clearUnread(chatId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get unread message count for a chat.
     */
    fun getUnreadCount(chatId: String, since: Long): Flow<Int> {
        return messageDao.getMessageCountSince(chatId, since)
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    /**
     * Update the chat's last message preview after sending/receiving a message.
     */
    private suspend fun updateChatPreview(
        chatId: String,
        content: MessageContent,
        senderId: String,
        timestamp: Long,
    ) {
        val preview = when (content) {
            is MessageContent.Text -> content.text.take(100)
            is MessageContent.Image -> "🖼 Photo"
            is MessageContent.Video -> "🎬 Video"
            is MessageContent.Audio -> "🎵 Audio"
            is MessageContent.File -> "📎 ${content.fileName}"
            is MessageContent.Location -> "📍 Location"
            is MessageContent.Sticker -> "🎨 Sticker"
            is MessageContent.Gif -> "GIF"
            is MessageContent.Call -> "📞 ${content.callType} call"
            is MessageContent.System -> content.text
            else -> "New message"
        }
        chatDao.updateLastMessage(chatId, preview, timestamp, senderId)
    }

    /**
     * Parse a reactions JSON string into a mutable map.
     * Expected format: {"emoji": ["user1", "user2"], ...}
     */
    private fun parseReactions(json: String): MutableMap<String, List<String>> {
        return try {
            // Simple JSON parsing without external dependency in this layer.
            // In production, use kotlinx.serialization or Gson.
            @Suppress("UNCHECKED_CAST")
            (com.google.gson.Gson().fromJson(json, Map::class.java) as? Map<String, List<String>>)
                ?.mapValues { it.value.toList() }
                ?.toMutableMap()
                ?: mutableMapOf()
        } catch (_: Exception) {
            mutableMapOf()
        }
    }

    /**
     * Serialize a reactions map to JSON.
     */
    private fun serializeReactions(reactions: Map<String, List<String>>): String {
        return com.google.gson.Gson().toJson(reactions)
    }

    // -----------------------------------------------------------------------
    // Mapping extensions
    // -----------------------------------------------------------------------

    /**
     * Map a Room entity to a domain [Message].
     */
    private fun MessageEntity.toDomain(): Message {
        return Message(
            id = id,
            chatId = chatId,
            sender = User(id = senderId, username = "", displayName = senderId),
            content = MessageContent.fromJson(content, type),
            type = MessageType.valueOf(type),
            timestamp = timestamp,
            status = MessageStatus.valueOf(status),
            isEdited = isEdited,
            isPinned = isPinned,
            isStarred = isStarred,
            replyToId = replyToId,
            reactions = reactionsJson?.let { parseReactions(it) } ?: emptyMap(),
            disappearingTimerSeconds = disappearingTimerSeconds,
            encryptionType = encryptionType,
            readBy = readByJson?.let { parseReadBy(it) } ?: emptyList(),
            deliveryStatus = deliveryStatus,
        )
    }

    /**
     * Map a domain [Message] to a Room [MessageEntity].
     */
    private fun Message.toEntity(): MessageEntity {
        return MessageEntity(
            id = id,
            chatId = chatId,
            senderId = sender.id,
            content = content.toJson(),
            type = type.name,
            timestamp = timestamp,
            status = status.name,
            isEdited = isEdited,
            isPinned = isPinned,
            isStarred = isStarred,
            replyToId = replyToId,
            reactionsJson = if (reactions.isNotEmpty()) serializeReactions(reactions) else null,
            disappearingTimerSeconds = disappearingTimerSeconds,
            encryptionType = encryptionType,
            readByJson = if (readBy.isNotEmpty()) serializeReadBy(readBy) else null,
            deliveryStatus = deliveryStatus,
        )
    }

    private fun parseReadBy(json: String): List<String> {
        return try {
            @Suppress("UNCHECKED_CAST")
            com.google.gson.Gson().fromJson(json, List::class.java) as? List<String>
                ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun serializeReadBy(userIds: List<String>): String {
        return com.google.gson.Gson().toJson(userIds)
    }
}

// ---------------------------------------------------------------------------
// Minimal domain model stubs (replace with actual domain model imports)
// ---------------------------------------------------------------------------

/**
 * Placeholder domain model — replace with actual import from domain layer.
 * These definitions are here to make the repository compile independently.
 * In the real project, remove these and import from com.novamesh.domain.model.
 */
object PlaceholderModels {

    data class Message(
        val id: String,
        val chatId: String,
        val sender: User,
        val content: MessageContent,
        val type: MessageType,
        val timestamp: Long,
        val status: MessageStatus,
        val isEdited: Boolean = false,
        val isPinned: Boolean = false,
        val isStarred: Boolean = false,
        val replyToId: String? = null,
        val reactions: Map<String, List<String>> = emptyMap(),
        val disappearingTimerSeconds: Int? = null,
        val encryptionType: String = "NONE",
        val readBy: List<String> = emptyList(),
        val deliveryStatus: String = "SENT",
    )

    data class User(
        val id: String,
        val username: String,
        val displayName: String,
        val avatarUri: String? = null,
        val presence: UserPresence = UserPresence.OFFLINE,
    )

    enum class UserPresence { ONLINE, OFFLINE, IDLE, BUSY }

    enum class MessageType { TEXT, IMAGE, VIDEO, AUDIO, FILE, LOCATION, STICKER, GIF, CALL, SYSTEM }

    enum class MessageStatus { SENDING, SENT, DELIVERED, READ, FAILED, PENDING }

    sealed class MessageContent {
        abstract val type: MessageType

        data class Text(val text: String) : MessageContent() {
            override val type = MessageType.TEXT
        }

        data class Image(val uri: String, val caption: String? = null) : MessageContent() {
            override val type = MessageType.IMAGE
        }

        data class Video(val uri: String, val thumbnailUri: String? = null) : MessageContent() {
            override val type = MessageType.VIDEO
        }

        data class Audio(val uri: String, val durationMs: Long? = null) : MessageContent() {
            override val type = MessageType.AUDIO
        }

        data class File(val uri: String, val fileName: String, val fileSize: Long? = null) : MessageContent() {
            override val type = MessageType.FILE
        }

        data class Location(val latitude: Double, val longitude: Double) : MessageContent() {
            override val type = MessageType.LOCATION
        }

        data class Sticker(val uri: String) : MessageContent() {
            override val type = MessageType.STICKER
        }

        data class Gif(val uri: String) : MessageContent() {
            override val type = MessageType.GIF
        }

        data class Call(val callType: String, val durationSeconds: Long? = null) : MessageContent() {
            override val type = MessageType.CALL
        }

        data class System(val text: String) : MessageContent() {
            override val type = MessageType.SYSTEM
        }

        fun toJson(): String = com.google.gson.Gson().toJson(this)

        companion object {
            fun fromJson(json: String, typeName: String): MessageContent {
                val type = try { MessageType.valueOf(typeName) } catch (_: Exception) { MessageType.TEXT }
                return try {
                    val gson = com.google.gson.Gson()
                    when (type) {
                        MessageType.TEXT -> gson.fromJson(json, Text::class.java)
                        MessageType.IMAGE -> gson.fromJson(json, Image::class.java)
                        MessageType.VIDEO -> gson.fromJson(json, Video::class.java)
                        MessageType.AUDIO -> gson.fromJson(json, Audio::class.java)
                        MessageType.FILE -> gson.fromJson(json, File::class.java)
                        MessageType.LOCATION -> gson.fromJson(json, Location::class.java)
                        MessageType.STICKER -> gson.fromJson(json, Sticker::class.java)
                        MessageType.GIF -> gson.fromJson(json, Gif::class.java)
                        MessageType.CALL -> gson.fromJson(json, Call::class.java)
                        MessageType.SYSTEM -> gson.fromJson(json, System::class.java)
                    }
                } catch (_: Exception) {
                    Text(json) // fallback
                }
            }
        }
    }
}

// Re-export for convenience — replace these with actual imports in production
typealias Message = PlaceholderModels.Message
typealias User = PlaceholderModels.User
typealias UserPresence = PlaceholderModels.UserPresence
typealias MessageContent = PlaceholderModels.MessageContent
typealias MessageType = PlaceholderModels.MessageType
typealias MessageStatus = PlaceholderModels.MessageStatus
