package com.novamesh.security

import android.content.Context
import android.util.Base64
import android.util.Log
import com.novamesh.domain.model.EncryptionType
import com.novamesh.domain.model.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.security.Signature
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Signal Protocol integration for end-to-end encryption.
 *
 * Manages:
 * - Identity key pairs (Ed25519)
 * - Pre-keys (signed + one-time)
 * - Session negotiation
 * - Message encryption/decryption (Double Ratchet algorithm)
 * - Key rotation for forward secrecy
 *
 * NOTE: This is a scaffold implementation. Full Signal Protocol
 * requires libsignal-client native library. The structure below
 * mirrors the Signal Protocol's architecture for production use.
 */
class SignalProtocolManager(private val context: Context) {

    companion object {
        private const val TAG = "SignalProtocol"
        private const val PRE_KEY_COUNT = 100
        private const val SIGNED_PRE_KEY_ROTATION_DAYS = 7
        private const val MAX_DEVICES = 4
        private const val KEY_SIZE = 256
        private const val GCM_TAG_LENGTH = 128 // bits
        private const val GCM_IV_LENGTH = 12 // bytes
        private const val SIGNATURE_ALGORITHM = "SHA256withECDSA"
        private const val KEY_AGREEMENT_ALGORITHM = "ECDH"
        private const val KEY_PAIR_ALGORITHM = "EC"
        private const val CIPHER_TRANSFORMATION = "AES/GCM/NoPadding"
    }

    // Identity key pair for this device
    private var identityKeyPair: KeyPair? = null

    // Signed pre-key (rotated periodically)
    private var signedPreKeyPair: KeyPair? = null
    private var signedPreKeyId: Int = 0
    private var signedPreKeyTimestamp: Long = 0L

    // One-time pre-keys
    private val preKeys: MutableMap<String, PreKeyPair> = ConcurrentHashMap()

    // Active sessions keyed by userId
    private val sessions: MutableMap<String, SignalSession> = ConcurrentHashMap()

    // Device-specific identifier
    private val deviceId: Int = generateDeviceId()

    // Initialization state
    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    // Secure random for cryptographic operations
    private val secureRandom = SecureRandom()

    // =========================================================================
    // Data Classes
    // =========================================================================

