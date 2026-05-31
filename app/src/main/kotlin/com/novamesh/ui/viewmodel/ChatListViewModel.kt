/**
 * ViewModel for the ChatListScreen — main conversations list.
 *
 * Manages the chat list state, search filtering, pull-to-refresh,
 * pin/mute/archive actions, and navigation triggers.
 */
package com.novamesh.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novamesh.domain.model.Chat
import com.novamesh.domain.usecase.GetChatsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** UI state for the chat list screen. */
data class ChatListUiState(
    val chats: List<Chat> = emptyList(),
    val searchQuery: String = "",
    val isRefreshing: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null,
)

/**
 * @param getChatsUseCase Use case for observing and filtering chats.
 */
class ChatListViewModel(
    private val getChatsUseCase: GetChatsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatListUiState())
    val uiState: StateFlow<ChatListUiState> = _uiState.asStateFlow()

    init {
        observeChats()
    }

    /** Observe all chats from the local database. */
    private fun observeChats() {
        viewModelScope.launch {
            getChatsUseCase(_uiState.value.searchQuery).collect { chats ->
                _uiState.update { it.copy(chats = chats, isLoading = false) }
            }
        }
    }

    /** Update the search query, which triggers re-filtering. */
    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        // Re-observe with new filter
        observeChats()
    }

    /** Clear the search query. */
    fun onSearchClear() {
        onSearchQueryChange("")
    }

    /** Pull-to-refresh: sync from remote. */
    fun onRefresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            // TODO: trigger sync from MatrixRepository
            kotlinx.coroutines.delay(500) // Simulate network
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    /** Toggle pin state for a chat. */
    fun togglePin(chatId: String, isPinned: Boolean) {
        viewModelScope.launch {
            getChatsUseCase.togglePin(chatId, isPinned)
        }
    }

    /** Toggle mute state for a chat. */
    fun toggleMute(chatId: String, isMuted: Boolean) {
        viewModelScope.launch {
            getChatsUseCase.toggleMute(chatId, isMuted)
        }
    }

    /** Archive (delete) a chat from the list. */
    fun archiveChat(chatId: String) {
        viewModelScope.launch {
            getChatsUseCase.archiveChat(chatId)
        }
    }
}
