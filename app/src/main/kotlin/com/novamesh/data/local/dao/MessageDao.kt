package com.novamesh.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.novamesh.data.local.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for [MessageEntity].
 *
 * Provides reactive queries (Flow) for observing messages in a chat,
 * plus suspend functions for write operations. All read operations use
 * Room's Flow support to automatically re-emit on table invalidation.
 */
@Dao
interface MessageDao {

    /**
     * Observe all messages for a given chat, ordered newest-first.
     * @param chatId The chat identifier
     * @return Flow that emits the full message list on any change
     */
    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY timestamp DESC")
    fun getMessagesForChat(chatId: String): Flow<List<MessageEntity>>

    /**
     * Get a single message by its ID.
     * @param messageId The message UUID
     * @return The message entity, or null if not found
     */
    @Query("SELECT * FROM messages WHERE id = :messageId")
    suspend fun getMessageById(messageId: String): MessageEntity?

    /**
     * Insert or replace a single message.
     * Uses REPLACE strategy so duplicate IDs overwrite existing rows.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    /**
     * Batch insert or replace multiple messages.
     * Useful when syncing a batch of messages from the remote server.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>)

    /**
     * Delete a single message from the database.
     */
    @Delete
    suspend fun deleteMessage(message: MessageEntity)

    /**
     * Delete all messages for a given chat (e.g., when clearing conversation).
     */
    @Query("DELETE FROM messages WHERE chatId = :chatId")
    suspend fun deleteMessagesForChat(chatId: String)

    /**
     * Toggle the pinned state of a message.
     */
    @Query("UPDATE messages SET isPinned = :isPinned WHERE id = :messageId")
    suspend fun pinMessage(messageId: String, isPinned: Boolean)

    /**
     * Toggle the starred state of a message.
     */
    @Query("UPDATE messages SET isStarred = :isStarred WHERE id = :messageId")
    suspend fun starMessage(messageId: String, isStarred: Boolean)

    /**
     * Update the delivery/read status of a message.
     */
    @Query("UPDATE messages SET status = :status WHERE id = :messageId")
    suspend fun updateMessageStatus(messageId: String, status: String)

    /**
     * Update the reactions JSON for a message.
     * @param reactions JSON string representing emoji -> userIds map
     */
    @Query("UPDATE messages SET reactionsJson = :reactions WHERE id = :messageId")
    suspend fun updateReactions(messageId: String, reactions: String)

    /**
     * Full-text search within a chat's messages.
     * @param chatId The chat to search in
     * @param query The search term (SQL LIKE %wildcard% applied)
     * @return Flow emitting matching messages ordered newest-first
     */
    @Query("SELECT * FROM messages WHERE chatId = :chatId AND content LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchMessages(chatId: String, query: String): Flow<List<MessageEntity>>

    /**
     * Observe all starred messages across all chats.
     */
    @Query("SELECT * FROM messages WHERE isStarred = 1 ORDER BY timestamp DESC")
    fun getStarredMessages(): Flow<List<MessageEntity>>

    /**
     * Observe the count of messages in a chat since a given timestamp.
     * Useful for unread badge computation.
     */
    @Query("SELECT COUNT(*) FROM messages WHERE chatId = :chatId AND timestamp > :since")
    fun getMessageCountSince(chatId: String, since: Long): Flow<Int>
}
