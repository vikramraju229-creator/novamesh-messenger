/**
 * ContactsScreen — WhatsApp-style People tab.
 *
 * Shows phone contacts matched with Firestore users.
 * Phone-only contacts appear with an "Invite" button.
 * No permission issues — handles denial gracefully.
 */

package com.novamesh.ui.screens.contacts

import android.Manifest
import android.content.ContentResolver
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.novamesh.data.remote.FirestoreRepository
import com.novamesh.data.remote.FirestoreUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

// ── Phone contact model ──
private data class PhoneContact(
    val name: String,
    val phone: String,
)

// ── Combined contact for display ──
private sealed class ContactItem {
    data class AppUser(
        val user: FirestoreUser,
        val phoneMatch: String? = null, // matched phone number, if any
    ) : ContactItem()

    data class PhoneOnly(
        val contact: PhoneContact,
    ) : ContactItem()
}

/** Normalize a phone number for matching (strip spaces/dashes/prefix). */
private fun normalizePhone(raw: String): String {
    return raw.replace(Regex("[^\\d+]"), "")
        .trimStart('+')
        .trimStart('9', '1') // remove India country code if present
        .trimStart('0')
}

/** Read phone contacts from the device content provider. */
private fun readPhoneContacts(contentResolver: ContentResolver): List<PhoneContact> {
    val contacts = mutableMapOf<String, PhoneContact>() // key = normalized phone
    val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
    val projection = arrayOf(
        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
        ContactsContract.CommonDataKinds.Phone.NUMBER,
    )

    contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
        val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
        val numIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

        while (cursor.moveToNext()) {
            val name = cursor.getString(nameIdx) ?: "Unknown"
            val rawPhone = cursor.getString(numIdx) ?: continue
            val normalized = normalizePhone(rawPhone)
            if (normalized.length >= 7) { // skip too short numbers
                // Keep the one with the longest name (most detailed contact)
                val existing = contacts[normalized]
                if (existing == null || name.length > existing.name.length) {
                    contacts[normalized] = PhoneContact(name = name, phone = rawPhone)
                }
            }
        }
    }
    return contacts.values.toList()
}

