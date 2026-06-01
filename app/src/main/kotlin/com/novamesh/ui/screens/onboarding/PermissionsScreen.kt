package com.novamesh.ui.screens.onboarding

import android.Manifest
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale

/** A permission item definition. */
private data class PermissionItem(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val permissions: List<String>,
    val isOptional: Boolean = false,
)

/** All permissions grouped by feature. */
private val permissionGroups = listOf(
    PermissionItem(
        icon = Icons.Default.Contacts,
        title = "Contacts",
        description = "Find friends already on NovaMesh",
        permissions = listOf(Manifest.permission.READ_CONTACTS),
    ),
    PermissionItem(
        icon = Icons.Default.CameraAlt,
        title = "Camera & Microphone",
        description = "Take snaps and make calls",
        permissions = listOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO),
    ),
    PermissionItem(
        icon = Icons.Default.PhotoLibrary,
        title = "Storage",
        description = "Share photos and files",
        permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            listOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        },
    ),
    PermissionItem(
        icon = Icons.Default.Notifications,
        title = "Notifications",
        description = "Never miss a message",
        permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            listOf(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            emptyList() // Granted automatically on older versions
        },
    ),
    PermissionItem(
        icon = Icons.Default.LocationOn,
        title = "Location",
        description = "Share your location in chats",
        permissions = listOf(Manifest.permission.ACCESS_FINE_LOCATION),
        isOptional = true,
    ),
)

/**
 * Onboarding permissions screen shown once after profile creation.
 *
 * Guides the user through granting essential permissions with explanation cards.
 * Tracks completion in a callback so the parent can save to DataStore.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionsScreen(
    onAllPermissionsDone: () -> Unit,
    onSkip: () -> Unit,
) {
    var currentIndex by remember { mutableIntStateOf(0) }
    var showRationale by remember { mutableStateOf(false) }
    var skippedOptional by remember { mutableStateOf(false) }

    // Filter out groups with no permissions to request (e.g., notifications on old API)
    val groups = permissionGroups.filter { it.permissions.isNotEmpty() }
    val currentGroup = groups.getOrNull(currentIndex)

    val permissionState = rememberMultiplePermissionsState(
        permissions = currentGroup?.permissions ?: emptyList(),
    )

    val notificationPermissionState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
    } else null

    // Move to next permission group
    fun nextPermission() {
        if (currentIndex < groups.size - 1) {
            currentIndex++
            showRationale = false
        } else {
            onAllPermissionsDone()
        }
    }

    LaunchedEffect(currentIndex) {
        // Reset rationale dialog when moving to next group
        showRationale = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = "Permissions",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "NovaMesh needs a few permissions to work its magic",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Progress indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            groups.forEachIndexed { index, _ ->
                val color = if (index <= currentIndex)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.surfaceVariant
                Card(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(width = 40.dp, height = 4.dp),
                    shape = RoundedCornerShape(2.dp),
                    colors = CardDefaults.cardColors(containerColor = color),
                ) {}
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Current permission card
        currentGroup?.let { group ->
            AnimatedVisibility(
                visible = true,
                enter = fadeIn() + slideInVertically { it / 4 },
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            imageVector = group.icon,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = group.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = group.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Allow button
                        Button(
                            onClick = {
                                if (permissionState.allPermissionsGranted) {
                                    nextPermission()
                                } else {
                                    permissionState.launchMultiplePermissionRequest()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Text(
                                if (permissionState.allPermissionsGranted) "Continue"
                                else "Allow",
                                fontWeight = FontWeight.SemiBold,
                            )
                        }

                        // Rationale prompt if denied
                        if (permissionState.permissions.any { it.status.shouldShowRationale }) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "This permission is needed for the feature to work. Please grant it in Settings if you change your mind.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        // Skip button for optional permissions
                        if (group.isOptional) {
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(onClick = {
                                skippedOptional = true
                                nextPermission()
                            }) {
                                Text("Skip")
                            }
                        }
                    }
                }
            }
        }

        // If all groups done, show finish button
        if (currentGroup == null) {
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onAllPermissionsDone,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Done", fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Skip all at any time
        if (currentIndex > 0 || currentGroup != null) {
            TextButton(onClick = onSkip) {
                Text("Skip All")
            }
        }
    }
}
