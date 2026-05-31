package com.novamesh.domain.model

import kotlinx.serialization.Serializable

/**
 * Represents a single chat message within NovaMesh.
 *
 * Supports all message types required by the spec:
 * - Text, image, video, audio, file, location, contact, sticker, GIF
 * - Reactions, replies, forwards, pins, stars
 * - Disappearing messages with configurable timer
 * - End-to-end encryption metadata
 */
@Serializable
data class Message(
    val id: String,
    val chatId: String,
    val senderId: String,
    val senderName: String,
    val content: MessageContent,
    val type: MessageType,
    val timestamp: Long,
    val status: MessageStatus = MessageStatus.SENDING,
    val isEdited: Boolean = false,
    val isPinned: Boolean = false,
    val isStarred: Boolean = false,
    val replyToId: String? = null,
    val forwardedFromId: String? = null,
    val reactions: Map<String, String> = emptyMap(), // userId -> emoji
    val disappearingTimerSeconds: Int? = null,        // null = permanent
    val encryptionType: EncryptionType = EncryptionType.NONE,
    val readBy: List<String> = emptyList(),          // user IDs who read it
    val deliveryStatus: DeliveryStatus = DeliveryStatus.SENT,
)

/**
 * The actual content payload of a message.
 */
@Serializable
sealed interface MessageContent {
    @Serializable data class Text(val text: String) : MessageContent
    @Serializable data class Image(val uri: String, val thumbnailUri: String? = null, val caption: String? = null) : MessageContent
    @Serializable data class Video(val uri: String, val thumbnailUri: String? = null, val durationMs: Long = 0) : MessageContent
    @Serializable data class Audio(val uri: String, val durationMs: Long = 0, val waveform: List<Float>? = null) : MessageContent
    @Serializable data class File(val uri: String, val fileName: String, val fileSize: Long, val mimeType: String) : MessageContent
    @Serializable data class Location(val latitude: Double, val longitude: Double, val name: String? = null) : MessageContent
    @Serializable data class Contact(val name: String, val phoneNumber: String, val avatarUri: String? = null) : MessageContent
    @Serializable data class Sticker(val stickerPackId: String, val stickerId: String) : MessageContent
    @Serializable data class Gif(val uri: String, val width: Int = 0, val height: Int = 0) : MessageContent
    @Serializable data class Voice(val uri: String, val durationMs: Long, val waveform: List<Float>? = null) : MessageContent
    @Serializable data class System(val text: String, val action: SystemAction = SystemAction.GENERIC) : MessageContent
}

/**
 * System message actions (e.g., "Alice joined the group").
 */
@Serializable
enum class SystemAction {
    GENERIC,
    MEMBER_JOINED,
    MEMBER_LEFT,
    GROUP_CREATED,
    GROUP_RENAMED,
    GROUP_ICON_CHANGED,
    CALL_STARTED,
    CALL_ENDED,
    DISAPPEARING_MESSAGES_ENABLED,
    DISAPPEARING_MESSAGES_DISABLED,
}

@Serializable
enum class MessageType {
    TEXT,
    IMAGE,
    VIDEO,
    AUDIO,
    FILE,
    LOCATION,
    CONTACT,
    STICKER,
    GIF,
    VOICE,
    SYSTEM,
}

@Serializable
enum class MessageStatus {
    SENDING,
    SENT,
    DELIVERED,
    READ,
    FAILED,
    DELETED,
}

@Serializable
enum class DeliveryStatus {
    SENDING,
    SENT,
    DELIVERED,
    READ,
    FAILED,
}

@Serializable
enum class EncryptionType {
    NONE,
    END_TO_END_ENCRYPTED,
    SIGNAL_PROTOCOL,
    OLM,
    MEGOLM,
}
