/**
 * StoriesScreen — "Updates" tab showing real stories from Firestore.
 *
 * Layout:
 *   Top bar: "Updates" title + camera icon
 *   My Story section: avatar + ring, tap to add or view
 *   Recent updates: horizontal scrollable list of story rings
 *   Empty state when no stories exist
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
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
import com.google.firebase.auth.FirebaseAuth
import com.novamesh.data.remote.FirestoreRepository
import com.novamesh.data.remote.FirestoreStory
import com.novamesh.data.remote.FirestoreUser
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import com.novamesh.ui.theme.NovaPrimary
import com.novamesh.ui.theme.NovaSecondary
import com.novamesh.ui.theme.NovaTertiary

/** Groups a Firestore user with their active stories. */
private data class StoryGroup(
    val user: FirestoreUser,
    val stories: List<FirestoreStory>,
    val isFullyViewed: Boolean,
)

/**
 * "Updates" tab — shows real stories from Firestore.
 *
 * @param onStoryClick Invoked when a story ring is tapped.
 * @param onCameraClick Invoked when the camera icon is tapped.
 */
@Composable
fun StoriesScreen(
    onStoryClick: (userId: String, storyId: String) -> Unit,
    onCameraClick: () -> Unit,
) {
    val repository = remember { FirestoreRepository() }
    val currentUid = remember { FirebaseAuth.getInstance().currentUser?.uid ?: "" }

    var allStories by remember { mutableStateOf<List<FirestoreStory>>(emptyList()) }
    var allUsers by remember { mutableStateOf<Map<String, FirestoreUser>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }

    // Load stories + users from Firestore
    LaunchedEffect(Unit) {
        try {
            // Get current user's name for "My Story"
            val userMap = mutableMapOf<String, FirestoreUser>()
            val users = withTimeout(10_000L) {
                repository.searchUsers("")
            }
            users.forEach { userMap[it.id] = it }
            allUsers = userMap

            // One-shot initial fetch — ensures loading stops even if no stories exist
            val currentStories = withTimeout(10_000L) {
                repository.observeStories().first()
            }
            allStories = currentStories
            isLoading = false

            // Continue observing for real-time updates (non-blocking for UI)
            repository.observeStories().collect { stories ->
                allStories = stories
            }
        } catch (_: kotlinx.coroutines.TimeoutCancellationException) {
            isLoading = false
        } catch (_: Exception) {
            isLoading = false
        }
    }

    // Group stories by userId
    val myStories = allStories.filter { it.userId == currentUid }
    val contactStories = allStories.filter { it.userId != currentUid }

    // Group contact stories by user
    val grouped = contactStories.groupBy { it.userId }.map { (userId, stories) ->
        val user = allUsers[userId] ?: FirestoreUser(id = userId, name = "Unknown", username = "")
        val isFullyViewed = stories.all { story ->
            currentUid in story.viewedBy
        }
        StoryGroup(user = user, stories = stories, isFullyViewed = isFullyViewed)
    }

    val contacts = grouped
    val allContactsHaveSeenStories = contacts.isNotEmpty() && contacts.all { it.isFullyViewed }

    Scaffold(
        topBar = {
            UpdatesTopBar(onCameraClick = onCameraClick)
        },
        modifier = Modifier.fillMaxSize(),
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) {
                androidx.compose.material3.CircularProgressIndicator()
            }
        } else if (myStories.isEmpty() && contacts.isEmpty()) {
            UpdatesEmptyState(
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
                // ── My Story section ──
                item(key = "my_story") {
                    MyStorySection(
                        myUserId = currentUid,
                        hasStories = myStories.isNotEmpty(),
                        storyCount = myStories.size,
                        onAddClick = onCameraClick,
                        onMyStoryClick = {
                            if (myStories.isNotEmpty()) {
                                onStoryClick(currentUid, myStories.first().id)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                    )
                }

                // ── Recent Updates section ──
                if (contacts.isNotEmpty()) {
                    item(key = "recent_header") {
                        SectionHeader(
                            title = "Recent updates",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, top = 20.dp, bottom = 8.dp),
                        )
                    }

                    item(key = "recent_list") {
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

                // ── Viewed all hint ──
                if (allContactsHaveSeenStories) {
                    item(key = "all_viewed_hint") {
                        Text(
                            text = "All caught up! Check back later for new updates.",
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
// Top app bar — "Updates"
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun UpdatesTopBar(onCameraClick: () -> Unit) {
    TopAppBar(
        title = {
            Text(
                text = "Updates",
                style = MaterialTheme.typography.titleLarge,
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
        Box(
            modifier = Modifier.size(72.dp),
            contentAlignment = Alignment.Center,
        ) {
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

        Column {
            Text(
                text = "My Story",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (hasStories) FontWeight.SemiBold else FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = if (hasStories) {
                    "$storyCount update${if (storyCount != 1) "s" else ""}"
                } else {
                    "Tap to add an update"
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

@Composable
private fun StoryRingItem(
    user: FirestoreUser,
    stories: List<FirestoreStory>,
    isFullyViewed: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var hasAnimated by remember { mutableStateOf(false) }
    val animatedAlpha by animateFloatAsState(
        targetValue = if (hasAnimated) 1f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "ringAlpha",
    )

    LaunchedEffect(Unit) {
        hasAnimated = true
    }

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
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = user.name.firstOrNull()?.uppercase() ?: "?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = user.name.split(" ").firstOrNull() ?: user.username,
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

@Composable
private fun UpdatesEmptyState(
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
            text = "No updates yet",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Contacts you follow will appear here when they post updates",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 48.dp),
        )
    }
}
