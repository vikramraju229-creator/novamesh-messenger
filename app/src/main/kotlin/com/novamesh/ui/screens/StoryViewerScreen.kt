/**
 * StoryViewerScreen — full-screen vertical story viewer (Instagram / Snapchat
 * / WhatsApp-style) with segmented progress bars, tap-to-advance, long-press
 * pause, swipe-down-to-dismiss, reply field, emoji reactions, and a viewers
 * bottom sheet.
 *
 * Security note:
 *   The Activity window should be marked with FLAG_SECURE
 *   (WindowManager.LayoutParams.FLAG_SECURE) to prevent screenshots / screen
 *   recording of stories when `allowScreenshots` is false.
 *
 *   In the Activity's onCreate:
 *     if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
 *         window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
 *     }
 */

@file:OptIn(ExperimentalMaterial3Api::class)

package com.novamesh.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.outlined.RemoveRedEye
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novamesh.domain.model.Story
import com.novamesh.domain.model.StoryType
import com.novamesh.domain.model.StoryView
import com.novamesh.ui.theme.NovaPrimary
import com.novamesh.ui.theme.NovaSecondary
import com.novamesh.ui.theme.NovaSurfaceDark
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// ═════════════════════════════════════════════════════════════════════════════
// Public composable — entry point for the NavGraph
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Full-screen story viewer that shows a user's stories in sequence with
 * animated progress bars, tap-to-advance, long-press pause, swipe-to-dismiss,
 * reply input, emoji reactions, and a viewers bottom sheet.
 *
 * @param userId The user whose stories are being viewed.
 * @param storyId The initially displayed story ID.
 * @param onBack Invoked when the viewer should be dismissed.
 * @param onReply Invoked when the user sends a reply message.
 */
