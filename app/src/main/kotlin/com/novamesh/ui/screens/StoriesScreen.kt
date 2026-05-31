/**
 * StoriesScreen — the main stories feed tab showing "My Story" and recent
 * contacts' stories with gradient rings (Instagram / Snapchat / WhatsApp-style).
 *
 * Features:
 * - My Story section: avatar + gradient ring + "+" add button
 * - Horizontal scrollable list of contacts with story rings
 * - Unseen stories: full purple-cyan-orange gradient ring
 * - Seen stories: muted gray ring
 * - Animated ring entrance on first load (rotate + fade)
 * - Empty state when no contacts have stories
 * - Top header with "Stories" title and camera icon button
 */

@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.novamesh.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novamesh.domain.model.Story
import com.novamesh.domain.model.StoryType
import com.novamesh.domain.model.StoryView
import com.novamesh.domain.model.User
import com.novamesh.ui.theme.NovaPrimary
import com.novamesh.ui.theme.NovaSecondary
import com.novamesh.ui.theme.NovaTertiary

// ═════════════════════════════════════════════════════════════════════════════
// Public composable — entry point for the NavGraph
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Main stories feed screen showing the user's own story and a scrollable list
 * of contacts with story rings.
 *
 * @param onStoryClick Invoked when a contact's story ring is tapped; receives
 *   the user's ID and the first (latest) story ID for that user.
 * @param onCameraClick Invoked when the camera icon in the header is tapped.
 */