    /**
     * Represents a pre-key pair used in the X3DH key agreement protocol.
     *
     * @property keyId Unique identifier for this pre-key.
     * @property publicKey The public component (raw bytes).
     * @property privateKey The private component (raw bytes).
     */
    data class PreKeyPair(
        val keyId: Int,
        val publicKey: ByteArray,
        val privateKey: ByteArray
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is PreKeyPair) return false
            return keyId == other.keyId &&
                publicKey.contentEquals(other.publicKey) &&
                privateKey.contentEquals(other.privateKey)
        }

        override fun hashCode(): Int {
            var result = keyId
            result = 31 * result + publicKey.contentHashCode()
            result = 31 * result + privateKey.contentHashCode()
            return result
        }
    }

    /**
     * Represents an established Signal Protocol session with a remote user.
     *
     * @property remoteUserId The identifier of the remote user.
     * @property sessionId Unique session identifier.
     * @property createdAt Timestamp (epoch millis) when the session was created.
     * @property lastUsedAt Timestamp (epoch millis) when the session was last used.
     * @property isActive Whether the session is still valid for encryption.
     */
    data class SignalSession(
        val remoteUserId: String,
        val sessionId: String,
        val createdAt: Long,
        val lastUsedAt: Long,
        val isActive: Boolean
    )

    /**
     * Wrapper for an encrypted message payload produced by the Double Ratchet.
     *
     * @property ciphertext The AES-GCM encrypted message bytes.
     * @property ephemeralKey The ephemeral public key used for key agreement.
     * @property preKeyId The ID of the one-time pre-key used (null if none).
     * @property salt The salt / IV used for encryption.
     */
    data class EncryptedPayload(
        val ciphertext: ByteArray,
        val ephemeralKey: ByteArray,
        val preKeyId: Int?,
        val salt: ByteArray
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is EncryptedPayload) return false
            return preKeyId == other.preKeyId &&
                ciphertext.contentEquals(other.ciphertext) &&
                ephemeralKey.contentEquals(other.ephemeralKey) &&
                salt.contentEquals(other.salt)
        }

        override fun hashCode(): Int {
            var result = ciphertext.contentHashCode()
            result = 31 * result + ephemeralKey.contentHashCode()
            result = 31 * result + (preKeyId ?: 0)
            result = 31 * result + salt.contentHashCode()
            return result
        }
    }

    /**
     * Bundle of pre-key data shared with a remote user to establish a session.
     *
     * @property identityKey The public identity key of the sender.
     * @property signedPreKey The signed pre-key public key.
     * @property signedPreKeySignature Signature of the signed pre-key by the identity key.
     * @property signedPreKeyId Identifier for the signed pre-key.
     * @property oneTimePreKey An optional one-time pre-key public key.
     * @property oneTimePreKeyId Identifier for the one-time pre-key.
     * @property deviceId The sender's device ID.
     */
    data class PreKeyBundle(
        val identityKey: ByteArray,
        val signedPreKey: ByteArray,
        val signedPreKeySignature: ByteArray,
        val signedPreKeyId: Int,
        val oneTimePreKey: ByteArray?,
        val oneTimePreKeyId: Int?,
        val deviceId: Int
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is PreKeyBundle) return false
            return signedPreKeyId == other.signedPreKeyId &&
                deviceId == other.deviceId &&
                identityKey.contentEquals(other.identityKey) &&
                signedPreKey.contentEquals(other.signedPreKey) &&
                signedPreKeySignature.contentEquals(other.signedPreKeySignature) &&
                (oneTimePreKey == null && other.oneTimePreKey == null ||
                    oneTimePreKey != null && other.oneTimePreKey != null &&
                    oneTimePreKey.contentEquals(other.oneTimePreKey)) &&
                oneTimePreKeyId == other.oneTimePreKeyId
        }

        override fun hashCode(): Int {
            var result = identityKey.contentHashCode()
            result = 31 * result + signedPreKey.contentHashCode()
            result = 31 * result + signedPreKeySignature.contentHashCode()
            result = 31 * result + signedPreKeyId
            result = 31 * result + (oneTimePreKey?.contentHashCode() ?: 0)
            result = 31 * result + (oneTimePreKeyId ?: 0)
            result = 31 * result + deviceId
            return result
        }
    }

    // =========================================================================
    // Errors
    // =========================================================================

    /**
     * Sealed hierarchy of errors that can occur during Signal Protocol operations.
     *
     * @property message Human-readable description of the error.
     */
    sealed class SignalError(message: String) : Exception(message) {
        /** The manager has not been initialized. Call [initialize] first. */
        data object NotInitialized : SignalError("SignalProtocolManager is not initialized. Call initialize() first.")

        /** No session exists for the requested remote user. */
        data class SessionNotFound(val userId: String) :
            SignalError("No active session found for user: $userId")

        /** Decryption of the payload failed (e.g. bad key, tampered data). */
        data object DecryptionFailed : SignalError("Failed to decrypt the provided payload.")

        /** The X3DH key exchange with the remote user failed. */
        data object KeyExchangeFailed : SignalError("Key exchange with remote user failed.")

        /** A cryptographic signature could not be verified. */
        data object InvalidSignature : SignalError("The cryptographic signature is invalid.")
    }

    // =========================================================================
    // Initialization
    // =========================================================================

    /**
     * Initializes the Signal Protocol manager.
     *
     * Generates the long-term identity key pair, a signed pre-key,
     * and a batch of one-time pre-keys. Must be called before any
     * encryption / decryption operations.
     *
     * @return `Result.Success(Unit)` on success, or `Result.failure(SignalError)`.
     */
    suspend fun initialize(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Initializing Signal Protocol manager...")

            // 1. Generate the long-term identity key pair (Ed25519 via EC)
            val identityGenerator = KeyPairGenerator.getInstance(KEY_PAIR_ALGORITHM)
            identityGenerator.initialize(KEY_SIZE, secureRandom)
            identityKeyPair = identityGenerator.generateKeyPair()

            // 2. Generate the signed pre-key and sign it with the identity key
            val signedPreKeyGenerator = KeyPairGenerator.getInstance(KEY_PAIR_ALGORITHM)
            signedPreKeyGenerator.initialize(KEY_SIZE, secureRandom)
            signedPreKeyPair = signedPreKeyGenerator.generateKeyPair()
            signedPreKeyId = secureRandom.nextInt(Int.MAX_VALUE)
            signedPreKeyTimestamp = System.currentTimeMillis()

            // 3. Generate a batch of one-time pre-keys
            generatePreKeyBatch()

            _isInitialized.value = true
            Log.d(TAG, "Signal Protocol manager initialized successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Initialization failed", e)
            _isInitialized.value = false
            Result.failure(SignalError.KeyExchangeFailed)
        }
    }

    /**
     * Generates a fresh batch of one-time pre-keys.
     */
    private fun generatePreKeyBatch() {
        val generator = KeyPairGenerator.getInstance(KEY_PAIR_ALGORITHM)
        generator.initialize(KEY_SIZE, secureRandom)

        for (i in 0 until PRE_KEY_COUNT) {
            val kp = generator.generateKeyPair()
            val keyId = secureRandom.nextInt(Int.MAX_VALUE)
            val pair = PreKeyPair(
                keyId = keyId,
                publicKey = kp.public.encoded,
                privateKey = kp.private.encoded
            )
            preKeys["$keyId"] = pair
        }

        Log.d(TAG, "Generated $PRE_KEY_COUNT one-time pre-keys")
    }

    // =========================================================================
    // Session Management
    // =========================================================================

    /**
     * Establishes an end-to-end encrypted session with a remote user using the
     * X3DH (Extended Triple Diffie-Hellman) key agreement protocol.
     *
     * Steps:
     * 1. Validates the remote user's [PreKeyBundle].
     * 2. Performs the DH key agreement using identity keys, signed pre-key,
     *    and the optional one-time pre-key.
     * 3. Derives a shared secret and creates a [SignalSession].
     *
     * @param remoteUserId The identifier of the remote user.
     * @param preKeyBundle The pre-key bundle published by the remote user.
     * @return `Result.success(SignalSession)` on success, or `Result.failure(SignalError)`.
     */
    suspend fun establishSession(
        remoteUserId: String,
        preKeyBundle: PreKeyBundle
    ): Result<SignalSession> = withContext(Dispatchers.IO) {
        if (!_isInitialized.value) {
            return@withContext Result.failure(SignalError.NotInitialized)
        }

        try {
            Log.d(TAG, "Establishing session with user: $remoteUserId")

            // Verify the signed pre-key signature
            val identityKey = identityKeyPair?.public ?: return@withContext Result.failure(
                SignalError.KeyExchangeFailed
            )

            val signature = Signature.getInstance(SIGNATURE_ALGORITHM)
            signature.initVerify(
                KeyPairGenerator.getInstance(KEY_PAIR_ALGORITHM)
                    .generateKeyPair().public // placeholder — use actual key factory
            )

            // Perform X3DH key agreement
            val sharedSecret = performKeyAgreement(preKeyBundle)

            // Create a unique session ID
            val sessionId = generateSessionId(remoteUserId, preKeyBundle.deviceId)

            val session = SignalSession(
                remoteUserId = remoteUserId,
                sessionId = sessionId,
                createdAt = System.currentTimeMillis(),
                lastUsedAt = System.currentTimeMillis(),
                isActive = true
            )

            sessions[remoteUserId] = session
            Log.d(TAG, "Session established: $sessionId")
            Result.success(session)
        } catch (e: SignalError) {
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Session establishment failed", e)
            Result.failure(SignalError.KeyExchangeFailed)
        }
    }

    /**
     * Performs the X3DH multi-way Diffie-Hellman key agreement.
     *
     * Combines:
     * - DH1 = DH(IKA, SPKB)
     * - DH2 = DH(EKA, IKB)
     * - DH3 = DH(EKA, SPKB)
     * - DH4 = DH(EKA, OPKB)  (if one-time pre-key is present)
     *
     * @param preKeyBundle The remote party's pre-key bundle.
     * @return A shared secret bytes.
     */
    private fun performKeyAgreement(preKeyBundle: PreKeyBundle): ByteArray {
        // In production this would perform actual EC point multiplication.
        // For the scaffold we derive a deterministic shared secret using HKDF.
        val identityKey = identityKeyPair?.public?.encoded ?: ByteArray(0)
        val ephemeralKey = ByteArray(32).also { secureRandom.nextBytes(it) }

        val dhMaterial = ByteArray(identityKey.size + ephemeralKey.size + 32)
        System.arraycopy(identityKey, 0, dhMaterial, 0, identityKey.size)
        System.arraycopy(ephemeralKey, 0, dhMaterial, identityKey.size, ephemeralKey.size)
        // Append a random salt to simulate DH output mixing
        val salt = ByteArray(32).also { secureRandom.nextBytes(it) }
        System.arraycopy(salt, 0, dhMaterial, identityKey.size + ephemeralKey.size, 32)

        // Use SHA-256 as a simple KDF to produce the shared secret
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(dhMaterial)
    }

    /**
     * Generates a unique session identifier.
     */
    private fun generateSessionId(userId: String, remoteDeviceId: Int): String {
        val raw = "$userId:$remoteDeviceId:${System.currentTimeMillis()}:${secureRandom.nextLong()}"
        val digest = MessageDigest.getInstance("SHA-256")
        return Base64.encodeToString(digest.digest(raw.toByteArray()), Base64.NO_WRAP)
    }

    // =========================================================================
    // Encryption / Decryption
    // =========================================================================

    /**
     * Encrypts a [Message] using the established [session].
     *
     * The encryption uses the Double Ratchet algorithm (AES-256-GCM)
     * with an ephemeral key exchange for forward secrecy.
     *
     * @param message The plaintext message to encrypt.
     * @param session The active session with the recipient.
     * @return `Result.success(EncryptedPayload)` or `Result.failure(SignalError)`.
     */
    suspend fun encryptMessage(
        message: Message,
        session: SignalSession
    ): Result<EncryptedPayload> = withContext(Dispatchers.IO) {
        if (!_isInitialized.value) {
            return@withContext Result.failure(SignalError.NotInitialized)
        }
        if (!session.isActive) {
            return@withContext Result.failure(
                SignalError.SessionNotFound(session.remoteUserId)
            )
        }

        try {
            Log.d(TAG, "Encrypting message for session: ${session.sessionId}")

            // Derive the message key using the Double Ratchet step.
            val messageKey = deriveMessageKey(session)

            // Generate a random IV (salt)
            val iv = ByteArray(GCM_IV_LENGTH).also { secureRandom.nextBytes(it) }

            // Encrypt the message content (serialize fields as UTF-8)
            val plaintext = message.content.toByteArray(Charsets.UTF_8)
            val ciphertext = aesGcmEncrypt(plaintext, messageKey, iv)

            // Generate a fresh ephemeral key for forward secrecy
            val ephemeralGenerator = KeyPairGenerator.getInstance(KEY_PAIR_ALGORITHM)
            ephemeralGenerator.initialize(KEY_SIZE, secureRandom)
            val ephemeralPair = ephemeralGenerator.generateKeyPair()

            // Optionally consume a one-time pre-key
            val consumedPreKey = preKeys.entries.firstOrNull()
            consumedPreKey?.let { preKeys.remove(it.key) }

            val payload = EncryptedPayload(
                ciphertext = ciphertext,
                ephemeralKey = ephemeralPair.public.encoded,
                preKeyId = consumedPreKey?.value?.keyId,
                salt = iv
            )

            // Update session's last-used timestamp
            sessions[session.remoteUserId] = session.copy(
                lastUsedAt = System.currentTimeMillis()
            )

            Log.d(TAG, "Message encrypted successfully")
            Result.success(payload)
        } catch (e: Exception) {
            Log.e(TAG, "Encryption failed", e)
            Result.failure(SignalError.KeyExchangeFailed)
        }
    }

    /**
     * Decrypts an [EncryptedPayload] sent by [senderId].
     *
     * Reverses the Double Ratchet step to derive the same message key,
     * then verifies the authentication tag and decrypts.
     *
     * @param payload The encrypted payload received.
     * @param senderId The identifier of the sending user.
     * @return `Result.success(plaintextBytes)` or `Result.failure(SignalError)`.
     */
    suspend fun decryptMessage(
        payload: EncryptedPayload,
        senderId: String
    ): Result<ByteArray> = withContext(Dispatchers.IO) {
        if (!_isInitialized.value) {
            return@withContext Result.failure(SignalError.NotInitialized)
        }

        val session = sessions[senderId]
        if (session == null || !session.isActive) {
            return@withContext Result.failure(SignalError.SessionNotFound(senderId))
        }

        try {
            Log.d(TAG, "Decrypting message from user: $senderId")

            // Derive the same message key the sender used
            val messageKey = deriveMessageKey(session)

            // Decrypt using AES-256-GCM
            val plaintext = aesGcmDecrypt(payload.ciphertext, messageKey, payload.salt)

            // If a one-time pre-key was used, remove it from our store
            payload.preKeyId?.let { id ->
                preKeys.remove("$id")
            }

            // Update session's last-used timestamp
            sessions[senderId] = session.copy(lastUsedAt = System.currentTimeMillis())

            Log.d(TAG, "Message decrypted successfully")
            Result.success(plaintext)
        } catch (e: Exception) {
            Log.e(TAG, "Decryption failed", e)
            Result.failure(SignalError.DecryptionFailed)
        }
    }

    /**
     * Derives a message key for a given session using a ratchet step.
     *
     * In a full implementation this would perform a Diffie-Hellman ratchet
     * and a symmetric (chain) ratchet to produce the message key.
     * Here we use HKDF-expand as a placeholder.
     */
    private fun deriveMessageKey(session: SignalSession): ByteArray {
        val hkdfInput = "${session.sessionId}:${session.lastUsedAt}:${secureRandom.nextLong()}"
            .toByteArray(Charsets.UTF_8)
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(hkdfInput)
    }

    /**
     * Encrypts plaintext using AES-256-GCM.
     */
    private fun aesGcmEncrypt(plaintext: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        val secretKey = SecretKeySpec(key, "AES")
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)
        return cipher.doFinal(plaintext)
    }

    /**
     * Decrypts ciphertext using AES-256-GCM.
     */
    private fun aesGcmDecrypt(ciphertext: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        val secretKey = SecretKeySpec(key, "AES")
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)
        return cipher.doFinal(ciphertext)
    }

    // =========================================================================
    // Key Management
    // =========================================================================

    /**
     * Rotates the signed pre-key.
     *
     * Should be called periodically (every [SIGNED_PRE_KEY_ROTATION_DAYS] days)
     * to maintain forward secrecy. The new pre-key is signed with the
     * identity key and the old one is discarded.
     *
     * @return `Result.success(Unit)` on success, or `Result.failure(SignalError)`.
     */
    fun rotateKeys(): Result<Unit> {
        if (!_isInitialized.value) {
            return Result.failure(SignalError.NotInitialized)
        }

        return try {
            Log.d(TAG, "Rotating signed pre-key...")

            val generator = KeyPairGenerator.getInstance(KEY_PAIR_ALGORITHM)
            generator.initialize(KEY_SIZE, secureRandom)
            val newKeyPair = generator.generateKeyPair()

            signedPreKeyPair = newKeyPair
            signedPreKeyId = secureRandom.nextInt(Int.MAX_VALUE)
            signedPreKeyTimestamp = System.currentTimeMillis()

            // Generate fresh one-time pre-keys
            preKeys.clear()
            generatePreKeyBatch()

            Log.d(TAG, "Keys rotated successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Key rotation failed", e)
            Result.failure(SignalError.KeyExchangeFailed)
        }
    }

    /**
     * Returns a [PreKeyBundle] containing this device's public identity key,
     * signed pre-key, current signature, and an available one-time pre-key.
     *
     * This bundle is shared with remote users so they can establish a session
     * with this device.
     */
    fun getPreKeyBundle(): PreKeyBundle {
        val identityPub = identityKeyPair?.public?.encoded ?: ByteArray(0)
        val signedPreKeyPub = signedPreKeyPair?.public?.encoded ?: ByteArray(0)

        // Create a dummy signature (in production, sign with the identity private key)
        val signatureBytes = ByteArray(64).also { secureRandom.nextBytes(it) }

        // Take one available pre-key (or null)
        val preKeyEntry = preKeys.entries.firstOrNull()

        return PreKeyBundle(
            identityKey = identityPub,
            signedPreKey = signedPreKeyPub,
            signedPreKeySignature = signatureBytes,
            signedPreKeyId = signedPreKeyId,
            oneTimePreKey = preKeyEntry?.value?.publicKey,
            oneTimePreKeyId = preKeyEntry?.value?.keyId,
            deviceId = deviceId
        )
    }

    /**
     * Returns the unique device identifier for this installation.
     */
    fun getDeviceId(): Int = deviceId

    /**
     * Performs a full reset of all cryptographic material.
     *
     * Destroys identity keys, pre-keys, and sessions. After this call
     * [initialize] must be invoked before any further operations.
     *
     * @return `Result.success(Unit)` on success, or `Result.failure(SignalError)`.
     */
    fun resetAllKeys(): Result<Unit> {
        return try {
            Log.w(TAG, "Performing factory reset of all keys...")

            identityKeyPair = null
            signedPreKeyPair = null
            signedPreKeyId = 0
            signedPreKeyTimestamp = 0L
            preKeys.clear()
            sessions.clear()
            _isInitialized.value = false

            Log.d(TAG, "All keys have been reset")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Key reset failed", e)
            Result.failure(SignalError.KeyExchangeFailed)
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /**
     * Generates a deterministic device ID from the Android context.
     */
    private fun generateDeviceId(): Int {
        val raw = "${context.packageName}:${context.packageManager?.let {
            try {
                it.getPackageInfo(context.packageName, 0)
            } catch (_: Exception) {
                null
            }
        }}"
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(raw.toByteArray(Charsets.UTF_8))
        // Take the first 4 bytes as an integer
        return ((hash[0].toInt() and 0xFF) shl 24) or
            ((hash[1].toInt() and 0xFF) shl 16) or
            ((hash[2].toInt() and 0xFF) shl 8) or
            (hash[3].toInt() and 0xFF)
    }
}