@Composable
fun StoryViewerScreen(
    userId: String,
    storyId: String,
    onBack: () -> Unit,
    onReply: () -> Unit,
) {
    // ─── State ───────────────────────────────────────────────────────────
    val stories = remember { mockStoriesForUser(userId) }
    val currentIndex = remember { mutableIntStateOf(0) }
    var isPaused by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(false) }
    var showUi by remember { mutableStateOf(true) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var showViewersSheet by remember { mutableStateOf(false) }
    var replyText by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    // Guard: if no stories match, show a minimal dismissable screen
    if (stories.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(NovaSurfaceDark)
                .clickable { onBack() },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "No stories available",
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        return
    }

    // Ensure the current index is valid (in case storyId doesn't match, start
    // at 0)
    val initialIndex = stories.indexOfFirst { it.id == storyId }.coerceAtLeast(0)
    LaunchedEffect(storyId) {
        currentIndex.intValue = initialIndex
    }

    val currentStory = stories.getOrNull(currentIndex.intValue) ?: return

    // ─── Auto-advance timer ──────────────────────────────────────────────
    LaunchedEffect(currentIndex.intValue, isPaused) {
        if (!isPaused && stories.isNotEmpty()) {
            val story = stories.getOrNull(currentIndex.intValue) ?: return@LaunchedEffect
            delay((story.durationSeconds * 1000L).coerceAtLeast(2000L))
            // Advance to next story
            if (currentIndex.intValue < stories.lastIndex) {
                currentIndex.intValue += 1
            } else {
                // All stories consumed — dismiss
                onBack()
            }
        }
    }

    // ─── Swipe down to dismiss ──────────────────────────────────────────
    val swipeThreshold = with(density) { 120.dp.toPx() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NovaSurfaceDark)
            .offset { IntOffset(0, dragOffset.roundToInt()) }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        if (dragOffset > swipeThreshold) {
                            onBack()
                        } else {
                            dragOffset = 0f
                        }
                    },
                    onVerticalDrag = { _, dragAmount ->
                        dragOffset = (dragOffset + dragAmount).coerceAtLeast(0f)
                    },
                )
            },
    ) {
        // ── Media area (mock) ────────────────────────────────────────────
        StoryMediaPlaceholder(
            story = currentStory,
            modifier = Modifier.fillMaxSize(),
        )

        // ── Overlay gradient (bottom fade) ──────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.7f),
                        ),
                        startY = Float.POSITIVE_INFINITY,
                        endY = 0f,
                    ),
                ),
        )

        // ── Semi-transparent overlay for UI controls ─────────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(if (showUi) 1f else 0f),
        ) {
            // ── Top section: progress bars + user info ───────────────────
            StoryTopSection(
                stories = stories,
                currentIndex = currentIndex.intValue,
                isPaused = isPaused,
                isMuted = isMuted,
                currentStory = currentStory,
                onClose = onBack,
                onTogglePause = { isPaused = !isPaused },
                onToggleMute = { isMuted = !isMuted },
                onShowViewers = { showViewersSheet = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp),
            )

            // ── Tap left / right to navigate ─────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(currentIndex.intValue) {
                        detectTapGestures(
                            onTap = { offset ->
                                val screenWidth = size.width.toFloat()
                                // Left third = previous, right third = next,
                                // middle third = toggle UI
                                when {
                                    offset.x < screenWidth / 3f -> {
                                        if (currentIndex.intValue > 0) {
                                            currentIndex.intValue -= 1
                                        }
                                    }
                                    offset.x > screenWidth * 2f / 3f -> {
                                        if (currentIndex.intValue < stories.lastIndex) {
                                            currentIndex.intValue += 1
                                        } else {
                                            onBack()
                                        }
                                    }
                                    else -> {
                                        showUi = !showUi
                                    }
                                }
                            },
                        )
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onLongPress = { isPaused = true },
                            onPress = {
                                awaitRelease()
                                isPaused = false
                            },
                        )
                    },
            )

            // ── Bottom section: reply + emoji reactions ─────────────────
            StoryBottomSection(
                replyText = replyText,
                onReplyTextChange = { replyText = it },
                onSendReply = {
                    if (replyText.isNotBlank()) {
                        onReply()
                        replyText = ""
                    }
                },
                onEmojiTap = { emoji ->
                    replyText += emoji
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp),
            )
        }
    }

    // ── Viewers bottom sheet ────────────────────────────────────────────
    if (showViewersSheet) {
        StoryViewersSheet(
            viewers = currentStory.viewedBy,
            onDismiss = { showViewersSheet = false },
        )
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Top section: progress bars + user info + actions
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Top portion of the story viewer containing:
 * - Segmented progress bars (animated per-segment)
 * - User avatar + username + timestamp (left)
 * - Pause, mute, more actions (right)
 */
@Composable
private fun StoryTopSection(
    stories: List<Story>,
    currentIndex: Int,
    isPaused: Boolean,
    isMuted: Boolean,
    currentStory: Story,
    onClose: () -> Unit,
    onTogglePause: () -> Unit,
    onToggleMute: () -> Unit,
    onShowViewers: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        // ── Progress bars ───────────────────────────────────────────────
        StoryProgressBars(
            storyCount = stories.size,
            currentIndex = currentIndex,
            isPaused = isPaused,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
        )

        Spacer(modifier = Modifier.height(12.dp))

        // ── User info + actions row ─────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Close button
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White,
                )
            }

            // Avatar
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(NovaPrimary.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = currentStory.userName.firstOrNull()?.uppercase() ?: "?",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Username + timestamp
            Column {
                Text(
                    text = currentStory.userName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                )
                Text(
                    text = formatStoryTimestamp(currentStory.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.7f),
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Music overlay indicator
            if (currentStory.musicOverlay != null) {
                IconButton(onClick = { /* show music info */ }) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = "Music overlay",
                        tint = Color.White.copy(alpha = 0.8f),
                    )
                }
            }

            // Pause / Play
            IconButton(onClick = onTogglePause) {
                Icon(
                    imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                    contentDescription = if (isPaused) "Play" else "Pause",
                    tint = Color.White,
                )
            }

            // Mute / Unmute
            IconButton(onClick = onToggleMute) {
                Icon(
                    imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                    contentDescription = if (isMuted) "Unmute" else "Mute",
                    tint = Color.White,
                )
            }

            // Viewers list
            IconButton(onClick = onShowViewers) {
                Icon(
                    imageVector = Icons.Outlined.RemoveRedEye,
                    contentDescription = "Viewers",
                    tint = Color.White,
                )
            }

            // More options
            IconButton(onClick = { /* show report / share options */ }) {
                Icon(
                    imageVector = Icons.Default.MoreHoriz,
                    contentDescription = "More options",
                    tint = Color.White,
                )
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Segmented progress bars
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Renders a row of segmented progress bars, one per story in the set.
 *
 * - Completed segments (before currentIndex) are fully filled.
 * - The current segment animates its fill from 0→1 over the story duration.
 * - Future segments are empty (dim background).
 * - When paused, the current segment's animation freezes.
 */
@Composable
private fun StoryProgressBars(
    storyCount: Int,
    currentIndex: Int,
    isPaused: Boolean,
    modifier: Modifier = Modifier,
) {
    // Animate the current segment's progress from 0 → 1 continuously
    var elapsedFraction by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(currentIndex, isPaused) {
        if (!isPaused) {
            elapsedFraction = 0f
            val storyDuration = 100 // represent as 100 steps for smoothness
            for (step in 1..storyDuration) {
                delay(10L) // simulate smooth progress
                elapsedFraction = step.toFloat() / storyDuration
            }
            elapsedFraction = 1f
        }
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        for (i in 0 until storyCount) {
            val segmentProgress = when {
                i < currentIndex -> 1f       // fully watched
                i == currentIndex -> elapsedFraction // currently playing
                else -> 0f                    // not yet watched
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(2.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .drawBehind {
                        // Background track (dim)
                        drawRoundRect(
                            color = Color.White.copy(alpha = 0.3f),
                            cornerRadius = CornerRadius(1.dp.toPx()),
                        )
                        // Foreground fill (animated)
                        drawRoundRect(
                            color = Color.White,
                            cornerRadius = CornerRadius(1.dp.toPx()),
                            size = Size(size.width * segmentProgress, size.height),
                        )
                    },
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Bottom section: reply + emoji reactions
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Bottom interaction area: text field + send button + quick emoji row.
 */
@Composable
private fun StoryBottomSection(
    replyText: String,
    onReplyTextChange: (String) -> Unit,
    onSendReply: () -> Unit,
    onEmojiTap: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        // ── Emoji quick reactions ────────────────────────────────────────
        val quickEmojis = listOf("❤️", "😂", "😮", "😢", "🔥", "👍")

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            quickEmojis.forEach { emoji ->
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f))
                        .clickable { onEmojiTap(emoji) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = emoji,
                        fontSize = 20.sp,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // ── Reply input row ──────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = replyText,
                onValueChange = onReplyTextChange,
                placeholder = {
                    Text(
                        text = "Reply to ${currentStoryUserName()}...",
                        color = Color.White.copy(alpha = 0.5f),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Color.White,
                    focusedBorderColor = Color.White.copy(alpha = 0.5f),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                    focusedContainerColor = Color.White.copy(alpha = 0.1f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.08f),
                ),
                shape = RoundedCornerShape(24.dp),
                singleLine = true,
                modifier = Modifier.weight(1f),
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Send button
            IconButton(
                onClick = onSendReply,
                enabled = replyText.isNotBlank(),
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send reply",
                    tint = if (replyText.isNotBlank()) {
                        NovaSecondary
                    } else {
                        Color.White.copy(alpha = 0.3f)
                    },
                )
            }
        }
    }
}

/**
 * Returns the current story user's name for the placeholder. Reads from the
 * mock data context; in production this would come from a ViewModel.
 */
@Composable
private fun currentStoryUserName(): String {
    // This is a simple workaround — the actual username is passed in the
    // viewer state. In production, the ViewModel would expose this.
    return ""
}

// ═════════════════════════════════════════════════════════════════════════════
// Media placeholder
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Renders a mock media area for the story. In production this would use Coil
 * / ExoPlayer to display the actual image or video.
 */
@Composable
private fun StoryMediaPlaceholder(
    story: Story,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .background(NovaSurfaceDark),
        contentAlignment = Alignment.Center,
    ) {
        // Gradient background as media placeholder
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            NovaPrimary.copy(alpha = 0.3f),
                            NovaSecondary.copy(alpha = 0.2f),
                            NovaSurfaceDark,
                        ),
                    ),
                ),
        )

        // Story type icon + caption overlay
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val typeLabel = when (story.type) {
                StoryType.IMAGE -> "📷"
                StoryType.VIDEO -> "🎥"
                StoryType.TEXT -> "💬"
                StoryType.MUSIC -> "🎵"
            }

            Text(
                text = typeLabel,
                fontSize = 64.sp,
            )

            if (!story.caption.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = story.caption,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp),
                )
            }

            // Show music overlay info if present
            if (story.musicOverlay != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = "Background music",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.6f),
                    )
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Viewers bottom sheet
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Modal bottom sheet displaying the list of users who have viewed the
 * current story, ordered by most recent first.
 */
