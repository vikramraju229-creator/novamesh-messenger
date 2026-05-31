/**
 * ViewModel for the StoriesScreen — stories feed with "My Story"
 * and contacts' story rings.
 */
package com.novamesh.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novamesh.domain.model.Story
import com.novamesh.domain.model.User
import com.novamesh.domain.usecase.GetStoriesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** A contact with their active stories and a pre-computed "fully viewed" flag. */
data class ContactStoryGroup(
    val user: User,
    val stories: List<Story>,
    val isFullyViewed: Boolean,
)

/** UI state for the stories screen. */
data class StoriesUiState(
    val myUserId: String = "user_self",
    val myStories: List<Story> = emptyList(),
    val contactStories: List<ContactStoryGroup> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
)

/**
 * @param getStoriesUseCase Use case for observing stories.
 */
class StoriesViewModel(
    private val getStoriesUseCase: GetStoriesUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(StoriesUiState())
    val uiState: StateFlow<StoriesUiState> = _uiState.asStateFlow()

    init {
        observeStories()
    }

    /** Observe stories for the current user and contacts. */
    private fun observeStories() {
        viewModelScope.launch {
            // Observe my stories
            getStoriesUseCase.getMyStories(_uiState.value.myUserId).collect { myStories ->
                _uiState.update { it.copy(myStories = myStories) }
            }
        }
        viewModelScope.launch {
            // Observe contact stories
            getStoriesUseCase.getContactStories().collect { pairs ->
                val groups = pairs.map { (user, stories) ->
                    val selfId = _uiState.value.myUserId
                    val viewed = stories.all { story ->
                        story.viewedBy.any { it.userId == selfId }
                    }
                    ContactStoryGroup(user, stories, viewed)
                }
                _uiState.update {
                    it.copy(contactStories = groups, isLoading = false)
                }
            }
        }
    }

    /** Mark a story as viewed. */
    fun onStoryViewed(storyId: String, userId: String) {
        viewModelScope.launch {
            getStoriesUseCase.markAsViewed(storyId, userId)
        }
    }
}
