package com.novamesh.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.novamesh.data.local.entity.ChatEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for [ChatEntity].
 *
 * Provides reactive queries for the chat list (sorted by pinned status
 * and most recent message timestamp) and granular update operations
 * for unread counts, mute, pin, and last message preview.
 */
@Dao
interface ChatDao {

    /**
     * Observe all chats, ordered by pinned status first, then by most
     * recent message timestamp descending.
     */
    @Query("SELECT * FROM chats ORDER BY isPinned DESC, lastMessageTimestamp DESC")
    fun getAllChats(): Flow<List<ChatEntity>>

    /**
     * Get a single chat by its ID.
     */
    @Query("SELECT * FROM chats WHERE id = :chatId")
    suspend fun getChatById(chatId: String): ChatEntity?

    /**
     * Insert or replace a chat entity.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChat(chat: ChatEntity)

    /**
     * Delete a chat entity (cascades to associated messages via FK).
     */
    @Delete
    suspend fun deleteChat(chat: ChatEntity)

    /**
     * Atomically increment the unread count for a chat.
     */
    @Query("UPDATE chats SET unreadCount = unreadCount + 1 WHERE id = :chatId")
    suspend fun incrementUnread(chatId: String)

    /**
     * Reset the unread count to zero (when user opens the chat).
     */
    @Query("UPDATE chats SET unreadCount = 0 WHERE id = :chatId")
    suspend fun clearUnread(chatId: String)

    /**
     * Toggle the pinned state of a chat.
     */
    @Query("UPDATE chats SET isPinned = :isPinned WHERE id = :chatId")
    suspend fun pinChat(chatId: String, isPinned: Boolean)

    /**
     * Toggle the muted state of a chat.
     */
    @Query("UPDATE chats SET isMuted = :isMuted WHERE id = :chatId")
    suspend fun muteChat(chatId: String, isMuted: Boolean)

    /**
     * Update the last message preview, timestamp, and sender ID.
     * Called after sending or receiving a new message in a chat.
     */
    @Query("""
        UPDATE chats 
        SET lastMessagePreview = :preview, 
            lastMessageTimestamp = :timestamp, 
            lastMessageSenderId = :senderId 
        WHERE id = :chatId
    """)
    suspend fun updateLastMessage(
        chatId: String,
        preview: String,
        timestamp: Long,
        senderId: String,
    )
}
