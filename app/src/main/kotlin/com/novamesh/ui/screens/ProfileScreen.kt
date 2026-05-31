/**
 * ProfileScreen — the user's own profile and settings tab.
 *
 * This screen serves as both a profile viewer and a settings hub. It is
 * organised into collapsible / grouped sections:
 *
 * - **Profile Header**: large avatar, display name, @handle, bio (tap to edit)
 * - **Status**: presence selector (Online, Away, Busy, Invisible)
 * - **Stats**: cards showing mock aggregate numbers (messages sent, streak,
 *   contacts, storage)
 * - **Security**: app lock, 2FA, encryption key fingerprint, privacy toggles
 * - **Chats**: wallpaper, bubble style, font size slider, enter-to-send
 * - **Notifications**: sounds, silent hours, per-contact settings
 * - **Storage**: clear cache, auto-download rules, usage breakdown
 * - **About**: version, help, policies, open-source licenses, logout button
 *
 * All data is mocked for prototyping. In production a ViewModel would
 * observe preferences and user data from the repository layer.
 */

@file:OptIn(ExperimentalMaterial3Api::class)

package com.novamesh.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpCenter
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Cached
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.HorizontalSplit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardReturn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.DataSaverOn
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.QuestionAnswer
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novamesh.ui.theme.NovaError
import com.novamesh.ui.theme.NovaPrimary
import com.novamesh.ui.theme.NovaSuccess
import com.novamesh.ui.theme.NovaWarning

// ═════════════════════════════════════════════════════════════════════════════
// Public composable — entry point for the NavGraph
// ═════════════════════════════════════════════════════════════════════════════

/**
 * User profile and settings screen.
 *
 * Displays the current user's avatar, display name, bio, status presence,
 * usage statistics, and a comprehensive set of setting groups covering
 * security, chats, notifications, storage, and about/account actions.
 *
 * @param onBack Navigate back to the previous screen.
 * @param onLogout Invoked when the user confirms logout via the dialog.
 */
