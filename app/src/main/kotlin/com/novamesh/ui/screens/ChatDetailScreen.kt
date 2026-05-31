/**
 * ChatDetailScreen — individual chat conversation view with full messaging
 * capabilities.
 *
 * Features:
 * - Top bar with back navigation, contact avatar + name, online status,
 *   call and video call action icons
 * - Message list ([LazyColumn] with reverse layout — newest at bottom,
 *   auto-scroll on new messages)
 * - Message bubbles: sent (right-aligned, primary container background) and
 *   received (left-aligned, surface container background)
 * - Each bubble displays: text or media content, relative timestamp,
 *   read/delivered status icon, and a reactions row
 * - Rich input bar: text field, emoji picker, attachment picker, voice
 *   message recorder, and send button
 * - Voice recording: long-press mic button with animated waveform
 * - Typing indicator ("typing…") shown beneath the message list
 * - Date separator chips between groups of same-day messages
 * - Swipe-to-reply gesture on individual message bubbles
 * - Disappearing messages timer indicator in the top bar
 *
 * All data is currently mocked. In production, a ViewModel would observe
 * the repository (Room / Signal-protocol store) and expose [Message] flows.
 */

@file:OptIn(ExperimentalMaterial3Api::class)

package com.novamesh.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.KeyboardVoice
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novamesh.domain.model.Message
import com.novamesh.domain.model.MessageContent
import com.novamesh.domain.model.MessageStatus
import com.novamesh.domain.model.MessageType
import com.novamesh.domain.model.Presence
import com.novamesh.domain.model.User
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ═════════════════════════════════════════════════════════════════════════════
// Public composable — entry point for the NavGraph
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Full chat conversation screen.
 *
 * @param chatId Identifies the conversation.
 * @param chatName Display name for the top bar.
 * @param onBack Navigate back to the chat list.
 * @param onCameraClick Open the camera from within the chat.
 */
