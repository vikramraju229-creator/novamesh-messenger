package com.novamesh.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Room entity representing a chat message stored locally with SQLCipher encryption.
 *
 * Messages are associated with a chat via [chatId] and contain serialized content,
 * metadata about encryption type, delivery status, and user interactions
 * (reactions, pinning, starring, read receipts).
 */
@Entity(
    tableName = "messages",
    indices = [
        Index("chatId"),
        Index("senderId"),
        Index("timestamp"),
        Index("chatId", "timestamp")
    ],
    foreignKeys = [
        ForeignKey(
            entity = ChatEntity::class,
            parentColumns = ["id"],
            childColumns = ["chatId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class MessageEntity(
    /** Unique message identifier (UUID) */
    @PrimaryKey val id: String,
    /** ID of the chat this message belongs to */
    val chatId: String,
    /** ID of the user who sent this message */
    val senderId: String,
    /** JSON-serialized [MessageContent] payload */
    val content: String,
    /** [MessageType] enum name (e.g. "TEXT", "IMAGE", "VIDEO") */
    val type: String,
    /** Unix timestamp in milliseconds of when the message was sent */
    val timestamp: Long,
    /** [MessageStatus] enum name (e.g. "SENDING", "SENT", "DELIVERED", "READ") */
    val status: String,
    /** Whether the message has been edited */
    val isEdited: Boolean = false,
    /** Whether the message is pinned in the chat */
    val isPinned: Boolean = false,
    /** Whether the message is starred by the current user */
    val isStarred: Boolean = false,
    /** ID of the message this message is replying to, or null */
    val replyToId: String? = null,
    /** JSON map of emoji -> list of userIds who reacted */
    val reactionsJson: String? = null,
    /** Optional disappearing-message timer in seconds */
    val disappearingTimerSeconds: Int? = null,
    /** Encryption type used: "NONE", "OLM", "MEGOLM", "CUSTOM" */
    val encryptionType: String = "NONE",
    /** JSON list of userIds who have read this message */
    val readByJson: String? = null,
    /** Delivery status: "SENT", "DELIVERED", "FAILED", "PENDING" */
    val deliveryStatus: String = "SENT",
)

/**
 * Room entity representing a chat (1:1 conversation or group room).
 */
@Entity(tableName = "chats")
data class ChatEntity(
    /** Unique chat/room identifier */
    @PrimaryKey val id: String,
    /** Display name of the chat */
    val name: String,
    /** URI for the chat avatar image, or null */
    val avatarUri: String? = null,
    /** Whether this is a group chat (false = 1:1 direct message) */
    val isGroup: Boolean = false,
    /** Preview text of the most recent message */
    val lastMessagePreview: String? = null,
    /** Unix timestamp of the most recent message */
    val lastMessageTimestamp: Long = 0L,
    /** ID of the user who sent the most recent message */
    val lastMessageSenderId: String? = null,
    /** Number of unread messages */
    val unreadCount: Int = 0,
    /** Whether the chat is pinned at the top of the list */
    val isPinned: Boolean = false,
    /** Whether notifications are muted for this chat */
    val isMuted: Boolean = false,
    /** Optional disappearing-message timer in seconds */
    val disappearingTimerSeconds: Int? = null,
    /** Unix timestamp of when the chat was created */
    val createdAt: Long,
)

/**
 * Room entity representing a user (contact or known peer).
 */
@Entity(tableName = "users")
data class UserEntity(
    /** Unique user identifier (Matrix user ID or internal ID) */
    @PrimaryKey val id: String,
    /** Username (unique handle) */
    val username: String,
    /** Display name shown in the UI */
    val displayName: String,
    /** URI for the user's avatar image, or null */
    val avatarUri: String? = null,
    /** Short user biography, or null */
    val bio: String? = null,
    /** Phone number (E.164 format), or null */
    val phoneNumber: String? = null,
    /** Current presence: "ONLINE", "OFFLINE", "IDLE", "BUSY" */
    val presence: String = "OFFLINE",
    /** Unix timestamp of last seen time */
    val lastSeen: Long = 0L,
    /** Whether the current user has blocked this user */
    val isBlocked: Boolean = false,
    /** Whether notifications from this user are muted */
    val isMuted: Boolean = false,
    /** Whether this user is in the current user's contact list */
    val isContact: Boolean = false,
    /** Snap streak counter (ephemeral feature) */
    val snapStreak: Int = 0,
    /** Whether ghost mode (invisible presence) is enabled */
    val ghostMode: Boolean = false,
)
