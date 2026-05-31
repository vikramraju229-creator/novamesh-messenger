/**
 * Use case for observing and filtering the chat list.
 *
 * Provides a reactive [Flow] of [Chat] objects from the local database,
 * sorted by pinned status then recency, with optional text search.
 */
package com.novamesh.domain.usecase

import com.novamesh.data.local.dao.ChatDao
import com.novamesh.data.local.dao.UserDao
import com.novamesh.data.local.entity.ChatEntity
import com.novamesh.data.local.entity.UserEntity
import com.novamesh.domain.model.Chat
import com.novamesh.domain.model.MessageStatus
import com.novamesh.domain.model.Presence
import com.novamesh.domain.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * @param chatDao Local DAO for chat entities.
 * @param userDao Local DAO for resolving participant profiles.
 */
class GetChatsUseCase(
    private val chatDao: ChatDao,
    private val userDao: UserDao,
) {
    /**
     * Observe all chats, optionally filtered by [searchQuery].
     */
    operator fun invoke(searchQuery: String = ""): Flow<List<Chat>> {
        return chatDao.getAllChats().map { entities ->
            val filtered = if (searchQuery.isBlank()) entities else {
                val q = searchQuery.trim().lowercase()
                entities.filter {
                    it.name.lowercase().contains(q) ||
                        it.lastMessagePreview?.lowercase()?.contains(q) == true
                }
            }
            filtered.map { it.toDomain() }
        }
    }

    /** Toggle pinned state. */
    suspend fun togglePin(chatId: String, isPinned: Boolean) =
        chatDao.pinChat(chatId, isPinned)

    /** Toggle muted state. */
    suspend fun toggleMute(chatId: String, isMuted: Boolean) =
        chatDao.muteChat(chatId, isMuted)

    /** Archive (delete) a chat. */
    suspend fun archiveChat(chatId: String) {
        chatDao.getChatById(chatId)?.let { chatDao.deleteChat(it) }
    }

    /** Clear unread count for a chat. */
    suspend fun clearUnread(chatId: String) =
        chatDao.clearUnread(chatId)

    /** Map [ChatEntity] to domain [Chat] with participants resolved. */
    private suspend fun ChatEntity.toDomain(): Chat {
        // Load participants from userDao (for 1:1 chats the single participant)
        val participant = lastMessageSenderId?.let { userDao.getUserById(it) }
        val participants = if (participant != null) listOf(participant.toDomain()) else emptyList()
        return Chat(
            id = id,
            name = name,
            avatarUri = avatarUri,
            lastMessage = lastMessagePreview,
            lastMessageTimestamp = lastMessageTimestamp,
            lastMessageStatus = MessageStatus.SENT,
            unreadCount = unreadCount,
            isPinned = isPinned,
            isMuted = isMuted,
            participants = participants,
            isGroup = isGroup,
            disappearingTimerSeconds = disappearingTimerSeconds,
            createdAt = createdAt,
        )
    }

    /** Map [UserEntity] to domain [User]. */
    private fun UserEntity.toDomain(): User = User(
        id = id,
        username = username,
        displayName = displayName,
        avatarUri = avatarUri,
        bio = bio,
        phoneNumber = phoneNumber,
        presence = try { Presence.valueOf(presence) } catch (_: Exception) { Presence.OFFLINE },
        lastSeen = lastSeen,
        isVerified = false,
        isBlocked = isBlocked,
        isMuted = isMuted,
        isContact = isContact,
    )
}
