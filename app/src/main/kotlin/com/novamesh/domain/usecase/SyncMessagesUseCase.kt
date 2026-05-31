/**
 * Use case for syncing messages from the Matrix server.
 */
package com.novamesh.domain.usecase

import com.novamesh.data.repository.MessageRepository

/**
 * @param messageRepository The unified message repository.
 */
class SyncMessagesUseCase(
    private val messageRepository: MessageRepository,
) {
    /**
     * Sync pending messages from the remote server for a given chat.
     */
    suspend operator fun invoke(chatId: String): Result<Unit> {
        return messageRepository.syncMessages(chatId)
    }
}
