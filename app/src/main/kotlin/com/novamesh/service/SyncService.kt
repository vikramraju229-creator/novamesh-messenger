/**
 * Foreground service for background Matrix sync.
 *
 * Maintains a persistent connection to the Matrix homeserver via the
 * matrix-android-sdk2, delivering real-time message updates even when
 * the app is in the background.
 *
 * Features:
 * - Configurable sync interval (default 30s)
 * - Exponential backoff on connection errors
 * - Network connectivity awareness (pauses when offline)
 * - Foreground notification (channel: `nova_sync`, low priority)
 * - Force-sync capability for immediate pull
 */
package com.novamesh.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.novamesh.MainActivity
import com.novamesh.NovaMeshApp
import com.novamesh.R
import kotlinx.coroutines.*
import timber.log.Timber

/**
 * Foreground service for background Matrix sync.
 *
 * Maintains a persistent WebSocket connection to the Matrix server
 * for real-time message delivery and sync.
 */
class SyncService : Service() {

    // ------------------------------------------------------------------
    // Sync state
    // ------------------------------------------------------------------

    /**
     * States in the sync lifecycle.
     */
    enum class SyncState {
        /** Not connected to the homeserver. */
        DISCONNECTED,
        /** Attempting to establish a connection. */
        CONNECTING,
        /** Actively syncing with the homeserver. */
        SYNCING,
        /** An error occurred (will retry with backoff). */
        ERROR,
    }

    /** Current sync state, published for UI observation. */
    private var syncState: SyncState = SyncState.DISCONNECTED

    // ------------------------------------------------------------------
    // Configuration
    // ------------------------------------------------------------------

    /** Base interval between sync requests (milliseconds). */
    private var syncIntervalMs = DEFAULT_SYNC_INTERVAL_MS

    /** Maximum backoff delay before retrying after an error. */
    private var maxBackoffMs = MAX_BACKOFF_MS

    // ------------------------------------------------------------------
    // Coroutine scope
    // ------------------------------------------------------------------

    /** Supervisor scope tied to this service's lifecycle. */
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Job for the ongoing sync loop (cancelled on stop). */
    private var syncJob: Job? = null

    // ------------------------------------------------------------------
    // Network connectivity
    // ------------------------------------------------------------------

    private lateinit var connectivityManager: ConnectivityManager
    private var isNetworkAvailable: Boolean = true

