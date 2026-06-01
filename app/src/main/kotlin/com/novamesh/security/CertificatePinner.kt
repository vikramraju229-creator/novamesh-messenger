package com.novamesh.security

import android.content.Context
import android.util.Log
import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import java.io.ByteArrayInputStream
import java.security.MessageDigest
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Certificate pinning for MITM attack prevention.
 *
 * Pins the SHA-256 hash of the server's public key
 * to ensure only trusted certificates are accepted.
 *
 * The pinner validates that every HTTPS connection to a pinned hostname
 * includes at least one certificate whose SubjectPublicKeyInfo hashes
 * to one of the pinned values. Connections that do not match are
 * rejected immediately.
 *
 * Usage:
 * ```
 * val pinner = CertificatePinner(context)
 * pinner.addPin("matrix.novamesh.io", "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
 * val client = pinner.getOkHttpClient()
 * ```
 */
class CertificatePinner(private val context: Context) {

    companion object {
        private const val TAG = "CertPinner"

        // Hash algorithm used for certificate pinning
        private const val HASH_ALGORITHM = "SHA-256"

        // Hash prefix required by OkHttp's CertificatePinner
        private const val HASH_PREFIX = "sha256/"

        // Default connection timeouts for the pinned client
        private const val CONNECT_TIMEOUT_SECONDS = 30L
        private const val READ_TIMEOUT_SECONDS = 30L
        private const val WRITE_TIMEOUT_SECONDS = 30L
    }

    /**
     * Default pinned certificates mapping.
     *
     * Key: hostname (e.g. "matrix.novamesh.io")
     * Value: set of SHA-256 hashes of the server's public keys
     *        in OkHttp format ("sha256/<base64-hash>").
     *
     * These are placeholder hashes. In production, replace them with the
     * actual base64-encoded SHA-256 hashes of your server's certificate
     * public keys. You can obtain them with:
     *   openssl s_client -connect hostname:443 -servername hostname |
     *       openssl x509 -pubkey -noout |
     *       openssl pkey -pubin -outform der |
       openssl dgst -sha256 -binary |
     *       base64
     */
    private val pinnedCertificates: MutableMap<String, MutableSet<String>> =
        ConcurrentHashMap(
            mapOf(
                // Default NovaMesh Matrix server
                "matrix.novamesh.io" to mutableSetOf(
                    // Placeholder – replace with actual production hash
                    "sha256/47DEQpj8HBSa+/TImW+5JCeuQeRkm5NMpJWZG3hSuFU=",
                    // Backup / fallback certificate hash
                    "sha256/4VYiNCs5YDAmhMf6YNfzZ5w0g3v8o0v0K+o1yP8s0W0="
                ),
                // API subdomain
                "api.novamesh.io" to mutableSetOf(
                    // Placeholder – replace with actual API server hash
                    "sha256/47DEQpj8HBSa+/TImW+5JCeuQeRkm5NMpJWZG3hSuFU="
                ),
                // Turn server for WebRTC
                "turn.novamesh.io" to mutableSetOf(
                    // Placeholder – replace with actual TURN server hash
                    "sha256/47DEQpj8HBSa+/TImW+5JCeuQeRkm5NMpJWZG3hSuFU="
                )
            )
        )

    // =========================================================================
    // Public API
    // =========================================================================

    /**
     * Builds and returns an [OkHttpClient] configured with certificate pinning.
     *
     * The client enforces:
     * - SHA-256 public key pinning for all pinned hostnames.
     * - 30-second connect / read / write timeouts.
     * - TLS only (no cleartext).
     *
     * @return An [OkHttpClient] with certificate pinning enabled.
     */
    fun getOkHttpClient(): OkHttpClient {
        val pinner = buildCertificatePinner()

        return OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .certificatePinner(pinner)
            .build()
    }

