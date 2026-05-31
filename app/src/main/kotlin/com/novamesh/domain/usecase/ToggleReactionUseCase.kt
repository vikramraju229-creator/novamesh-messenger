/**
 * Use case for toggling an emoji reaction on a message.
 *
 * Reads current reactions JSON, adds or removes the current user's reaction,
 * persists locally, and syncs to the Matrix server.
 */
package com.novamesh.domain.usecase

import com.novamesh.data.local.dao.MessageDao
import com.novamesh.data.remote.MatrixRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * @param messageDao Local DAO for messages.
 * @param matrixRepository Remote repository for sync.
 * @param currentUserId The logged-in user's ID.
 */
class ToggleReactionUseCase(
    private val messageDao: MessageDao,
    private val matrixRepository: MatrixRepository,
    private val currentUserId: String,
) {
    /**
     * Toggle a reaction on a message.
     *
     * @param messageId The target message ID.
     * @param emoji The emoji to toggle (e.g. "🔥", "❤️").
     * @return Result indicating success or failure.
     */
    suspend operator fun invoke(messageId: String, emoji: String): Result<Unit> {
        return try {
            val entity = messageDao.getMessageById(messageId)
                ?: return Result.failure(IllegalArgumentException("Message not found: $messageId"))

            val reactions = parseReactions(entity.reactionsJson).toMutableMap()
            val currentEmoji = reactions[currentUserId]

            if (currentEmoji == emoji) {
                // Remove reaction
                reactions.remove(currentUserId)
            } else {
                // Add/replace reaction
                reactions[currentUserId] = emoji
            }

            val updatedJson = if (reactions.isEmpty()) null else serializeReactions(reactions)
            messageDao.updateReactions(messageId, updatedJson ?: "")

            // Sync to Matrix
            matrixRepository.sendReaction(entity.chatId, messageId, emoji)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Parse reactions JSON into a userId -> emoji map. */
    private fun parseReactions(json: String?): Map<String, String> {
        if (json.isNullOrBlank()) return emptyMap()
        return try {
            val type = object : TypeToken<Map<String, String>>() {}.type
            Gson().fromJson(json, type)
        } catch (_: Exception) { emptyMap() }
    }

    /** Serialize a userId -> emoji map to JSON. */
    private fun serializeReactions(reactions: Map<String, String>): String {
        return Gson().toJson(reactions)
    }
}
