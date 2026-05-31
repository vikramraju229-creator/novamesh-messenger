/**
 * Use case for sending a message in a chat.
 *
 * Writes directly to the local database (optimistic insert with SENDING status)
 * and dispatches to the Matrix repository for remote delivery.
 */
package com.novamesh.domain.usecase

import com.novamesh.data.local.dao.ChatDao
import com.novamesh.data.local.dao.MessageDao
import com.novamesh.data.local.entity.MessageEntity
import com.novamesh.data.remote.MatrixRepository
import com.novamesh.domain.model.DeliveryStatus
import com.novamesh.domain.model.Message
import com.novamesh.domain.model.MessageContent
import com.novamesh.domain.model.MessageStatus
import com.novamesh.domain.model.MessageType
import com.google.gson.Gson
import java.util.UUID

/**
 * @param messageDao Local DAO for messages.
 * @param chatDao Local DAO for updating chat preview.
 * @param matrixRepository Remote Matrix repository.
 * @param currentUserId The logged-in user's ID.
 */
class SendMessageUseCase(
    private val messageDao: MessageDao,
    private val chatDao: ChatDao,
    private val matrixRepository: MatrixRepository,
    private val currentUserId: String,
) {
    /**
     * Send a message with optimistic local insert + remote delivery.
     *
     * @param chatId Target chat ID.
     * @param content The message content.
     * @return Result containing the sent [Message] or failure.
     */
    suspend operator fun invoke(chatId: String, content: MessageContent): Result<Message> {
        return try {
            val messageId = UUID.randomUUID().toString()
            val timestamp = System.currentTimeMillis()
            val type = when (content) {
                is MessageContent.Text -> MessageType.TEXT
                is MessageContent.Image -> MessageType.IMAGE
                is MessageContent.Video -> MessageType.VIDEO
                is MessageContent.Audio -> MessageType.AUDIO
                is MessageContent.Voice -> MessageType.VOICE
                is MessageContent.File -> MessageType.FILE
                is MessageContent.Location -> MessageType.LOCATION
                is MessageContent.Contact -> MessageType.CONTACT
                is MessageContent.Sticker -> MessageType.STICKER
                is MessageContent.Gif -> MessageType.GIF
                is MessageContent.System -> MessageType.SYSTEM
            }

            val contentJson = Gson().toJson(content)

            val entity = MessageEntity(
                id = messageId,
                chatId = chatId,
                senderId = currentUserId,
                content = contentJson,
                type = type.name,
                timestamp = timestamp,
                status = MessageStatus.SENDING.name,
            )

            // Optimistic insert
            messageDao.insertMessage(entity)

            // Send via Matrix (fire-and-forget; status updated on completion)
            val msg = entity.toMessage()
            matrixRepository.sendMessage(chatId, msg)

            // Update status to SENT
            messageDao.updateMessageStatus(messageId, MessageStatus.SENT.name)

            // Update chat preview
            val preview = contentPreview(content)
            chatDao.updateLastMessage(chatId, preview, timestamp, currentUserId)

            val updated = messageDao.getMessageById(messageId)
            Result.success(updated?.toMessage() ?: msg)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Generate a text preview of the content for the chat list. */
    private fun contentPreview(content: MessageContent): String = when (content) {
        is MessageContent.Text -> content.text.take(100)
        is MessageContent.Image -> "🖼 Photo"
        is MessageContent.Video -> "🎬 Video"
        is MessageContent.Audio -> "🎵 Audio"
        is MessageContent.Voice -> "🎤 Voice message"
        is MessageContent.File -> "📎 ${content.fileName}"
        is MessageContent.Location -> "📍 Location"
        is MessageContent.Contact -> "👤 Contact: ${content.name}"
        is MessageContent.Sticker -> "🎨 Sticker"
        is MessageContent.Gif -> "GIF"
        is MessageContent.System -> content.text
    }

    /** Quick map from entity to a minimal domain Message. */
    private fun MessageEntity.toMessage(): Message = Message(
        id = id,
        chatId = chatId,
        senderId = senderId,
        senderName = senderId,
        content = try { Gson().fromJson(content, MessageContent::class.java) } catch (_: Exception) { MessageContent.Text("") },
        type = try { MessageType.valueOf(type) } catch (_: Exception) { MessageType.TEXT },
        timestamp = timestamp,
        status = try { MessageStatus.valueOf(status) } catch (_: Exception) { MessageStatus.SENT },
        deliveryStatus = try { DeliveryStatus.valueOf(deliveryStatus) } catch (_: Exception) { DeliveryStatus.SENT },
    )
}
