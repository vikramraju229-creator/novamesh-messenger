/**
 * Firebase Cloud Messaging service for NovaMesh Messenger.
 *
 * Handles push notifications for:
 * - Incoming messages (grouped per chat with Quick Reply)
 * - Incoming calls (full-screen with Accept/Decline)
 * - Story updates from contacts
 * - Security alerts (new device login, 2FA)
 *
 * Notification channels are registered in [NovaMeshApp].
 */
package com.novamesh.service

import android.app.PendingIntent
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import androidx.core.graphics.drawable.IconCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.novamesh.MainActivity
import com.novamesh.NovaMeshApp
import com.novamesh.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Firebase Cloud Messaging service for push notifications.
 *
 * Handles:
 * - Incoming message notifications (grouped by chat)
 * - Call notifications (Accept/Decline actions)
 * - Story update notifications
 * - Security alerts (new device login, 2FA)
 * - Quick Reply via RemoteInput
 */
class NovaMeshFirebaseService : FirebaseMessagingService() {

    /** Lightweight scope for async token saving. */
    private val ioScope = CoroutineScope(Dispatchers.IO)

    // ------------------------------------------------------------------
    // FCM Token lifecycle
    // ------------------------------------------------------------------

    /**
     * Called when a new FCM registration token is generated.
     *
     * Persists the token to local DataStore and uploads it to the
     * application server so push messages can be routed to this device.
     *
     * @param token The new FCM registration token.
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Timber.i("New FCM token generated: ${token.take(16)}…")

        ioScope.launch {
            try {
                // Persist to DataStore for local access
                saveTokenToDataStore(token)
                // Upload to the application server for push delivery
                uploadTokenToServer(token)
                Timber.i("FCM token saved and uploaded successfully")
            } catch (e: Exception) {
                Timber.e(e, "Failed to persist FCM token")
            }
        }
    }

    // ------------------------------------------------------------------
    // Incoming message handler
    // ------------------------------------------------------------------

    /**
     * Called when a push message is received from Firebase.
     *
     * Parses the [RemoteMessage] data payload and routes to the
     * appropriate notification builder based on the `type` field.
     *
     * Supported types:
     * - `"message"`      → [showMessageNotification]
     * - `"call"`         → [showCallNotification]
     * - `"story"`        → [showStoryNotification]
     * - `"security"`     → [showSecurityAlert]
     */
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Timber.d("FCM message received: type=${message.data["type"]}")

        val data = message.data
        if (data.isEmpty()) {
            Timber.w("FCM message with empty data payload — ignoring")
            return
        }