@Composable
fun ProfileScreen(
  onBack: () -> Unit = {},
  onLogout: () -> Unit = {},
) {
  // ─── State ───────────────────────────────────────────────────────────────
  var selectedStatus by remember { mutableStateOf(PresenceOption.ONLINE) }
  var showLogoutDialog by remember { mutableStateOf(false) }

  // Security toggles
  var appLockEnabled by remember { mutableStateOf(false) }
  var appLockTimerMinutes by remember { mutableStateOf(5) }
  var twoFactorEnabled by remember { mutableStateOf(false) }
  var profilePhotoPrivate by remember { mutableStateOf(false) }
  var lastSeenVisible by remember { mutableStateOf(true) }
  var readReceiptsEnabled by remember { mutableStateOf(true) }

  // Chats settings
  var selectedBubbleStyle by remember { mutableStateOf(BubbleStyle.ROUNDED) }
  var fontSize by remember { mutableFloatStateOf(16f) }
  var enterKeySends by remember { mutableStateOf(false) }

  // Notifications
  var notificationSounds by remember { mutableStateOf(true) }
  var silentHoursEnabled by remember { mutableStateOf(false) }

  // Storage
  var autoDownloadWifi by remember { mutableStateOf(true) }
  var autoDownloadCellular by remember { mutableStateOf(false) }
  var autoDownloadRoaming by remember { mutableStateOf(false) }

  // ─── Settings section data ───────────────────────────────────────────────

  val securityItems = listOf(
    SettingsItem(
      icon = Icons.Filled.Lock,
      title = "App Lock",
      subtitle = if (appLockEnabled) "Lock after $appLockTimerMinutes min" else "Off",
      trailing = { SettingsSwitch(checked = appLockEnabled, onCheck = { appLockEnabled = it }) },
    ),
    SettingsItem(
      icon = Icons.Filled.VerifiedUser,
      title = "Two-Factor Authentication",
      subtitle = if (twoFactorEnabled) "Enabled" else "Set up for extra security",
      trailing = {
        FilledTonalButton(
          onClick = { twoFactorEnabled = !twoFactorEnabled },
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
        ) {
          Text(
            text = if (twoFactorEnabled) "Disable" else "Setup",
            style = MaterialTheme.typography.labelMedium,
          )
        }
      },
    ),
    SettingsItem(
      icon = Icons.Filled.Fingerprint,
      title = "Encryption Key",
      subtitle = "A1B2 C3D4 E5F6 G7H8 I9J0",
      trailing = {
        IconButton(onClick = { /* TODO: copy key fingerprint */ }) {
          Icon(
            imageVector = Icons.Filled.ContentCopy,
            contentDescription = "Copy key fingerprint",
            modifier = Modifier.size(20.dp),
          )
        }
      },
    ),
    SettingsItem(
      icon = Icons.Filled.PrivacyTip,
      title = "Privacy Settings",
      subtitle = "Profile photo, last seen, read receipts",
      onClick = { /* TODO: navigate to privacy sub-screen */ },
    ),
  )

  val chatItems = listOf(
    SettingsItem(
      icon = Icons.Filled.Wallpaper,
      title = "Chat Wallpaper",
      subtitle = "Default wallpaper",
      onClick = { /* TODO: open wallpaper picker */ },
    ),
    SettingsItem(
      icon = Icons.Filled.Palette,
      title = "Bubble Style",
      subtitle = selectedBubbleStyle.label,
      trailing = { BubbleStyleSelector(selected = selectedBubbleStyle, onSelect = { selectedBubbleStyle = it }) },
    ),
    SettingsItem(
      icon = Icons.Filled.FontDownload,
      title = "Font Size",
      subtitle = "${fontSize.toInt()}sp",
      trailing = {
        Slider(
          value = fontSize,
          onValueChange = { fontSize = it },
          valueRange = 12f..24f,
          steps = 11,
          modifier = Modifier.width(140.dp),
        )
      },
    ),
    SettingsItem(
      icon = Icons.Filled.KeyboardReturn,
      title = "Enter Key Sends",
      subtitle = if (enterKeySends) "Enabled" else "Disabled",
      trailing = { SettingsSwitch(checked = enterKeySends, onCheck = { enterKeySends = it }) },
    ),
  )

  val notificationItems = listOf(
    SettingsItem(
      icon = Icons.Filled.VolumeUp,
      title = "Notification Sounds",
      subtitle = "Message & group sounds",
      trailing = { SettingsSwitch(checked = notificationSounds, onCheck = { notificationSounds = it }) },
    ),
    SettingsItem(
      icon = Icons.Filled.AccessTime,
      title = "Silent Hours",
      subtitle = if (silentHoursEnabled) "10:00 PM — 7:00 AM" else "Off",
      trailing = { SettingsSwitch(checked = silentHoursEnabled, onCheck = { silentHoursEnabled = it }) },
    ),
    SettingsItem(
      icon = Icons.Filled.Notifications,
      title = "Per-Contact Settings",
      subtitle = "Customise notifications per chat",
      onClick = { /* TODO: navigate to per-contact settings */ },
    ),
  )

  val storageItems = listOf(
    SettingsItem(
      icon = Icons.Filled.Delete,
      title = "Clear Cache",
      subtitle = "256.3 MB",
      trailing = {
        FilledTonalButton(
          onClick = { /* TODO: clear cache */ },
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
        ) {
          Text(
            text = "Clear",
            style = MaterialTheme.typography.labelMedium,
          )
        }
      },
    ),
    SettingsItem(
      icon = Icons.Filled.Wifi,
      title = "Auto-Download on Wi-Fi",
      trailing = { SettingsSwitch(checked = autoDownloadWifi, onCheck = { autoDownloadWifi = it }) },
    ),
    SettingsItem(
      icon = Icons.Filled.Circle, // cellular network icon (approximate)
      title = "Auto-Download on Cellular",
      subtitle = "May use mobile data",
      trailing = { SettingsSwitch(checked = autoDownloadCellular, onCheck = { autoDownloadCellular = it }) },
    ),
    SettingsItem(
      icon = Icons.Filled.MoreHoriz,
      title = "Auto-Download on Roaming",
      subtitle = "Not recommended",
      trailing = { SettingsSwitch(checked = autoDownloadRoaming, onCheck = { autoDownloadRoaming = it }) },
    ),
    SettingsItem(
      icon = Icons.Filled.Storage,
      title = "Storage Usage Breakdown",
      subtitle = "Chats: 1.2 GB · Media: 3.4 GB · Cache: 256 MB",
      onClick = { /* TODO: show breakdown detail */ },
    ),
  )

  val aboutItems = listOf(
    SettingsItem(
      icon = Icons.Filled.Info,
      title = "Version",
      subtitle = "NovaMesh v1.0.0 (Build 42)",
    ),
    SettingsItem(
      icon = Icons.AutoMirrored.Filled.HelpCenter,
      title = "Help Center",
      onClick = { /* TODO: open help URL */ },
    ),
    SettingsItem(
      icon = Icons.Filled.Description,
      title = "Privacy Policy",
      onClick = { /* TODO: open privacy URL */ },
    ),
    SettingsItem(
      icon = Icons.Filled.TextSnippet,
      title = "Terms of Service",
      onClick = { /* TODO: open terms URL */ },
    ),
    SettingsItem(
      icon = Icons.Filled.ShoppingCart,
      title = "Open Source Licenses",
      onClick = { /* TODO: open licenses screen */ },
    ),
  )

  // ─── Logout confirmation dialog ──────────────────────────────────────────

  if (showLogoutDialog) {
    AlertDialog(
      onDismissRequest = { showLogoutDialog = false },
      icon = {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.Logout,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.error,
          modifier = Modifier.size(32.dp),
        )
      },
      title = {
        Text(
          text = "Log Out",
          style = MaterialTheme.typography.headlineSmall,
        )
      },
      text = {
        Text(
          text = "Are you sure you want to log out? Your messages will still " +
            "be stored on this device, but you will need to sign in again " +
            "to access your account.",
          style = MaterialTheme.typography.bodyMedium,
        )
      },
      confirmButton = {
        Button(
          onClick = {
            showLogoutDialog = false
            onLogout()
          },
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError,
          ),
        ) {
          Text("Log Out")
        }
      },
      dismissButton = {
        TextButton(onClick = { showLogoutDialog = false }) {
          Text("Cancel")
        }
      },
    )
  }

  // ═════════════════════════════════════════════════════════════════════════
  // Screen scaffold
  // ═════════════════════════════════════════════════════════════════════════

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = "Profile",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
          )
        },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back",
            )
          }
        },
        actions = {
          IconButton(onClick = { /* TODO: open settings search */ }) {
            Icon(
              imageVector = Icons.Filled.Settings,
              contentDescription = "Settings",
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface,
        ),
      )
    },
    modifier = Modifier.fillMaxSize(),
  ) { paddingValues ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues),
      contentPadding = PaddingValues(bottom = 32.dp),
    ) {
      // ── Profile Header ─────────────────────────────────────────────────
      item(key = "profile_header") {
        ProfileHeader(
          displayName = "Alex Nova",
          username = "alexnova",
          bio = "Building the future of decentralised messaging. 🚀\n" +
            "Photography | Open source | Coffee addict",
          avatarUri = null,
          onEditAvatar = { /* TODO: open avatar picker */ },
          onEditName = { /* TODO: open name editor */ },
          onEditBio = { /* TODO: open bio editor */ },
        )
      }

      // ── Status Selector ─────────────────────────────────────────────────
      item(key = "status_section") {
        StatusSelectorSection(
          selectedStatus = selectedStatus,
          onStatusSelected = { selectedStatus = it },
        )
      }

      // ── Stats Cards ────────────────────────────────────────────────────
      item(key = "stats_section") {
        StatsSection(
          stats = mockProfileStats(),
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
      }

      // ── Security Group ─────────────────────────────────────────────────
      item(key = "security_header") {
        SettingsGroupHeader(
          icon = Icons.Filled.Security,
          title = "Security",
        )
      }
      items(items = securityItems, key = { "security_${it.title}" }) { item ->
        SettingsRow(item = item)
      }

      // ── Chats Group ────────────────────────────────────────────────────
      item(key = "chats_divider") {
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
      }
      item(key = "chats_header") {
        SettingsGroupHeader(
          icon = Icons.AutoMirrored.Filled.Send,
          title = "Chats",
        )
      }
      items(items = chatItems, key = { "chats_${it.title}" }) { item ->
        SettingsRow(item = item)
      }

      // ── Notifications Group ────────────────────────────────────────────
      item(key = "notifications_divider") {
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
      }
      item(key = "notifications_header") {
        SettingsGroupHeader(
          icon = Icons.Filled.Notifications,
          title = "Notifications",
        )
      }
      items(items = notificationItems, key = { "notifications_${it.title}" }) { item ->
        SettingsRow(item = item)
      }

      // ── Storage Group ──────────────────────────────────────────────────
      item(key = "storage_divider") {
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
      }
      item(key = "storage_header") {
        SettingsGroupHeader(
          icon = Icons.Filled.Storage,
          title = "Storage",
        )
      }
      items(items = storageItems, key = { "storage_${it.title}" }) { item ->
        SettingsRow(item = item)
      }

      // ── About Group ────────────────────────────────────────────────────
      item(key = "about_divider") {
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
      }
      item(key = "about_header") {
        SettingsGroupHeader(
          icon = Icons.Filled.Info,
          title = "About",
        )
      }
      items(items = aboutItems, key = { "about_${it.title}" }) { item ->
        SettingsRow(item = item)
      }

      // ── Logout Button ──────────────────────────────────────────────────
      item(key = "logout_button") {
        LogoutButton(
          onClick = { showLogoutDialog = true },
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
        )
      }
    }
  }
}

