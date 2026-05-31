/**
 * NovaDialog — Reusable dialog composables for NovaMesh Messenger.
 *
 * Includes:
 * - NovaAlertDialog (confirmation / info)
 * - NovaInputDialog (text input prompt)
 * - NovaOptionDialog (list of options)
 * - NovaPermissionDialog (permission rationale)
 * - NovaBottomSheet (modal bottom sheet wrapper)
 */

@file:OptIn(ExperimentalMaterial3Api::class)

package com.novamesh.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

// ─────────────────────────────────────────────────────────────────────────────
// Alert Dialog
// ─────────────────────────────────────────────────────────────────────────────

/**
 * A styled alert dialog with icon, title, message, and confirm/dismiss actions.
 *
 * @param icon Optional leading icon.
 * @param iconTint Color for the icon.
 * @param title Bold title text.
 * @param message Body text.
 * @param confirmLabel Label for the confirm button.
 * @param onConfirm Click handler for confirm.
 * @param dismissLabel Label for the dismiss button (null = no dismiss).
 * @param onDismiss Click handler for dismiss (null = use default).
 * @param destructive Whether the confirm action is destructive (red).
 * @param onDismissRequest Outside-tap / back-press handler.
 */
@Composable
fun NovaAlertDialog(
    icon: ImageVector? = null,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    title: String,
    message: String,
    confirmLabel: String = "OK",
    onConfirm: () -> Unit,
    dismissLabel: String? = "Cancel",
    onDismiss: (() -> Unit)? = null,
    destructive: Boolean = false,
    onDismissRequest: () -> Unit = { onDismiss?.invoke() },
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        icon = if (icon != null) {
            {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(32.dp),
                )
            }
        } else null,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = if (destructive) {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    )
                } else {
                    ButtonDefaults.buttonColors()
                },
            ) {
                Text(confirmLabel, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = if (dismissLabel != null && onDismiss != null) {
            {
                TextButton(onClick = onDismiss) {
                    Text(dismissLabel)
                }
            }
        } else null,
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Input Dialog
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Dialog with a text input field.
 *
 * @param title Title text.
 * @param placeholder Placeholder for the input field.
 * @param initialValue Initial text value.
 * @param confirmLabel Label for confirm button.
 * @param onConfirm Called with the entered text.
 * @param dismissLabel Label for dismiss button.
 * @param onDismiss Dismiss click handler.
 */
@Composable
fun NovaInputDialog(
    title: String,
    placeholder: String = "",
    initialValue: String = "",
    confirmLabel: String = "Save",
    onConfirm: (String) -> Unit,
    dismissLabel: String = "Cancel",
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf(initialValue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text(placeholder) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(text) },
                enabled = text.isNotBlank(),
            ) {
                Text(confirmLabel, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dismissLabel)
            }
        },
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Option Dialog
// ─────────────────────────────────────────────────────────────────────────────

/**
 * A single option item in [NovaOptionDialog].
 */
data class OptionItem(
    val id: String,
    val label: String,
    val icon: ImageVector? = null,
    val destructive: Boolean = false,
)

/**
 * Dialog showing a list of selectable options (like a context menu).
 *
 * @param title Title text.
 * @param options List of [OptionItem] to display.
 * @param onOptionClick Called with the selected option's ID.
 * @param onDismiss Dismiss handler.
 */
@Composable
fun NovaOptionDialog(
    title: String,
    options: List<OptionItem>,
    onOptionClick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            Column {
                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onOptionClick(option.id)
                                onDismiss()
                            }
                            .padding(vertical = 12.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (option.icon != null) {
                            Icon(
                                imageVector = option.icon,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = if (option.destructive) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                        }
                        Text(
                            text = option.label,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (option.destructive) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                        )
                    }
                    if (option != options.last()) {
                        HorizontalDivider()
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Modal Bottom Sheet
// ─────────────────────────────────────────────────────────────────────────────

/**
 * A reusable modal bottom sheet wrapper.
 *
 * @param sheetState Optional [SheetState]; defaults to skip-partially-expanded.
 * @param onDismissRequest Called when the sheet is dismissed.
 * @param content Content composable inside the sheet.
 */
@Composable
fun NovaBottomSheet(
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        content()
    }
}
