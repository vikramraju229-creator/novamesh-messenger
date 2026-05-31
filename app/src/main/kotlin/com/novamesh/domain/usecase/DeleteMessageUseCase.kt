/**
 * Use case for deleting a message from a chat.
 */
package com.novamesh.domain.usecase

import com.novamesh.data.local.dao.MessageDao
import com.novamesh.data.remote.MatrixRepository

/**
 * @param messageDao Local DAO for messages.
 * @param matrixRepository Remote repository for sync.
 */
class DeleteMessageUseCase(
    private val messageDao: MessageDao,
    private val matrixRepository: MatrixRepository,
) {
    /**
     * Delete a message by ID — removes locally and redacts on Matrix.
     */
    suspend operator fun invoke(messageId: String): Result<Unit> {
        return try {
            val entity = messageDao.getMessageById(messageId)
                ?: return Result.failure(IllegalArgumentException("Message not found: $messageId"))

            matrixRepository.sendMessageDelete(entity.chatId, entity.id)
            messageDao.deleteMessage(entity)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