// ═════════════════════════════════════════════════════════════════════════════
// Profile header
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Large profile header with avatar, display name, username, and bio.
 *
 * The avatar, name, and bio are all tappable to trigger an edit flow.
 *
 * @param displayName The user's shown name.
 * @param username The @handle.
 * @param bio Short about-me text (may include line breaks).
 * @param avatarUri Optional URI for the avatar image; falls back to initial.
 * @param onEditAvatar Invoked when the avatar area is tapped.
 * @param onEditName Invoked when the display name is tapped.
 * @param onEditBio Invoked when the bio text is tapped.
 */
@Composable
private fun ProfileHeader(
  displayName: String,
  username: String,
  bio: String,
  avatarUri: String?,
  onEditAvatar: () -> Unit,
  onEditName: () -> Unit,
  onEditBio: () -> Unit,
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(top = 16.dp, bottom = 8.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    // ── Avatar ───────────────────────────────────────────────────────
    Box(
      modifier = Modifier
        .size(80.dp)
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.primaryContainer)
        .clickable(onClick = onEditAvatar),
      contentAlignment = Alignment.Center,
    ) {
      if (avatarUri != null) {
        // In production use AsyncImage from Coil
        Text(
          text = displayName.firstOrNull()?.uppercase() ?: "?",
          style = MaterialTheme.typography.displaySmall,
          color = MaterialTheme.colorScheme.onPrimaryContainer,
          fontWeight = FontWeight.Bold,
        )
      } else {
        Text(
          text = displayName.firstOrNull()?.uppercase() ?: "?",
          style = MaterialTheme.typography.displaySmall,
          color = MaterialTheme.colorScheme.onPrimaryContainer,
          fontWeight = FontWeight.Bold,
        )
      }

      // Edit icon overlay
      Icon(
        imageVector = Icons.Filled.CameraAlt,
        contentDescription = "Change profile photo",
        modifier = Modifier
          .align(Alignment.BottomEnd)
          .size(24.dp)
          .clip(CircleShape)
          .background(MaterialTheme.colorScheme.primary)
          .padding(4.dp),
        tint = MaterialTheme.colorScheme.onPrimary,
      )
    }

    Spacer(modifier = Modifier.height(12.dp))

    // ── Display name (tap to edit) ───────────────────────────────────
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.clickable(onClick = onEditName),
    ) {
      Text(
        text = displayName,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
      )
      Spacer(modifier = Modifier.width(6.dp))
      Icon(
        imageVector = Icons.Filled.Edit,
        contentDescription = "Edit display name",
        modifier = Modifier.size(18.dp),
        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
      )
    }

    Spacer(modifier = Modifier.height(2.dp))

    // ── Username ─────────────────────────────────────────────────────
    Text(
      text = "@$username",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Spacer(modifier = Modifier.height(8.dp))

    // ── Bio (tap to edit) ────────────────────────────────────────────
    Surface(
      onClick = onEditBio,
      shape = RoundedCornerShape(12.dp),
      color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
      modifier = Modifier
        .padding(horizontal = 32.dp)
        .fillMaxWidth(),
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = bio.ifBlank { "Tap to add a bio" },
          style = MaterialTheme.typography.bodyMedium,
          color = if (bio.isBlank())
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
          else
            MaterialTheme.colorScheme.onSurface,
          maxLines = 3,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
          imageVector = Icons.Filled.Edit,
          contentDescription = "Edit bio",
          modifier = Modifier.size(16.dp),
          tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        )
      }
    }
  }
}

