package com.novamesh.security

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricManager.Authenticators
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume

/**
 * Manages biometric authentication (fingerprint + face unlock)
 * and PIN/pattern fallback for app lock.
 *
 * Features:
 * - Fingerprint and face unlock
 * - PIN/pattern fallback
 * - Configurable lock timer
 * - FLAG_SECURE enforcement
 * - Root detection
 */
class NovaBiometricManager(private val context: Context) {

    companion object {
        private const val TAG = "NovaBiometric"

        // Encrypted SharedPreferences keys
        private const val PREF_NAME = "novamesh_biometric_prefs"
        private const val KEY_APP_LOCK_ENABLED = "app_lock_enabled"
        private const val KEY_LOCK_TIMER = "lock_timer_minutes"

        // Default lock timer: IMMEDIATELY (0 minutes)
        private const val DEFAULT_LOCK_TIMER_MINUTES = 0

        // Root detection paths
        private val ROOT_INDICATOR_PATHS = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su"
        )

        // Magisk detection paths
        private val MAGISK_PATHS = arrayOf(
            "/data/adb/magisk",
            "/data/adb/magisk.db",
            "/data/adb/magisk.img"
        )
    }

    // Lazy-initialized EncryptedSharedPreferences for secure storage
    private val securePrefs by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            PREF_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    // =========================================================================
    // Lock Timer
    // =========================================================================

    /**
     * Defines the available intervals after which the app lock is re-engaged.
     *
     * @property delayMinutes The number of minutes before the app re-locks.
     */
    enum class LockTimer(val delayMinutes: Int) {
        /** Lock the app immediately when it goes to background. */
        IMMEDIATELY(0),

        /** Lock after 1 minute of inactivity. */
        AFTER_1_MIN(1),

        /** Lock after 5 minutes of inactivity. */
        AFTER_5_MIN(5),

        /** Lock after 15 minutes of inactivity. */
        AFTER_15_MIN(15),

        /** Lock after 30 minutes of inactivity. */
        AFTER_30_MIN(30);

        companion object {
            /**
             * Returns the [LockTimer] whose [delayMinutes] matches [minutes],
             * or `null` if no predefined timer matches.
             */
            fun fromMinutes(minutes: Int): LockTimer? =
                entries.find { it.delayMinutes == minutes }
        }
    }

    // =========================================================================
    // Biometric Result
    // =========================================================================

    /**
     * Represents the outcome of a biometric authentication request.
     */
    sealed class BiometricResult {
        /** Authentication succeeded, optionally with a [CryptoObject]. */
        data class Success(val cryptoObject: BiometricPrompt.CryptoObject? = null) : BiometricResult()

        /** Authentication failed with an error [code] and [message]. */
        data class Error(val code: Int, val message: String) : BiometricResult()

        /** Biometric authentication is not available on this device. */
        data object NotAvailable : BiometricResult()
    }

    // =========================================================================
    // Capability Checks
    // =========================================================================

    /**
     * Checks whether the device has biometric hardware available and
     * can authenticate the user.
     *
     * @return `true` if biometric authentication is possible, `false` otherwise.
     */
    fun canAuthenticate(): Boolean {
        val biometricManager = BiometricManager.from(context)
        val result = biometricManager.canAuthenticate(
            Authenticators.BIOMETRIC_STRONG or Authenticators.BIOMETRIC_WEAK
        )
        return result == BiometricManager.BIOMETRIC_SUCCESS
    }

    /**
     * Returns a bitfield of the available authenticators on this device.
     *
     * Possible values (from [Authenticators]):
     * - [Authenticators.BIOMETRIC_STRONG]
     * - [Authenticators.BIOMETRIC_WEAK]
     * - [Authenticators.DEVICE_CREDENTIAL]
     */
    fun getAvailableAuthenticators(): Int {
        val biometricManager = BiometricManager.from(context)
        var authenticators = 0

        if (biometricManager.canAuthenticate(Authenticators.BIOMETRIC_STRONG) ==
            BiometricManager.BIOMETRIC_SUCCESS
        ) {
            authenticators = authenticators or Authenticators.BIOMETRIC_STRONG
        }

        if (biometricManager.canAuthenticate(Authenticators.BIOMETRIC_WEAK) ==
            BiometricManager.BIOMETRIC_SUCCESS
        ) {
            authenticators = authenticators or Authenticators.BIOMETRIC_WEAK
        }

        if (biometricManager.canAuthenticate(Authenticators.DEVICE_CREDENTIAL) ==
            BiometricManager.BIOMETRIC_SUCCESS
        ) {
            authenticators = authenticators or Authenticators.DEVICE_CREDENTIAL
        }

        return authenticators
    }

    // =========================================================================
    // Authentication
    // =========================================================================

    /**
     * Launches the biometric authentication prompt and suspends until the
     * user completes (or cancels) authentication.
     *
     * The prompt supports both biometric (fingerprint / face) and
     * device credential (PIN / pattern / password) fallback.
     *
     * @param activity The current [FragmentActivity] that hosts the prompt.
     * @param title    Title for the biometric dialog.
     * @param subtitle Subtitle for the biometric dialog.
     * @param description Optional description text for the biometric dialog.
     * @return [BiometricResult] indicating success, error, or unavailability.
     */
    suspend fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        description: String? = null
    ): BiometricResult = suspendCancellableCoroutine { continuation ->
        if (!canAuthenticate()) {
            continuation.resume(BiometricResult.NotAvailable)
            return@suspendCancellableCoroutine
        }

        val executor = ContextCompat.getMainExecutor(context)

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                if (continuation.isActive) {
                    continuation.resume(
                        BiometricResult.Success(result.cryptoObject)
                    )
                }
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                if (continuation.isActive) {
                    continuation.resume(
                        BiometricResult.Error(errorCode, errString.toString())
                    )
                }
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                // onAuthenticationFailed is called when the sensor doesn't
                // recognize the biometric; we don't resume here because the
                // prompt stays open for another attempt.
                Log.w(TAG, "Biometric authentication failed (not recognized)")
            }
        }

        val promptInfoBuilder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(
                Authenticators.BIOMETRIC_STRONG or
                    Authenticators.DEVICE_CREDENTIAL
            )

        if (description != null) {
            promptInfoBuilder.setDescription(description)
        }

        val promptInfo = promptInfoBuilder.build()

        val biometricPrompt = BiometricPrompt(activity, executor, callback)
        biometricPrompt.authenticate(promptInfo)

        // If the coroutine is cancelled, cancel the prompt
        continuation.invokeOnCancellation {
            biometricPrompt.cancelAuthentication()
        }
    }

    // =========================================================================
    // App Lock
    // =========================================================================

    /**
     * Returns whether the app lock is currently enabled.
     *
     * @return `true` if the app lock feature is turned on.
     */
    fun isAppLockEnabled(): Boolean = securePrefs.getBoolean(KEY_APP_LOCK_ENABLED, false)

    /**
     * Enables or disables the app lock feature.
     *
     * @param enabled `true` to enable app lock, `false` to disable.
     */
    suspend fun setAppLockEnabled(enabled: Boolean) = withContext(Dispatchers.IO) {
        securePrefs.edit().putBoolean(KEY_APP_LOCK_ENABLED, enabled).apply()
        Log.d(TAG, "App lock ${if (enabled) "enabled" else "disabled"}")
    }

    /**
     * Returns the currently configured [LockTimer].
     *
     * Defaults to [LockTimer.IMMEDIATELY] if no value has been saved.
     */
    fun getLockTimer(): LockTimer {
        val minutes = securePrefs.getInt(KEY_LOCK_TIMER, DEFAULT_LOCK_TIMER_MINUTES)
        return LockTimer.fromMinutes(minutes) ?: LockTimer.IMMEDIATELY
    }

    /**
     * Persists the lock timer setting.
     *
     * @param timer The [LockTimer] to save.
     */
    suspend fun setLockTimer(timer: LockTimer) = withContext(Dispatchers.IO) {
        securePrefs.edit().putInt(KEY_LOCK_TIMER, timer.delayMinutes).apply()
        Log.d(TAG, "Lock timer set to ${timer.delayMinutes} minutes")
    }

    /**
     * Determines whether the app should be locked based on the elapsed
     * time since [lastUnlockTime].
     *
     * @param lastUnlockTime The epoch millis of the last successful unlock.
     * @return `true` if the lock timer has expired and the app should re-lock.
     */
    fun shouldLockApp(lastUnlockTime: Long): Boolean {
        if (!isAppLockEnabled()) return false

        val timer = getLockTimer()
        if (timer == LockTimer.IMMEDIATELY) return true

        val elapsed = System.currentTimeMillis() - lastUnlockTime
        val timerMillis = timer.delayMinutes * 60_000L
        return elapsed >= timerMillis
    }

    // =========================================================================
    // Root Detection
    // =========================================================================

    /**
     * Checks whether the device appears to be rooted.
     *
     * Detection methods:
     * 1. Checks if [Build.TAGS] contains "test-keys" (indicating a
     *    non-production build).
     * 2. Checks for the presence of known su binary paths.
     * 3. Checks for Magisk installation paths.
     *
     * @return `true` if any root indicator is found.
     */
    fun isDeviceRooted(): Boolean {
        // 1. Check for test-keys in build tags
        if (isTestKeysBuild()) {
            Log.w(TAG, "Root detected: Build.TAGS contains test-keys")
            return true
        }

        // 2. Check for su binaries
        if (checkRootIndicators()) {
            Log.w(TAG, "Root detected: su binary found")
            return true
        }

        // 3. Check for Magisk
        if (checkMagisk()) {
            Log.w(TAG, "Root detected: Magisk found")
            return true
        }

        return false
    }

    /**
     * Returns `true` if the build tags contain "test-keys".
     */
    private fun isTestKeysBuild(): Boolean {
        val tags = Build.TAGS
        return tags != null && tags.contains("test-keys", ignoreCase = true)
    }

    /**
     * Checks for the presence of known su binary paths on the filesystem.
     *
     * @return `true` if any su binary exists.
     */
    private fun checkRootIndicators(): Boolean {
        for (path in ROOT_INDICATOR_PATHS) {
            try {
                if (File(path).exists()) {
                    Log.d(TAG, "Root indicator found at: $path")
                    return true
                }
            } catch (e: SecurityException) {
                // Cannot access the file; skip this path
                Log.w(TAG, "Access denied checking path: $path")
            }
        }
        return false
    }

    /**
     * Checks for Magisk-specific files that indicate root access.
     *
     * @return `true` if Magisk installation is detected.
     */
    private fun checkMagisk(): Boolean {
        for (path in MAGISK_PATHS) {
            try {
                if (File(path).exists()) {
                    Log.d(TAG, "Magisk indicator found at: $path")
                    return true
                }
            } catch (e: SecurityException) {
                Log.w(TAG, "Access denied checking Magisk path: $path")
            }
        }
        return false
    }
}
