/**
 * NovaLoadingIndicator — Collection of loading/shimmer composables.
 *
 * Includes:
 * - Full-screen centered circular progress
 * - Inline loading row
 * - Shimmer/skeleton loading placeholders for chat list items
 * - Pull-to-refresh indicator wrapper
 */

package com.novamesh.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ─────────────────────────────────────────────────────────────────────────────
// Full-screen loading
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Full-screen centered circular progress indicator with optional label.
 *
 * @param label Optional text shown below the spinner.
 * @param modifier Modifier for the container.
 */
@Composable
fun NovaFullScreenLoader(
    label: String = "",
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(40.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 3.dp,
            )
            if (label.isNotBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Inline loading row
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Small inline loading row for use inside lists (e.g. at the bottom of a
 * paginated list).
 */
@Composable
fun NovaInlineLoader(
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 2.dp,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Loading…",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Linear progress bar (e.g. for media upload progress)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Determinate linear progress bar for upload/download progress.
 *
 * @param progress Float in range 0f..1f.
 * @param modifier Modifier for the container.
 */
@Composable
fun NovaProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    LinearProgressIndicator(
        progress = { progress.coerceIn(0f, 1f) },
        modifier = modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp)),
        color = MaterialTheme.colorScheme.primary,
        trackColor = MaterialTheme.colorScheme.surfaceVariant,
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Shimmer / Skeleton loading placeholders
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Shimmer animation state — provides a [Float] that oscillates 0f..1f for
 * use as a gradient offset in shimmer effects.
 */
@Composable
private fun shimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateX by transition.animateFloat(
        initialValue = -300f,
        targetValue = 900f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerOffset",
    )

    return Brush.linearGradient(
        colors = listOf(
            Color.LightGray.copy(alpha = 0.6f),
            Color.LightGray.copy(alpha = 0.2f),
            Color.LightGray.copy(alpha = 0.6f),
        ),
        start = Offset(translateX, 0f),
        end = Offset(translateX + 300f, 0f),
    )
}

/**
 * A single shimmer placeholder — a rounded rectangle with animated shimmer.
 *
 * @param width Width of the placeholder.
 * @param height Height of the placeholder.
 * @param shape Corner radius of the placeholder.
 */
@Composable
fun NovaShimmerPlaceholder(
    width: Dp = 100.dp,
    height: Dp = 16.dp,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(4.dp),
) {
    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .clip(shape)
            .background(shimmerBrush()),
    )
}

/**
 * Shimmer placeholder for a chat list item — mimics avatar + two text lines.
 */
@Composable
fun NovaChatListItemShimmer(
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Avatar shimmer
        NovaShimmerPlaceholder(
            width = 56.dp,
            height = 56.dp,
            shape = CircleShape,
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            // Name shimmer
            NovaShimmerPlaceholder(width = 140.dp, height = 16.dp)
            Spacer(modifier = Modifier.height(8.dp))
            // Message shimmer
            NovaShimmerPlaceholder(width = 200.dp, height = 14.dp)
        }
    }
}

/**
 * A full list of shimmer placeholders, useful while data is loading.
 *
 * @param count Number of shimmer items to show.
 */
@Composable
fun NovaShimmerList(
    count: Int = 6,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        repeat(count) {
            NovaChatListItemShimmer()
        }
    }
}
