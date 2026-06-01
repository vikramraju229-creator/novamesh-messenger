/**
 * Foreground service for managing WebRTC voice/video calls.
 *
 * Keeps the NovaMesh process alive during active calls, even when the
 * app is in the background. Manages:
 * - Call state machine (IDLE → RINGING → CONNECTING → CONNECTED → ENDED)
 * - Audio focus (requests/abandons via [AudioManager])
 * - Proximity sensor (turns screen off during active calls)
 * - Persistent notification with in-call controls (End, Mute, Speaker, Video)
 */
package com.novamesh.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.AudioManager.OnAudioFocusChangeListener
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.novamesh.MainActivity
import com.novamesh.NovaMeshApp
import com.novamesh.R
import timber.log.Timber

/**
 * Foreground service for managing WebRTC voice/video calls.
 *
 * Keeps the app alive during calls, even when in background.
 * Manages:
 * - Call state machine (RINGING -> CONNECTING -> CONNECTED -> ENDED)
 * - Audio focus
 * - Proximity sensor (earpiece detection)
 * - Notification with call controls
 */
class CallService : Service() {

    // ------------------------------------------------------------------
    // Call state
    // ------------------------------------------------------------------

    /**
     * States in the call lifecycle.
     */
    enum class CallState {
        /** No active or incoming call. */
        IDLE,
        /** Incoming call ringing (outgoing call waiting to connect). */
        RINGING,
        /** Negotiating WebRTC peer connection. */
        CONNECTING,
        /** Call is active and media is flowing. */
        CONNECTED,
        /** Call placed on hold. */
        ON_HOLD,
        /** Call has ended (cleanup pending). */
        ENDED,
    }

    /** Current call state, observed by the notification builder. */
    private var callState: CallState = CallState.IDLE

    /** Unique ID for the current call (null when idle). */
    private var currentCallId: String? = null

    /** Display name of the remote participant. */
    private var peerName: String = ""

    /** Whether the current call is a video call. */
    private var isVideoCall: Boolean = false

    /** Whether audio is muted. */
    private var isMuted: Boolean = false

    /** Whether the speakerphone is on. */
    private var isSpeakerOn: Boolean = false

    // ------------------------------------------------------------------
    // System services
    // ------------------------------------------------------------------

    private lateinit var audioManager: AudioManager
    private lateinit var sensorManager: SensorManager
    private lateinit var powerManager: PowerManager
    private lateinit var notificationManager: NotificationManagerCompat

    /** Proximity sensor (turns screen off when phone is held to ear). */
    private var proximitySensor: Sensor? = null
    private var isNearEar: Boolean = false

    // ------------------------------------------------------------------
    // Audio focus
    // ------------------------------------------------------------------

    private var audioFocusRequest: AudioFocusRequest? = null

