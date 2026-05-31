/**
 * Use case for marking messages as read and sending read receipts.
 */
package com.novamesh.domain.usecase

import com.novamesh.data.local.dao.ChatDao
import com.novamesh.data.local.dao.MessageDao
import com.novamesh.data.remote.MatrixRepository
import com.novamesh.domain.model.MessageStatus

/**
 * @param messageDao Local DAO for messages.
 * @param chatDao Local DAO for clearing unread counts.
 * @param matrixRepository Remote repository for read receipts.
 */
class MarkAsReadUseCase(
    private val messageDao: MessageDao,
    private val chatDao: ChatDao,
    private val matrixRepository: MatrixRepository,
) {
    /**
     * Mark all messages in a chat as read up to [messageId].
     */
    suspend operator fun invoke(chatId: String, messageId: String): Result<Unit> {
        return try {
            messageDao.updateMessageStatus(messageId, MessageStatus.READ.name)
            matrixRepository.sendReadReceipt(chatId, messageId)
            chatDao.clearUnread(chatId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
