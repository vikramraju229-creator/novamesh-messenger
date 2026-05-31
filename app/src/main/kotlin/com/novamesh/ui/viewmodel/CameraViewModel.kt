/**
 * ViewModel for the CameraScreen — Snapchat-style camera with AR filters,
 * drawing tools, text overlay, and post-capture actions.
 *
 * Manages camera state, capture flow, and snap sharing.
 */
package com.novamesh.ui.viewmodel

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novamesh.domain.model.MessageContent
import com.novamesh.domain.model.Story
import com.novamesh.domain.model.StoryType
import com.novamesh.domain.usecase.SendMessageUseCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Flash mode enum. */
enum class FlashMode { OFF, ON, AUTO }

/** Self-timer option. */
data class TimerOption(val seconds: Int, val label: String) {
    companion object {
        val OFF = TimerOption(0, "Off")
        val THREE = TimerOption(3, "3s")
        val TEN = TimerOption(10, "10s")
    }
}

/** AR filter definition. */
data class CameraFilter(
    val id: String,
    val name: String,
    val emoji: String,
    val overlayColor: Color,
)

/** Capture mode. */
enum class CaptureMode { PHOTO, VIDEO }

/** Drawing stroke. */
data class DrawStroke(
    val points: List<Offset> = emptyList(),
    val color: Color = Color.White,
)

/** Post-capture snap data. */
data class CapturedSnap(
    val uri: String,
    val isVideo: Boolean,
)

/** UI state for the camera screen. */
data class CameraUiState(
    val flashMode: FlashMode = FlashMode.OFF,
    val timerOption: TimerOption = TimerOption.OFF,
    val isFrontCamera: Boolean = true,
    val captureMode: CaptureMode = CaptureMode.PHOTO,
    val selectedFilterId: String? = null,
    val isCapturing: Boolean = false,
    val isRecording: Boolean = false,
    val recordingElapsedMs: Long = 0L,
    val timerCountdown: Int = 0,
    val showTextTool: Boolean = false,
    val overlayText: String = "",
    val isDrawingMode: Boolean = false,
    val selectedDrawColor: Color = Color.White,
    val drawStrokes: List<DrawStroke> = emptyList(),
    val currentStroke: DrawStroke = DrawStroke(),
    val capturedSnap: CapturedSnap? = null,
    val showSendSheet: Boolean = false,
    val flipRotation: Float = 0f,
    val availableFilters: List<CameraFilter> = defaultFilters,
)

/** Default set of AR face filters. */
private val defaultFilters = listOf(
    CameraFilter("dog", "Dog", "🐶", Color(0x44FF9800)),
    CameraFilter("crown", "Crown", "👑", Color(0x44FFD600)),
    CameraFilter("glasses", "Glasses", "👓", Color(0x444FC3F7)),
    CameraFilter("rainbow", "Rainbow", "🌈", Color(0x44E040FB)),
    CameraFilter("retro", "Retro", "📺", Color(0x44FF5252)),
    CameraFilter("heart", "Heart", "❤️", Color(0x44FF1744)),
    CameraFilter("star", "Star", "⭐", Color(0x44FFD600)),
    CameraFilter("fire", "Fire", "🔥", Color(0x44FF6D00)),
)

/** Drawing colors palette. */
val drawingColors = listOf(
    Color.White, Color.Red, Color(0xFFFF9800), Color(0xFFFFEB3B),
    Color(0xFF4CAF50), Color(0xFF2196F3), Color(0xFF9C27B0), Color.Black,
)

/**
 * @param sendMessageUseCase For sending captured snaps to chats.
 */