// ═════════════════════════════════════════════════════════════════════════════
// Status selector
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Presence status options matching the user's current availability.
 */
private enum class PresenceOption(val label: String, val dotColor: Color) {
  ONLINE("Online", NovaSuccess),
  AWAY("Away", NovaWarning),
  BUSY("Busy", NovaError),
  INVISIBLE("Invisible", Color.Gray),
}

/**
 * Horizontal row of [FilterChip]s for selecting the current presence status.
 */
@Composable
private fun StatusSelectorSection(
  selectedStatus: PresenceOption,
  onStatusSelected: (PresenceOption) -> Unit,
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 8.dp),
  ) {
    Text(
      text = "Status",
      style = MaterialTheme.typography.titleSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      fontWeight = FontWeight.SemiBold,
      modifier = Modifier.padding(bottom = 8.dp),
    )

    Row(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier.fillMaxWidth(),
    ) {
      PresenceOption.entries.forEach { option ->
        val isSelected = option == selectedStatus
        FilterChip(
          selected = isSelected,
          onClick = { onStatusSelected(option) },
          label = {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(8.dp)
                  .clip(CircleShape)
                  .background(option.dotColor),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(text = option.label)
            }
          },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = option.dotColor.copy(alpha = 0.15f),
            selectedLabelColor = option.dotColor,
          ),
        )
      }
    }
  }
}

