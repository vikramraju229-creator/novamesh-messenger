/**
 * NovaAvatar — Reusable avatar composable for NovaMesh Messenger.
 *
 * Features:
 * - Circular avatar with optional image loading via Coil
 * - Fallback to user initial letter on colored background
 * - Online/offline presence dot (green/gray)
 * - Badge support (unread count, verified check, etc.)
 * - Configurable sizes: small (32dp), medium (40dp), large (56dp), xl (80dp)
 * - Story ring (gradient border for stories)
 * - Ripple effect on click
 */

package com.novamesh.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.novamesh.ui.theme.NovaPrimary
import com.novamesh.ui.theme.NovaSuccess
import com.novamesh.ui.theme.StoryRingColors

// ─────────────────────────────────────────────────────────────────────────────
// Avatar sizes
// ─────────────────────────────────────────────────────────────────────────────

/** Predefined avatar size variants. */
enum class AvatarSize(val size: Dp, val fontSize: Int, val dotSize: Dp) {
    XSMALL(24.dp, 10, 6.dp),
    SMALL(32.dp, 14, 8.dp),
    MEDIUM(40.dp, 18, 10.dp),
    LARGE(56.dp, 24, 12.dp),
    XLARGE(80.dp, 36, 14.dp),
}

// ─────────────────────────────────────────────────────────────────────────────
// Public composables
// ─────────────────────────────────────────────────────────────────────────────

/**
 * A circular avatar with image fallback, presence dot, and optional story ring.
 *
 * @param imageUri Optional URI for the avatar image (loaded via Coil).
 * @param displayName Used to extract the initial letter fallback.
 * @param size Size variant (XSMALL, SMALL, MEDIUM, LARGE, XLARGE).
 * @param isOnline Whether to show the green online presence dot.
 * @param hasStory Whether to show the gradient story ring border.
 * @param isVerified Whether to show the verified check badge.
 * @param unreadCount If > 0, shows an unread count badge.
 * @param onClick Optional click handler.
 * @param modifier Additional modifiers.
 */
@Composable
fun NovaAvatar(
    imageUri: String?,
    displayName: String,
    avatarSize: AvatarSize = AvatarSize.MEDIUM,
    isOnline: Boolean = false,
    hasStory: Boolean = false,
    isVerified: Boolean = false,
    unreadCount: Int = 0,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val initial = displayName.firstOrNull()?.uppercase() ?: "?"
    val containerSize = avatarSize.size
    val innerSize = if (hasStory) containerSize - 4.dp else containerSize
    val hasClick = onClick != null
    val safeOnClick = onClick

    Box(
        modifier = modifier.size(containerSize),
        contentAlignment = Alignment.Center,
    ) {
        // ── Story ring (gradient border) ──────────────────────────────
        if (hasStory) {
            Box(
                modifier = Modifier
                    .size(containerSize)
                    .clip(CircleShape)
                    .drawBehind {
                        drawCircle(
                            brush = Brush.sweepGradient(
                                colors = StoryRingColors,
                                center = Offset(size.width / 2f, size.height / 2f),
                            ),
                            style = Stroke(width = 2.5.dp.toPx()),
                        )
                    },
            )
        }

        // ── Avatar circle ─────────────────────────────────────────────
        Surface(
            modifier = Modifier
                .size(innerSize)
                .then(if (hasClick && safeOnClick != null) Modifier.clickable(onClick = safeOnClick) else Modifier),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                if (imageUri != null) {
                    AsyncImage(
                        model = imageUri,
                        contentDescription = displayName,
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    // Fallback: initial letter
                    Text(
                        text = initial,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = size.fontSize.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        // ── Online dot ────────────────────────────────────────────────
        if (isOnline) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(size.dotSize + 4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface), // white ring
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(size.dotSize)
                        .clip(CircleShape)
                        .background(NovaSuccess),
                )
            }
        }

        // ── Verified badge ────────────────────────────────────────────
        if (isVerified) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = "Verified",
                tint = NovaSuccess,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(18.dp),
            )
        }

        // ── Unread count badge ────────────────────────────────────────
        if (unreadCount > 0) {
            val badgeText = if (unreadCount > 99) "99+" else unreadCount.toString()
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(width = if (unreadCount > 9) 24.dp else 20.dp, height = 20.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.error),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = badgeText,
                    color = MaterialTheme.colorScheme.onError,
                    fontSize = if (unreadCount > 9) 9.sp else 10.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/**
 * A simple horizontal row of stacked avatars (for group chat indicators).
 * Each avatar overlaps the previous one.
 *
 * @param imageUris List of avatar URIs.
 * @param displayNames Corresponding display names.
 * @param maxVisible Maximum avatars to show before a "+N" overflow.
 * @param size Size of each avatar.
 */
@Composable
fun NovaAvatarStack(
    imageUris: List<String?>,
    displayNames: List<String>,
    maxVisible: Int = 3,
    size: AvatarSize = AvatarSize.SMALL,
    modifier: Modifier = Modifier,
) {
    val visible = imageUris.take(maxVisible)
    val overflow = imageUris.size - maxVisible

    Box(modifier = modifier) {
        visible.forEachIndexed { index, uri ->
            val offset = index * (size.size.value * 0.6f).dp
            NovaAvatar(
                imageUri = uri,
                displayName = displayNames.getOrElse(index) { "?" },
                size = size,
                modifier = Modifier.padding(start = offset),
            )
        }

        if (overflow > 0) {
            Surface(
                modifier = Modifier
                    .size(size.size)
                    .padding(start = (maxVisible * size.size.value * 0.6f).dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "+$overflow",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
