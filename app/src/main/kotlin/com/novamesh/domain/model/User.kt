package com.novamesh.domain.model

import kotlinx.serialization.Serializable

/**
 * Represents a NovaMesh user / contact.
 */
@Serializable
data class User(
    val id: String,
    val username: String,
    val displayName: String,
    val avatarUri: String? = null,
    val bio: String? = null,
    val phoneNumber: String? = null,
    val email: String? = null,
    val presence: Presence = Presence.OFFLINE,
    val lastSeen: Long = 0L,
    val isVerified: Boolean = false,
    val isBlocked: Boolean = false,
    val isMuted: Boolean = false,
    val isContact: Boolean = false,
    val customRingtone: String? = null,
    val disappearingTimerOverride: Int? = null, // seconds
    val snapStreak: Int = 0,                    // consecutive days
    val ghostMode: Boolean = false,             // hides read receipts
)

@Serializable
enum class Presence {
    ONLINE,
    OFFLINE,
    AWAY,
    BUSY,
    TYPING,
    RECORDING,
}

/**
 * Represents a group chat.
 */
@Serializable
data class Group(
    val id: String,
    val name: String,
    val avatarUri: String? = null,
    val description: String? = null,
    val members: List<GroupMember>,
    val admins: List<String>,           // user IDs
    val createdAt: Long,
    val maxMembers: Int = 1024,
    val disappearingTimer: Int? = null, // seconds
    val slowModeInterval: Int? = null,  // seconds between messages
    val isBroadcast: Boolean = false,   // WhatsApp Channels-style
    val inviteLink: String? = null,
    val requiresApproval: Boolean = false,
)

@Serializable
data class GroupMember(
    val userId: String,
    val role: GroupRole,
    val joinedAt: Long,
)

@Serializable
enum class GroupRole {
    ADMIN,
    MODERATOR,
    MEMBER,
}