// ═════════════════════════════════════════════════════════════════════════════
// Stats section
// ═════════════════════════════════════════════════════════════════════════════

/**
 * A single stat displayed as a small card.
 */
private data class StatCard(
  val label: String,
  val value: String,
  val icon: ImageVector,
  val color: Color,
)

/**
 * Returns mock statistics for the profile.
 */
@Composable
private fun mockProfileStats(): List<StatCard> = listOf(
  StatCard(label = "Messages Sent", value = "12,847", icon = Icons.Outlined.QuestionAnswer, color = NovaPrimary),
  StatCard(label = "Streak Days", value = "47", icon = Icons.Outlined.Star, color = NovaWarning),
  StatCard(label = "Contacts", value = "128", icon = Icons.Outlined.Person, color = NovaSuccess),
  StatCard(label = "Storage Used", value = "4.8 GB", icon = Icons.Outlined.Storage, color = MaterialTheme.colorScheme.tertiary),
)

/**
 * Row of small stat cards showing aggregate account numbers.
 */
@Composable
private fun StatsSection(
  stats: List<StatCard>,
  modifier: Modifier = Modifier,
) {
  // Wrap in a Column with a Row that splits into pairs
  Column(modifier = modifier.fillMaxWidth()) {
    Text(
      text = "Stats",
      style = MaterialTheme.typography.titleSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      fontWeight = FontWeight.SemiBold,
      modifier = Modifier.padding(bottom = 8.dp),
    )

    // Two rows of two cards each
    stats.chunked(2).forEach { rowCards ->
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        rowCards.forEach { stat ->
          StatCardItem(
            stat = stat,
            modifier = Modifier.weight(1f),
          )
        }

        // Fill remaining space if odd count
        if (rowCards.size < 2) {
          Spacer(modifier = Modifier.weight(1f))
        }
      }
    }
  }
}

/**
 * A single stat card for the profile stats section.
 */
@Composable
private fun StatCardItem(
  stat: StatCard,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier = modifier,
    colors = CardDefaults.cardColors(
      containerColor = stat.color.copy(alpha = 0.08f),
    ),
    shape = RoundedCornerShape(12.dp),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Icon(
        imageVector = stat.icon,
        contentDescription = stat.label,
        tint = stat.color,
        modifier = Modifier.size(24.dp),
      )
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = stat.value,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = stat.color,
      )
      Text(
        text = stat.label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

// ═════════════════════════════════════════════════════════════════════════════
// Settings group header
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Section header for a settings group, with an icon and title label.
 */
@Composable
private fun SettingsGroupHeader(
  icon: ImageVector,
  title: String,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      modifier = Modifier.size(20.dp),
      tint = MaterialTheme.colorScheme.primary,
    )
    Text(
      text = title,
      style = MaterialTheme.typography.titleSmall,
      fontWeight = FontWeight.SemiBold,
      color = MaterialTheme.colorScheme.primary,
    )
  }
}