    /** Callback for connectivity changes. */
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            Timber.i("Network available — resuming sync")
            isNetworkAvailable = true
            // If we were idle due to no network, restart the loop
            if (syncState == SyncState.DISCONNECTED) {
                startSyncLoop()
            }
        }

        override fun onLost(network: Network) {
            Timber.w("Network lost — pausing sync")
            isNetworkAvailable = false
            syncState = SyncState.DISCONNECTED
            syncJob?.cancel()
            updateSyncNotification()
        }

        override fun onCapabilitiesChanged(
            network: Network,
            capabilities: NetworkCapabilities,
        ) {
            val hasInternet = capabilities.hasCapability(
                NetworkCapabilities.NET_CAPABILITY_INTERNET,
            )
            if (!hasInternet && isNetworkAvailable) {
                Timber.w("Network lost internet capability — pausing sync")
                isNetworkAvailable = false
                syncState = SyncState.DISCONNECTED
                syncJob?.cancel()
                updateSyncNotification()
            }
        }
    }

    // ------------------------------------------------------------------
    // Service lifecycle
    // ------------------------------------------------------------------

    override fun onCreate() {
        super.onCreate()
        Timber.i("SyncService created")

        connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        // Register network callback
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(request, networkCallback)

        // Check initial network state
        val activeNetwork = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        isNetworkAvailable = capabilities?.hasCapability(
            NetworkCapabilities.NET_CAPABILITY_INTERNET,
        ) == true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            Timber.w("SyncService started with null intent")
            return START_STICKY
        }

        Timber.d("SyncService onStartCommand: action=${intent.action}")

        when (intent.action) {
            ACTION_START_SYNC -> {
                startForeground(NOTIFICATION_ID, buildSyncNotification())
                startSyncLoop()
            }

            ACTION_STOP_SYNC -> {
                stopSync()
            }

            ACTION_FORCE_SYNC -> {
                // Trigger an immediate sync cycle
                forceSync()
            }
        }

        // STICKY so the system restarts us if killed
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        Timber.i("SyncService destroyed")
        serviceScope.cancel()
        connectivityManager.unregisterNetworkCallback(networkCallback)
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    // ------------------------------------------------------------------
    // Sync loop
    // ------------------------------------------------------------------

    /**
     * Starts the periodic sync loop with exponential backoff.
     *
     * The loop:
     * 1. Checks network availability.
     * 2. Attempts to connect/sync with the Matrix homeserver.
     * 3. On success, waits for [syncIntervalMs].
     * 4. On failure, waits with exponential backoff (up to [maxBackoffMs]).
     */
    private fun startSyncLoop() {
        syncJob?.cancel()
        syncJob = serviceScope.launch {
            syncLoop()
        }
    }

    /**
     * The core sync loop with exponential backoff.
     *
     * Continues until:
     * - The service is stopped ([ACTION_STOP_SYNC])
     * - Network is lost (pauses until reconnection)
     * - The coroutine scope is cancelled (service destroyed)
     */
    private suspend fun syncLoop() {
        var backoffMs = INITIAL_BACKOFF_MS
        var consecutiveErrors = 0

        while (isActive) {
            // Check network
            if (!isNetworkAvailable) {
                syncState = SyncState.DISCONNECTED
                updateSyncNotification()
                Timber.d("Sync paused — no network")
                // Wait for network to come back (checked in networkCallback)
                delay(10_000L)
                continue
            }

            try {
                syncState = SyncState.CONNECTING
                updateSyncNotification()

                // Attempt to connect / sync
                performSync()

                // Success — reset backoff
                syncState = SyncState.SYNCING
                updateSyncNotification()
                consecutiveErrors = 0
                backoffMs = INITIAL_BACKOFF_MS

                // Wait for the next sync interval
                delay(syncIntervalMs)

            } catch (e: CancellationException) {
                throw e // Propagate cancellation
            } catch (e: Exception) {
                consecutiveErrors++
                syncState = SyncState.ERROR
                updateSyncNotification()

                Timber.e(e, "Sync error (#$consecutiveErrors) — backing off ${backoffMs}ms")

                // Exponential backoff with jitter
                delay(backoffMs)
                backoffMs = (backoffMs * 2).coerceAtMost(maxBackoffMs)

                // Add jitter: ±10%
                val jitter = (backoffMs * 0.1).toLong()
                if (jitter > 0) {
                    delay((-jitter..jitter).random())
                }
            }
        }
    }

    /**
     * Performs a single sync cycle.
     *
     * In production, this calls the Matrix SDK's sync API:
     * ```
     * matrixClient?.sync(object : MatrixSyncCallback {
     *     override fun onSyncResponse(syncToken: String, rooms: List<Room>) {
     *         // Process room events
     *     }
     *     override fun onSyncError(error: MatrixError) {
     *         throw error.toException()
     *     }
     * })
     * ```
     *
     * For the scaffold, we simulate a brief network call.
     */
    private suspend fun performSync() {
        withContext(Dispatchers.IO) {
            // TODO: Replace with real Matrix SDK sync call:
            //
            // val matrixClient = NovaMeshApp.instance.matrixClient
            // if (matrixClient == null) {
            //     throw IllegalStateException("Matrix client not initialized")
            // }
            // matrixClient.sync(object : MatrixSyncCallback {
            //     override fun onSyncResponse(...) { ... }
            //     override fun onSyncError(...) { ... }
            // })

            // Simulate sync work (remove in production)
            if (!isNetworkAvailable) {
                throw java.io.IOException("No network available")
            }

            // Simulate a brief sync operation
            delay(500L)

            Timber.d("Sync cycle completed")
        }
    }

    /**
     * Forces an immediate sync, resetting the backoff timer.
     */
    private fun forceSync() {
        Timber.d("Force sync requested")
        syncJob?.cancel()
        startSyncLoop()
    }

    /**
     * Stops the sync loop and transitions to disconnected state.
     */
    private fun stopSync() {
        Timber.i("Sync stopped by user")
        syncJob?.cancel()
        syncState = SyncState.DISCONNECTED
        updateSyncNotification()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    // ------------------------------------------------------------------
    // Foreground notification
    // ------------------------------------------------------------------

    /**
     * Builds the low-priority sync notification shown while the service
     * is running in the foreground.
     */
    private fun buildSyncNotification(): Notification {
        val title = getString(R.string.sync_notification_title)
        val content = when (syncState) {
            SyncState.DISCONNECTED -> getString(R.string.sync_disconnected)
            SyncState.CONNECTING -> getString(R.string.sync_connecting)
            SyncState.SYNCING -> getString(R.string.sync_in_progress)
            SyncState.ERROR -> getString(R.string.sync_error)
        }

        // Tap notification → open MainActivity
        val openIntent = Intent(this, MainActivity::class.java).apply {
            action = "com.novamesh.action.OPEN_SYNC"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this,
            REQUEST_OPEN,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(this, NovaMeshApp.CHANNEL_SYNC)
            .setSmallIcon(R.drawable.ic_notification_sync)
            .setContentTitle(title)
            .setContentText(content)
            .setContentIntent(openPendingIntent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .build()
    }

    /**
     * Refreshes the foreground notification with the current sync state.
     */
    private fun updateSyncNotification() {
        val notification = buildSyncNotification()
        notificationManager?.notify(NOTIFICATION_ID, notification)
            ?: run {
                // NotificationManagerCompat may not be initialized yet
                // Rebuild and notify via the system service
                val manager = NotificationManagerCompat.from(this)
                manager.notify(NOTIFICATION_ID, notification)
            }
    }

    // ------------------------------------------------------------------
    // Lazy notification manager
    // ------------------------------------------------------------------

    private val notificationManager: NotificationManagerCompat? by lazy {
        NotificationManagerCompat.from(this)
    }

    // ------------------------------------------------------------------
    // Companion
    // ------------------------------------------------------------------

    companion object {
        /** Notification ID for the foreground sync notification. */
        private const val NOTIFICATION_ID = 8001

        /** PendingIntent request code for the open action. */
        private const val REQUEST_OPEN = 200

        // ─── Default timing values ──────────────────────────────────────

        /** Default interval between sync cycles (30 seconds). */
        private const val DEFAULT_SYNC_INTERVAL_MS = 30_000L

        /** Initial backoff after first error (1 second). */
        private const val INITIAL_BACKOFF_MS = 1_000L

        /** Maximum backoff delay (5 minutes). */
        private const val MAX_BACKOFF_MS = 300_000L

        // ─── Intent actions ─────────────────────────────────────────────

        /** Start the sync service and begin periodic sync. */
        const val ACTION_START_SYNC = "com.novamesh.sync.START_SYNC"

        /** Stop the sync service. */
        const val ACTION_STOP_SYNC = "com.novamesh.sync.STOP_SYNC"

        /** Trigger an immediate sync cycle. */
        const val ACTION_FORCE_SYNC = "com.novamesh.sync.FORCE_SYNC"

        // ─── Configuration extras ───────────────────────────────────────

        /** Extra: custom sync interval (in milliseconds). */
        const val EXTRA_SYNC_INTERVAL = "extra_sync_interval"

        /** Extra: custom max backoff (in milliseconds). */
        const val EXTRA_MAX_BACKOFF = "extra_max_backoff"

        /**
         * Build an Intent to start the sync service.
         *
         * @param context      The application context.
         * @param syncInterval Optional custom sync interval (ms).
         * @param maxBackoff   Optional custom max backoff (ms).
         * @return A configured [Intent] for [SyncService].
         */
        fun startIntent(
            context: Context,
            syncInterval: Long? = null,
            maxBackoff: Long? = null,
        ): Intent {
            return Intent(context, SyncService::class.java).apply {
                action = ACTION_START_SYNC
                syncInterval?.let { putExtra(EXTRA_SYNC_INTERVAL, it) }
                maxBackoff?.let { putExtra(EXTRA_MAX_BACKOFF, it) }
            }
        }

        /**
         * Build an Intent to stop the sync service.
         *
         * @param context The application context.
         * @return A configured [Intent] for [SyncService].
         */
        fun stopIntent(context: Context): Intent {
            return Intent(context, SyncService::class.java).apply {
                action = ACTION_STOP_SYNC
            }
        }
    }
}
