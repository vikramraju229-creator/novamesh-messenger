package com.novamesh.data.repository

import android.net.Uri
import com.novamesh.data.remote.FirestoreMessage
import com.novamesh.data.remote.FirestoreRepository
import kotlinx.coroutines.flow.Flow

/**
 * Repository for chat-related operations.
 *
 * Wraps [FirestoreRepository] and provides domain-level
 * methods for sending/receiving messages.
 */
class ChatRepository(
    private val firestoreRepository: FirestoreRepository = FirestoreRepository(),
) {

    /** Send a text message. */
    suspend fun sendMessage(chatId: String, text: String): String? {
        return firestoreRepository.sendMessage(chatId, text)
    }

    /** Send a media message (image, video, etc.). */
    suspend fun sendMediaMessage(chatId: String, uri: Uri, type: String = "image"): String? {
        val mediaUrl = firestoreRepository.uploadMedia(uri) ?: return null
        return firestoreRepository.sendMessage(chatId, "", type, mediaUrl)
    }

    /** Observe messages in a chat in real-time. */
    fun observeMessages(chatId: String): Flow<List<FirestoreMessage>> {
        return firestoreRepository.observeMessages(chatId)
    }

    /** Mark a message as delivered. */
    suspend fun markDelivered(messageId: String, chatId: String) {
        firestoreRepository.updateMessageStatus(messageId, chatId, "delivered")
    }

    /** Mark a message as read. */
    suspend fun markRead(messageId: String, chatId: String) {
        firestoreRepository.updateMessageStatus(messageId, chatId, "read")
    }

    /** Delete a message (soft delete). */
    suspend fun deleteMessage(messageId: String, chatId: String) {
        firestoreRepository.deleteMessage(messageId, chatId)
    }

    /** Get or create a 1:1 chat. */
    suspend fun getOrCreateChat(otherUserId: String): String? {
        return firestoreRepository.getOrCreateChat(otherUserId)
    }

    /** Observe all chats for the current user. */
    fun observeChats(): Flow<List<com.novamesh.data.remote.FirestoreChat>> {
        return firestoreRepository.observeChats()
    }
}
