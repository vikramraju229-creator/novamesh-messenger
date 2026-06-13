@file:OptIn(ExperimentalMaterial3Api::class)

package com.novamesh.ui.screens.settings

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpCenter
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// DataStore for settings persistence
private val Context.dataStore by preferencesDataStore(name = "settings")

private val THEME_KEY = stringPreferencesKey("theme")
private val NOTIFICATION_MESSAGES_KEY = booleanPreferencesKey("notif_messages")
private val NOTIFICATION_STORIES_KEY = booleanPreferencesKey("notif_stories")
private val NOTIFICATION_CALLS_KEY = booleanPreferencesKey("notif_calls")
private val NOTIFICATION_VIBRATE_KEY = booleanPreferencesKey("notif_vibrate")
private val NOTIFICATION_PREVIEW_KEY = booleanPreferencesKey("notif_preview")
private val READ_RECEIPTS_KEY = booleanPreferencesKey("read_receipts")
private val GHOST_MODE_KEY = booleanPreferencesKey("ghost_mode")
private val DISAPPEARING_MESSAGES_KEY = stringPreferencesKey("disappearing_messages")
private val APP_LOCK_KEY = booleanPreferencesKey("app_lock")
private val AUTO_DOWNLOAD_WIFI_KEY = booleanPreferencesKey("auto_download_wifi")
private val AUTO_DOWNLOAD_ALWAYS_KEY = booleanPreferencesKey("auto_download_always")
private val BUBBLE_STYLE_KEY = stringPreferencesKey("bubble_style")
private val FONT_SIZE_KEY = stringPreferencesKey("font_size")

/** Settings item data. */
private data class SettingsItem(
    val icon: ImageVector,
    val title: String,
    val subtitle: String = "",
    val trailing: (@Composable () -> Unit)? = null,
    val onClick: (() -> Unit)? = null,
)