@Composable
fun ChatDetailScreen(
    chatId: String,
    chatName: String,
    onBack: () -> Unit,
    onCameraClick: () -> Unit,
) {
    // ─── State ───────────────────────────────────────────────────────────────
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var messageInput by remember { mutableStateOf("") }
    val messages = remember { mutableStateListOf<Message>() }
    var isTyping by remember { mutableStateOf(false) }

    // Voice recording state
    var isRecording by remember { mutableStateOf(false) }
    var recordingDuration by remember { mutableLongStateOf(0L) }
    var isSwipeToReplyActive by remember { mutableStateOf(false) }

    // Populate mock messages on first composition
    LaunchedEffect(chatId) {
        messages.addAll(mockMessages(chatId))
    }

    // Simulate typing indicator appearing/disappearing
    LaunchedEffect(Unit) {
        while (true) {
            delay(8_000)
            isTyping = true
            delay(4_000)
            isTyping = false
            delay(12_000)
        }
    }

    // Simulate an incoming message after a few seconds
    LaunchedEffect(chatId) {
        delay(6_000)
        val incoming = Message(
            id = "incoming_${System.currentTimeMillis()}",
            chatId = chatId,
            senderId = "user_2",
            senderName = "Alice",
            content = MessageContent.Text("Hey! How's it going? 😊"),
            type = MessageType.TEXT,
            timestamp = System.currentTimeMillis(),
            status = MessageStatus.DELIVERED,
            readBy = emptyList(),
            deliveryStatus = com.novamesh.domain.model.DeliveryStatus.DELIVERED,
        )
        messages.add(0, incoming) // prepend because reverse layout
    }

    // Auto-scroll to bottom (newest message) on list changes
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    // ─── Disappearing timer info (from chat, 30s example) ─────────────────
    val disappearingTimerSeconds = 30

    Scaffold(
        topBar = {
            ChatDetailTopBar(
                chatName = chatName,
                avatarUri = null,
                isOnline = true,
                disappearingTimerSeconds = disappearingTimerSeconds,
                onBack = onBack,
                onCallClick = { /* TODO: initiate WebRTC call */ },
                onVideoCallClick = { /* TODO: initiate WebRTC video call */ },
            )
        },
        bottomBar = {
            ChatInputBar(
                text = messageInput,
                onTextChange = { messageInput = it },
                onSend = {
                    if (messageInput.isNotBlank()) {
                        val newMsg = Message(
                            id = "outgoing_${System.currentTimeMillis()}",
                            chatId = chatId,
                            senderId = "me",
                            senderName = "Me",
                            content = MessageContent.Text(messageInput.trim()),
                            type = MessageType.TEXT,
                            timestamp = System.currentTimeMillis(),
                            status = MessageStatus.SENT,
                            readBy = emptyList(),
                            deliveryStatus = com.novamesh.domain.model.DeliveryStatus.SENT,
                        )
                        messages.add(0, newMsg)
                        messageInput = ""
                        scope.launch {
                            listState.animateScrollToItem(0)
                        }
                    }
                },
                isRecording = isRecording,
                recordingDuration = recordingDuration,
                onStartRecording = {
                    isRecording = true
                    // Simulate recording duration ticker
                    scope.launch {
                        recordingDuration = 0L
                        while (isRecording) {
                            delay(1000L)
                            recordingDuration += 1000L
                        }
                    }
                },
                onStopRecording = {
                    isRecording = false
                    // Add a voice message mock
                    val voiceMsg = Message(
                        id = "voice_${System.currentTimeMillis()}",
                        chatId = chatId,
                        senderId = "me",
                        senderName = "Me",
                        content = MessageContent.Voice(
                            uri = "file:///recording_${System.currentTimeMillis()}.ogg",
                            durationMs = recordingDuration,
                            waveform = listOf(0.1f, 0.3f, 0.5f, 0.7f, 0.4f, 0.6f, 0.8f, 0.5f, 0.3f),
                        ),
                        type = MessageType.VOICE,
                        timestamp = System.currentTimeMillis(),
                        status = MessageStatus.SENT,
                        readBy = emptyList(),
                        deliveryStatus = com.novamesh.domain.model.DeliveryStatus.SENT,
                    )
                    messages.add(0, voiceMsg)
                    recordingDuration = 0L
                    scope.launch {
                        listState.animateScrollToItem(0)
                    }
                },
                onCameraClick = onCameraClick,
                onEmojiClick = { /* TODO: show emoji picker bottom sheet */ },
                onAttachmentClick = { /* TODO: show attachment picker */ },
            )
        },
        modifier = Modifier.fillMaxSize(),
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            reverseLayout = true,           // newest items at the bottom
            contentPadding = PaddingValues(
                start = 12.dp,
                end = 12.dp,
                top = 8.dp,
                bottom = 8.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            // ── Typing indicator at the bottom (first item due to reverse) ──
            if (isTyping) {
                item(key = "typing_indicator") {
                    TypingIndicator(
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
            }

            // ── Date separators + messages ─────────────────────────────────
            // messages is stored newest-first (index 0 = latest). We reverse to
            // chronological order for date grouping, then reverse again to
            // newest-first so that with reverseLayout=true the newest item
            // (index 0) renders at the bottom of the list.
            val chronological = messages.reversed()
            val displayList = buildMessageDisplayList(chronological).asReversed()

            items(
                items = displayList,
                key = { it.key },
            ) { entry ->
                when (entry) {
                    is DisplayEntry.DateSeparator -> {
                        DateSeparator(text = entry.label)
                    }
                    is DisplayEntry.MessageEntry -> {
                        SwipeableMessage(
                            message = entry.message,
                            onReply = { /* TODO: set reply-to context */ },
                        )
                    }
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Top bar
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Chat detail top app bar with contact info, call actions, and a
 * disappearing-messages timer indicator.
 */
@Composable
private fun ChatDetailTopBar(
    chatName: String,
    avatarUri: String?,
    isOnline: Boolean,
    disappearingTimerSeconds: Int?,
    onBack: () -> Unit,
    onCallClick: () -> Unit,
    onVideoCallClick: () -> Unit,
) {
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Small avatar
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = chatName.firstOrNull()?.uppercase() ?: "?",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = chatName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = if (isOnline) "Online" else "Offline",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isOnline)
                            Color(0xFF4CAF50)
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
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
            // Disappearing messages indicator
            if (disappearingTimerSeconds != null) {
                Box(
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.tertiaryContainer)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.HourglassBottom,
                            contentDescription = "Disappearing messages",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                        Text(
                            text = "${disappearingTimerSeconds}s",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                    }
                }
            }

            IconButton(onClick = onCallClick) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = "Voice call",
                )
            }
            IconButton(onClick = onVideoCallClick) {
                Icon(
                    imageVector = Icons.Default.KeyboardVoice,
                    contentDescription = "Video call",
                    modifier = Modifier.size(24.dp),
                )
            }
            IconButton(onClick = { /* TODO: overflow menu */ }) {
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
}

// ═════════════════════════════════════════════════════════════════════════════
// Message list — display entries for flat list rendering
// ═════════════════════════════════════════════════════════════════════════════

/**
 * A single element rendered in the chat [LazyColumn].
 */
private sealed interface DisplayEntry {
    /** Unique key for the lazy list. */
    val key: String

    /** A chip showing a date like "Today", "Yesterday", or "Jun 1". */
    data class DateSeparator(val label: String) : DisplayEntry {
        override val key: String get() = "date_$label"
    }

    /** A single message bubble. */
    data class MessageEntry(val message: Message) : DisplayEntry {
        override val key: String get() = "msg_${message.id}"
    }
}

/**
 * Converts a chronologically ordered list of [Message]s into a flat list
 * of [DisplayEntry] items, injecting [DisplayEntry.DateSeparator] entries
 * wherever the day changes between consecutive messages.
 */
private fun buildMessageDisplayList(messages: List<Message>): List<DisplayEntry> {
    if (messages.isEmpty()) return emptyList()

    val result = mutableListOf<DisplayEntry>()
    val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
    var lastDate: String? = null

    for (msg in messages) {
        val msgDate = dateFormat.format(Date(msg.timestamp))
        if (msgDate != lastDate) {
            result.add(
                DisplayEntry.DateSeparator(
                    label = formatDateSeparator(msg.timestamp),
                ),
            )
            lastDate = msgDate
        }
        result.add(DisplayEntry.MessageEntry(msg))
    }

    return result
}

// ═════════════════════════════════════════════════════════════════════════════
// Date separator
// ═════════════════════════════════════════════════════════════════════════════

/**
 * A small centered chip that separates messages from different days.
 */
@Composable
private fun DateSeparator(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
            shadowElevation = 0.dp,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Swipeable message wrapper (swipe-to-reply)
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Wraps a message bubble with a swipe-to-reply gesture. Swiping right reveals
 * a "Reply" label and fires [onReply].
 */
@Composable
private fun SwipeableMessage(
    message: Message,
    onReply: () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.StartToEnd) {
                onReply()
                true
            } else {
                false
            }
        },
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .padding(start = 20.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    text = "Reply",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        },
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = false,
    ) {
        MessageBubble(message = message)
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Message bubble
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Displays a single message as a chat bubble.
 *
 * - Outgoing messages (sent by the current user) appear right-aligned with a
 *   primary-container background.
 * - Incoming messages appear left-aligned with a surface-container background.
 * - System messages are displayed as centered, muted text.
 *
 * Each bubble includes the message content, a relative timestamp, delivery
 * status icon, and a reactions row.
 */
@Composable
private fun MessageBubble(message: Message) {
    // Identify outgoing messages — in production this compares against the
    // authenticated user's ID. For mock purposes we treat "me" as the local user.
    val isOutgoing = message.senderId == "me"

    // System messages get a completely different layout
    if (message.type == MessageType.SYSTEM) {
        SystemMessageBubble(message = message)
        return
    }

    val bubbleShape = RoundedCornerShape(
        topStart = 16.dp,
        topEnd = 16.dp,
        bottomStart = if (isOutgoing) 16.dp else 4.dp,
        bottomEnd = if (isOutgoing) 4.dp else 16.dp,
    )

    val bubbleColor by animateColorAsState(
        targetValue = if (isOutgoing)
            MaterialTheme.colorScheme.primaryContainer
        else
            MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = tween(200),
        label = "bubbleColor",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        horizontalArrangement = if (isOutgoing) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            shape = bubbleShape,
            color = bubbleColor,
            shadowElevation = 0.dp,
            modifier = Modifier.widthIn(max = 280.dp),
        ) {
            Column(
                modifier = Modifier.padding(
                    start = 12.dp,
                    end = 8.dp,
                    top = 8.dp,
                    bottom = 6.dp,
                ),
            ) {
                // ── Message content ────────────────────────────────────────
                MessageContentDisplay(
                    content = message.content,
                    isOutgoing = isOutgoing,
                )

                Spacer(modifier = Modifier.height(2.dp))

                // ── Timestamp + status row ────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = formatMessageTime(message.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isOutgoing)
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontSize = 11.sp,
                    )

                    if (isOutgoing) {
                        Spacer(modifier = Modifier.width(3.dp))
                        MessageStatusIcon(
                            status = message.status,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }

                // ── Reactions row ─────────────────────────────────────────
                if (message.reactions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    ReactionsRow(
                        reactions = message.reactions,
                        isOutgoing = isOutgoing,
                    )
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Message content renderer
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Displays the appropriate UI for each variant of [MessageContent].
 */
@Composable
private fun MessageContentDisplay(
    content: MessageContent,
    isOutgoing: Boolean,
) {
    val contentColor = if (isOutgoing)
        MaterialTheme.colorScheme.onPrimaryContainer
    else
        MaterialTheme.colorScheme.onSurface

    when (content) {
        is MessageContent.Text -> {
            Text(
                text = content.text,
                style = MaterialTheme.typography.bodyLarge,
                color = contentColor,
            )
        }

        is MessageContent.Image -> {
            Column {
                // Placeholder for image thumbnail
                Box(
                    modifier = Modifier
                        .size(width = 200.dp, height = 150.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "📷",
                        fontSize = 32.sp,
                    )
                }
                if (!content.caption.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = content.caption,
                        style = MaterialTheme.typography.bodyMedium,
                        color = contentColor,
                    )
                }
            }
        }

        is MessageContent.Video -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(8.dp),
            ) {
                Text(text = "🎬", fontSize = 24.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Video",
                        style = MaterialTheme.typography.bodyMedium,
                        color = contentColor,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = formatDuration(content.durationMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = contentColor.copy(alpha = 0.6f),
                    )
                }
            }
        }

        is MessageContent.Audio -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(8.dp),
            ) {
                Text(text = "🎵", fontSize = 24.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Audio · ${formatDuration(content.durationMs)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor,
                )
            }
        }

        is MessageContent.Voice -> {
            VoiceMessageContent(
                durationMs = content.durationMs,
                waveform = content.waveform,
                isOutgoing = isOutgoing,
            )
        }

        is MessageContent.File -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(8.dp),
            ) {
                Text(text = "📎", fontSize = 24.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = content.fileName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = contentColor,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = formatFileSize(content.fileSize),
                        style = MaterialTheme.typography.labelSmall,
                        color = contentColor.copy(alpha = 0.6f),
                    )
                }
            }
        }

        is MessageContent.Location -> {
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(8.dp),
            ) {
                Text(text = "📍", fontSize = 24.sp)
                Text(
                    text = content.name ?: "Shared location",
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = "${content.latitude}, ${content.longitude}",
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(alpha = 0.6f),
                )
            }
        }

        is MessageContent.Contact -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(8.dp),
            ) {
                Text(text = "👤", fontSize = 24.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = content.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = contentColor,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = content.phoneNumber,
                        style = MaterialTheme.typography.labelSmall,
                        color = contentColor.copy(alpha = 0.6f),
                    )
                }
            }
        }

        is MessageContent.Sticker -> {
            Text(
                text = "🎨 Sticker #${content.stickerId}",
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor,
            )
        }

        is MessageContent.Gif -> {
            Box(
                modifier = Modifier
                    .size(width = 160.dp, height = 120.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "🎞️ GIF", fontSize = 24.sp)
            }
        }

        is MessageContent.System -> {
            // Rendered via SystemMessageBubble, so this branch is a fallback
            Text(
                text = content.text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Voice message content
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Renders a voice message bubble with an animated waveform and duration label.
 */
@Composable
private fun VoiceMessageContent(
    durationMs: Long,
    waveform: List<Float>?,
    isOutgoing: Boolean,
) {
    val tint = if (isOutgoing)
        MaterialTheme.colorScheme.onPrimaryContainer
    else
        MaterialTheme.colorScheme.onSurface

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.widthIn(max = 200.dp),
    ) {
        Icon(
            imageVector = Icons.Default.Mic,
            contentDescription = "Voice message",
            modifier = Modifier.size(20.dp),
            tint = tint,
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Simple waveform visualization
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.height(24.dp),
        ) {
            val bars = waveform ?: listOf(0.2f, 0.5f, 0.8f, 0.4f, 0.6f, 0.9f, 0.3f)
            bars.forEach { amplitude ->
                val barHeight = (amplitude * 20).dp.coerceAtLeast(4.dp)
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(barHeight)
                        .clip(RoundedCornerShape(2.dp))
                        .background(tint.copy(alpha = 0.6f)),
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = formatDuration(durationMs),
            style = MaterialTheme.typography.labelSmall,
            color = tint.copy(alpha = 0.7f),
        )
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// System message
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Renders a system-generated message (e.g. "Alice joined the group") as
 * centered, muted text with a horizontal divider.
 */
@Composable
private fun SystemMessageBubble(message: Message) {
    val text = when (val content = message.content) {
        is MessageContent.System -> content.text
        is MessageContent.Text -> content.text
        else -> "System event"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 32.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp, vertical = 4.dp),
        )
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 32.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
        )
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Reactions row
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Displays a row of emoji reaction chips below a message bubble.
 *
 * Reactions are supplied as a map of userId → emoji. We group by emoji
 * value and show each unique emoji. If multiple users reacted with the
 * same emoji, a count is displayed.
 */
@Composable
private fun ReactionsRow(
    reactions: Map<String, String>,
    isOutgoing: Boolean,
) {
    // Group reactions by emoji and count occurrences
    val grouped = reactions.values.groupingBy { it }.eachCount()

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isOutgoing)
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                else
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            )
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        grouped.forEach { (emoji, count) ->
            Text(
                text = if (count > 1) "$emoji $count" else emoji,
                style = MaterialTheme.typography.labelMedium,
                fontSize = 13.sp,
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Status icon (reused, matches ChatListScreen style)
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Renders a small icon representing the delivery status of a message.
 *
 * - [MessageStatus.SENT]: single outlined check
 * - [MessageStatus.DELIVERED]: double outlined check
 * - [MessageStatus.READ]: filled double check in blue
 */
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
            tint = Color(0xFF8696A0)
        }
        MessageStatus.DELIVERED -> {
            icon = Icons.Outlined.DoneAll
            tint = Color(0xFF8696A0)
        }
        MessageStatus.READ -> {
            icon = Icons.Filled.DoneAll
            tint = Color(0xFF53BDEB)
        }
        MessageStatus.SENDING,
        MessageStatus.FAILED,
        MessageStatus.DELETED -> {
            icon = Icons.Outlined.CheckCircle
            tint = Color(0xFF8696A0).copy(alpha = 0.4f)
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
// Typing indicator
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Animated "typing…" indicator shown beneath the message list.
 */
@Composable
private fun TypingIndicator(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "dotAlpha",
    )

    Row(
        modifier = modifier
            .padding(start = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Small avatar
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "A",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold,
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text(
                    text = "typing",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )
                repeat(3) {
                    Text(
                        text = ".",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = dotAlpha),
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Input bar
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Bottom input bar for composing and sending messages.
 *
 * Includes: text field, emoji button, attachment button, voice message
 * recorder (long-press), and send button. When recording, the UI switches
 * to show a recording state with a waveform and timer.
 */
@Composable
private fun ChatInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    isRecording: Boolean,
    recordingDuration: Long,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onCameraClick: () -> Unit,
    onEmojiClick: () -> Unit,
    onAttachmentClick: () -> Unit,
) {
    Surface(
        tonalElevation = 2.dp,
        shadowElevation = 4.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (isRecording) {
            // ── Recording UI ──────────────────────────────────────────────
            RecordingBar(
                durationMs = recordingDuration,
                onStop = onStopRecording,
            )
        } else {
            // ── Normal input UI ───────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                // Attachment button
                IconButton(
                    onClick = onAttachmentClick,
                    modifier = Modifier.size(28.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.AddCircle,
                        contentDescription = "Attach file",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(28.dp),
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Camera shortcut
                IconButton(
                    onClick = onCameraClick,
                    modifier = Modifier.size(28.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = "Gallery / Camera",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(28.dp),
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Text input field
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    BasicTextField(
                        value = text,
                        onValueChange = onTextChange,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        decorationBox = { innerTextField ->
                            if (text.isEmpty()) {
                                Text(
                                    text = "Message",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                )
                            }
                            innerTextField()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Emoji button
                IconButton(
                    onClick = onEmojiClick,
                    modifier = Modifier.size(28.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEmotions,
                        contentDescription = "Emoji picker",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(28.dp),
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Send or Voice button
                if (text.isNotBlank()) {
                    // Send button
                    IconButton(
                        onClick = onSend,
                        modifier = Modifier.size(28.dp),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send message",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                } else {
                    // Voice record button (long-press)
                    VoiceRecordButton(
                        onStart = onStartRecording,
                        onStop = onStopRecording,
                    )
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Voice record button
// ═════════════════════════════════════════════════════════════════════════════

/**
 * A mic button that the user presses and holds to record a voice message.
 * In a real implementation this would use [Modifier.pointerInput] with
 * detectTapGestures for onLongPress / onRelease.
 */
@Composable
private fun VoiceRecordButton(
    onStart: () -> Unit,
    onStop: () -> Unit,
) {
    var isPressed by remember { mutableStateOf(false) }

    // Visual feedback
    val tint by animateColorAsState(
        targetValue = if (isPressed) Color(0xFFE53935) else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(150),
        label = "micTint",
    )

    IconButton(
        onClick = {
            // Toggle: in production this would be a long-press gesture
            if (!isPressed) {
                isPressed = true
                onStart()
            } else {
                isPressed = false
                onStop()
            }
        },
        modifier = Modifier.size(28.dp),
    ) {
        Icon(
            imageVector = Icons.Default.Mic,
            contentDescription = if (isPressed) "Stop recording" else "Start voice recording",
            tint = tint,
            modifier = Modifier.size(28.dp),
        )
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Recording bar (shown while voice is being recorded)
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Inline recording UI shown when the user is recording a voice message.
 *
 * Displays a live waveform simulation, elapsed time, and a stop/trash button.
 */
@Composable
private fun RecordingBar(
    durationMs: Long,
    onStop: () -> Unit,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "recordPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseAlpha",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Red recording indicator dot
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(Color(0xFFE53935).copy(alpha = pulseAlpha)),
        )

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = formatDuration(durationMs),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(60.dp),
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Simulated waveform
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            modifier = Modifier.weight(1f).height(30.dp),
        ) {
            val sampleWaveform = listOf(0.2f, 0.6f, 0.9f, 0.4f, 0.7f, 1.0f, 0.5f, 0.3f, 0.8f, 0.6f)
            sampleWaveform.forEach { amp ->
                val barHeight = (amp * 26).dp.coerceAtLeast(4.dp)
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(barHeight)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Stop / send button
        IconButton(onClick = onStop) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "Send voice message",
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Utility formatters
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Formats a timestamp as a short time string (e.g. "10:30 AM").
 */
private fun formatMessageTime(timestampMillis: Long): String {
    val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
    return sdf.format(Date(timestampMillis))
}

/**
 * Formats a timestamp as a date separator label.
 *
 * - Today → "Today"
 * - Yesterday → "Yesterday"
 * - This year → "Mon DD" (e.g. "Jun 1")
 * - Other years → "Mon DD, YYYY"
 */
private fun formatDateSeparator(timestampMillis: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestampMillis
    val day = 86_400_000L

    return when {
        diff < day -> "Today"
        diff < 2 * day -> "Yesterday"
        else -> {
            val sdf = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
            sdf.format(Date(timestampMillis))
        }
    }
}

/**
 * Formats a duration in milliseconds as "m:ss" (e.g. "1:23").
 */
private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

/**
 * Formats a file size in bytes to a human-readable string (e.g. "2.5 MB").
 */
private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
        bytes < 1024 * 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
        else -> "%.1f GB".format(bytes / (1024.0 * 1024.0 * 1024.0))
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Mock data — replaces repository layer during prototyping
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Returns a hardcoded list of sample messages for a given [chatId].
 *
 * Includes a variety of message types to exercise all UI branches:
 * text, image, video, voice, system events, and reactions.
 */
private fun mockMessages(chatId: String): List<Message> {
    val now = System.currentTimeMillis()
    val minute = 60_000L
    val hour = 60 * minute

    return listOf(
        // ── Older messages (appear higher up, before reverse) ──────────
        Message(
            id = "${chatId}_sys_1",
            chatId = chatId,
            senderId = "system",
            senderName = "System",
            content = MessageContent.System(
                text = "Messages are end-to-end encrypted. Tap for more info.",
                action = com.novamesh.domain.model.SystemAction.GENERIC,
            ),
            type = MessageType.SYSTEM,
            timestamp = now - 2 * hour - 30 * minute,
            status = MessageStatus.SENT,
            readBy = emptyList(),
            deliveryStatus = com.novamesh.domain.model.DeliveryStatus.SENT,
        ),
        Message(
            id = "${chatId}_img_1",
            chatId = chatId,
            senderId = "user_2",
            senderName = "Alice",
            content = MessageContent.Image(
                uri = "file:///photos/sunset.jpg",
                thumbnailUri = null,
                caption = "Look at this sunset! 🌅",
            ),
            type = MessageType.IMAGE,
            timestamp = now - 2 * hour - 20 * minute,
            status = MessageStatus.READ,
            reactions = mapOf("user_1" to "🔥", "user_3" to "🔥", "user_4" to "❤️"),
            readBy = listOf("user_1", "user_3"),
            deliveryStatus = com.novamesh.domain.model.DeliveryStatus.READ,
        ),
        Message(
            id = "${chatId}_text_1",
            chatId = chatId,
            senderId = "me",
            senderName = "Me",
            content = MessageContent.Text("That's beautiful! Where was this taken?"),
            type = MessageType.TEXT,
            timestamp = now - 2 * hour - 15 * minute,
            status = MessageStatus.READ,
            reactions = mapOf("user_2" to "👍"),
            readBy = listOf("user_2"),
            deliveryStatus = com.novamesh.domain.model.DeliveryStatus.READ,
        ),
        Message(
            id = "${chatId}_text_2",
            chatId = chatId,
            senderId = "user_2",
            senderName = "Alice",
            content = MessageContent.Text("It's from the hike last weekend! 📸"),
            type = MessageType.TEXT,
            timestamp = now - 2 * hour - 10 * minute,
            status = MessageStatus.READ,
            reactions = mapOf("me" to "❤️"),
            readBy = listOf("me"),
            deliveryStatus = com.novamesh.domain.model.DeliveryStatus.READ,
        ),
        Message(
            id = "${chatId}_voice_1",
            chatId = chatId,
            senderId = "user_2",
            senderName = "Alice",
            content = MessageContent.Voice(
                uri = "file:///voice/msg_${chatId}_001.ogg",
                durationMs = 12_000,
                waveform = listOf(0.1f, 0.4f, 0.7f, 0.9f, 0.5f, 0.8f, 0.3f),
            ),
            type = MessageType.VOICE,
            timestamp = now - hour - 30 * minute,
            status = MessageStatus.DELIVERED,
            readBy = emptyList(),
            deliveryStatus = com.novamesh.domain.model.DeliveryStatus.DELIVERED,
        ),
        Message(
            id = "${chatId}_text_3",
            chatId = chatId,
            senderId = "me",
            senderName = "Me",
            content = MessageContent.Text("Great, let's plan another one soon!"),
            type = MessageType.TEXT,
            timestamp = now - hour - 25 * minute,
            status = MessageStatus.DELIVERED,
            readBy = emptyList(),
            deliveryStatus = com.novamesh.domain.model.DeliveryStatus.DELIVERED,
        ),
        Message(
            id = "${chatId}_sys_2",
            chatId = chatId,
            senderId = "system",
            senderName = "System",
            content = MessageContent.System(
                text = "Disappearing messages enabled (30s)",
                action = com.novamesh.domain.model.SystemAction.DISAPPEARING_MESSAGES_ENABLED,
            ),
            type = MessageType.SYSTEM,
            timestamp = now - 30 * minute,
            status = MessageStatus.SENT,
            readBy = emptyList(),
            deliveryStatus = com.novamesh.domain.model.DeliveryStatus.SENT,
        ),
        Message(
            id = "${chatId}_video_1",
            chatId = chatId,
            senderId = "user_2",
            senderName = "Alice",
            content = MessageContent.Video(
                uri = "file:///videos/timelapse.mp4",
                thumbnailUri = null,
                durationMs = 45_000,
            ),
            type = MessageType.VIDEO,
            timestamp = now - 15 * minute,
            status = MessageStatus.DELIVERED,
            reactions = mapOf("me" to "🔥", "user_3" to "🔥"),
            readBy = emptyList(),
            deliveryStatus = com.novamesh.domain.model.DeliveryStatus.DELIVERED,
        ),
        // ── Newest message (appears at bottom after reverse) ───────────
        Message(
            id = "${chatId}_text_4",
            chatId = chatId,
            senderId = "me",
            senderName = "Me",
            content = MessageContent.Text("Awesome video! 🔥"),
            type = MessageType.TEXT,
            timestamp = now - 5 * minute,
            status = MessageStatus.SENT,
            readBy = emptyList(),
            deliveryStatus = com.novamesh.domain.model.DeliveryStatus.SENT,
        ),
    )
}