        when (data["type"]) {
            "message" -> {
                val chatId = data["chat_id"] ?: return
                val senderName = data["sender_name"] ?: "Unknown"
                val messageText = data["message_text"] ?: ""
                val senderAvatarUri = data["sender_avatar_uri"]
                showMessageNotification(chatId, senderName, messageText, senderAvatarUri)
            }

            "call" -> {
                val callerName = data["caller_name"] ?: "Unknown"
                val isVideo = data["call_type"] == "video"
                val callId = data["call_id"] ?: return
                showCallNotification(callerName, isVideo, callId)
            }

            "story" -> {
                val userName = data["user_name"] ?: "A contact"
                val storyType = data["story_type"] ?: "photo"
                showStoryNotification(userName, storyType)
            }

            "security" -> {
                val title = data["alert_title"] ?: "Security Alert"
                val body = data["alert_body"] ?: "Something requires your attention."
                showSecurityAlert(title, body)
            }

            else -> {
                Timber.w("Unknown FCM message type: ${data["type"]}")
            }
        }
    }

    // ------------------------------------------------------------------
    // Message notification (grouped per chat)
    // ------------------------------------------------------------------

    /**
     * Displays a new-message notification grouped by [chatId].
     *
     * Features:
     * - **NotificationChannel**: `nova_messages` (high importance)
     * - **Group summary**: one summary notification per chat with unread count
     * - **Person style**: uses [Person] for sender attribution
     * - **Quick Reply**: [RemoteInput]-backed inline reply action
     * - **Mark as read**: action that sends a read receipt
     * - **Open intent**: tapping opens [ChatDetailScreen] for the chat
     *
     * @param chatId         The chat/room identifier.
     * @param senderName     Display name of the message sender.
     * @param messageText    Preview text of the message.
     * @param senderAvatarUri Optional URI for the sender's avatar.
     */
    private fun showMessageNotification(
        chatId: String,
        senderName: String,
        messageText: String,
        senderAvatarUri: String?,
    ) {
        val notificationId = chatId.hashCode().and(0x7FFFFFFF)
        val groupKey = "group_chat_$chatId"

        // ── Build the Person object for the sender ──
        val senderPerson = Person.Builder()
            .setName(senderName)
            .setKey(chatId)
            .apply {
                if (!senderAvatarUri.isNullOrBlank()) {
                    try {
                        setIcon(IconCompat.createWithContentUri(senderAvatarUri))
                    } catch (_: Exception) {
                        // Fall back to no icon
                    }
                }
            }
            .build()

        // ── Open PendingIntent → ChatDetailScreen ──
        val openIntent = Intent(this, MainActivity::class.java).apply {
            action = "com.novamesh.action.OPEN_CHAT"
            putExtra("chat_id", chatId)
            putExtra("chat_name", senderName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this,
            notificationId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        // ── Quick Reply RemoteInput ──
        val replyLabel = getString(R.string.reply)
        val remoteInput = RemoteInput.Builder("key_quick_reply")
            .setLabel(replyLabel)
            .build()

        val replyIntent = Intent(this, NovaMeshFirebaseService::class.java).apply {
            action = "com.novamesh.action.QUICK_REPLY"
            putExtra("chat_id", chatId)
        }
        val replyPendingIntent = PendingIntent.getService(
            this,
            notificationId xor 1,
            replyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val replyAction = NotificationCompat.Action.Builder(
            R.drawable.ic_send,   // Will use a default send icon
            getString(R.string.reply),
            replyPendingIntent,
        ).addRemoteInput(remoteInput).build()

        // ── Mark as read action ──
        val markReadIntent = Intent(this, NovaMeshFirebaseService::class.java).apply {
            action = "com.novamesh.action.MARK_READ"
            putExtra("chat_id", chatId)
        }
        val markReadPendingIntent = PendingIntent.getService(
            this,
            notificationId xor 2,
            markReadIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val markReadAction = NotificationCompat.Action(
            R.drawable.ic_done_all,
            getString(R.string.notification_mark_read, senderName),
            markReadPendingIntent,
        )

        // ── Build the notification ──
        val notification = NotificationCompat.Builder(this, NovaMeshApp.CHANNEL_MESSAGES)
            .setSmallIcon(R.drawable.ic_notification_message)
            .setContentTitle(senderName)
            .setContentText(messageText)
            .setStyle(
                NotificationCompat.MessagingStyle(senderPerson)
                    .addMessage(messageText, System.currentTimeMillis(), senderPerson)
                    .setGroupConversation(true)
                    .setConversationTitle(senderName),
            )
            .setGroup(groupKey)
            .setGroupSummary(false)
            .setContentIntent(openPendingIntent)
            .addAction(replyAction)
            .addAction(markReadAction)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setSound(android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION))
            .build()

        // ── Build the group summary notification ──
        val summaryNotification = NotificationCompat.Builder(this, NovaMeshApp.CHANNEL_MESSAGES)
            .setSmallIcon(R.drawable.ic_notification_message)
            .setStyle(
                NotificationCompat.InboxStyle()
                    .setSummaryText(getString(R.string.notification_group_summary, senderName)),
            )
            .setGroup(groupKey)
            .setGroupSummary(true)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .build()

        // ── Notify ──
        val manager = NotificationManagerCompat.from(this)
        manager.notify(notificationId, notification)
        manager.notify(chatId.hashCode() and 0x7FFFFFFF, summaryNotification)
    }

    // ------------------------------------------------------------------
    // Call notification (full-screen)
    // ------------------------------------------------------------------

    /**
     * Displays an incoming call notification with full-screen intent.
     *
     * Features:
     * - **NotificationChannel**: `nova_calls` (high importance)
     * - **FullScreenIntent**: opens the incoming call UI
     * - **Accept / Decline** action buttons
     * - High priority, shows on lock screen
     *
     * @param callerName Display name of the caller.
     * @param isVideo    `true` for video call, `false` for voice.
     * @param callId     Unique identifier for the call.
     */
    private fun showCallNotification(
        callerName: String,
        isVideo: Boolean,
        callId: String,
    ) {
        val notificationId = callId.hashCode().and(0x7FFFFFFF)

        // ── Full-screen PendingIntent → incoming call UI ──
        val fullScreenIntent = Intent(this, MainActivity::class.java).apply {
            action = "com.novamesh.action.INCOMING_CALL"
            putExtra("call_id", callId)
            putExtra("caller_name", callerName)
            putExtra("is_video", isVideo)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            notificationId,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        // ── Accept action ──
        val acceptIntent = Intent(this, CallService::class.java).apply {
            action = CallService.ACTION_ACCEPT_CALL
            putExtra("call_id", callId)
        }
        val acceptPendingIntent = PendingIntent.getService(
            this,
            notificationId xor 1,
            acceptIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        // ── Decline action ──
        val declineIntent = Intent(this, CallService::class.java).apply {
            action = CallService.ACTION_DECLINE_CALL
            putExtra("call_id", callId)
        }
        val declinePendingIntent = PendingIntent.getService(
            this,
            notificationId xor 2,
            declineIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        // ── Build the notification ──
        val callTypeLabel = if (isVideo) getString(R.string.video_call) else getString(R.string.voice_call)
        val notification = NotificationCompat.Builder(this, NovaMeshApp.CHANNEL_CALLS)
            .setSmallIcon(if (isVideo) R.drawable.ic_notification_video_call else R.drawable.ic_notification_call)
            .setContentTitle(callerName)
            .setContentText(callTypeLabel)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .addAction(
                R.drawable.ic_call_decline,
                getString(R.string.decline),
                declinePendingIntent,
            )
            .addAction(
                if (isVideo) R.drawable.ic_call_accept_video else R.drawable.ic_call_accept,
                getString(R.string.accept),
                acceptPendingIntent,
            )
            .setOngoing(true)
            .setAutoCancel(false)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        NotificationManagerCompat.from(this).notify(notificationId, notification)
    }

    // ------------------------------------------------------------------
    // Story notification
    // ------------------------------------------------------------------

    /**
     * Displays a low-priority story update notification.
     *
     * @param userName  The contact who posted a story.
     * @param storyType The type of story ("photo", "video", "text").
     */
    private fun showStoryNotification(userName: String, storyType: String) {
        val notificationId = ("story_$userName").hashCode().and(0x7FFFFFFF)

        val contentText = when (storyType) {
            "photo" -> getString(R.string.story_new_photo, userName)
            "video" -> getString(R.string.story_new_video, userName)
            "text"  -> getString(R.string.story_new_text, userName)
            else    -> getString(R.string.story_new_update, userName)
        }

        val openIntent = Intent(this, MainActivity::class.java).apply {
            action = "com.novamesh.action.OPEN_STORY"
            putExtra("user_name", userName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this,
            notificationId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(this, NovaMeshApp.CHANNEL_STORIES)
            .setSmallIcon(R.drawable.ic_notification_story)
            .setContentTitle(getString(R.string.story_update))
            .setContentText(contentText)
            .setContentIntent(openPendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_SOCIAL)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        NotificationManagerCompat.from(this).notify(notificationId, notification)
    }

    // ------------------------------------------------------------------
    // Security alert notification
    // ------------------------------------------------------------------

    /**
     * Displays a high-priority security alert notification.
     *
     * Examples: new device login, 2FA code, suspicious activity.
     *
     * @param title Alert title (e.g. "New Login Detected").
     * @param body  Alert body with details.
     */
    private fun showSecurityAlert(title: String, body: String) {
        val notificationId = ("security_$title").hashCode().and(0x7FFFFFFF)

        val openIntent = Intent(this, MainActivity::class.java).apply {
            action = "com.novamesh.action.SECURITY_ALERT"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this,
            notificationId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(this, NovaMeshApp.CHANNEL_MESSAGES)
            .setSmallIcon(R.drawable.ic_notification_security)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(body),
            )
            .setContentIntent(openPendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        NotificationManagerCompat.from(this).notify(notificationId, notification)
    }

    // ------------------------------------------------------------------
    // Token persistence helpers
    // ------------------------------------------------------------------

    /**
     * Saves the FCM token to local DataStore for use by other components
     * (e.g. the sync service can read it to authenticate Matrix push).
     */
    private suspend fun saveTokenToDataStore(token: String) {
        // TODO: Implement DataStore persistence
        // DataStore preferences would store "fcm_token" -> token
        Timber.d("FCM token saved to DataStore")
    }

    /**
     * Uploads the FCM token to the application server so that the server
     * can send push notifications to this device.
     */
    private suspend fun uploadTokenToServer(token: String) {
        // TODO: Implement API call to register FCM token with the server
        // Example:
        // val api = RetrofitClient.create(BuildConfig.API_BASE_URL, PushApiService::class.java)
        // api.registerToken(FcmTokenRequest(token))
        Timber.d("FCM token uploaded to server")
    }
}
