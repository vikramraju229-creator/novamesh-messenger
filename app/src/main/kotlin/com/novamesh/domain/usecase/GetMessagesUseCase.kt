/**
 * Use case for observing messages within a single chat.
 *
 * Provides reactive [Flow] access to messages sorted newest-first,
 * and supports search within the chat.
 */
package com.novamesh.domain.usecase

import com.novamesh.data.local.dao.MessageDao
import com.novamesh.data.local.entity.MessageEntity
import com.novamesh.domain.model.DeliveryStatus
import com.novamesh.domain.model.EncryptionType
import com.novamesh.domain.model.Message
import com.novamesh.domain.model.MessageContent
import com.novamesh.domain.model.MessageStatus
import com.novamesh.domain.model.MessageType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * @param messageDao Local DAO for message entities.
 */
class GetMessagesUseCase(
    private val messageDao: MessageDao,
) {
    /**
     * Observe messages for a given chat, newest-first.
     */
    operator fun invoke(chatId: String): Flow<List<Message>> {
        return messageDao.getMessagesForChat(chatId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    /**
     * Search messages within a chat.
     */
    fun search(chatId: String, query: String): Flow<List<Message>> {
        return messageDao.searchMessages(chatId, query).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    /** Map [MessageEntity] to domain [Message]. */
    private fun MessageEntity.toDomain(): Message = Message(
        id = id,
        chatId = chatId,
        senderId = senderId,
        senderName = senderId,
        content = parseContent(content, type),
        type = try { MessageType.valueOf(type) } catch (_: Exception) { MessageType.TEXT },
        timestamp = timestamp,
        status = try { MessageStatus.valueOf(status) } catch (_: Exception) { MessageStatus.SENT },
        isEdited = isEdited,
        isPinned = isPinned,
        isStarred = isStarred,
        replyToId = replyToId,
        reactions = parseReactions(reactionsJson),
        disappearingTimerSeconds = disappearingTimerSeconds,
        encryptionType = try { EncryptionType.valueOf(encryptionType) } catch (_: Exception) { EncryptionType.NONE },
        readBy = parseReadBy(readByJson),
        deliveryStatus = try { DeliveryStatus.valueOf(deliveryStatus) } catch (_: Exception) { DeliveryStatus.SENT },
    )

    private fun parseContent(json: String, typeName: String): MessageContent {
        val type = try { MessageType.valueOf(typeName) } catch (_: Exception) { MessageType.TEXT }
        return try {
            val gson = Gson()
            when (type) {
                MessageType.TEXT -> gson.fromJson(json, MessageContent.Text::class.java)
                MessageType.IMAGE -> gson.fromJson(json, MessageContent.Image::class.java)
                MessageType.VIDEO -> gson.fromJson(json, MessageContent.Video::class.java)
                MessageType.AUDIO -> gson.fromJson(json, MessageContent.Audio::class.java)
                MessageType.FILE -> gson.fromJson(json, MessageContent.File::class.java)
                MessageType.LOCATION -> gson.fromJson(json, MessageContent.Location::class.java)
                MessageType.CONTACT -> gson.fromJson(json, MessageContent.Contact::class.java)
                MessageType.STICKER -> gson.fromJson(json, MessageContent.Sticker::class.java)
                MessageType.GIF -> gson.fromJson(json, MessageContent.Gif::class.java)
                MessageType.VOICE -> gson.fromJson(json, MessageContent.Voice::class.java)
                MessageType.SYSTEM -> gson.fromJson(json, MessageContent.System::class.java)
            }
        } catch (_: Exception) {
            MessageContent.Text(json.take(200))
        }
    }

    private fun parseReactions(json: String?): Map<String, String> {
        if (json.isNullOrBlank()) return emptyMap()
        return try {
            val type = object : TypeToken<Map<String, List<String>>>() {}.type
            val raw: Map<String, List<String>> = Gson().fromJson(json, type)
            // Flatten to userId -> first emoji
            raw.flatMap { (emoji, users) -> users.map { it to emoji } }.toMap()
        } catch (_: Exception) { emptyMap() }
    }

    private fun parseReadBy(json: String?): List<String> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val type = object : TypeToken<List<String>>() {}.type
            Gson().fromJson(json, type)
        } catch (_: Exception) { emptyList() }
    }
}
