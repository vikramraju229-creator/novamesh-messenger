package com.novamesh

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.util.DebugLogger
import android.util.Log
import com.novamesh.security.CertificatePinner
import com.novamesh.security.SignalProtocolManager
import java.io.File

/**
 * NovaMesh Messenger — Application class.
 *
 * Initializes core services:
 * - Notification channels
 * - Coil image loading with optimized cache
 * - Signal Protocol (E2EE)
 * - Certificate pinning
 * - Timber logging (debug only)
 */
class NovaMeshApp : Application(), ImageLoaderFactory {

    /** Signal Protocol manager for E2EE */
    lateinit var signalProtocolManager: SignalProtocolManager
        private set

    /** Certificate pinner for MITM prevention */
    lateinit var certificatePinner: CertificatePinner
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        // ─── Initialize logging ───
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "NovaMesh Messenger initializing (debug mode)")
        }

        // ─── Notification channels ───
        createNotificationChannels()

        // ─── Certificate pinning ───
        certificatePinner = CertificatePinner(this)

        // ─── Signal Protocol (E2EE) ───
        signalProtocolManager = SignalProtocolManager(this)

        Log.i(TAG, "NovaMesh Messenger initialized")
    }

    /**
     * Creates notification channels for Android 8+.
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = getSystemService(NotificationManager::class.java)

        // Messages channel
        val messagesChannel = NotificationChannel(
            CHANNEL_MESSAGES,
            getString(R.string.channel_messages),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = getString(R.string.channel_messages_desc)
            enableVibration(true)
            setShowBadge(true)
            enableLights(true)
        }

        // Calls channel
        val callsChannel = NotificationChannel(
            CHANNEL_CALLS,
            getString(R.string.channel_calls),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = getString(R.string.channel_calls_desc)
            setShowBadge(false)
            enableVibration(true)
            // setFullScreenActionRequired removed in API 36+
            // Use full-screen intent for incoming calls instead
        }

        // Stories channel
        val storiesChannel = NotificationChannel(
            CHANNEL_STORIES,
            getString(R.string.channel_stories),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.channel_stories_desc)
            setShowBadge(false)
        }

        // Silent / background sync channel
        val syncChannel = NotificationChannel(
            CHANNEL_SYNC,
            getString(R.string.channel_sync),
            NotificationManager.IMPORTANCE_MIN
        ).apply {
            description = getString(R.string.channel_sync_desc)
            setShowBadge(false)
        }

        manager.createNotificationChannel(messagesChannel)
        manager.createNotificationChannel(callsChannel)
        manager.createNotificationChannel(storiesChannel)
        manager.createNotificationChannel(syncChannel)
    }

    // ─── Coil ImageLoader ───

    override fun newImageLoader(): ImageLoader {
        val cacheDir = File(cacheDir, "image_cache")
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.20) // 20% of app heap
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir)
                    .maxSizeBytes(50 * 1024 * 1024) // 50 MB
                    .build()
            }
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.ENABLED)
            .crossfade(true)
            .apply {
                if (BuildConfig.DEBUG) {
                    logger(DebugLogger())
                }
            }
            .build()
    }

    companion object {
        /** Singleton instance */
        lateinit var instance: NovaMeshApp
            private set

        // Notification channel IDs
        const val CHANNEL_MESSAGES = "nova_messages"
        const val CHANNEL_CALLS = "nova_calls"
        const val CHANNEL_STORIES = "nova_stories"
        const val CHANNEL_SYNC = "nova_sync"

        private const val TAG = "NovaMeshApp"
    }
}
