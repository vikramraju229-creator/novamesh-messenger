/**
 * ViewModel for the ProfileScreen — user profile, settings, and preferences.
 *
 * Manages user profile data, presence status, privacy settings,
 * notification preferences, storage management, and app info.
 */
package com.novamesh.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novamesh.domain.model.Presence
import com.novamesh.domain.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** User stats card. */
data class ProfileStats(
    val messagesSent: Int = 1284,
    val snapStreak: Int = 15,
    val contacts: Int = 42,
    val storageUsed: String = "1.2 GB",
)

/** Notification settings. */
data class NotificationSettings(
    val messageSounds: Boolean = true,
    val groupSounds: Boolean = true,
    val callSounds: Boolean = true,
    val vibration: Boolean = true,
    val showPreview: Boolean = true,
    val silentHoursEnabled: Boolean = false,
    val silentStart: String = "22:00",
    val silentEnd: String = "08:00",
)

/** Privacy settings. */
data class PrivacySettings(
    val lastSeen: String = "Everyone",
    val profilePhoto: String = "Everyone",
    val readReceipts: Boolean = true,
    val typingIndicator: Boolean = true,
    val disappearingMessages: Boolean = false,
    val ghostMode: Boolean = false,
    val screenshotBlocking: Boolean = true,
)

/** UI state for the profile screen. */
data class ProfileUiState(
    val user: User = defaultUser,
    val currentPresence: Presence = Presence.ONLINE,
    val stats: ProfileStats = ProfileStats(),
    val notifications: NotificationSettings = NotificationSettings(),
    val privacy: PrivacySettings = PrivacySettings(),
    val isDarkMode: Boolean = false,
    val bubbleStyle: String = "Rounded",
    val fontSizeScale: Float = 1.0f,
    val enterToSend: Boolean = false,
    val appLockEnabled: Boolean = false,
    val twoFactorEnabled: Boolean = false,
    val encryptionFingerprint: String = "A1:B2:C3:D4:E5:F6:...",
    val appVersion: String = "1.0.0",
    val isLoading: Boolean = false,
)

private val defaultUser = User(
    id = "user_self",
    username = "novauser",
    displayName = "Nova User",
    bio = "Exploring the mesh 🌐",
    presence = Presence.ONLINE,
)

/**
 * ViewModel for the Profile/Settings screen.
 */
class ProfileViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    // ─── Presence ───────────────────────────────────────────────────────

    fun onPresenceChange(presence: Presence) {
        _uiState.update { it.copy(currentPresence = presence) }
        // TODO: Sync presence to Matrix
    }

    // ─── Display name ───────────────────────────────────────────────────

    fun onDisplayNameChange(name: String) {
        _uiState.update { it.copy(user = it.user.copy(displayName = name)) }
        // TODO: Persist + sync
    }

    // ─── Bio ────────────────────────────────────────────────────────────

    fun onBioChange(bio: String) {
        _uiState.update { it.copy(user = it.user.copy(bio = bio)) }
    }

    // ─── Dark mode ──────────────────────────────────────────────────────

    fun onDarkModeToggle(enabled: Boolean) {
        _uiState.update { it.copy(isDarkMode = enabled) }
    }

    // ─── App Lock ───────────────────────────────────────────────────────

    fun onAppLockToggle(enabled: Boolean) {
        _uiState.update { it.copy(appLockEnabled = enabled) }
    }

    // ─── Two-factor ─────────────────────────────────────────────────────

    fun onTwoFactorToggle(enabled: Boolean) {
        _uiState.update { it.copy(twoFactorEnabled = enabled) }
    }

    // ─── Notifications ──────────────────────────────────────────────────

    fun onMessageSoundsToggle(enabled: Boolean) {
        _uiState.update {
            it.copy(notifications = it.notifications.copy(messageSounds = enabled))
        }
    }

    fun onGroupSoundsToggle(enabled: Boolean) {
        _uiState.update {
            it.copy(notifications = it.notifications.copy(groupSounds = enabled))
        }
    }

    fun onCallSoundsToggle(enabled: Boolean) {
        _uiState.update {
            it.copy(notifications = it.notifications.copy(callSounds = enabled))
        }
    }

    fun onVibrationToggle(enabled: Boolean) {
        _uiState.update {
            it.copy(notifications = it.notifications.copy(vibration = enabled))
        }
    }

    fun onShowPreviewToggle(enabled: Boolean) {
        _uiState.update {
            it.copy(notifications = it.notifications.copy(showPreview = enabled))
        }
    }

    fun onSilentHoursToggle(enabled: Boolean) {
        _uiState.update {
            it.copy(notifications = it.notifications.copy(silentHoursEnabled = enabled))
        }
    }

    // ─── Privacy ────────────────────────────────────────────────────────

    fun onReadReceiptsToggle(enabled: Boolean) {
        _uiState.update {
            it.copy(privacy = it.privacy.copy(readReceipts = enabled))
        }
    }

    fun onTypingIndicatorToggle(enabled: Boolean) {
        _uiState.update {
            it.copy(privacy = it.privacy.copy(typingIndicator = enabled))
        }
    }

    fun onGhostModeToggle(enabled: Boolean) {
        _uiState.update {
            it.copy(privacy = it.privacy.copy(ghostMode = enabled))
        }
    }

    fun onDisappearingMessagesToggle(enabled: Boolean) {
        _uiState.update {
            it.copy(privacy = it.privacy.copy(disappearingMessages = enabled))
        }
    }

    // ─── Storage / Cache ────────────────────────────────────────────────

    fun onClearCache() {
        viewModelScope.launch {
            // TODO: Clear image cache, temp files, etc.
            _uiState.update { it.copy(stats = it.stats.copy(storageUsed = "0 B")) }
        }
    }

    // ─── Logout ─────────────────────────────────────────────────────────

    fun onLogout() {
        viewModelScope.launch {
            // TODO: Clear session, disconnect Matrix, wipe local DB
        }
    }
}
