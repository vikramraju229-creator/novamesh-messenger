/**
 * ChatsListScreen — WhatsApp-style main chat list tab.
 *
 * Layout (top→bottom):
 *   Top bar: "Chats" title + icons (camera, menu)
 *   Search pill: "Ask Meta AI or Search"
 *   Archived row (if any archived chats exist)
 *   Chat rows: Avatar | Name + Timestamp | Message + Status
 *   Empty state when no chats exist
 *
 * All data is real Firestore — no mock data.
 */
@file:OptIn(ExperimentalMaterial3Api::class)

package com.novamesh.ui.screens.chat

import android.Manifest
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.novamesh.data.remote.FirestoreRepository
import com.novamesh.domain.model.Chat
import com.novamesh.domain.model.MessageStatus
import com.novamesh.domain.model.Presence
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// WhatsApp green color for unread badges
private val WhatsAppGreen = Color(0xFF25D366)

/**
 * Main chat list screen — WhatsApp-style.
 *
 * @param onChatClick Invoked when user taps a chat item.
 * @param onCameraClick Invoked when the camera icon is tapped.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ChatListScreen(
    onChatClick: (chatId: String, chatName: String) -> Unit,
    onCameraClick: () -> Unit,
) {
    var searchQuery by remember { mutableStateOf("") }
    val context = LocalContext.current

    // ─── Real chats from Firestore ────────────────────────────────────────
    val repository = remember { FirestoreRepository() }
    var realChats by remember { mutableStateOf<List<Chat>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        repository.observeChats().collect { firestoreChats ->
            realChats = firestoreChats.map { fc ->
                Chat(
                    id = fc.id,
                    name = fc.lastMessage?.text ?: "Chat",
                    avatarUri = null,
                    lastMessage = fc.lastMessage?.text,
                    lastMessageTimestamp = fc.lastMessage?.timestamp ?: 0L,
                    lastMessageStatus = MessageStatus.DELIVERED,
                    unreadCount = fc.unreadCount?.values?.sum() ?: 0,
                    isPinned = false,
                    isMuted = false,
                    participants = emptyList(),
                    isGroup = (fc.participants?.size ?: 0) > 2,
                    disappearingTimerSeconds = null,
                    createdAt = 0L,
                )
            }
            isLoading = false
        }
    }

    val chats = realChats
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    // Filter chats based on search query
    val filteredChats by derivedStateOf {
        if (searchQuery.isBlank()) {
            chats.sortedByDescending { it.lastMessageTimestamp }
        } else {
            val query = searchQuery.trim().lowercase()
            chats.filter { chat ->
                chat.name.lowercase().contains(query) ||
                    chat.lastMessage?.lowercase()?.contains(query) == true
            }.sortedByDescending { it.lastMessageTimestamp }
        }
    }

    // Separate archived chats (not yet implemented, but structure ready)
    val activeChats = filteredChats
    val archivedCount = 0 // TODO: track archived chats

    Scaffold(
        topBar = {
            Column {
                // ── WhatsApp-style Top Bar ──
                TopAppBar(
                    title = {
                        Text(
                            text = "Chats",
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.titleLarge,
                        )
                    },
                    actions = {
                        IconButton(onClick = {
                            if (cameraPermissionState.status.isGranted) {
                                onCameraClick()
                            } else {
                                cameraPermissionState.launchPermissionRequest()
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Camera",
                            )
                        }
                        IconButton(onClick = { /* TODO: more options menu */ }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More options",
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                )

                // ── WhatsApp-style Search Pill ──
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    onClick = { /* TODO: open search active */ },
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Ask Meta AI or Search",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        )
                    }
                }
            }
        },
        modifier = Modifier.fillMaxSize(),
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Loading...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else if (activeChats.isEmpty()) {
                ChatListEmptyState(modifier = Modifier.fillMaxSize())
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    // ── Archived row (if any) ──
                    if (archivedCount > 0) {
                        item(key = "archived") {
                            ArchivedRow(count = archivedCount)
                        }
                    }

                    // ── Chat items ──
                    items(
                        items = activeChats,
                        key = { it.id },
                    ) { chat ->
                        ChatListItem(
                            chat = chat,
                            onClick = { onChatClick(chat.id, chat.name) },
                        )
                    }
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Archived row
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun ArchivedRow(count: Int) {
    Surface(
        onClick = { /* TODO: navigate to archived chats */ },
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Folder icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE8E8E8)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.MailOutline,
                    contentDescription = "Archived",
                    tint = Color(0xFF6B6B6B),
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Archived",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "$count",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    HorizontalDivider(
        modifier = Modifier.padding(start = 76.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
    )
}

// ═════════════════════════════════════════════════════════════════════════════
// Chat list item — WhatsApp-style row
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun ChatListItem(
    chat: Chat,
    onClick: () -> Unit,
) {
    val hasUnread = chat.unreadCount > 0

    Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // ── Avatar (52dp circle with initial) ──
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = chat.name.firstOrNull()?.uppercase() ?: "?",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // ── Name + Message ──
                Column(modifier = Modifier.weight(1f)) {
                    // Row 1: Name + Timestamp
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = chat.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (hasUnread) FontWeight.SemiBold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )

                        Spacer(modifier = Modifier.width(4.dp))

                        Text(
                            text = formatRelativeTime(chat.lastMessageTimestamp),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (hasUnread)
                                WhatsAppGreen
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // Row 2: Message preview + status + unread
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = chat.lastMessage ?: "No messages yet",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (hasUnread) FontWeight.Medium else FontWeight.Normal,
                            color = if (hasUnread)
                                MaterialTheme.colorScheme.onSurface
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )

                        if (hasUnread) {
                            Spacer(modifier = Modifier.width(6.dp))
                            UnreadBadge(count = chat.unreadCount)
                        }
                    }
                }
            }

            // Divider (indented past avatar)
            HorizontalDivider(
                modifier = Modifier.padding(start = 76.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Unread badge — WhatsApp green
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun UnreadBadge(count: Int) {
    val displayText = if (count > 99) "99+" else count.toString()

    Box(
        modifier = Modifier
            .size(width = 24.dp, height = 20.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(WhatsAppGreen),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = displayText,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Empty state
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun ChatListEmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "No chats yet",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Tap the camera or search for people to start chatting",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 48.dp),
        )
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Time formatting — WhatsApp style
// ═════════════════════════════════════════════════════════════════════════════

private fun formatRelativeTime(timestampMillis: Long): String {
    if (timestampMillis <= 0L) return ""

    val now = System.currentTimeMillis()
    val diff = now - timestampMillis
    val day = 86_400_000L

    return when {
        diff < day -> {
            // Today — show time like "10:30 AM"
            val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
            sdf.format(Date(timestampMillis))
        }
        diff < 2 * day -> "Yesterday"
        diff < 7 * day -> {
            // Within a week — show day name
            val sdf = SimpleDateFormat("EEE", Locale.getDefault())
            sdf.format(Date(timestampMillis))
        }
        else -> {
            val date = Date(timestampMillis)
            val sdf = SimpleDateFormat("MMM d", Locale.getDefault())
            sdf.format(date)
        }
    }
}