@Composable
fun StoriesScreen(
    onStoryClick: (userId: String, storyId: String) -> Unit,
    onCameraClick: () -> Unit,
) {
    // ─── Mock data ───────────────────────────────────────────────────────
    val myUserId = "user_self"
    val myStories = remember { mockMyStories(myUserId) }
    val contacts = remember { mockContactStories() }

    val allContactsHaveSeenStories = contacts.all { it.isFullyViewed }

    Scaffold(
        topBar = {
            StoriesTopBar(onCameraClick = onCameraClick)
        },
        modifier = Modifier.fillMaxSize(),
    ) { paddingValues ->
        if (contacts.isEmpty() && myStories.isEmpty()) {
            StoriesEmptyState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(bottom = 16.dp),
            ) {
                // ── My Story section ──────────────────────────────────────
                item(key = "my_story") {
                    MyStorySection(
                        myUserId = myUserId,
                        hasStories = myStories.isNotEmpty(),
                        storyCount = myStories.size,
                        onAddClick = onCameraClick,
                        onMyStoryClick = {
                            if (myStories.isNotEmpty()) {
                                onStoryClick(myUserId, myStories.first().id)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                    )
                }

                // ── Recent Stories section ────────────────────────────────
                item(key = "recent_header") {
                    SectionHeader(
                        title = "Recent Stories",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, top = 20.dp, bottom = 8.dp),
                    )
                }

                item(key = "recent_list") {
                    if (contacts.isEmpty()) {
                        // Show empty state inline if no contacts have stories
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoLibrary,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No recent stories",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            )
                        }
                    } else {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            items(
                                items = contacts,
                                key = { it.user.id },
                            ) { contactStory ->
                                StoryRingItem(
                                    user = contactStory.user,
                                    stories = contactStory.stories,
                                    isFullyViewed = contactStory.isFullyViewed,
                                    onClick = {
                                        val firstStoryId = contactStory.stories.firstOrNull()?.id
                                        if (firstStoryId != null) {
                                            onStoryClick(contactStory.user.id, firstStoryId)
                                        }
                                    },
                                )
                            }
                        }
                    }
                }

                // ── Viewed stories section (if all are seen) ─────────────
                if (allContactsHaveSeenStories && contacts.isNotEmpty()) {
                    item(key = "all_viewed_hint") {
                        Text(
                            text = "All caught up! Check back later for new stories.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 16.dp),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Top app bar
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Stories screen top bar with the "Stories" title and a camera icon button.
 */
@Composable
private fun StoriesTopBar(
    onCameraClick: () -> Unit,
) {
    TopAppBar(
        title = {
            Text(
                text = "Stories",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
        },
        actions = {
            IconButton(onClick = onCameraClick) {
                Icon(
                    imageVector = Icons.Filled.CameraAlt,
                    contentDescription = "Camera",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
        ),
    )
}

// ═════════════════════════════════════════════════════════════════════════════
// My Story section
// ═════════════════════════════════════════════════════════════════════════════

/**
 * "My Story" row showing the current user's avatar with a gradient ring and a
 * small "+" add button when no story exists, or a tap-able ring when they have
 * stories.
 */
@Composable
private fun MyStorySection(
    myUserId: String,
    hasStories: Boolean,
    storyCount: Int,
    onAddClick: () -> Unit,
    onMyStoryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // ── Avatar ring ──────────────────────────────────────────────────
        Box(
            modifier = Modifier.size(72.dp),
            contentAlignment = Alignment.Center,
        ) {
            // Gradient ring behind the avatar
            val ringColors = if (hasStories) {
                listOf(NovaPrimary, NovaSecondary, NovaTertiary)
            } else {
                listOf(
                    MaterialTheme.colorScheme.outlineVariant,
                    MaterialTheme.colorScheme.outlineVariant,
                )
            }

            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .drawBehind {
                        drawCircle(
                            brush = Brush.sweepGradient(
                                colors = ringColors,
                                center = Offset(size.width / 2f, size.height / 2f),
                            ),
                            style = Stroke(width = 3.dp.toPx()),
                        )
                    }
                    .clickable(enabled = hasStories) { onMyStoryClick() },
                contentAlignment = Alignment.Center,
            ) {
                // Avatar placeholder
                Box(
                    modifier = Modifier
                        .size(62.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "You",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }

            // "+" add button (bottom-right corner)
            if (!hasStories) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(NovaPrimary)
                        .clickable { onAddClick() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add story",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // ── Label ────────────────────────────────────────────────────────
        Column {
            Text(
                text = "My Story",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (hasStories) FontWeight.SemiBold else FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = if (hasStories) {
                    "$storyCount story${if (storyCount != 1) "ies" else "y"}"
                } else {
                    "Tap to add a story"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Section header
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Reusable section header with a bold title.
 */
@Composable
private fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier,
    )
}

// ═════════════════════════════════════════════════════════════════════════════
// Story ring item
// ═════════════════════════════════════════════════════════════════════════════

/**
 * A single contact story ring in the horizontal list.
 *
 * Displays a circular avatar surrounded by:
 * - A multi-color gradient ring if there are unseen stories.
 * - A muted gray ring if all stories have been viewed.
 *
 * The ring animates in on first composition (rotation + alpha).
 */
@Composable
private fun StoryRingItem(
    user: User,
    stories: List<Story>,
    isFullyViewed: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // ─── First-load animation ────────────────────────────────────────────
    var hasAnimated by remember { mutableStateOf(false) }
    val animatedAlpha by animateFloatAsState(
        targetValue = if (hasAnimated) 1f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "ringAlpha",
    )
    val animatedRotation by animateFloatAsState(
        targetValue = if (hasAnimated) 360f else 0f,
        animationSpec = tween(durationMillis = 600),
        label = "ringRotation",
    )

    LaunchedEffect(Unit) {
        hasAnimated = true
    }

    // Ring colors: gradient for unseen, gray outline for viewed
    val ringColors = if (isFullyViewed) {
        listOf(
            MaterialTheme.colorScheme.outlineVariant,
            MaterialTheme.colorScheme.outlineVariant,
        )
    } else {
        listOf(NovaPrimary, NovaSecondary, NovaTertiary)
    }

    Column(
        modifier = modifier
            .width(76.dp)
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // ── Avatar with ring ─────────────────────────────────────────────
        Box(
            modifier = Modifier.size(64.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .drawBehind {
                        drawCircle(
                            brush = Brush.sweepGradient(
                                colors = ringColors,
                                center = Offset(size.width / 2f, size.height / 2f),
                            ),
                            style = Stroke(width = 2.5.dp.toPx()),
                            alpha = animatedAlpha,
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                // Avatar circle
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = user.displayName.firstOrNull()?.uppercase() ?: "?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // ── Username label ───────────────────────────────────────────────
        Text(
            text = user.displayName.split(" ").firstOrNull() ?: user.username,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = if (isFullyViewed) {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Empty state
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Shown when no stories are available from any contact.
 */
@Composable
private fun StoriesEmptyState(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Default.PhotoLibrary,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "No stories yet",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Contacts you follow will appear here when they post stories",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 48.dp),
        )
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Data holders for mock state
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Groups a user with their active stories and a pre-computed "fully viewed"
 * flag so the ring rendering is simple.
 */
private data class ContactStoryGroup(
    val user: User,
    val stories: List<Story>,
    val isFullyViewed: Boolean,
)

// ═════════════════════════════════════════════════════════════════════════════
// Mock data
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Returns mock stories for the current user ("My Story") to demonstrate the
 * top section with an existing story.
 */
private fun mockMyStories(myUserId: String): List<Story> {
    val now = System.currentTimeMillis()
    val hour = 3_600_000L

    return listOf(
        Story(
            id = "my_story_1",
            userId = myUserId,
            userName = "You",
            userAvatarUri = null,
            mediaUri = "https://picsum.photos/1080/1920?random=1",
            type = StoryType.IMAGE,
            caption = "Good morning! ☀️",
            durationSeconds = 10,
            createdAt = now - 2 * hour,
            expiresAt = now + 22 * hour,
            viewedBy = listOf(
                StoryView(userId = "user_1", userName = "Alice", viewedAt = now - 1 * hour),
                StoryView(userId = "user_2", userName = "Bob", viewedAt = now - 30 * 60_000),
            ),
        ),
    )
}

/**
 * Returns a list of mock contacts with their stories for the horizontal feed.
 *
 * - 8 contacts total
 * - Some have unseen stories (full gradient ring)
 * - Some have all stories viewed (gray muted ring)
 */
private fun mockContactStories(): List<ContactStoryGroup> {
    val now = System.currentTimeMillis()
    val minute = 60_000L
    val hour = 60 * minute

    // ── Contact definitions ──────────────────────────────────────────────
    val alice = User(
        id = "user_1",
        username = "alice_wonder",
        displayName = "Alice Wonderland",
        avatarUri = null,
        isContact = true,
    )
    val bob = User(
        id = "user_2",
        username = "bob_builder",
        displayName = "Bob Builder",
        avatarUri = null,
        isContact = true,
    )
    val charlie = User(
        id = "user_3",
        username = "charlie_dev",
        displayName = "Charlie Developer",
        avatarUri = null,
        isContact = true,
    )
    val diana = User(
        id = "user_4",
        username = "diana_prince",
        displayName = "Diana Prince",
        avatarUri = null,
        isContact = true,
    )
    val eve = User(
        id = "user_5",
        username = "eve_adam",
        displayName = "Eve Adams",
        avatarUri = null,
        isContact = true,
    )
    val frank = User(
        id = "user_6",
        username = "frank_castle",
        displayName = "Frank Castle",
        avatarUri = null,
        isContact = true,
    )
    val grace = User(
        id = "user_7",
        username = "grace_hopper",
        displayName = "Grace Hopper",
        avatarUri = null,
        isContact = true,
    )
    val henry = User(
        id = "user_8",
        username = "henry_cavill",
        displayName = "Henry Cavill",
        avatarUri = null,
        isContact = true,
    )

    // ── Stories per contact ──────────────────────────────────────────────
    // Alice — unseen (1 story)
    val aliceStories = listOf(
        Story(
            id = "story_alice_1",
            userId = alice.id,
            userName = alice.displayName,
            userAvatarUri = null,
            mediaUri = "https://picsum.photos/1080/1920?random=10",
            type = StoryType.IMAGE,
            caption = "Sunset vibes 🌅",
            durationSeconds = 10,
            createdAt = now - 3 * hour,
            expiresAt = now + 21 * hour,
            viewedBy = emptyList(), // no one viewed yet -> unseen
        ),
    )

    // Bob — unseen (2 stories)
    val bobStories = listOf(
        Story(
            id = "story_bob_1",
            userId = bob.id,
            userName = bob.displayName,
            userAvatarUri = null,
            mediaUri = "https://picsum.photos/1080/1920?random=20",
            type = StoryType.IMAGE,
            caption = "New bike! 🚴",
            durationSeconds = 10,
            createdAt = now - 5 * hour,
            expiresAt = now + 19 * hour,
            viewedBy = emptyList(),
        ),
        Story(
            id = "story_bob_2",
            userId = bob.id,
            userName = bob.displayName,
            userAvatarUri = null,
            mediaUri = "https://picsum.photos/1080/1920?random=21",
            type = StoryType.VIDEO,
            caption = "Check this out 🎥",
            durationSeconds = 15,
            createdAt = now - 4 * hour,
            expiresAt = now + 20 * hour,
            viewedBy = emptyList(),
        ),
    )

    // Charlie — seen (1 story, already viewed by us)
    val charlieStories = listOf(
        Story(
            id = "story_charlie_1",
            userId = charlie.id,
            userName = charlie.displayName,
            userAvatarUri = null,
            mediaUri = "https://picsum.photos/1080/1920?random=30",
            type = StoryType.TEXT,
            caption = "Code never lies, comments sometimes do 😄",
            durationSeconds = 8,
            createdAt = now - 8 * hour,
            expiresAt = now + 16 * hour,
            viewedBy = listOf(
                StoryView(userId = "user_self", userName = "You", viewedAt = now - 2 * hour),
                StoryView(userId = alice.id, userName = alice.displayName, viewedAt = now - 3 * hour),
            ),
        ),
    )

    // Diana — unseen (1 story)
    val dianaStories = listOf(
        Story(
            id = "story_diana_1",
            userId = diana.id,
            userName = diana.displayName,
            userAvatarUri = null,
            mediaUri = "https://picsum.photos/1080/1920?random=40",
            type = StoryType.IMAGE,
            caption = "Training session 💪",
            durationSeconds = 10,
            createdAt = now - 1 * hour,
            expiresAt = now + 23 * hour,
            viewedBy = emptyList(),
        ),
    )

    // Eve — seen (2 stories, both viewed)
    val eveStories = listOf(
        Story(
            id = "story_eve_1",
            userId = eve.id,
            userName = eve.displayName,
            userAvatarUri = null,
            mediaUri = "https://picsum.photos/1080/1920?random=50",
            type = StoryType.IMAGE,
            caption = "Brunch with friends 🥂",
            durationSeconds = 10,
            createdAt = now - 10 * hour,
            expiresAt = now + 14 * hour,
            viewedBy = listOf(
                StoryView(userId = "user_self", userName = "You", viewedAt = now - 6 * hour),
            ),
        ),
        Story(
            id = "story_eve_2",
            userId = eve.id,
            userName = eve.displayName,
            userAvatarUri = null,
            mediaUri = "https://picsum.photos/1080/1920?random=51",
            type = StoryType.IMAGE,
            caption = "New painting 🎨",
            durationSeconds = 10,
            createdAt = now - 9 * hour,
            expiresAt = now + 15 * hour,
            viewedBy = listOf(
                StoryView(userId = "user_self", userName = "You", viewedAt = now - 5 * hour),
                StoryView(userId = bob.id, userName = bob.displayName, viewedAt = now - 7 * hour),
            ),
        ),
    )

    // Frank — unseen (1 story)
    val frankStories = listOf(
        Story(
            id = "story_frank_1",
            userId = frank.id,
            userName = frank.displayName,
            userAvatarUri = null,
            mediaUri = "https://picsum.photos/1080/1920?random=60",
            type = StoryType.IMAGE,
            caption = "Quiet evening 🏠",
            durationSeconds = 10,
            createdAt = now - 30 * minute,
            expiresAt = now + 23 * hour + 30 * minute,
            viewedBy = emptyList(),
        ),
    )

    // Grace — seen (1 story, viewed)
    val graceStories = listOf(
        Story(
            id = "story_grace_1",
            userId = grace.id,
            userName = grace.displayName,
            userAvatarUri = null,
            mediaUri = "https://picsum.photos/1080/1920?random=70",
            type = StoryType.MUSIC,
            caption = "Jazz vibes 🎷",
            durationSeconds = 15,
            createdAt = now - 12 * hour,
            expiresAt = now + 12 * hour,
            viewedBy = listOf(
                StoryView(userId = "user_self", userName = "You", viewedAt = now - 8 * hour),
                StoryView(userId = eve.id, userName = eve.displayName, viewedAt = now - 9 * hour),
                StoryView(userId = bob.id, userName = bob.displayName, viewedAt = now - 10 * hour),
            ),
        ),
    )

    // Henry — unseen (1 story)
    val henryStories = listOf(
        Story(
            id = "story_henry_1",
            userId = henry.id,
            userName = henry.displayName,
            userAvatarUri = null,
            mediaUri = "https://picsum.photos/1080/1920?random=80",
            type = StoryType.IMAGE,
            caption = "Movie night 🍿",
            durationSeconds = 10,
            createdAt = now - 15 * minute,
            expiresAt = now + 23 * hour + 45 * minute,
            viewedBy = emptyList(),
        ),
    )

    // ── Assemble list ────────────────────────────────────────────────────
    // Helper to check if the current user (represented as "user_self") has
    // viewed every story in a group.
    fun isFullyViewedBySelf(stories: List<Story>): Boolean {
        val selfId = "user_self"
        return stories.all { story ->
            story.viewedBy.any { it.userId == selfId }
        }
    }

    return listOf(
        ContactStoryGroup(user = alice, stories = aliceStories, isFullyViewed = isFullyViewedBySelf(aliceStories)),
        ContactStoryGroup(user = bob, stories = bobStories, isFullyViewed = isFullyViewedBySelf(bobStories)),
        ContactStoryGroup(user = charlie, stories = charlieStories, isFullyViewed = isFullyViewedBySelf(charlieStories)),
        ContactStoryGroup(user = diana, stories = dianaStories, isFullyViewed = isFullyViewedBySelf(dianaStories)),
        ContactStoryGroup(user = eve, stories = eveStories, isFullyViewed = isFullyViewedBySelf(eveStories)),
        ContactStoryGroup(user = frank, stories = frankStories, isFullyViewed = isFullyViewedBySelf(frankStories)),
        ContactStoryGroup(user = grace, stories = graceStories, isFullyViewed = isFullyViewedBySelf(graceStories)),
        ContactStoryGroup(user = henry, stories = henryStories, isFullyViewed = isFullyViewedBySelf(henryStories)),
    )
}
