/**
 * NovaPermissionDialog — Runtime permission request and rationale UI.
 *
 * Since NovaMesh requires many permissions (camera, microphone, storage,
 * notifications, contacts), this component provides a consistent UX for
 * requesting them with proper rationale.
 *
 * Integrates with Accompanist Permissions library for runtime handling.
 */

@file:OptIn(ExperimentalPermissionsApi::class)

package com.novamesh.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi

// ─────────────────────────────────────────────────────────────────────────────
// Permission types
// ─────────────────────────────────────────────────────────────────────────────

/** Permission type identifiers used by NovaMesh. */
enum class NovaPermissionType(
    val label: String,
    val description: String,
    val icon: ImageVector,
) {
    CAMERA(
        label = "Camera",
        description = "NovaMesh needs camera access for snaps, stories, and video calls",
        icon = Icons.Default.CameraAlt,
    ),
    MICROPHONE(
        label = "Microphone",
        description = "NovaMesh needs microphone access for voice messages and calls",
        icon = Icons.Default.Mic,
    ),
    STORAGE(
        label = "Storage",
        description = "NovaMesh needs storage access to send and save media",
        icon = Icons.Default.PhotoLibrary,
    ),
    NOTIFICATIONS(
        label = "Notifications",
        description = "NovaMesh needs notification access to alert you of new messages",
        icon = Icons.Default.Notifications,
    ),
    CONTACTS(
        label = "Contacts",
        description = "NovaMesh needs your contacts to help you connect with friends",
        icon = Icons.Default.Contacts,
    ),
}

// ─────────────────────────────────────────────────────────────────────────────
// Permission dialog
// ─────────────────────────────────────────────────────────────────────────────

/**
 * A dialog explaining why a specific permission is needed, with Grant/Deny
 * options.
 *
 * @param permission The [NovaPermissionType] being requested.
 * @param onGrant Called when the user taps "Grant".
 * @param onDeny Called when the user taps "Deny" or "Not now".
 * @param onDismissRequest Outside-tap dismiss.
 */
@Composable
fun NovaPermissionDialog(
    permission: NovaPermissionType,
    onGrant: () -> Unit,
    onDeny: () -> Unit,
    onDismissRequest: () -> Unit = onDeny,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        icon = {
            Icon(
                imageVector = permission.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp),
            )
        },
        title = {
            Text(
                text = "${permission.label} access needed",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        text = {
            Text(
                text = permission.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            Button(
                onClick = onGrant,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("Grant", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDeny,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Not now")
            }
        },
    )
}

/**
 * A permission request banner that sits inline (e.g. in the camera screen
 * before the preview is active).
 */
@Composable
fun NovaPermissionBanner(
    permission: NovaPermissionType,
    onGrant: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = permission.icon,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = permission.description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onGrant,
            shape = RoundedCornerShape(12.dp),
        ) {
            Text("Allow ${permission.label}", fontWeight = FontWeight.SemiBold)
        }
    }
}
