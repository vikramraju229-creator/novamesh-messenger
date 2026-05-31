package com.novamesh.data.remote

import com.novamesh.BuildConfig
import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.logging.HttpLoggingInterceptor.Level.BODY
import okhttp3.logging.HttpLoggingInterceptor.Level.NONE
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Singleton Retrofit/OkHttp client for the NovaMesh Messenger.
 *
 * Provides a pre-configured [OkHttpClient] with:
 *   - 30-second connect/read/write timeouts
 *   - HTTP request/response logging (body-level in debug builds only)
 *   - Optional certificate pinning for security
 *   - Gson serialization via [GsonConverterFactory]
 *
 * Usage:
 * ```
 * val api = RetrofitClient.create("https://api.example.com/", MyApiService::class.java)
 * ```
 *
 * For Matrix CS API calls, use [MatrixRepository] instead, which wraps
 * the matrix-android-sdk2. This client is intended for auxiliary services
 * such as the app's own backend API or media proxy.
 */
object RetrofitClient {

    /** Timeout for all OkHttp operations (connect, read, write). */
    private const val TIMEOUT_SECONDS = 30L

    /** Optional certificate pinner for TLS pinning. */
    private var certificatePinner: CertificatePinner? = null

    /**
     * Lazy-initialized [OkHttpClient] singleton.
     *
     * Configuration:
     * - Timeouts set to [TIMEOUT_SECONDS]
     * - [HttpLoggingInterceptor] at BODY level for debug builds, NONE for release
     * - Optional [CertificatePinner] for public key pinning
     */
    private val okHttpClient: OkHttpClient by lazy {
        val builder = OkHttpClient.Builder()
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = if (BuildConfig.DEBUG) BODY else NONE
                }
            )

        // Attach certificate pinner if configured (e.g., via setCertificatePinner)
        certificatePinner?.let { builder.certificatePinner(it) }

        builder.build()
    }

    /**
     * Create a Retrofit service interface for the given [baseUrl].
     *
     * @param baseUrl Base URL (must end with '/')
     * @param service The Retrofit service interface class
     * @return An implementation of [service] that makes HTTP calls
     */
    fun <T> create(baseUrl: String, service: Class<T>): T {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(service)
    }

    /**
     * Configure certificate pinning for the OkHttp client.
     *
     * Must be called **before** the first use of [create], because the
     * underlying [OkHttpClient] is lazily initialized. Subsequent calls
     * after the client has been built will have no effect until the
     * process restarts.
     *
     * Example:
     * ```
     * RetrofitClient.setCertificatePinner(
     *     CertificatePinner.Builder()
     *         .add("api.example.com", "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
     *         .build()
     * )
     * ```
     *
     * @param pinner The [CertificatePinner] with SHA-256 hashes of trusted
     *               public keys
     */
    fun setCertificatePinner(pinner: CertificatePinner) {
        certificatePinner = pinner
    }

    /**
     * Reset the internal state (useful for testing).
     */
    internal fun resetForTesting() {
        certificatePinner = null
    }
}
