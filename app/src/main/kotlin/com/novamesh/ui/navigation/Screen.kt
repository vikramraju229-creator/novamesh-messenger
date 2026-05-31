package com.novamesh.ui.navigation

import kotlinx.serialization.Serializable

/**
 * Navigation route definitions for NovaMesh Messenger.
 *
 * Uses type-safe navigation with Kotlin Serialization.
 */
sealed interface Screen {

    // ─── Bottom nav tabs ───
    @Serializable data object Chats : Screen
    @Serializable data object Stories : Screen
    @Serializable data object Discover : Screen
    @Serializable data object Profile : Screen

    // ─── Chat details ───
    @Serializable data class ChatDetail(val chatId: String, val chatName: String = "") : Screen

    // ─── Camera / Snap ───
    @Serializable data object Camera : Screen
    @Serializable data class SnapPreview(val mediaUri: String, val isVideo: Boolean = false) : Screen

    // ─── Story viewer ───
    @Serializable data class StoryViewer(val userId: String, val storyId: String) : Screen

    // ─── Calls ───
    @Serializable data class IncomingCall(val callId: String, val callerId: String, val callerName: String) : Screen
    @Serializable data class ActiveCall(val callId: String, val isVideo: Boolean = false) : Screen

    // ─── Settings / Onboarding ───
    @Serializable data object Settings : Screen
    @Serializable data object Onboarding : Screen
    @Serializable data object SecuritySettings : Screen
    @Serializable data object AppLock : Screen
    @Serializable data object TwoFactorSetup : Screen

    // ─── Profile / Contact ───
    @Serializable data class ContactProfile(val userId: String) : Screen
    @Serializable data class GroupInfo(val groupId: String) : Screen

    // ─── Media viewer ───
    @Serializable data class MediaViewer(val mediaUri: String, val mimeType: String = "image/*") : Screen

    // ─── QR Scanner ───
    @Serializable data object QrScanner : Screen
}