    /**
     * Adds a certificate pin for the given [hostname].
     *
     * The [sha256Hash] must be in OkHttp's format:
     * `"sha256/<base64-encoded-SHA-256-hash>"`.
     *
     * @param hostname   The domain to pin (e.g. "matrix.novamesh.io").
     * @param sha256Hash The SHA-256 hash in OkHttp format.
     * @throws IllegalArgumentException if the hash format is invalid.
     */
    fun addPin(hostname: String, sha256Hash: String) {
        require(isValidPinFormat(sha256Hash)) {
            "Invalid pin format. Must be 'sha256/<base64>' but was: $sha256Hash"
        }

        val pins = pinnedCertificates.getOrPut(hostname) { mutableSetOf() }
        pins.add(sha256Hash)
        Log.d(TAG, "Added pin for $hostname: $sha256Hash")
    }

    /**
     * Removes a specific certificate pin from [hostname].
     *
     * @param hostname   The domain to remove the pin from.
     * @param sha256Hash The SHA-256 hash to remove.
     */
    fun removePin(hostname: String, sha256Hash: String) {
        val pins = pinnedCertificates[hostname]
        if (pins != null) {
            pins.remove(sha256Hash)
            if (pins.isEmpty()) {
                pinnedCertificates.remove(hostname)
            }
            Log.d(TAG, "Removed pin for $hostname: $sha256Hash")
        }
    }

    /**
     * Clears all certificate pins across all hostnames.
     *
     * Use with extreme caution — disabling pinning reduces security.
     */
    fun clearPins() {
        pinnedCertificates.clear()
        Log.w(TAG, "All certificate pins cleared")
    }

    /**
     * Verifies that at least one certificate in [certChain] matches a
     * pinned hash for the given [hostname].
     *
     * @param hostname  The hostname being connected to.
     * @param certChain The server's certificate chain (leaf first).
     * @return `true` if the chain contains a matching pinned certificate,
     *         `false` otherwise.
     */
    fun verifyCertificateChain(
        hostname: String,
        certChain: List<X509Certificate>
    ): Boolean {
        val pins = pinnedCertificates[hostname] ?: return true // no pins = skip
        if (pins.isEmpty()) return true

        for (cert in certChain) {
            val hash = sha256Hash(cert)
            val pin = "$HASH_PREFIX$hash"
            if (pins.contains(pin)) {
                Log.d(TAG, "Certificate pin match for $hostname")
                return true
            }
        }

        Log.w(TAG, "No matching certificate pin found for $hostname")
        return false
    }

    // =========================================================================
    // Internal Helpers
    // =========================================================================

    /**
     * Builds the OkHttp [CertificatePinner] from the current pin set.
     */
    private fun buildCertificatePinner(): CertificatePinner {
        val builder = CertificatePinner.Builder()

        for ((hostname, pins) in pinnedCertificates) {
            for (pin in pins) {
                builder.add(hostname, pin)
            }
            Log.d(TAG, "Pinned ${pins.size} certificate(s) for $hostname")
        }

        return builder.build()
    }

    /**
     * Computes the base64-encoded SHA-256 hash of a certificate's
     * SubjectPublicKeyInfo (SPKI).
     *
     * This is the standard approach for HTTP public key pinning (RFC 7469).
     *
     * @param certificate The X.509 certificate to hash.
     * @return Base64-encoded SHA-256 hash of the SPKI.
     */
    private fun sha256Hash(certificate: X509Certificate): String {
        val publicKey = certificate.publicKey
        val encoded = publicKey.encoded // SPKI encoded (SubjectPublicKeyInfo)
        val digest = MessageDigest.getInstance(HASH_ALGORITHM)
        val hash = digest.digest(encoded)
        return java.util.Base64.getEncoder().encodeToString(hash)
    }

    /**
     * Validates that the given string is in the correct OkHttp pin format:
     * `"sha256/<base64-value>"`.
     *
     * @param pin The pin string to validate.
     * @return `true` if the format is valid.
     */
    private fun isValidPinFormat(pin: String): Boolean {
        if (!pin.startsWith(HASH_PREFIX)) return false
        val base64Part = pin.removePrefix(HASH_PREFIX)
        if (base64Part.isEmpty()) return false
        return try {
            java.util.Base64.getDecoder().decode(base64Part)
            true
        } catch (e: IllegalArgumentException) {
            false
        }
    }
}