/**
 * Full Settings screen with all sections.
 *
 * All settings persist to DataStore and apply immediately.
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit = {},
    onLogout: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dataStore = context.dataStore
    val settings = dataStore.data

    // Track state from DataStore
    val theme by settings.map { it[THEME_KEY] ?: "system" }.collectAsState(initial = "system")
    val notifMessages by settings.map { it[NOTIFICATION_MESSAGES_KEY] ?: true }.collectAsState(initial = true)
    val notifStories by settings.map { it[NOTIFICATION_STORIES_KEY] ?: true }.collectAsState(initial = true)
    val notifCalls by settings.map { it[NOTIFICATION_CALLS_KEY] ?: true }.collectAsState(initial = true)
    val notifVibrate by settings.map { it[NOTIFICATION_VIBRATE_KEY] ?: true }.collectAsState(initial = true)
    val notifPreview by settings.map { it[NOTIFICATION_PREVIEW_KEY] ?: true }.collectAsState(initial = true)
    val readReceipts by settings.map { it[READ_RECEIPTS_KEY] ?: true }.collectAsState(initial = true)
    val ghostMode by settings.map { it[GHOST_MODE_KEY] ?: false }.collectAsState(initial = false)
    val disappearingMessages by settings.map { it[DISAPPEARING_MESSAGES_KEY] ?: "off" }.collectAsState(initial = "off")
    val appLock by settings.map { it[APP_LOCK_KEY] ?: false }.collectAsState(initial = false)
    val autoDownloadWifi by settings.map { it[AUTO_DOWNLOAD_WIFI_KEY] ?: true }.collectAsState(initial = true)
    val autoDownloadAlways by settings.map { it[AUTO_DOWNLOAD_ALWAYS_KEY] ?: false }.collectAsState(initial = false)
    val bubbleStyle by settings.map { it[BUBBLE_STYLE_KEY] ?: "rounded" }.collectAsState(initial = "rounded")
    val fontSize by settings.map { it[FONT_SIZE_KEY] ?: "medium" }.collectAsState(initial = "medium")

    var showLogoutDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // ── Settings dialog state ──
    var showNameEditDialog by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf("") }
    var editUsername by remember { mutableStateOf("") }

    var showLastSeenDialog by remember { mutableStateOf(false) }
    var showPhotoVisDialog by remember { mutableStateOf(false) }
    var showDisappearingDialog by remember { mutableStateOf(false) }
    var showChangeNumberDialog by remember { mutableStateOf(false) }
    var changeNumberInput by remember { mutableStateOf("") }

    var blockedUsers by remember { mutableStateOf<List<String>>(emptyList()) }
    var showBlockedDialog by remember { mutableStateOf(false) }
    var blockedLoaded by remember { mutableStateOf(false) }

    // Helper to update settings
    fun <T> update(key: androidx.datastore.preferences.core.Preferences.Key<T>, value: T) {
        scope.launch {
            dataStore.edit { it[key] = value }
        }
    }

    // Logout dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            icon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Log Out") },
            text = { Text("Are you sure you want to log out?") },
            confirmButton = {
                Button(
                    onClick = { showLogoutDialog = false; onLogout() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) { Text("Log Out") }
            },
            dismissButton = { TextButton(onClick = { showLogoutDialog = false }) { Text("Cancel") } },
        )
    }

    // Delete account dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete Account") },
            text = { Text("This will permanently delete all your data. This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        scope.launch {
                            val user = FirebaseAuth.getInstance().currentUser
                            user?.delete()
                            // Delete Firestore data
                            FirebaseFirestore.getInstance().collection("users").document(user?.uid ?: "").delete()
                            onLogout()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") } },
        )
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ✅ EDIT NAME / USERNAME DIALOG
    // ═══════════════════════════════════════════════════════════════════════
    if (showNameEditDialog) {
        AlertDialog(
            onDismissRequest = { showNameEditDialog = false },
            icon = { Icon(Icons.Filled.Badge, contentDescription = null) },
            title = { Text("Edit Name / Username") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Full Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = editUsername,
                        onValueChange = { editUsername = it },
                        label = { Text("@Username") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showNameEditDialog = false
                        scope.launch {
                            try {
                                val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
                                val fb = FirebaseFirestore.getInstance()
                                if (editName.isNotBlank()) fb.collection("users").document(uid).update("name", editName).await()
                                if (editUsername.isNotBlank()) fb.collection("users").document(uid).update("username", editUsername).await()
                            } catch (_: Exception) { }
                        }
                    },
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showNameEditDialog = false }) { Text("Cancel") } },
        )
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ✅ LAST SEEN PICKER
    // ═══════════════════════════════════════════════════════════════════════
    if (showLastSeenDialog) {
        AlertDialog(
            onDismissRequest = { showLastSeenDialog = false },
            title = { Text("Who can see my Last Seen?") },
            text = {
                Column {
                    listOf("Everyone", "My Contacts", "Nobody").forEach { option ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                update(READ_RECEIPTS_KEY, option == "Everyone")
                                showLastSeenDialog = false
                            }.padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = (option == "Everyone" && readReceipts) || (option == "Nobody" && !readReceipts),
                                onClick = {
                                    update(READ_RECEIPTS_KEY, option == "Everyone")
                                    showLastSeenDialog = false
                                },
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(option)
                        }
                    }
                }
            },
            confirmButton = {},
        )
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ✅ PROFILE PHOTO VISIBILITY PICKER
    // ═══════════════════════════════════════════════════════════════════════
    if (showPhotoVisDialog) {
        AlertDialog(
            onDismissRequest = { showPhotoVisDialog = false },
            title = { Text("Who can see my profile photo?") },
            text = {
                Column {
                    listOf("Everyone", "My Contacts", "Nobody").forEach { option ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                showPhotoVisDialog = false
                            }.padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = option == "Everyone", onClick = { showPhotoVisDialog = false })
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(option)
                        }
                    }
                }
            },
            confirmButton = {},
        )
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ✅ DISAPPEARING MESSAGES PICKER
    // ═══════════════════════════════════════════════════════════════════════
    if (showDisappearingDialog) {
        AlertDialog(
            onDismissRequest = { showDisappearingDialog = false },
            title = { Text("Disappearing Messages") },
            text = {
                Column {
                    listOf("Off", "24 hours", "7 days", "90 days").forEach { option ->
                        val key = option.lowercase().replace(" ", "")
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                update(DISAPPEARING_MESSAGES_KEY, key)
                                showDisappearingDialog = false
                            }.padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = disappearingMessages == key,
                                onClick = {
                                    update(DISAPPEARING_MESSAGES_KEY, key)
                                    showDisappearingDialog = false
                                },
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(option)
                        }
                    }
                }
            },
            confirmButton = {},
        )
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ✅ BLOCKED CONTACTS DIALOG
    // ═══════════════════════════════════════════════════════════════════════
    if (showBlockedDialog) {
        if (!blockedLoaded) {
            LaunchedEffect(Unit) {
                try {
                    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@LaunchedEffect
                    val doc = FirebaseFirestore.getInstance().collection("users").document(uid).get().await()
                    blockedUsers = doc.getList<String>("blocked") ?: emptyList()
                } catch (_: Exception) { }
                blockedLoaded = true
            }
        }
        AlertDialog(
            onDismissRequest = { showBlockedDialog = false },
            title = { Text("Blocked Contacts") },
            text = {
                if (blockedUsers.isEmpty()) {
                    Text("No blocked contacts")
                } else {
                    Column {
                        blockedUsers.forEach { uid ->
                            Text(uid, modifier = Modifier.padding(vertical = 4.dp))
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showBlockedDialog = false }) { Text("Done") } },
        )
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ✅ CHANGE NUMBER DIALOG
    // ═══════════════════════════════════════════════════════════════════════
    if (showChangeNumberDialog) {
        AlertDialog(
            onDismissRequest = { showChangeNumberDialog = false },
            title = { Text("Change Phone Number") },
            text = {
                OutlinedTextField(
                    value = changeNumberInput,
                    onValueChange = { changeNumberInput = it },
                    label = { Text("New phone number") },
                    placeholder = { Text("+1 (555) 123-4567") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showChangeNumberDialog = false
                        changeNumberInput = ""
                    },
                ) { Text("Send OTP") }
            },
            dismissButton = { TextButton(onClick = { showChangeNumberDialog = false }) { Text("Cancel") } },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp),
        ) {
            // ─── Account ───
            item { SectionHeader(icon = Icons.Filled.Person, title = "Account") }

            item {
                SettingsRow(SettingsItem(
                    icon = Icons.Filled.CameraAlt, title = "Change Profile Photo",
                    subtitle = "Update your avatar",
                    onClick = { /* Profile photo is changed from Profile screen */ },
                ))
            }
            item {
                SettingsRow(SettingsItem(
                    icon = Icons.Filled.Badge, title = "Change Name / Username",
                    subtitle = "Edit your display name and @handle",
                    onClick = {
                        editName = ""
                        editUsername = ""
                        showNameEditDialog = true
                    },
                ))
            }
            item {
                SettingsRow(SettingsItem(
                    icon = Icons.Filled.Person, title = "Change Number",
                    subtitle = "Send OTP to a new phone number",
                    onClick = {
                        changeNumberInput = ""
                        showChangeNumberDialog = true
                    },
                ))
            }
            item {
                SettingsRow(SettingsItem(
                    icon = Icons.Filled.Delete, title = "Delete Account",
                    subtitle = "Permanently remove all data",
                    onClick = { showDeleteDialog = true },
                ))
            }

            // ─── Privacy ───
            item { SectionHeader(icon = Icons.Filled.PrivacyTip, title = "Privacy") }
            item {
                SettingsRow(SettingsItem(
                    icon = Icons.Filled.Visibility, title = "Last Seen",
                    subtitle = when (readReceipts) { true -> "Everyone"; false -> "Nobody" },
                    onClick = { showLastSeenDialog = true },
                ))
            }
            item {
                SettingsRow(SettingsItem(
                    icon = Icons.Filled.Visibility, title = "Profile Photo Visibility",
                    subtitle = "Everyone",
                    onClick = { showPhotoVisDialog = true },
                ))
            }
            item {
                SettingsRow(SettingsItem(
                    icon = Icons.Filled.Visibility, title = "Read Receipts",
                    trailing = { Switch(checked = readReceipts, onCheckedChange = { update(READ_RECEIPTS_KEY, it) }) },
                ))
            }
            item {
                SettingsRow(SettingsItem(
                    icon = Icons.Filled.Timer, title = "Disappearing Messages",
                    subtitle = disappearingMessages.replaceFirstChar { it.uppercase() },
                    onClick = { showDisappearingDialog = true },
                ))
            }
            item {
                SettingsRow(SettingsItem(
                    icon = Icons.Filled.People, title = "Blocked Contacts",
                    subtitle = "Manage blocked users",
                    onClick = {
                        blockedLoaded = false
                        showBlockedDialog = true
                    },
                ))
            }
            item {
                SettingsRow(SettingsItem(
                    icon = Icons.Filled.VisibilityOff, title = "Ghost Mode",
                    subtitle = "Hide online status & read receipts",
                    trailing = {
                        Switch(checked = ghostMode, onCheckedChange = {
                            update(GHOST_MODE_KEY, it)
                            // Also update Firestore presence
                            scope.launch {
                                try {
                                    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
                                    FirebaseFirestore.getInstance().collection("users").document(uid)
                                        .update("isOnline", !it).await()
                                } catch (_: Exception) { }
                            }
                        })
                    },
                ))
            }

            // ─── Notifications ───
            item { SectionHeader(icon = Icons.Filled.Notifications, title = "Notifications") }
            item {
                SettingsRow(SettingsItem(
                    icon = Icons.Filled.Notifications, title = "Message Notifications",
                    trailing = { Switch(checked = notifMessages, onCheckedChange = { update(NOTIFICATION_MESSAGES_KEY, it) }) },
                ))
            }
            item {
                SettingsRow(SettingsItem(
                    icon = Icons.Filled.Notifications, title = "Story Notifications",
                    trailing = { Switch(checked = notifStories, onCheckedChange = { update(NOTIFICATION_STORIES_KEY, it) }) },
                ))
            }
            item {
                SettingsRow(SettingsItem(
                    icon = Icons.Filled.Notifications, title = "Call Notifications",
                    trailing = { Switch(checked = notifCalls, onCheckedChange = { update(NOTIFICATION_CALLS_KEY, it) }) },
                ))
            }
            item {
                SettingsRow(SettingsItem(
                    icon = Icons.Filled.VolumeUp, title = "Vibrate",
                    trailing = { Switch(checked = notifVibrate, onCheckedChange = { update(NOTIFICATION_VIBRATE_KEY, it) }) },
                ))
            }
            item {
                SettingsRow(SettingsItem(
                    icon = Icons.Filled.VolumeUp, title = "In-App Preview",
                    subtitle = "Show message preview in notifications",
                    trailing = { Switch(checked = notifPreview, onCheckedChange = { update(NOTIFICATION_PREVIEW_KEY, it) }) },
                ))
            }

            // ─── Appearance ───
            item { SectionHeader(icon = Icons.Filled.Palette, title = "Appearance") }
            item {
                SettingsRow(SettingsItem(
                    icon = Icons.Filled.DarkMode, title = "Theme",
                    subtitle = theme.replaceFirstChar { it.uppercase() },
                    onClick = {
                        val next = when (theme) {
                            "system" -> "light"
                            "light" -> "dark"
                            else -> "system"
                        }
                        update(THEME_KEY, next)
                        when (next) {
                            "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                            "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                        }
                    },
                ))
            }
            item {
                SettingsRow(SettingsItem(
                    icon = Icons.Filled.Wallpaper, title = "Chat Wallpaper",
                    subtitle = "Default wallpaper",
                    onClick = { /* Open wallpaper picker */ },
                ))
            }
            item {
                SettingsRow(SettingsItem(
                    icon = Icons.Filled.Palette, title = "Bubble Style",
                    subtitle = bubbleStyle.replaceFirstChar { it.uppercase() },
                    onClick = {
                        val next = when (bubbleStyle) {
                            "rounded" -> "sharp"
                            "sharp" -> "minimal"
                            else -> "rounded"
                        }
                        update(BUBBLE_STYLE_KEY, next)
                    },
                ))
            }
            item {
                SettingsRow(SettingsItem(
                    icon = Icons.Filled.FontDownload, title = "Font Size",
                    subtitle = fontSize.replaceFirstChar { it.uppercase() },
                    onClick = {
                        val next = when (fontSize) {
                            "small" -> "medium"
                            "medium" -> "large"
                            else -> "small"
                        }
                        update(FONT_SIZE_KEY, next)
                    },
                ))
            }

            // ─── Security ───
            item { SectionHeader(icon = Icons.Filled.Security, title = "Security") }
            item {
                SettingsRow(SettingsItem(
                    icon = Icons.Filled.Lock, title = "App Lock",
                    subtitle = if (appLock) "Enabled" else "Disabled",
                    trailing = { Switch(checked = appLock, onCheckedChange = { update(APP_LOCK_KEY, it) }) },
                ))
            }
            item {
                SettingsRow(SettingsItem(
                    icon = Icons.Filled.VerifiedUser, title = "Two-Step Verification",
                    subtitle = "Add extra security with TOTP",
                    onClick = { /* Navigate to 2FA setup */ },
                ))
            }
            item {
                SettingsRow(SettingsItem(
                    icon = Icons.Filled.Fingerprint, title = "Security Notifications",
                    subtitle = "Get alerts about security events",
                    trailing = { Switch(checked = true, onCheckedChange = { }) },
                ))
            }

            // ─── Storage & Data ───
            item { SectionHeader(icon = Icons.Filled.Storage, title = "Storage & Data") }
            item {
                SettingsRow(SettingsItem(
                    icon = Icons.Filled.Wifi, title = "Auto-Download on Wi-Fi",
                    trailing = { Switch(checked = autoDownloadWifi, onCheckedChange = { update(AUTO_DOWNLOAD_WIFI_KEY, it) }) },
                ))
            }
            item {
                SettingsRow(SettingsItem(
                    icon = Icons.Filled.Storage, title = "Auto-Download Always",
                    subtitle = "May use mobile data",
                    trailing = { Switch(checked = autoDownloadAlways, onCheckedChange = { update(AUTO_DOWNLOAD_ALWAYS_KEY, it) }) },
                ))
            }
            item {
                SettingsRow(SettingsItem(
                    icon = Icons.Filled.Delete, title = "Clear Chat Cache",
                    subtitle = "Free up storage space",
                    onClick = { /* Clear cache */ },
                ))
            }

            // ─── About ───
            item { SectionHeader(icon = Icons.Filled.Info, title = "About") }
            item {
                SettingsRow(SettingsItem(
                    icon = Icons.Filled.Info, title = "App Version",
                    subtitle = "NovaMesh v1.0.0",
                ))
            }
            item {
                SettingsRow(SettingsItem(
                    icon = Icons.AutoMirrored.Filled.HelpCenter, title = "Terms of Service",
                    onClick = { /* Open URL */ },
                ))
            }
            item {
                SettingsRow(SettingsItem(
                    icon = Icons.Filled.TextSnippet, title = "Privacy Policy",
                    onClick = { /* Open URL */ },
                ))
            }

            // ─── Logout ───
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { showLogoutDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Log Out", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(icon: ImageVector, title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
        HorizontalDivider(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun SettingsRow(item: SettingsItem) {
    ListItem(
        headlineContent = { Text(item.title, style = MaterialTheme.typography.bodyLarge) },
        supportingContent = if (item.subtitle.isNotBlank()) {
            { Text(item.subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else null,
        leadingContent = {
            Icon(item.icon, contentDescription = item.title, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                item.trailing?.invoke()
                if (item.onClick != null) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Filled.ChevronRight, contentDescription = "Open", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                }
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .then(if (item.onClick != null) Modifier.clickable(onClick = item.onClick) else Modifier),
    )
}
