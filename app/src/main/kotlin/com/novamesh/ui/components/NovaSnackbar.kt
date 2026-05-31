/**
 * NovaSnackbar — Custom snackbar and notification banner helpers.
 *
 * Provides:
 * - A styled snackbar host with severity-based theming
 * - Custom Visuals type with severity
 * - Inline notification banners
 */
package com.novamesh.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Severity level for the snackbar, which affects colour and icon. */
enum class NovaSnackbarSeverity {
    SUCCESS, INFO, WARNING, ERROR,
}

/**
 * Pre-configured snackbar host for use in a Scaffold.
 */
@Composable
fun NovaSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    SnackbarHost(
        hostState = hostState,
        modifier = modifier,
        snackbar = { data -> NovaSnackbar(data) },
    )
}

/**
 * Styled snackbar composable used by [NovaSnackbarHost].
 */
@Composable
private fun NovaSnackbar(snackbarData: SnackbarData) {
    val severity = (snackbarData.visuals as? NovaSnackbarVisuals)?.severity ?: NovaSnackbarSeverity.INFO

    val icon: ImageVector
    val tint: Color

    when (severity) {
        NovaSnackbarSeverity.SUCCESS -> { icon = Icons.Default.CheckCircle; tint = Color(0xFF00C853) }
        NovaSnackbarSeverity.INFO -> { icon = Icons.Default.Info; tint = MaterialTheme.colorScheme.primary }
        NovaSnackbarSeverity.WARNING -> { icon = Icons.Default.Warning; tint = Color(0xFFFFD600) }
        NovaSnackbarSeverity.ERROR -> { icon = Icons.Default.Error; tint = MaterialTheme.colorScheme.error }
    }

    Snackbar(
        modifier = Modifier.padding(8.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 6.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = snackbarData.visuals.message,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * Extended [SnackbarVisuals] that includes a severity level.
 */
data class NovaSnackbarVisuals(
    override val message: String,
    override val actionLabel: String? = null,
    override val withDismissAction: Boolean = false,
    override val duration: androidx.compose.material3.SnackbarDuration =
        androidx.compose.material3.SnackbarDuration.Short,
    val severity: NovaSnackbarSeverity = NovaSnackbarSeverity.INFO,
) : androidx.compose.material3.SnackbarVisuals

/**
 * An inline notification banner (e.g. "Connected", "No internet").
 */
@Composable
fun NovaNotificationBanner(
    message: String,
    severity: NovaSnackbarSeverity = NovaSnackbarSeverity.INFO,
    visible: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val icon: ImageVector
    val backgroundColor: Color
    val textColor: Color

    when (severity) {
        NovaSnackbarSeverity.SUCCESS -> {
            icon = Icons.Default.CheckCircle
            backgroundColor = Color(0xFF00C853).copy(alpha = 0.15f)
            textColor = Color(0xFF00C853)
        }
        NovaSnackbarSeverity.INFO -> {
            icon = Icons.Default.Info
            backgroundColor = MaterialTheme.colorScheme.primaryContainer
            textColor = MaterialTheme.colorScheme.onPrimaryContainer
        }
        NovaSnackbarSeverity.WARNING -> {
            icon = Icons.Default.Warning
            backgroundColor = Color(0xFFFFD600).copy(alpha = 0.15f)
            textColor = Color(0xFFFFD600)
        }
        NovaSnackbarSeverity.ERROR -> {
            icon = Icons.Default.Error
            backgroundColor = MaterialTheme.colorScheme.errorContainer
            textColor = MaterialTheme.colorScheme.onErrorContainer
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically { -it } + fadeIn(),
        exit = androidx.compose.animation.slideOutVertically { -it } + androidx.compose.animation.fadeOut(),
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .background(backgroundColor)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = textColor,
            )
        }
    }
}
