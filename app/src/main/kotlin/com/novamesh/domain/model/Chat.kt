/**
 * Domain model representing a chat conversation in NovaMesh Messenger.
 *
 * @property id Unique identifier for this chat.
 * @property name Display name of the chat (contact name or group name).
 * @property avatarUri Optional URI pointing to the chat's avatar image.
 * @property lastMessage Text preview of the most recent message.
 * @property lastMessageTimestamp Epoch millis of the last message.
 * @property lastMessageStatus Delivery status of the last outgoing message.
 * @property unreadCount Number of unread messages in this chat.
 * @property isPinned Whether this chat is pinned to the top of the list.
 * @property isMuted Whether notifications are muted for this chat.
 * @property participants List of [User]s participating in this conversation.
 * @property isGroup Whether this is a group chat (vs. 1:1).
 * @property disappearingTimerSeconds Optional auto-delete timer (null = permanent).
 * @property createdAt Epoch millis when this chat was created.
 */
package com.novamesh.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Chat(
    val id: String,
    val name: String,
    val avatarUri: String? = null,
    val lastMessage: String? = null,
    val lastMessageTimestamp: Long = 0L,
    val lastMessageStatus: MessageStatus = MessageStatus.SENT,
    val unreadCount: Int = 0,
    val isPinned: Boolean = false,
    val isMuted: Boolean = false,
    val participants: List<User> = emptyList(),
    val isGroup: Boolean = false,
    val disappearingTimerSeconds: Int? = null,
    val createdAt: Long = 0L,
)
