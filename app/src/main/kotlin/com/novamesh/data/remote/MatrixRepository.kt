package com.novamesh.data.remote

import android.content.Context
import android.net.Uri
import com.novamesh.data.local.dao.ChatDao
import com.novamesh.data.local.dao.MessageDao
import com.novamesh.data.local.dao.UserDao
import com.novamesh.data.local.entity.ChatEntity
import com.novamesh.data.local.entity.MessageEntity
import com.novamesh.data.local.entity.UserEntity
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Matrix SDK integration for the NovaMesh Messenger.
 *
 * This repository wraps the matrix-android-sdk2 to:
 * - Connect to a Synapse (or other Matrix-compatible) homeserver
 * - Create and manage rooms (1:1 and groups)
 * - Send and receive messages with end-to-end encryption
 * - Handle sync and presence
 * - Upload and download media via the Matrix content repository
 *
 * ## Architecture
 *
 * The class maintains a [MatrixClient] instance that holds the session.
 * It exposes reactive [StateFlow] for connection state and [SharedFlow]
 * for incoming messages, allowing the UI layer to observe real-time events.
 *
 * ## Current Implementation
 *
 * This is a **scaffold/simulation** that demonstrates the API surface.
 * The actual Matrix SDK integration requires:
 *   - A running Synapse server (or compatible homeserver)
 *   - matrix-android-sdk2 dependency in build.gradle
 *   - Proper authentication flow (SSO or password login)
 *   - End-to-end encryption via Olm/Megolm
 *
 * Replace the simulated blocks with real Matrix SDK calls once the
 * server infrastructure is available.
 *
 * @param context Application context
 * @param messageDao Local DAO for caching synced messages
 * @param chatDao Local DAO for caching room metadata
 * @param userDao Local DAO for caching user profiles
 */
