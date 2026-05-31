package com.novamesh.domain.model

import kotlinx.serialization.Serializable

/**
 * Represents a voice or video call (WebRTC-based).
 */
@Serializable
data class Call(
    val id: String,
    val type: CallType,
    val status: CallStatus,
    val initiatorId: String,
    val participants: List<CallParticipant>,
    val startTime: Long,
    val endTime: Long? = null,
    val durationMs: Long = 0L,
    val isGroupCall: Boolean = false,
    val maxVideoParticipants: Int = 8,
    val maxVoiceParticipants: Int = 32,
    val isNoiseCancellationEnabled: Boolean = true,
    val isBlurBackgroundEnabled: Boolean = false,
    val isRecording: Boolean = false,
    val recordingFilePath: String? = null,
)

@Serializable
enum class CallType {
    VOICE,
    VIDEO,
}

@Serializable
enum class CallStatus {
    RINGING,
    CONNECTING,
    CONNECTED,
    ON_HOLD,
    ENDED,
    MISSED,
    REJECTED,
}

@Serializable
data class CallParticipant(
    val userId: String,
    val userName: String,
    val avatarUri: String? = null,
    val isVideoEnabled: Boolean = false,
    val isAudioEnabled: Boolean = true,
    val isSpeakerEnabled: Boolean = false,
    val joinedAt: Long,
    val leftAt: Long? = null,
)

/**
 * Represents a "channel" (WhatsApp Channels-style broadcast).
 */
@Serializable
data class Channel(
    val id: String,
    val name: String,
    val description: String? = null,
    val avatarUri: String? = null,
    val ownerId: String,
    val subscriberCount: Int = 0,
    val isVerified: Boolean = false,
    val createdAt: Long,
    val lastPostAt: Long? = null,
)

/**
 * Represents a community with sub-channels.
 */
@Serializable
data class Community(
    val id: String,
    val name: String,
    val description: String? = null,
    val avatarUri: String? = null,
    val ownerId: String,
    val channels: List<Channel> = emptyList(),
    val memberCount: Int = 0,
    val createdAt: Long,
)