// ═════════════════════════════════════════════════════════════════════════════
// Settings row (ListItem-based)
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Internal data model for a single settings row.
 *
 * @property icon Leading icon.
 * @property title Row title.
 * @property subtitle Optional secondary text.
 * @property trailing Optional composable slot for a switch, button, etc.
 * @property onClick Optional click action for the whole row.
 */
private data class SettingsItem(
  val icon: ImageVector,
  val title: String,
  val subtitle: String = "",
  val trailing: (@Composable () -> Unit)? = null,
  val onClick: (() -> Unit)? = null,
)

/**
 * A Material 3 [ListItem] used for a single settings entry.
 *
 * Supports a leading icon, title, optional subtitle, and trailing content.
 */
@Composable
private fun SettingsRow(
  item: SettingsItem,
) {
  ListItem(
    headlineContent = {
      Text(
        text = item.title,
        style = MaterialTheme.typography.bodyLarge,
      )
    },
    supportingContent = if (item.subtitle.isNotBlank()) {
      {
        Text(
          text = item.subtitle,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    } else null,
    leadingContent = {
      Icon(
        imageVector = item.icon,
        contentDescription = item.title,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(24.dp),
      )
    },
    trailingContent = item.trailing?.let { trailing ->
      {
        Row(verticalAlignment = Alignment.CenterVertically) {
          trailing()
          if (item.onClick != null) {
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
              imageVector = Icons.Filled.ChevronRight,
              contentDescription = "Open",
              modifier = Modifier.size(20.dp),
              tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            )
          }
        }
      }
    },
    colors = ListItemDefaults.colors(
      containerColor = Color.Transparent,
    ),
    modifier = Modifier
      .fillMaxWidth()
      .then(
        if (item.onClick != null) Modifier.clickable(onClick = item.onClick)
        else Modifier
      ),
  )
}

// ═════════════════════════════════════════════════════════════════════════════
// Bubble style selector
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Chat bubble visual style options.
 */
private enum class BubbleStyle(val label: String) {
  ROUNDED("Rounded"),
  SHARP("Sharp"),
  IOS("iOS"),
  MINIMAL("Minimal"),
}

/**
 * A small row of text chips for selecting the chat bubble style.
 */
@Composable
private fun BubbleStyleSelector(
  selected: BubbleStyle,
  onSelect: (BubbleStyle) -> Unit,
) {
  Row(
    horizontalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    BubbleStyle.entries.forEach { style ->
      val isSelected = style == selected
      Surface(
        onClick = { onSelect(style) },
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected)
          MaterialTheme.colorScheme.primaryContainer
        else
          MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier,
      ) {
        Text(
          text = style.label,
          style = MaterialTheme.typography.labelSmall,
          fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
          color = if (isSelected)
            MaterialTheme.colorScheme.onPrimaryContainer
          else
            MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
      }
    }
  }
}

// ═════════════════════════════════════════════════════════════════════════════
// Reusable components
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Standard Material 3 switch used for settings toggles.
 */
@Composable
private fun SettingsSwitch(
  checked: Boolean,
  onCheck: (Boolean) -> Unit,
) {
  Switch(
    checked = checked,
    onCheckedChange = onCheck,
  )
}

/**
 * Red logout button with a leading icon, placed at the bottom of the settings
 * list. Opens a confirmation dialog when tapped.
 */
@Composable
private fun LogoutButton(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Button(
    onClick = onClick,
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(12.dp),
    colors = ButtonDefaults.buttonColors(
      containerColor = MaterialTheme.colorScheme.error,
      contentColor = MaterialTheme.colorScheme.onError,
    ),
  ) {
    Icon(
      imageVector = Icons.AutoMirrored.Filled.Logout,
      contentDescription = null,
      modifier = Modifier.size(20.dp),
    )
    Spacer(modifier = Modifier.width(8.dp))
    Text(
      text = "Log Out",
      style = MaterialTheme.typography.labelLarge,
      fontWeight = FontWeight.SemiBold,
    )
  }
}