class MatrixRepository(
    private val context: Context,
    private val messageDao: MessageDao? = null,
    private val chatDao: ChatDao? = null,
    private val userDao: UserDao? = null,
) {

    // -------------------------------------------------------------------
    // Connection state
    // -------------------------------------------------------------------

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    // -------------------------------------------------------------------
    // Incoming messages (global stream from all rooms)
    // -------------------------------------------------------------------

    private val _incomingMessages = MutableSharedFlow<MatrixMessage>(extraBufferCapacity = 100)
    val incomingMessages: SharedFlow<MatrixMessage> = _incomingMessages.asSharedFlow()

    // -------------------------------------------------------------------
    // Internal state
    // -------------------------------------------------------------------

    /** Simulated Matrix client stub — replace with real matrix-android-sdk2 client */
    private var matrixClient: Any? = null

    /** Active session data */
    private var currentSession: MatrixSession? = null

    /** In-memory room cache (simulated; replaced by Matrix SDK's store) */
    private val rooms = ConcurrentHashMap<String, MatrixRoom>()

    /** In-memory user cache */
    private val knownUsers = ConcurrentHashMap<String, MatrixUser>()

    // -------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------

    /**
     * Connection states for the Matrix client.
     */
    enum class ConnectionState {
        /** Not connected to the homeserver */
        DISCONNECTED,
        /** Attempting to establish a connection */
        CONNECTING,
        /** Successfully connected and session is active */
        CONNECTED,
        /** Initial sync is in progress */
        SYNCING,
        /** Connection failed or an error occurred */
        ERROR,
    }

    /**
     * Simplified Matrix message model used by the repository layer.
     */
    data class MatrixMessage(
        val eventId: String,
        val roomId: String,
        val senderId: String,
        val body: String,
        val type: String, // "m.text", "m.image", etc.
        val timestamp: Long,
        val encrypted: Boolean = false,
    )

    /**
     * Simplified session data.
     */
    data class MatrixSession(
        val userId: String,
        val deviceId: String,
        val accessToken: String,
        val homeServerUrl: String,
    )

    /**
     * Simplified room data.
     */
    data class MatrixRoom(
        val roomId: String,
        val name: String = "",
        val topic: String = "",
        val isGroup: Boolean = false,
        val memberIds: List<String> = emptyList(),
    )

    /**
     * Simplified user profile from Matrix.
     */
    data class MatrixUser(
        val userId: String,
        val displayName: String = "",
        val avatarUrl: String? = null,
    )

    // -------------------------------------------------------------------
    // Connection management
    // -------------------------------------------------------------------

    /**
     * Connect to a Matrix homeserver using an access token.
     *
     * Real implementation:
     * ```
     * val auth = AuthenticationService.getInstance()
     * val session = auth.loginWithToken(homeserverUrl, accessToken)
     * matrixClient = MatrixClient(context, session)
     * matrixClient?.startSync()
     * ```
     *
     * @param homeserverUrl Base URL of the Synapse server (e.g. "https://matrix.example.com")
     * @param accessToken Valid Matrix access token
     */
    suspend fun connect(homeserverUrl: String, accessToken: String): Result<Unit> {
        return try {
            _connectionState.value = ConnectionState.CONNECTING

            // --- Simulated connection logic ---
            // Replace with real MatrixClient setup:
            // val authenticationService = AuthenticationService.getInstance()
            // val session = authenticationService.loginWithToken(homeserverUrl, accessToken)
            // matrixClient = MatrixClient(context, session)
            // matrixClient?.startSync { syncData -> handleSync(syncData) }

            currentSession = MatrixSession(
                userId = extractUserIdFromToken(accessToken),
                deviceId = "android-${UUID.randomUUID().toString().take(8)}",
                accessToken = accessToken,
                homeServerUrl = homeserverUrl,
            )

            // Simulate connection delay
            delay(500)
            _connectionState.value = ConnectionState.SYNCING

            // Simulate sync
            delay(300)
            _connectionState.value = ConnectionState.CONNECTED

            Result.success(Unit)
        } catch (e: Exception) {
            _connectionState.value = ConnectionState.ERROR
            Result.failure(e)
        }
    }

    /**
     * Disconnect from the homeserver and stop sync.
     */
    suspend fun disconnect() {
        try {
            // Real: matrixClient?.stopSync()
            matrixClient = null
            currentSession = null
            _connectionState.value = ConnectionState.DISCONNECTED
        } catch (_: Exception) {
            _connectionState.value = ConnectionState.DISCONNECTED
        }
    }

    // -------------------------------------------------------------------
    // Room operations
    // -------------------------------------------------------------------

    /**
     * Create a new room.
     *
     * Real implementation uses RoomService:
     * ```
     * val roomService = matrixClient?.roomService()
     * val createParams = CreateRoomParams().apply {
     *     name(name)
     *     isDirect = !isGroup
     *     invitedUserIds = userIds
     * }
     * val room = roomService?.createRoom(createParams)
     * return Result.success(room.roomId)
     * ```
     *
     * @param name Display name for the room
     * @param isGroup Whether this is a group room (vs 1:1 DM)
     * @param userIds List of user IDs to invite
     * @return Result containing the new room ID
     */
    suspend fun createRoom(
        name: String,
        isGroup: Boolean,
        userIds: List<String>,
    ): Result<String> {
        return try {
            val roomId = generateRoomId()
            val room = MatrixRoom(
                roomId = roomId,
                name = name,
                isGroup = isGroup,
                memberIds = listOfNotNull(currentSession?.userId) + userIds,
            )
            rooms[roomId] = room

            // Persist to local database
            chatDao?.insertChat(
                ChatEntity(
                    id = roomId,
                    name = name,
                    isGroup = isGroup,
                    createdAt = System.currentTimeMillis(),
                )
            )

            Result.success(roomId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Send a message to a room.
     *
     * Real implementation:
     * ```
     * val room = matrixClient?.roomService()?.getRoom(roomId)
     * room?.sendTextMessage(message.content.toJson())
     * ```
     */
    suspend fun sendMessage(roomId: String, message: Any): Result<Unit> {
        return try {
            // Simulated: emit as incoming for local echo
            val msg = MatrixMessage(
                eventId = UUID.randomUUID().toString(),
                roomId = roomId,
                senderId = currentSession?.userId ?: "unknown",
                body = message.toString().take(200),
                type = "m.text",
                timestamp = System.currentTimeMillis(),
            )
            _incomingMessages.emit(msg)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Send a read receipt for a message.
     */
    suspend fun sendReadReceipt(roomId: String, messageId: String): Result<Unit> {
        // Real: room?.sendReadReceipt(messageId)
        return try {
            // Simulated — no-op
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Send a message deletion/redaction.
     */
    suspend fun sendMessageDelete(roomId: String, messageId: String): Result<Unit> {
        // Real: room?.redactEvent(messageId)
        return try {
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Send a reaction to a message.
     */
    suspend fun sendReaction(roomId: String, messageId: String, emoji: String): Result<Unit> {
        // Real: room?.sendReaction(messageId, emoji)
        return try {
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Observe messages for a specific room.
     *
     * Real implementation uses the Matrix SDK's room timeline:
     * ```
     * val room = matrixClient?.roomService()?.getRoom(roomId)
     * room?.timeline()?.let { timeline ->
     *     timeline.all { events ->
     *         events.map { it.toMessage() }
     *     }
     * }
     * ```
     */
    fun getMessages(roomId: String): Flow<List<MatrixMessage>> {
        return flow {
            // In production, this would observe the Room timeline from the Matrix SDK.
            // For now, emit an empty list; messages arrive via incomingMessages.
            emit(emptyList())

            // Simulate a delayed sync
            delay(1000)
            val cachedMessages = messageDao?.getMessagesForChat(roomId)?.let { dao ->
                // We can't collect a Flow inside a Flow builder easily,
                // so we just emit once for simulation.
                emptyList<MatrixMessage>()
            } ?: emptyList()
            emit(cachedMessages)
        }
    }

    /**
     * Join a room by its ID or alias.
     */
    suspend fun joinRoom(roomIdOrAlias: String): Result<Unit> {
        return try {
            // Real: matrixClient?.roomService()?.joinRoom(roomIdOrAlias)
            rooms.getOrPut(roomIdOrAlias) {
                MatrixRoom(roomId = roomIdOrAlias, name = roomIdOrAlias)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Leave a room.
     */
    suspend fun leaveRoom(roomId: String): Result<Unit> {
        return try {
            // Real: matrixClient?.roomService()?.leaveRoom(roomId)
            rooms.remove(roomId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Invite a user to a room.
     */
    suspend fun inviteUser(roomId: String, userId: String): Result<Unit> {
        return try {
            // Real: matrixClient?.roomService()?.invite(roomId, userId)
            rooms[roomId]?.let { room ->
                rooms[roomId] = room.copy(memberIds = room.memberIds + userId)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Set the topic of a room.
     */
    suspend fun setRoomTopic(roomId: String, topic: String): Result<Unit> {
        return try {
            // Real: matrixClient?.roomService()?.setRoomTopic(roomId, topic)
            rooms[roomId]?.let { room ->
                rooms[roomId] = room.copy(topic = topic)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // -------------------------------------------------------------------
    // Media operations
    // -------------------------------------------------------------------

    /**
     * Upload media to the Matrix content repository.
     *
     * @param uri Local URI of the file to upload
     * @param mimeType MIME type (e.g. "image/jpeg", "video/mp4")
     * @return Result containing the MXC URI (e.g. "mxc://server/mediaId")
     */
    suspend fun uploadMedia(uri: Uri, mimeType: String): Result<String> {
        return try {
            // Real:
            // val mediaService = matrixClient?.mediaService()
            // val mxcUri = mediaService?.upload(uri, mimeType)
            // return Result.success(mxcUri.toString())

            val mxcUri = "mxc://${currentSession?.homeServerUrl ?: "localhost"}/${UUID.randomUUID()}"
            Result.success(mxcUri)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get a downloadable HTTP URL for a Matrix content URI.
     *
     * @param mxcUri The MXC URI (e.g. "mxc://server/mediaId")
     * @return Full HTTP URL to download the content
     */
    fun getMediaUrl(mxcUri: String): String {
        // Real: matrixClient?.mediaService()?.downloadUrl(mxcUri)
        return mxcUri.replace("mxc://", "https://")
    }

    // -------------------------------------------------------------------
    // User profile operations
    // -------------------------------------------------------------------

    /**
     * Get a user's profile from the Matrix server.
     */
    suspend fun getUserProfile(userId: String): Result<MatrixUser> {
        return try {
            knownUsers.getOrPut(userId) {
                // Real: matrixClient?.userService()?.getProfile(userId)
                MatrixUser(userId = userId, displayName = userId)
            }.let { Result.success(it) }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Set the current user's presence.
     */
    suspend fun setPresence(presence: String): Result<Unit> {
        return try {
            // Real: matrixClient?.userService()?.setPresence(presence)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // -------------------------------------------------------------------
    // Sync handler (simulated)
    // -------------------------------------------------------------------

    /**
     * Handle incoming sync data from the Matrix SDK.
     *
     * In production, this is called by the MatrixClient sync callback.
     * It processes room events, decrypts where needed, and persists
     * to the local database.
     */
    fun handleSync(syncData: Any) {
        // Real implementation iterates over rooms -> timeline events,
        // decrypts using the crypto service, and writes to Room.
        // For the scaffold, this is a no-op that would be connected
        // to the Matrix SDK's sync listener.
    }

    // -------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------

    private fun generateRoomId(): String {
        return "!${UUID.randomUUID().toString().replace("-", "")}:${currentSession?.homeServerUrl ?: "localhost"}"
    }

    /**
     * Extract a user ID from the access token (simulated).
     * In production, the login response provides the user ID directly.
     */
    private fun extractUserIdFromToken(token: String): String {
        return "@user_${token.take(8)}:${currentSession?.homeServerUrl ?: "localhost"}"
    }

    /**
     * Map a Matrix event type to the app's MessageType name.
     */
    fun mapMatrixEventType(matrixType: String): String {
        return when (matrixType) {
            "m.text" -> "TEXT"
            "m.image" -> "IMAGE"
            "m.video" -> "VIDEO"
            "m.audio" -> "AUDIO"
            "m.file" -> "FILE"
            "m.location" -> "LOCATION"
            "m.sticker" -> "STICKER"
            "m.emote" -> "TEXT"
            "m.notice" -> "SYSTEM"
            else -> "TEXT"
        }
    }
}

/**
 * Extension to create a [MatrixRepository.MatrixMessage] from a generic event.
 * In production this would parse Matrix event JSON.
 */
fun Map<String, Any?>.toMatrixMessage(roomId: String): MatrixRepository.MatrixMessage? {
    return try {
        MatrixRepository.MatrixMessage(
            eventId = this["event_id"] as? String ?: return null,
            roomId = roomId,
            senderId = this["sender"] as? String ?: "",
            body = (this["content"] as? Map<*, *>)?.get("body") as? String ?: "",
            type = (this["content"] as? Map<*, *>)?.get("msgtype") as? String ?: "m.text",
            timestamp = (this["origin_server_ts"] as? Long) ?: System.currentTimeMillis(),
        )
    } catch (_: Exception) {
        null
    }
}
