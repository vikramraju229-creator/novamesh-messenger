/**
 * NovaSearchBar — Reusable animated search bar for chat and discover screens.
 *
 * Features:
 * - Material 3 SearchBar with animated expansion
 * - Leading search icon, trailing clear button
 * - Dropdown suggestion support
 * - Voice search button integration
 */
@file:OptIn(ExperimentalMaterial3Api::class)

package com.novamesh.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * A reusable search bar with voice search support.
 *
 * @param query The current search query text.
 * @param onQueryChange Called when the query text changes.
 * @param onSearch Called when the user presses the search key.
 * @param onClear Called when the clear button is tapped.
 * @param onVoiceSearch Called when the voice search button is tapped.
 * @param placeholder Placeholder text hint.
 * @param active Whether the search bar is active (showing suggestions).
 * @param onActiveChange Called when active state changes.
 * @param showVoiceSearch Whether to show the voice search icon.
 * @param suggestions Optional composable slot for dropdown suggestions.
 * @param modifier Additional modifiers.
 */
@Composable
fun NovaSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit = {},
    onClear: () -> Unit = {},
    onVoiceSearch: (() -> Unit)? = null,
    placeholder: String = "Search…",
    active: Boolean = false,
    onActiveChange: (Boolean) -> Unit = {},
    showVoiceSearch: Boolean = false,
    suggestions: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    SearchBar(
        query = query,
        onQueryChange = onQueryChange,
        onSearch = onSearch,
        active = active,
        onActiveChange = onActiveChange,
        placeholder = { Text(placeholder) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        trailingIcon = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (query.isNotEmpty()) {
                    IconButton(onClick = onClear) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear search",
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                if (showVoiceSearch && onVoiceSearch != null) {
                    IconButton(onClick = onVoiceSearch) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Voice search",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        suggestions?.invoke()
    }
}
