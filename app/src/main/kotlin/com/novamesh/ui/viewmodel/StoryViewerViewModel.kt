/**
 * ViewModel for the StoryViewerScreen — full-screen vertical story viewer
 * with segmented progress bars, tap-to-advance, long-press pause,
 * swipe-down-to-dismiss, and reply actions.
 */
package com.novamesh.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novamesh.domain.model.Story
import com.novamesh.domain.model.StoryPrivacy
import com.novamesh.domain.model.StoryType
import com.novamesh.domain.model.StoryView
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** UI state for the story viewer. */
data class StoryViewerUiState(
    val stories: List<Story> = emptyList(),
    val currentIndex: Int = 0,
    val isPaused: Boolean = false,
    val isMuted: Boolean = false,
    val showUi: Boolean = true,
    val dragOffset: Float = 0f,
    val replyText: String = "",
    val showViewersSheet: Boolean = false,
    val progressFraction: Float = 0f,
    val isLoading: Boolean = true,
)

/**
 * ViewModel for the story viewer screen.
 *
 * Manages story navigation, auto-advance timer, pause/play,
 * mute/unmute, reply, and viewers sheet.
 */
class StoryViewerViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(StoryViewerUiState())
    val uiState: StateFlow<StoryViewerUiState> = _uiState.asStateFlow()

    private var autoAdvanceJob: Job? = null

    /** Initialize with stories for a given user. */
    fun initialize(userId: String, startStoryId: String) {
        if (_uiState.value.stories.isNotEmpty()) return

        val stories = mockStoriesForUser(userId)
        val initialIndex = stories.indexOfFirst { it.id == startStoryId }.coerceAtLeast(0)

        _uiState.update {
            it.copy(stories = stories, currentIndex = initialIndex, isLoading = false)
        }
        startAutoAdvance()
    }

    /** Start the auto-advance timer for the current story. */
    private fun startAutoAdvance() {
        autoAdvanceJob?.cancel()
        autoAdvanceJob = viewModelScope.launch {
            val story = _uiState.value.stories.getOrNull(_uiState.value.currentIndex) ?: return@launch
            val totalDuration = (story.durationSeconds * 1000L).coerceAtLeast(2000L)
            val stepMs = 50L
            val steps = totalDuration / stepMs

            for (step in 1..steps) {
                if (!isActive) break
                if (_uiState.value.isPaused) {
                    // Wait while paused, then continue
                    while (_uiState.value.isPaused && isActive) {
                        delay(stepMs)
                    }
                    if (!isActive) break
                }
                delay(stepMs)
                _uiState.update {
                    it.copy(progressFraction = step.toFloat() / steps)
                }
            }

            // Advance to next story
            if (_uiState.value.currentIndex < _uiState.value.stories.lastIndex) {
                _uiState.update { it.copy(currentIndex = it.currentIndex + 1, progressFraction = 0f) }
                startAutoAdvance()
            } else {
                // All stories consumed — signal completion via state
                _uiState.update { it.copy(stories = emptyList()) }
            }
        }
    }

    // ─── User actions ───────────────────────────────────────────────────

    /** Navigate to previous story or dismiss if at first. */
    fun onNavigatePrevious() {
        if (_uiState.value.currentIndex > 0) {
            _uiState.update {
                it.copy(currentIndex = it.currentIndex - 1, progressFraction = 0f)
            }
            startAutoAdvance()
        }
    }

    /** Navigate to next story or dismiss if at last. */
    fun onNavigateNext() {
        if (_uiState.value.currentIndex < _uiState.value.stories.lastIndex) {
            _uiState.update {
                it.copy(currentIndex = it.currentIndex + 1, progressFraction = 0f)
            }
            startAutoAdvance()
        } else {
            _uiState.update { it.copy(stories = emptyList()) }
        }
    }

    /** Toggle pause state. */
    fun onTogglePause() {
        _uiState.update { it.copy(isPaused = !it.isPaused) }
    }

    /** Toggle mute state. */
    fun onToggleMute() {
        _uiState.update { it.copy(isMuted = !it.isMuted) }
    }

    /** Toggle UI overlay visibility. */
    fun onToggleUi() {
        _uiState.update { it.copy(showUi = !it.showUi) }
    }

    /** Update drag offset for swipe-to-dismiss. */
    fun onDrag(delta: Float) {
        _uiState.update { it.copy(dragOffset = (it.dragOffset + delta).coerceAtLeast(0f)) }
    }

    /** Reset drag offset (swipe cancelled). */
    fun onDragEnd() {
        _uiState.update { it.copy(dragOffset = 0f) }
    }

    /** Update reply text. */
    fun onReplyTextChange(text: String) {
        _uiState.update { it.copy(replyText = text) }
    }

    /** Send a reply. */
    fun onSendReply() {
        // TODO: Send reply message via SendMessageUseCase
        _uiState.update { it.copy(replyText = "") }
    }

    /** Add emoji to reply text. */
    fun onEmojiTap(emoji: String) {
        _uiState.update { it.copy(replyText = it.replyText + emoji) }
    }

    /** Toggle viewers bottom sheet. */
    fun onToggleViewersSheet() {
        _uiState.update { it.copy(showViewersSheet = !it.showViewersSheet) }
    }

    /** Current story being viewed. */
    val currentStory: Story?
        get() = _uiState.value.stories.getOrNull(_uiState.value.currentIndex)

    override fun onCleared() {
        super.onCleared()
        autoAdvanceJob?.cancel()
    }

    // ─── Mock data (shared with StoriesScreen) ──────────────────────────

    private fun mockStoriesForUser(userId: String): List<Story> {
        val now = System.currentTimeMillis()
        val hour = 3_600_000L

        return when (userId) {
            "user_1" -> listOf(
                Story("story_alice_1", userId, "Alice Wonderland", null,
                    "https://picsum.photos/1080/1920?random=10", null,
                    StoryType.IMAGE, "Sunset vibes 🌅", null, 10,
                    now - 3 * hour, now + 21 * hour,
                    listOf(StoryView("user_2", "Bob Builder", now - 2 * hour)),
                    emptyMap(), StoryPrivacy.ALL_CONTACTS, emptyList(), emptyList(),
                    true, true, false),
                Story("story_alice_2", userId, "Alice Wonderland", null,
                    "https://picsum.photos/1080/1920?random=11", null,
                    StoryType.VIDEO, "Walking in the park 🚶‍♀️", null, 15,
                    now - 2 * hour, now + 22 * hour,
                    emptyList(), emptyMap(), StoryPrivacy.ALL_CONTACTS,
                    emptyList(), emptyList(), true, true, false),
            )
            "user_self" -> listOf(
                Story("my_story_1", userId, "You", null,
                    "https://picsum.photos/1080/1920?random=1", null,
                    StoryType.IMAGE, "Good morning! ☀️", null, 10,
                    now - 2 * hour, now + 22 * hour,
                    listOf(StoryView("user_1", "Alice", now - hour)),
                    emptyMap(), StoryPrivacy.ALL_CONTACTS, emptyList(), emptyList(),
                    true, true, false),
                Story("my_story_2", userId, "You", null,
                    "https://picsum.photos/1080/1920?random=2", null,
                    StoryType.IMAGE, "Coffee time ☕", null, 10,
                    now - 30 * 60_000, now + 23 * hour + 30 * 60_000,
                    emptyList(), emptyMap(), StoryPrivacy.ALL_CONTACTS,
                    emptyList(), emptyList(), true, true, false),
            )
            else -> emptyList()
        }
    }
}
