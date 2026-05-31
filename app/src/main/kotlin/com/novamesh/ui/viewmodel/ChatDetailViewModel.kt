/**
 * ViewModel for the ChatDetailScreen — individual chat conversation.
 *
 * Manages message list, sending messages, toggling reactions, reply context,
 * voice recording state, typing indicator, and marking messages as read.
 */
package com.novamesh.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novamesh.domain.model.Message
import com.novamesh.domain.model.MessageContent
import com.novamesh.domain.usecase.GetMessagesUseCase
import com.novamesh.domain.usecase.MarkAsReadUseCase
import com.novamesh.domain.usecase.SendMessageUseCase
import com.novamesh.domain.usecase.ToggleReactionUseCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Voice recording state. */
data class VoiceRecordingState(
    val isRecording: Boolean = false,
    val durationMs: Long = 0L,
    val waveform: List<Float> = emptyList(),
)

/** Reply context — the message being replied to. */
data class ReplyContext(
    val messageId: String,
    val senderName: String,
    val preview: String,
)

/** UI state for the chat detail screen. */
data class ChatDetailUiState(
    val chatId: String = "",
    val chatName: String = "",
    val messages: List<Message> = emptyList(),
    val messageInput: String = "",
    val isTyping: Boolean = false,
    val voiceRecording: VoiceRecordingState = VoiceRecordingState(),
    val replyContext: ReplyContext? = null,
    val disappearingTimerSeconds: Int? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
)

/**
 * @param getMessagesUseCase Use case for observing messages.
 * @param sendMessageUseCase Use case for sending new messages.
 * @param toggleReactionUseCase Use case for toggling emoji reactions.
 * @param markAsReadUseCase Use case for read receipts.
 */
class ChatDetailViewModel(
    private val getMessagesUseCase: GetMessagesUseCase,
    private val sendMessageUseCase: SendMessageUseCase,
    private val toggleReactionUseCase: ToggleReactionUseCase,
    private val markAsReadUseCase: MarkAsReadUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatDetailUiState())
    val uiState: StateFlow<ChatDetailUiState> = _uiState.asStateFlow()

    /** Initialize the ViewModel with a chat context. */
    fun initialize(chatId: String, chatName: String) {
        if (_uiState.value.chatId == chatId) return // Already initialized
        _uiState.update { it.copy(chatId = chatId, chatName = chatName) }
        observeMessages(chatId)
        simulateTyping()
    }

    /** Observe messages for this chat. */
    private fun observeMessages(chatId: String) {
        viewModelScope.launch {
            getMessagesUseCase(chatId).collect { messages ->
                _uiState.update { it.copy(messages = messages, isLoading = false) }
            }
        }
    }

    /** Simulate the typing indicator cycling on/off. */
    private fun simulateTyping() {
        viewModelScope.launch {
            while (true) {
                delay(8_000)
                _uiState.update { it.copy(isTyping = true) }
                delay(4_000)
                _uiState.update { it.copy(isTyping = false) }
                delay(12_000)
            }
        }
    }

    // ─── User actions ───────────────────────────────────────────────────

    /** Update the message input text. */
    fun onMessageInputChange(text: String) {
        _uiState.update { it.copy(messageInput = text) }
    }

    /** Send the current message. */
    fun onSendMessage() {
        val text = _uiState.value.messageInput.trim()
        if (text.isBlank()) return
        viewModelScope.launch {
            sendMessageUseCase(
                chatId = _uiState.value.chatId,
                content = MessageContent.Text(text),
            )
            _uiState.update { it.copy(messageInput = "") }
        }
    }

    /** Toggle a reaction on a message. */
    fun onToggleReaction(messageId: String, emoji: String) {
        viewModelScope.launch {
            toggleReactionUseCase(messageId, emoji)
        }
    }

    /** Set the reply context (swipe-to-reply). */
    fun onReply(messageId: String, senderName: String, preview: String) {
        _uiState.update {
            it.copy(replyContext = ReplyContext(messageId, senderName, preview))
        }
    }

    /** Clear the reply context. */
    fun onClearReply() {
        _uiState.update { it.copy(replyContext = null) }
    }

    /** Start voice recording. */
    fun onStartRecording() {
        _uiState.update {
            it.copy(voiceRecording = VoiceRecordingState(isRecording = true))
        }
        // Simulate recording duration ticker
        viewModelScope.launch {
            while (_uiState.value.voiceRecording.isRecording) {
                delay(1_000)
                _uiState.update {
                    it.copy(voiceRecording = it.voiceRecording.copy(
                        durationMs = it.voiceRecording.durationMs + 1_000,
                        waveform = listOf(0.1f, 0.3f, 0.5f, 0.7f, 0.4f, 0.6f, 0.8f, 0.5f),
                    ))
                }
            }
        }
    }

    /** Stop voice recording and send the voice message. */
    fun onStopRecording() {
        val recording = _uiState.value.voiceRecording
        viewModelScope.launch {
            sendMessageUseCase(
                chatId = _uiState.value.chatId,
                content = MessageContent.Voice(
                    uri = "file:///recording_${System.currentTimeMillis()}.ogg",
                    durationMs = recording.durationMs,
                    waveform = recording.waveform.ifEmpty { null },
                ),
            )
            _uiState.update {
                it.copy(voiceRecording = VoiceRecordingState())
            }
        }
    }

    /** Mark messages as read when the chat is opened. */
    fun onMarkAsRead(messageId: String) {
        viewModelScope.launch {
            markAsReadUseCase(_uiState.value.chatId, messageId)
        }
    }
}