// ═════════════════════════════════════════════════════════════════════════════
// ContactsScreen — main composable
// ═════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(
    onChatClick: (chatId: String, chatName: String) -> Unit = { _, _ -> },
    onBack: () -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    val repository = remember { FirestoreRepository() }
    val context = LocalContext.current

    // ── State ──
    var isLoading by remember { mutableStateOf(true) }
    var appUsers by remember { mutableStateOf(listOf<FirestoreUser>()) }
    var phoneContacts by remember { mutableStateOf(listOf<PhoneContact>()) }
    var hasPermission by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    // ── Permission launcher ──
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasPermission = granted
        if (granted) {
            scope.launch(Dispatchers.IO) {
                val contacts = readPhoneContacts(context.contentResolver)
                phoneContacts = contacts
            }
        }
    }

    // ── Request permission on first composition ──
    LaunchedEffect(Unit) {
        hasPermission = context.checkSelfPermission(Manifest.permission.READ_CONTACTS) ==
            PackageManager.PERMISSION_GRANTED
        if (!hasPermission) {
            permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        } else {
            // Permission already granted — read contacts
            scope.launch(Dispatchers.IO) {
                val contacts = readPhoneContacts(context.contentResolver)
                phoneContacts = contacts
            }
        }
    }

    // ── Load Firestore users with timeout ──
    LaunchedEffect(Unit) {
        try {
            val currentUid = FirebaseAuth.getInstance().currentUser?.uid
            val users = withTimeout(10_000L) {
                repository.searchUsers("")
            }
            appUsers = users.filter { it.id != currentUid }
        } catch (_: Exception) {
            // Timeout or error — show whatever we have
        }
        isLoading = false
    }

    // ── Rematch when phone contacts or app users change ──
    val matchedPhones = remember(phoneContacts, appUsers) {
        val phoneMap = phoneContacts.associate { normalizePhone(it.phone) to it.name }
        val appPhones = appUsers.mapNotNull { user ->
            val norm = normalizePhone(user.phone)
            if (norm.isNotBlank() && norm in phoneMap) norm else null
        }.toSet()
        appPhones
    }

    // Build combined contact list for display
    val allItems = remember(appUsers, phoneContacts, matchedPhones, searchQuery) {
        val q = searchQuery.trim().lowercase()

        val appItems = appUsers
            .filter { user ->
                q.isBlank() || user.name.lowercase().contains(q) ||
                    user.username.lowercase().contains(q)
            }
            .map { user ->
                // Find which phone number matched (if any)
                val match = if (user.phone.isNotBlank()) {
                    val norm = normalizePhone(user.phone)
                    if (norm in matchedPhones) user.phone else null
                } else null
                ContactItem.AppUser(user, phoneMatch = match)
            }

        val phoneOnlyItems = phoneContacts
            .filter { contact ->
                val norm = normalizePhone(contact.phone)
                norm !in matchedPhones &&
                    (q.isBlank() || contact.name.lowercase().contains(q))
            }
            .map { ContactItem.PhoneOnly(it) }

        // Sort: app users first, then phone-only — both alphabetically
        val sortedApp = appItems.sortedBy {
            (it as ContactItem.AppUser).user.name.lowercase()
        }
        val sortedPhone = phoneOnlyItems.sortedBy {
            (it as ContactItem.PhoneOnly).contact.name.lowercase()
        }
        sortedApp + sortedPhone
    }

    // ── UI ──
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("People", fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // ── Search ──
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                placeholder = { Text("Search contacts") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            )

            Spacer(modifier = Modifier.height(4.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            } else if (allItems.isEmpty()) {
                // ── Empty state ──
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) {
                                "No contacts found"
                            } else {
                                "No contacts yet"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                LazyColumn {
                    // ── Invite friends row (like WhatsApp) ──
                    item(key = "invite_header") {
                        InviteFriendsRow(
                            onClick = {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        "Join me on Nova Messenger! https://novamesh.app/download"
                                    )
                                }
                                context.startActivity(
                                    Intent.createChooser(intent, "Invite friends")
                                )
                            },
                        )
                    }

                    items(allItems, key = { it.hashCode().toString() }) { item ->
                        when (item) {
                            is ContactItem.AppUser -> AppUserRow(
                                user = item.user,
                                hasPhoneMatch = item.phoneMatch != null,
                                onClick = {
                                    scope.launch {
                                        val chatId = repository.getOrCreateChat(item.user.id)
                                        if (chatId != null) {
                                            onChatClick(chatId, item.user.name)
                                        }
                                    }
                                },
                            )

                            is ContactItem.PhoneOnly -> PhoneOnlyRow(
                                name = item.contact.name,
                                phone = item.contact.phone,
                                onInvite = {
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(
                                            Intent.EXTRA_TEXT,
                                            "Hey ${item.contact.name}, join me on Nova Messenger! https://novamesh.app/download"
                                        )
                                        putExtra(Intent.EXTRA_SUBJECT, "Join Nova Messenger")
                                    }
                                    context.startActivity(
                                        Intent.createChooser(intent, "Invite ${item.contact.name}")
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Invite friends row
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun InviteFriendsRow(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Share,
                    contentDescription = "Invite",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Invite friends",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
    HorizontalDivider(
        modifier = Modifier.padding(start = 72.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
    )
}

// ═════════════════════════════════════════════════════════════════════════════
// User row — someone already on the app
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun AppUserRow(
    user: FirestoreUser,
    hasPhoneMatch: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = user.name.firstOrNull()?.uppercase() ?: "?",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = user.name.ifBlank { "Unknown" },
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = if (hasPhoneMatch) {
                            "On Nova Messenger"
                        } else {
                            "@${user.username.ifBlank { "user" }}"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            HorizontalDivider(
                modifier = Modifier.padding(start = 72.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Phone-only row — contact not on the app, show invite
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun PhoneOnlyRow(
    name: String,
    phone: String,
    onInvite: () -> Unit,
) {
    Surface(
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = name.firstOrNull()?.uppercase() ?: "?",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = name.ifBlank { "Unknown" },
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "Not on Nova Messenger",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onInvite,
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                    ),
                ) {
                    Text(
                        text = "Invite",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            HorizontalDivider(
                modifier = Modifier.padding(start = 72.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            )
        }
    }
}