    private val audioFocusChangeListener = OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                Timber.d("Audio focus gained")
                // Resume normal audio volume
                audioManager.adjustStreamVolume(
                    AudioManager.STREAM_VOICE_CALL,
                    AudioManager.ADJUST_RAISE,
                    0,
                )
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                Timber.d("Audio focus lost — ending call")
                stopCall()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                Timber.d("Audio focus lost transiently — ducking")
                // Lower volume temporarily
                audioManager.adjustStreamVolume(
                    AudioManager.STREAM_VOICE_CALL,
                    AudioManager.ADJUST_LOWER,
                    0,
                )
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                Timber.d("Audio focus lost transiently (can duck)")
                audioManager.adjustStreamVolume(
                    AudioManager.STREAM_VOICE_CALL,
                    AudioManager.ADJUST_LOWER,
                    0,
                )
            }
        }
    }

    // ------------------------------------------------------------------
    // Proximity sensor listener
    // ------------------------------------------------------------------

    private val proximityListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor.type == Sensor.TYPE_PROXIMITY) {
                val newValue = event.values[0] < proximitySensor?.maximumRange ?: 1f
                if (newValue != isNearEar) {
                    isNearEar = newValue
                    updateProximityMode()
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            // No-op
        }
    }

    // ------------------------------------------------------------------
    // Service lifecycle
    // ------------------------------------------------------------------

    override fun onCreate() {
        super.onCreate()
        Timber.i("CallService created")

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        notificationManager = NotificationManagerCompat.from(this)

        proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            Timber.w("CallService started with null intent")
            return START_NOT_STICKY
        }

        Timber.d("CallService onStartCommand: action=${intent.action}")

        when (intent.action) {
            ACTION_START_CALL -> {
                val callId = intent.getStringExtra(EXTRA_CALL_ID) ?: return START_NOT_STICKY
                val name = intent.getStringExtra(EXTRA_PEER_NAME) ?: "Unknown"
                val video = intent.getBooleanExtra(EXTRA_IS_VIDEO, false)
                startNewCall(callId, name, video)
            }

            ACTION_END_CALL -> {
                stopCall()
            }

            ACTION_ACCEPT_CALL -> {
                val callId = intent.getStringExtra(EXTRA_CALL_ID)
                if (callId != null) acceptCall(callId)
            }

            ACTION_DECLINE_CALL -> {
                val callId = intent.getStringExtra(EXTRA_CALL_ID)
                if (callId != null) declineCall(callId)
            }

            ACTION_MUTE -> {
                isMuted = !isMuted
                updateCallNotification()
                // TODO: Toggle WebRTC audio track
                Timber.d("Mute toggled: $isMuted")
            }

            ACTION_SPEAKER -> {
                isSpeakerOn = !isSpeakerOn
                audioManager.isSpeakerphoneOn = isSpeakerOn
                updateCallNotification()
                Timber.d("Speaker toggled: $isSpeakerOn")
            }

            ACTION_TOGGLE_VIDEO -> {
                isVideoCall = !isVideoCall
                updateCallNotification()
                // TODO: Toggle WebRTC video track
                Timber.d("Video toggled: $isVideoCall")
            }
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        Timber.i("CallService destroyed")
        cleanup()
        super.onDestroy()
    }

    // ------------------------------------------------------------------
    // Call lifecycle methods
    // ------------------------------------------------------------------

    /**
     * Initiates a new outgoing call and starts the foreground service.
     */
    private fun startNewCall(callId: String, peerName: String, video: Boolean) {
        this.currentCallId = callId
        this.peerName = peerName
        this.isVideoCall = video
        this.isMuted = false
        this.isSpeakerOn = false

        requestAudioFocus()
        updateCallState(CallState.CONNECTING, callId, peerName, video)
        startForeground(NOTIFICATION_ID, buildCallNotification())

        // Vibrate to indicate call is starting
        vibrateOnCallStart()
    }

    /**
     * Accepts an incoming call.
     */
    private fun acceptCall(callId: String) {
        if (currentCallId != callId) {
            Timber.w("acceptCall: callId mismatch ($callId vs $currentCallId)")
            return
        }
        // Dismiss the incoming-call notification (posted by FCM service)
        notificationManager.cancel(callId.hashCode().and(0x7FFFFFFF))

        requestAudioFocus()
        updateCallState(CallState.CONNECTING, callId, peerName, isVideoCall)
        updateCallNotification()

        // TODO: Accept WebRTC call
    }

    /**
     * Declines an incoming call.
     */
    private fun declineCall(callId: String) {
        // Dismiss the incoming-call notification
        notificationManager.cancel(callId.hashCode().and(0x7FFFFFFF))
        Timber.d("Call declined: $callId")
        // TODO: Send decline signal via signaling server
        stopSelf()
    }

    /**
     * Ends the current call and cleans up resources.
     */
    private fun stopCall() {
        Timber.i("Stopping call: $currentCallId")

        updateCallState(CallState.ENDED, currentCallId ?: "", peerName, isVideoCall)

        // Clean up
        abandonAudioFocus()
        unregisterProximitySensor()

        // Dismiss the call notification
        notificationManager.cancel(NOTIFICATION_ID)

        // Stop the foreground service
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    // ------------------------------------------------------------------
    // Audio focus
    // ------------------------------------------------------------------

    /**
     * Requests audio focus for VOICE_CALL stream with Usage Media.
     * Uses [AudioFocusRequest] on API 26+.
     */
    private fun requestAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()

            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(attrs)
                .setOnAudioFocusChangeListener(audioFocusChangeListener)
                .setWillPauseWhenDucked(true)
                .build()

            val result = audioManager.requestAudioFocus(audioFocusRequest!!)
            Timber.d("Audio focus requested: result=$result")
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                audioFocusChangeListener,
                AudioManager.STREAM_VOICE_CALL,
                AudioManager.AUDIOFOCUS_GAIN,
            )
        }
    }

    /**
     * Abandons audio focus when the call ends.
     */
    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(audioFocusChangeListener)
        }
        Timber.d("Audio focus abandoned")
    }

    // ------------------------------------------------------------------
    // Proximity sensor
    // ------------------------------------------------------------------

    /**
     * Registers the proximity sensor listener to detect when the device
     * is held to the ear (turns screen off).
     */
    private fun registerProximitySensor() {
        proximitySensor?.let { sensor ->
            sensorManager.registerListener(
                proximityListener,
                sensor,
                SensorManager.SENSOR_DELAY_NORMAL,
            )
            Timber.d("Proximity sensor registered")
        }
    }

    /**
     * Unregisters the proximity sensor listener.
     */
    private fun unregisterProximitySensor() {
        sensorManager.unregisterListener(proximityListener)
        isNearEar = false
        Timber.d("Proximity sensor unregistered")
    }

    /**
     * Turns the screen on/off based on proximity to the ear.
     * When near the ear, the screen is turned off to prevent accidental touches.
     */
    private fun updateProximityMode() {
        // In production, use PowerManager wake locks to manage screen state.
        // For now we just log the state change.
        if (isNearEar) {
            Timber.d("Device near ear — screen should turn off")
            // TODO: Acquire PROXIMITY_SCREEN_OFF_WAKE_LOCK
        } else {
            Timber.d("Device away from ear — screen should turn on")
            // TODO: Release PROXIMITY_SCREEN_OFF_WAKE_LOCK
        }
    }

    // ------------------------------------------------------------------
    // Call state updates
    // ------------------------------------------------------------------

    /**
     * Updates the call state and refreshes the foreground notification.
     *
     * @param state    New [CallState].
     * @param callId   The current call identifier.
     * @param name     Display name of the peer.
     * @param video    Whether this is a video call.
     */
    fun updateCallState(
        state: CallState,
        callId: String,
        name: String = peerName,
        video: Boolean = isVideoCall,
    ) {
        callState = state
        currentCallId = callId
        peerName = name
        isVideoCall = video

        Timber.d("Call state updated: $state (call=$callId)")

        when (state) {
            CallState.RINGING -> {
                registerProximitySensor()
                startForeground(NOTIFICATION_ID, buildCallNotification())
            }

            CallState.CONNECTING,
            CallState.CONNECTED -> {
                registerProximitySensor()
                updateCallNotification()
            }

            CallState.ON_HOLD -> {
                updateCallNotification()
            }

            CallState.ENDED,
            CallState.IDLE -> {
                // Notification is removed by stopCall()
            }
        }
    }

    /**
     * Refreshes the foreground notification with current call state.
     */
    private fun updateCallNotification() {
        val notification = buildCallNotification()
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    // ------------------------------------------------------------------
    // Notification builder
    // ------------------------------------------------------------------

    /**
     * Builds the in-call foreground notification with control actions.
     *
     * Actions shown:
     * - End Call
     * - Mute / Unmute
     * - Speaker On / Off
     * - Toggle Video
     */
    private fun buildCallNotification(): Notification {
        val callLabel = if (isVideoCall) getString(R.string.video_call) else getString(R.string.voice_call)

        // Title based on state
        val title = when (callState) {
            CallState.RINGING -> getString(R.string.incoming_call)
            CallState.CONNECTING -> getString(R.string.connecting_call)
            CallState.CONNECTED -> peerName
            CallState.ON_HOLD -> getString(R.string.call_on_hold)
            CallState.ENDED,
            CallState.IDLE -> peerName
        }

        // Subtitle based on state
        val subtitle = when (callState) {
            CallState.RINGING -> callLabel
            CallState.CONNECTING -> getString(R.string.connecting)
            CallState.CONNECTED -> {
                if (isMuted) getString(R.string.muted) else getString(R.string.call_in_progress)
            }
            CallState.ON_HOLD -> getString(R.string.on_hold)
            CallState.ENDED -> getString(R.string.call_ended)
            CallState.IDLE -> ""
        }

        // ── End call action ──
        val endCallIntent = Intent(this, CallService::class.java).apply {
            action = ACTION_END_CALL
        }
        val endCallPendingIntent = PendingIntent.getService(
            this,
            REQUEST_END_CALL,
            endCallIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        // ── Mute action ──
        val muteIntent = Intent(this, CallService::class.java).apply {
            action = ACTION_MUTE
        }
        val mutePendingIntent = PendingIntent.getService(
            this,
            REQUEST_MUTE,
            muteIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val muteIcon = if (isMuted) R.drawable.ic_mic_off else R.drawable.ic_mic_on
        val muteLabel = if (isMuted) getString(R.string.unmute) else getString(R.string.mute_call)

        // ── Speaker action ──
        val speakerIntent = Intent(this, CallService::class.java).apply {
            action = ACTION_SPEAKER
        }
        val speakerPendingIntent = PendingIntent.getService(
            this,
            REQUEST_SPEAKER,
            speakerIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val speakerIcon = if (isSpeakerOn) R.drawable.ic_speaker_off else R.drawable.ic_speaker_on
        val speakerLabel = if (isSpeakerOn) getString(R.string.speaker_off) else getString(R.string.speaker)

        // ── Toggle video action ──
        val videoIntent = Intent(this, CallService::class.java).apply {
            action = ACTION_TOGGLE_VIDEO
        }
        val videoPendingIntent = PendingIntent.getService(
            this,
            REQUEST_TOGGLE_VIDEO,
            videoIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val videoIcon = if (isVideoCall) R.drawable.ic_video_off else R.drawable.ic_video_on
        val videoLabel = if (isVideoCall) getString(R.string.video_off) else getString(R.string.video_on)

        // ── Open PendingIntent (tap notification → MainActivity) ──
        val openIntent = Intent(this, MainActivity::class.java).apply {
            action = "com.novamesh.action.CALL_ACTIVE"
            putExtra("call_id", currentCallId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this,
            REQUEST_OPEN,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(this, NovaMeshApp.CHANNEL_CALLS)
            .setSmallIcon(
                if (isVideoCall) R.drawable.ic_notification_video_call
                else R.drawable.ic_notification_call,
            )
            .setContentTitle(title)
            .setContentText(subtitle)
            .setContentIntent(openPendingIntent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            // Call control actions
            .addAction(R.drawable.ic_call_end, getString(R.string.end_call), endCallPendingIntent)
            .addAction(muteIcon, muteLabel, mutePendingIntent)
            .addAction(speakerIcon, speakerLabel, speakerPendingIntent)
            .addAction(videoIcon, videoLabel, videoPendingIntent)
            .build()
    }

    // ------------------------------------------------------------------
    // Haptic feedback
    // ------------------------------------------------------------------

    /**
     * Vibrates briefly to indicate call initiation.
     */
    private fun vibrateOnCallStart() {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(VIBRATOR_SERVICE) as Vibrator
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(
                    VibrationEffect.createOneShot(200L, VibrationEffect.DEFAULT_AMPLITUDE),
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(200L)
            }
        } catch (_: Exception) {
            // Vibrator may not be available on all devices
        }
    }

    // ------------------------------------------------------------------
    // Cleanup
    // ------------------------------------------------------------------

    /**
     * Releases all resources held by the service.
     */
    private fun cleanup() {
        abandonAudioFocus()
        unregisterProximitySensor()
        callState = CallState.IDLE
        currentCallId = null
        Timber.i("CallService cleaned up")
    }

    // ------------------------------------------------------------------
    // Companion: Intent actions & extras
    // ------------------------------------------------------------------

    companion object {
        // Notification ID for the foreground call notification.
        private const val NOTIFICATION_ID = 9001

        // PendingIntent request codes (used to differentiate actions)
        private const val REQUEST_END_CALL = 100
        private const val REQUEST_MUTE = 101
        private const val REQUEST_SPEAKER = 102
        private const val REQUEST_TOGGLE_VIDEO = 103
        private const val REQUEST_OPEN = 104

        // ─── Intent actions ────────────────────────────────────────────

        /** Start a new outgoing call. */
        const val ACTION_START_CALL = "com.novamesh.call.START_CALL"

        /** End the current call. */
        const val ACTION_END_CALL = "com.novamesh.call.END_CALL"

        /** Accept an incoming call. */
        const val ACTION_ACCEPT_CALL = "com.novamesh.call.ACCEPT_CALL"

        /** Decline an incoming call. */
        const val ACTION_DECLINE_CALL = "com.novamesh.call.DECLINE_CALL"

        /** Toggle mute. */
        const val ACTION_MUTE = "com.novamesh.call.MUTE"

        /** Toggle speakerphone. */
        const val ACTION_SPEAKER = "com.novamesh.call.SPEAKER"

        /** Toggle video on/off. */
        const val ACTION_TOGGLE_VIDEO = "com.novamesh.call.TOGGLE_VIDEO"

        // ─── Intent extras ─────────────────────────────────────────────

        /** Extra: unique call identifier. */
        const val EXTRA_CALL_ID = "extra_call_id"

        /** Extra: display name of the remote peer. */
        const val EXTRA_PEER_NAME = "extra_peer_name"

        /** Extra: whether this is a video call (vs voice). */
        const val EXTRA_IS_VIDEO = "extra_is_video"

        /**
         * Build an Intent to start a new outgoing call.
         *
         * @param context  The context from which the call is initiated.
         * @param callId   Unique identifier for the call session.
         * @param peerName Display name of the person being called.
         * @param isVideo  `true` for video call, `false` for voice.
         * @return A configured [Intent] for [CallService].
         */
        fun newCallIntent(
            context: Context,
            callId: String,
            peerName: String,
            isVideo: Boolean,
        ): Intent {
            return Intent(context, CallService::class.java).apply {
                action = ACTION_START_CALL
                putExtra(EXTRA_CALL_ID, callId)
                putExtra(EXTRA_PEER_NAME, peerName)
                putExtra(EXTRA_IS_VIDEO, isVideo)
            }
        }
    }
}