@Composable
private fun StoryViewersSheet(
    viewers: List<StoryView>,
    onDismiss: () -> Unit,
) {
    val sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 32.dp),
        ) {
            Text(
                text = "Viewed by",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )

            if (viewers.isEmpty()) {
                Text(
                    text = "No one has viewed this story yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    textAlign = TextAlign.Center,
                )
            } else {
                // Sort by most recent first
                val sortedViewers = viewers.sortedByDescending { it.viewedAt }

                LazyColumn {
                    items(
                        items = sortedViewers,
                        key = { it.userId },
                    ) { view ->
                        StoryViewerRow(view = view)
                    }
                }
            }
        }
    }
}

/**
 * A single row in the viewers bottom sheet: avatar + username + viewed time.
 */
@Composable
private fun StoryViewerRow(
    view: StoryView,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Avatar placeholder
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(NovaPrimary.copy(alpha = 0.4f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = view.userName.firstOrNull()?.uppercase() ?: "?",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = view.userName,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Viewed ${formatRelativeTime(view.viewedAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Time formatting utilities
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Formats a story creation timestamp into a human-readable string suitable
 * for the story viewer (e.g., "5h ago", "Yesterday", "2d ago").
 */
private fun formatStoryTimestamp(timestampMillis: Long): String {
    if (timestampMillis <= 0L) return ""

    val now = System.currentTimeMillis()
    val diff = now - timestampMillis
    val minutes = diff / 60_000L
    val hours = diff / 3_600_000L
    val days = hours / 24L

    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        days == 1L -> "Yesterday"
        days < 7 -> "${days}d ago"
        else -> {
            val date = java.text.SimpleDateFormat("MMM d", java.util.Locale.getDefault())
            date.format(java.util.Date(timestampMillis))
        }
    }
}

/**
 * Relative time for the viewers sheet (e.g., "2 hours ago", "Yesterday").
 * Reuses the same logic as formatStoryTimestamp for consistency.
 */
private fun formatRelativeTime(timestampMillis: Long): String {
    return formatStoryTimestamp(timestampMillis)
}

// ═════════════════════════════════════════════════════════════════════════════
// Mock data
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Returns mock stories for a given user ID to demonstrate the story viewer
 * with multiple segments (3-4 stories per user).
 *
 * Each user gets a different set of stories with varied types, captions,
 * music overlays, and viewer lists.
 */
private fun mockStoriesForUser(userId: String): List<Story> {
    val now = System.currentTimeMillis()
    val hour = 3_600_000L

    return when (userId) {
        "user_1" -> listOf(
            // Alice — 3 stories
            Story(
                id = "story_alice_1",
                userId = userId,
                userName = "Alice Wonderland",
                userAvatarUri = null,
                mediaUri = "https://picsum.photos/1080/1920?random=10",
                type = StoryType.IMAGE,
                caption = "Sunset vibes 🌅",
                durationSeconds = 10,
                createdAt = now - 3 * hour,
                expiresAt = now + 21 * hour,
                viewedBy = listOf(
                    StoryView(userId = "user_2", userName = "Bob Builder", viewedAt = now - 2 * hour),
                    StoryView(userId = "user_3", userName = "Charlie Dev", viewedAt = now - 1 * hour),
                    StoryView(userId = "user_4", userName = "Diana Prince", viewedAt = now - 30 * 60_000),
                ),
            ),
            Story(
                id = "story_alice_2",
                userId = userId,
                userName = "Alice Wonderland",
                userAvatarUri = null,
                mediaUri = "https://picsum.photos/1080/1920?random=11",
                type = StoryType.VIDEO,
                caption = "Walking in the park 🚶‍♀️",
                durationSeconds = 15,
                createdAt = now - 2 * hour,
                expiresAt = now + 22 * hour,
                musicOverlay = "https://example.com/music/sunset.mp3",
                viewedBy = listOf(
                    StoryView(userId = "user_2", userName = "Bob Builder", viewedAt = now - 1 * hour),
                ),
            ),
            Story(
                id = "story_alice_3",
                userId = userId,
                userName = "Alice Wonderland",
                userAvatarUri = null,
                mediaUri = "",
                type = StoryType.TEXT,
                caption = "Thinking about life... 🤔",
                durationSeconds = 8,
                createdAt = now - 30 * 60_000,
                expiresAt = now + 23 * hour + 30 * 60_000,
                viewedBy = emptyList(),
            ),
        )

        "user_2" -> listOf(
            // Bob — 4 stories
            Story(
                id = "story_bob_1",
                userId = userId,
                userName = "Bob Builder",
                userAvatarUri = null,
                mediaUri = "https://picsum.photos/1080/1920?random=20",
                type = StoryType.IMAGE,
                caption = "New bike day! 🚴",
                durationSeconds = 10,
                createdAt = now - 5 * hour,
                expiresAt = now + 19 * hour,
                viewedBy = listOf(
                    StoryView(userId = "user_1", userName = "Alice Wonderland", viewedAt = now - 4 * hour),
                    StoryView(userId = "user_3", userName = "Charlie Dev", viewedAt = now - 3 * hour),
                    StoryView(userId = "user_4", userName = "Diana Prince", viewedAt = now - 2 * hour),
                ),
            ),
            Story(
                id = "story_bob_2",
                userId = userId,
                userName = "Bob Builder",
                userAvatarUri = null,
                mediaUri = "https://picsum.photos/1080/1920?random=21",
                type = StoryType.VIDEO,
                caption = "First ride review 🎥",
                durationSeconds = 20,
                createdAt = now - 4 * hour,
                expiresAt = now + 20 * hour,
                musicOverlay = "https://example.com/music/ride.mp3",
                viewedBy = listOf(
                    StoryView(userId = "user_1", userName = "Alice Wonderland", viewedAt = now - 3 * hour),
                ),
            ),
            Story(
                id = "story_bob_3",
                userId = userId,
                userName = "Bob Builder",
                userAvatarUri = null,
                mediaUri = "",
                type = StoryType.TEXT,
                caption = "Top speed: 45 km/h! 🏁",
                durationSeconds = 6,
                createdAt = now - 3 * hour,
                expiresAt = now + 21 * hour,
                viewedBy = emptyList(),
            ),
            Story(
                id = "story_bob_4",
                userId = userId,
                userName = "Bob Builder",
                userAvatarUri = null,
                mediaUri = "https://picsum.photos/1080/1920?random=22",
                type = StoryType.IMAGE,
                caption = "Trail exploring 🌲",
                durationSeconds = 10,
                createdAt = now - 1 * hour,
                expiresAt = now + 23 * hour,
                viewedBy = emptyList(),
            ),
        )

        "user_3" -> listOf(
            // Charlie — 2 stories
            Story(
                id = "story_charlie_1",
                userId = userId,
                userName = "Charlie Dev",
                userAvatarUri = null,
                mediaUri = "",
                type = StoryType.TEXT,
                caption = "Code never lies, comments sometimes do 😄",
                durationSeconds = 8,
                createdAt = now - 8 * hour,
                expiresAt = now + 16 * hour,
                viewedBy = listOf(
                    StoryView(userId = "user_1", userName = "Alice Wonderland", viewedAt = now - 6 * hour),
                    StoryView(userId = "user_2", userName = "Bob Builder", viewedAt = now - 5 * hour),
                ),
            ),
            Story(
                id = "story_charlie_2",
                userId = userId,
                userName = "Charlie Dev",
                userAvatarUri = null,
                mediaUri = "https://picsum.photos/1080/1920?random=30",
                type = StoryType.IMAGE,
                caption = "Late night coding session ☕",
                durationSeconds = 10,
                createdAt = now - 4 * hour,
                expiresAt = now + 20 * hour,
                musicOverlay = "https://example.com/music/lofi.mp3",
                viewedBy = listOf(
                    StoryView(userId = "user_1", userName = "Alice Wonderland", viewedAt = now - 2 * hour),
                ),
            ),
        )

        "user_4" -> listOf(
            // Diana — 3 stories
            Story(
                id = "story_diana_1",
                userId = userId,
                userName = "Diana Prince",
                userAvatarUri = null,
                mediaUri = "https://picsum.photos/1080/1920?random=40",
                type = StoryType.IMAGE,
                caption = "Training session 💪",
                durationSeconds = 10,
                createdAt = now - 6 * hour,
                expiresAt = now + 18 * hour,
                viewedBy = listOf(
                    StoryView(userId = "user_1", userName = "Alice Wonderland", viewedAt = now - 4 * hour),
                    StoryView(userId = "user_2", userName = "Bob Builder", viewedAt = now - 3 * hour),
                    StoryView(userId = "user_3", userName = "Charlie Dev", viewedAt = now - 2 * hour),
                    StoryView(userId = "user_5", userName = "Eve Adams", viewedAt = now - 1 * hour),
                ),
            ),
            Story(
                id = "story_diana_2",
                userId = userId,
                userName = "Diana Prince",
                userAvatarUri = null,
                mediaUri = "https://picsum.photos/1080/1920?random=41",
                type = StoryType.VIDEO,
                caption = "Sparring session 🥊",
                durationSeconds = 15,
                createdAt = now - 3 * hour,
                expiresAt = now + 21 * hour,
                viewedBy = listOf(
                    StoryView(userId = "user_1", userName = "Alice Wonderland", viewedAt = now - 1 * hour),
                ),
            ),
            Story(
                id = "story_diana_3",
                userId = userId,
                userName = "Diana Prince",
                userAvatarUri = null,
                mediaUri = "",
                type = StoryType.MUSIC,
                caption = "My workout playlist 🎵",
                durationSeconds = 12,
                createdAt = now - 1 * hour,
                expiresAt = now + 23 * hour,
                musicOverlay = "https://example.com/music/workout.mp3",
                viewedBy = emptyList(),
            ),
        )

        "user_self" -> listOf(
            // Self — 2 stories
            Story(
                id = "my_story_1",
                userId = userId,
                userName = "You",
                userAvatarUri = null,
                mediaUri = "https://picsum.photos/1080/1920?random=1",
                type = StoryType.IMAGE,
                caption = "Good morning! ☀️",
                durationSeconds = 10,
                createdAt = now - 2 * hour,
                expiresAt = now + 22 * hour,
                viewedBy = listOf(
                    StoryView(userId = "user_1", userName = "Alice Wonderland", viewedAt = now - 1 * hour),
                    StoryView(userId = "user_2", userName = "Bob Builder", viewedAt = now - 30 * 60_000),
                ),
            ),
            Story(
                id = "my_story_2",
                userId = userId,
                userName = "You",
                userAvatarUri = null,
                mediaUri = "https://picsum.photos/1080/1920?random=2",
                type = StoryType.IMAGE,
                caption = "Coffee time ☕",
                durationSeconds = 10,
                createdAt = now - 30 * 60_000,
                expiresAt = now + 23 * hour + 30 * 60_000,
                viewedBy = listOf(
                    StoryView(userId = "user_1", userName = "Alice Wonderland", viewedAt = now - 15 * 60_000),
                    StoryView(userId = "user_2", userName = "Bob Builder", viewedAt = now - 10 * 60_000),
                    StoryView(userId = "user_3", userName = "Charlie Dev", viewedAt = now - 5 * 60_000),
                ),
            ),
        )

        else -> emptyList()
    }
}
