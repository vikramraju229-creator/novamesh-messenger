package com.novamesh.domain.model

import kotlinx.serialization.Serializable

/**
 * Represents a 24-hour ephemeral story (WhatsApp / Snapchat hybrid).
 */
@Serializable
data class Story(
    val id: String,
    val userId: String,
    val userName: String,
    val userAvatarUri: String? = null,
    val mediaUri: String,
    val thumbnailUri: String? = null,
    val type: StoryType,
    val caption: String? = null,
    val musicOverlay: String? = null,   // URI of background music
    val durationSeconds: Int = 10,      // 1-60 seconds
    val createdAt: Long,
    val expiresAt: Long,                // createdAt + 24h
    val viewedBy: List<StoryView> = emptyList(),
    val reactions: Map<String, String> = emptyMap(), // userId -> emoji
    val privacy: StoryPrivacy = StoryPrivacy.ALL_CONTACTS,
    val customViewers: List<String> = emptyList(),   // whitelist user IDs
    val exceptViewers: List<String> = emptyList(),    // blacklist user IDs
    val allowReplies: Boolean = true,
    val allowScreenshots: Boolean = false,
    val isCloseFriends: Boolean = false,
)

@Serializable
enum class StoryType {
    IMAGE,
    VIDEO,
    TEXT,          // Text-only status
    MUSIC,         // Music sharing status
}

@Serializable
data class StoryView(
    val userId: String,
    val userName: String,
    val viewedAt: Long,
)

@Serializable
enum class StoryPrivacy {
    ALL_CONTACTS,
    CONTACTS_EXCEPT,
    CUSTOM,
    CLOSE_FRIENDS,
}

/**
 * Represents a snap streak between two users.
 */
@Serializable
data class SnapStreak(
    val userId1: String,
    val userId2: String,
    val currentStreak: Int,        // consecutive days
    val longestStreak: Int,
    val lastSnapDate: String,      // "2026-05-31"
    val isExpiring: Boolean = false, // true if about to expire
)
