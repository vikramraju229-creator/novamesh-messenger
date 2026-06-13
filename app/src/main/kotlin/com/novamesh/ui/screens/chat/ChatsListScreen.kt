/**
 * ChatsListScreen — the main chats list tab showing all active conversations.
 *
 * Features:
 * - Material 3 lazy column with chat list items
 * - Search bar with camera icon button in top-right (checks CAMERA permission)
 * - Each item shows: circular avatar, name, last message preview, timestamp,
 *   unread count badge, delivery status icon, online indicator
 * - Swipe-to-archive gesture via [SwipeToDismissBox]
 * - Empty state with contextual messaging
 */
@file:OptIn(ExperimentalMaterial3Api::class)

package com.novamesh.ui.screens.chat

import android.Manifest
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
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
import com.novamesh.domain.model.Chat
import com.novamesh.domain.model.MessageStatus
import com.novamesh.domain.model.Presence
import com.novamesh.domain.model.User
import com.novamesh.data.remote.FirestoreRepository
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Main chat list screen showing all conversations.
 *
 * @param onChatClick Invoked when user taps a chat item; receives chatId and chatName.
 * @param onCameraClick Invoked when the camera icon button is tapped.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ChatListScreen(
    onChatClick: (chatId: String, chatName: String) -> Unit,
    onCameraClick: () -> Unit,
) {
    // ─── State ───────────────────────────────────────────────────────────────
    var searchQuery by remember { mutableStateOf("") }
    val context = LocalContext.current

    // ─── Real chats from Firestore ────────────────────────────────────────────
    val firestore = remember { FirebaseFirestore.getInstance() }
    val repository = remember { FirestoreRepository(firestore) }
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
                    lastMessageStatus = null,
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
            chats.sortedByDescending { it.isPinned }
                .sortedByDescending { it.lastMessageTimestamp }
        } else {
            val query = searchQuery.trim().lowercase()
            chats.filter { chat ->
                chat.name.lowercase().contains(query) ||
                    chat.lastMessage?.lowercase()?.contains(query) == true
            }.sortedByDescending { it.lastMessageTimestamp }
        }
    }

    Scaffold(
        topBar = {
            ChatListSearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                onClear = { searchQuery = "" },
                onCameraClick = {
                    // Check camera permission before navigating
                    if (cameraPermissionState.status.isGranted) {
                        onCameraClick()
                    } else {
                        cameraPermissionState.launchPermissionRequest()
                    }
                },
            )
        },
        modifier = Modifier.fillMaxSize(),
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            if (filteredChats.isEmpty()) {
                ChatListEmptyState(
                    isSearching = searchQuery.isNotBlank(),
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        vertical = 4.dp,
                    ),
                ) {
                    items(
                        items = filteredChats,
                        key = { it.id },
                    ) { chat ->
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { value ->
                                if (value == SwipeToDismissBoxValue.EndToStart) {
                                    true
                                } else {
                                    false
                                }
                            },
                        )

                        SwipeToDismissBox(
                            state = dismissState,
                            backgroundContent = { SwipeArchiveBackground() },
                            enableDismissFromStartToEnd = false,
                            enableDismissFromEndToStart = true,
                        ) {
                            AnimatedChatItem {
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
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Search bar with camera icon
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Top search bar with camera icon button on the right.
 */
@Composable
private fun ChatListSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    onCameraClick: () -> Unit,
) {
    SearchBar(
        query = query,
        onQueryChange = onQueryChange,
        onSearch = { },
        active = false,
        onActiveChange = { },
        placeholder = { Text("Search chats") },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
            )
        },
        trailingIcon = {
            Row {
                if (query.isNotEmpty()) {
                    IconButton(onClick = onClear) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear search",
                        )
                    }
                }
                IconButton(onClick = onCameraClick) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Open camera",
                    )
                }
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) { }
}