class CameraViewModel(
    private val sendMessageUseCase: SendMessageUseCase? = null,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    // ─── Flash ───────────────────────────────────────────────────────────

    fun onFlashClick() {
        _uiState.update {
            it.copy(flashMode = when (it.flashMode) {
                FlashMode.OFF -> FlashMode.ON
                FlashMode.ON -> FlashMode.AUTO
                FlashMode.AUTO -> FlashMode.OFF
            })
        }
    }

    // ─── Timer ──────────────────────────────────────────────────────────

    fun onTimerClick() {
        _uiState.update {
            it.copy(timerOption = when (it.timerOption.seconds) {
                0 -> TimerOption.THREE
                3 -> TimerOption.TEN
                else -> TimerOption.OFF
            })
        }
    }

    // ─── Camera flip ────────────────────────────────────────────────────

    fun onFlipClick() {
        _uiState.update {
            it.copy(
                isFrontCamera = !it.isFrontCamera,
                flipRotation = it.flipRotation + 180f,
            )
        }
    }

    // ─── Filters ────────────────────────────────────────────────────────

    fun onFilterSelect(filterId: String?) {
        _uiState.update {
            it.copy(selectedFilterId = if (filterId == it.selectedFilterId) null else filterId)
        }
    }

    // ─── Capture ────────────────────────────────────────────────────────

    /** Take a photo (or start timer countdown if set). */
    fun onCaptureClick() {
        val timerSecs = _uiState.value.timerOption.seconds
        if (timerSecs > 0) {
            _uiState.update { it.copy(timerCountdown = timerSecs) }
            viewModelScope.launch {
                while (_uiState.value.timerCountdown > 0) {
                    delay(1000)
                    _uiState.update { it.copy(timerCountdown = it.timerCountdown - 1) }
                }
                performCapture()
            }
        } else {
            performCapture()
        }
    }

    /** Start video recording (long-press). */
    fun onCaptureLongPress() {
        _uiState.update {
            it.copy(isRecording = true, captureMode = CaptureMode.VIDEO)
        }
        viewModelScope.launch {
            while (_uiState.value.isRecording) {
                delay(1000)
                _uiState.update {
                    it.copy(recordingElapsedMs = it.recordingElapsedMs + 1000)
                }
            }
        }
    }

    /** Stop video recording. */
    fun onCaptureLongPressRelease() {
        _uiState.update {
            it.copy(
                isRecording = false,
                capturedSnap = CapturedSnap(
                    uri = "file:///nova_video_${System.currentTimeMillis()}.mp4",
                    isVideo = true,
                ),
            )
        }
    }

    private fun performCapture() {
        _uiState.update { it.copy(isCapturing = true) }
        viewModelScope.launch {
            delay(300)
            _uiState.update {
                it.copy(
                    isCapturing = false,
                    capturedSnap = CapturedSnap(
                        uri = "file:///nova_snap_${System.currentTimeMillis()}.jpg",
                        isVideo = false,
                    ),
                )
            }
        }
    }

    // ─── Text overlay ───────────────────────────────────────────────────

    fun onTextToolClick() {
        _uiState.update { it.copy(showTextTool = !it.showTextTool) }
    }

    fun onOverlayTextChange(text: String) {
        _uiState.update { it.copy(overlayText = text) }
    }

    // ─── Drawing ────────────────────────────────────────────────────────

    fun onDrawingToolClick() {
        _uiState.update { it.copy(isDrawingMode = !it.isDrawingMode) }
    }

    fun onDrawColorSelect(color: Color) {
        _uiState.update { it.copy(selectedDrawColor = color) }
    }

    fun onDrawingPoint(point: Offset) {
        _uiState.update {
            it.copy(currentStroke = it.currentStroke.copy(
                points = it.currentStroke.points + point,
                color = it.selectedDrawColor,
            ))
        }
    }

    fun onDrawingStrokeEnd() {
        _uiState.update {
            it.copy(
                drawStrokes = it.drawStrokes + it.currentStroke,
                currentStroke = DrawStroke(),
            )
        }
    }

    // ─── Post-capture ───────────────────────────────────────────────────

    /** Discard captured snap and retake. */
    fun onRetake() {
        _uiState.update { it.copy(capturedSnap = null) }
    }

    /** Show the send-to sheet. */
    fun onShowSendSheet() {
        _uiState.update { it.copy(showSendSheet = true) }
    }

    /** Hide the send-to sheet. */
    fun onDismissSendSheet() {
        _uiState.update { it.copy(showSendSheet = false) }
    }

    /** Send snap to a chat. */
    fun onSendToChat(chatId: String) {
        val snap = _uiState.value.capturedSnap ?: return
        viewModelScope.launch {
            if (snap.isVideo) {
                sendMessageUseCase?.invoke(chatId, MessageContent.Video(uri = snap.uri))
            } else {
                sendMessageUseCase?.invoke(chatId, MessageContent.Image(uri = snap.uri))
            }
            _uiState.update { it.copy(capturedSnap = null, showSendSheet = false) }
        }
    }

    /** Add snap to my story. */
    fun onAddToStory() {
        // TODO: Upload media and create story entry
        _uiState.update { it.copy(capturedSnap = null, showSendSheet = false) }
    }
}