// ═════════════════════════════════════════════════════════════════════════════
// Chat list item
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun ChatListItem(
    chat: Chat,
    onClick: () -> Unit,
) {
    val hasUnread = chat.unreadCount > 0
    val nameWeight = if (hasUnread) FontWeight.SemiBold else FontWeight.Normal
    val messageWeight = if (hasUnread) FontWeight.Medium else FontWeight.Normal

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(0.dp),
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // ── Avatar with online dot ──
            ChatAvatar(
                name = chat.name,
                avatarUri = chat.avatarUri,
                isOnline = chat.participants.any { it.presence == Presence.ONLINE },
                modifier = Modifier.size(56.dp),
            )

            Spacer(modifier = Modifier.width(12.dp))

            // ── Text content ──
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = chat.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = nameWeight,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    if (chat.isPinned) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = "Pinned",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                    }

                    if (chat.isMuted) {
                        Icon(
                            imageVector = Icons.Default.VolumeOff,
                            contentDescription = "Muted",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                    }

                    Text(
                        text = formatRelativeTime(chat.lastMessageTimestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (hasUnread)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = chat.lastMessage ?: "No messages yet",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = messageWeight,
                        color = if (hasUnread)
                            MaterialTheme.colorScheme.onSurface
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    if (chat.lastMessage != null) {
                        MessageStatusIcon(
                            status = chat.lastMessageStatus,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    if (hasUnread) {
                        UnreadBadge(count = chat.unreadCount)
                    }
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Avatar composable
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun ChatAvatar(
    name: String,
    avatarUri: String?,
    isOnline: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = name.firstOrNull()?.uppercase() ?: "?",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold,
            )
        }

        if (isOnline) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4CAF50)),
                )
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Status icon
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun MessageStatusIcon(
    status: MessageStatus,
    modifier: Modifier = Modifier,
) {
    val icon: ImageVector
    val tint: Color

    when (status) {
        MessageStatus.SENT -> {
            icon = Icons.Outlined.CheckCircle
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        }
        MessageStatus.DELIVERED -> {
            icon = Icons.Outlined.DoneAll
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        }
        MessageStatus.READ -> {
            icon = Icons.Filled.DoneAll
            tint = Color(0xFF53BDEB)
        }
        MessageStatus.SENDING,
        MessageStatus.FAILED,
        MessageStatus.DELETED -> {
            icon = Icons.Outlined.CheckCircle
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
        }
    }

    Icon(
        imageVector = icon,
        contentDescription = "Status: $status",
        modifier = modifier,
        tint = tint,
    )
}

// ═════════════════════════════════════════════════════════════════════════════
// Unread badge
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun UnreadBadge(count: Int) {
    val displayText = if (count > 99) "99+" else count.toString()

    Box(
        modifier = Modifier
            .size(width = 24.dp, height = 20.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.error),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = displayText,
            color = MaterialTheme.colorScheme.onError,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Swipe-to-archive background
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun SwipeArchiveBackground() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = Icons.Default.MailOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                text = "Archive",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Empty state
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun ChatListEmptyState(
    isSearching: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = if (isSearching) Icons.Default.Search else Icons.Default.Person,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (isSearching) "No chats found" else "No chats yet",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = if (isSearching) {
                "Try a different search term"
            } else {
                "Start a new conversation by tapping the camera or the chat icon"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 48.dp),
        )
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Animated item wrapper
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun AnimatedChatItem(
    content: @Composable () -> Unit,
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(
            animationSpec = tween(durationMillis = 350),
        ) + slideInVertically(
            initialOffsetY = { fullHeight -> fullHeight / 3 },
            animationSpec = tween(durationMillis = 350),
        ),
    ) {
        content()
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Relative time formatting
// ═════════════════════════════════════════════════════════════════════════════

private fun formatRelativeTime(timestampMillis: Long): String {
    if (timestampMillis <= 0L) return ""

    val now = System.currentTimeMillis()
    val diff = now - timestampMillis

    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
        seconds < 60 -> "Just now"
        minutes < 2 -> "1m ago"
        minutes < 60 -> "${minutes}m ago"
        hours < 2 -> "1h ago"
        hours < 24 -> "${hours}h ago"
        days == 1L -> "Yesterday"
        days < 7 -> "${days}d ago"
        else -> {
            val date = Date(timestampMillis)
            val sdf = SimpleDateFormat("MMM d", Locale.getDefault())
            sdf.format(date)
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Mock data
// ═════════════════════════════════════════════════════════════════════════════

private fun mockChats(): List<Chat> {
    val now = System.currentTimeMillis()
    val minute = 60_000L
    val hour = 60 * minute
    val day = 24 * hour

    val alice = User(
        id = "user_1",
        username = "alice_wonder",
        displayName = "Alice Wonderland",
        avatarUri = null,
        presence = Presence.ONLINE,
        lastSeen = now,
        isContact = true,
    )
    val bob = User(
        id = "user_2",
        username = "bob_builder",
        displayName = "Bob Builder",
        avatarUri = null,
        presence = Presence.OFFLINE,
        lastSeen = now - 15 * minute,
        isContact = true,
    )
    val charlie = User(
        id = "user_3",
        username = "charlie_dev",
        displayName = "Charlie Developer",
        avatarUri = null,
        presence = Presence.AWAY,
        lastSeen = now - 2 * hour,
        isContact = true,
    )
    val diana = User(
        id = "user_4",
        username = "diana_prince",
        displayName = "Diana Prince",
        avatarUri = null,
        presence = Presence.ONLINE,
        lastSeen = now,
        isContact = true,
    )
    val eve = User(
        id = "user_5",
        username = "eve_adam",
        displayName = "Eve Adams",
        avatarUri = null,
        presence = Presence.BUSY,
        lastSeen = now - 5 * minute,
        isContact = true,
    )

    return listOf(
        Chat(
            id = "chat_1",
            name = alice.displayName,
            avatarUri = alice.avatarUri,
            lastMessage = "See you tomorrow! \uD83D\uDE0A",
            lastMessageTimestamp = now - 2 * minute,
            lastMessageStatus = MessageStatus.READ,
            unreadCount = 0,
            isPinned = true,
            participants = listOf(alice),
        ),
        Chat(
            id = "chat_2",
            name = bob.displayName,
            lastMessage = "Sure, I'll send the files over",
            lastMessageTimestamp = now - 15 * minute,
            lastMessageStatus = MessageStatus.DELIVERED,
            unreadCount = 3,
            participants = listOf(bob),
        ),
        Chat(
            id = "chat_3",
            name = charlie.displayName,
            lastMessage = "Can you review the PR when you get a chance?",
            lastMessageTimestamp = now - 2 * hour,
            lastMessageStatus = MessageStatus.SENT,
            unreadCount = 1,
            isMuted = true,
            participants = listOf(charlie),
        ),
        Chat(
            id = "chat_4",
            name = diana.displayName,
            avatarUri = diana.avatarUri,
            lastMessage = "\uD83D\uDD25",
            lastMessageTimestamp = now - 8 * hour,
            lastMessageStatus = MessageStatus.READ,
            unreadCount = 0,
            isPinned = true,
            participants = listOf(diana),
        ),
        Chat(
            id = "chat_5",
            name = eve.displayName,
            lastMessage = "Meeting at 3pm tomorrow",
            lastMessageTimestamp = now - 1 * day,
            lastMessageStatus = MessageStatus.READ,
            unreadCount = 0,
            participants = listOf(eve),
        ),
        Chat(
            id = "chat_6",
            name = "NovaMesh Team",
            lastMessage = "Eve: Deployment scheduled for Friday",
            lastMessageTimestamp = now - 3 * day,
            lastMessageStatus = MessageStatus.DELIVERED,
            unreadCount = 12,
            isGroup = true,
            participants = listOf(alice, bob, charlie, diana, eve),
        ),
        Chat(
            id = "chat_7",
            name = "Family Group",
            lastMessage = "Mom: Don't forget dinner at 7 \uD83C\uDF55",
            lastMessageTimestamp = now - 5 * day,
            lastMessageStatus = MessageStatus.READ,
            unreadCount = 5,
            isGroup = true,
            isMuted = true,
            participants = listOf(alice, bob, eve),
        ),
    )
}
