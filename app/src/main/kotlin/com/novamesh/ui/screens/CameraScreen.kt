/**
 * CameraScreen — a full-screen camera UI inspired by Snapchat.
 *
 * Features:
 * - Full-screen camera preview via CameraX PreviewView (mock fallback)
 * - Front / rear camera toggle with smooth rotation animation
 * - Photo capture (tap) and video recording (long-press) with red pulsing
 *   indicator + elapsed timer
 * - 3 s / 10 s self-timer option
 * - Flash toggle (on / off / auto)
 * - AR face filters carousel ("Dog", "Crown", "Glasses", "Rainbow", "Retro")
 *   with mock overlay
 * - Text overlay tool (add editable text on the snap)
 * - Drawing tool (toggle drawing mode)
 * - Post-capture preview with "Send to Chat" and "Add to My Story" bottom sheet
 *
 * All data is mocked. In production CameraX bindings and ViewModel integration
 * would replace the simulated preview and capture flow.
 */

@file:OptIn(ExperimentalMaterial3Api::class)

package com.novamesh.ui.screens

import android.view.ViewGroup
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CameraFront
import androidx.compose.material.icons.filled.CameraRear
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.HistoryToggleOff
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RotateLeft
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Timer10
import androidx.compose.material.icons.filled.Timer3
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.FiberManualRecord
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.novamesh.ui.theme.NovaGlass
import com.novamesh.ui.theme.NovaGlassDark
import com.novamesh.ui.theme.NovaPrimary
import com.novamesh.ui.theme.NovaSecondary
import com.novamesh.ui.theme.NovaSuccess
import com.novamesh.ui.theme.NovaSurfaceDark
import com.novamesh.ui.theme.NovaTertiary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// ═════════════════════════════════════════════════════════════════════════════
// Data models — internal to this screen
// ═════════════════════════════════════════════════════════════════════════════

/** Flash mode state machine. */
private enum class FlashMode { OFF, ON, AUTO }

/** Self-timer options. */
private enum class TimerOption(val seconds: Int, val label: String) {
  OFF(0, "Off"),
  THREE(3, "3s"),
  TEN(10, "10s"),
}

/** An AR face filter definition. */
private data class Filter(
  val id: String,
  val name: String,
  val emoji: String,
  val overlayColor: Color,
)

/** The current capture mode. */
private enum class CaptureMode { PHOTO, VIDEO }

// ═════════════════════════════════════════════════════════════════════════════
// Mock data
// ═════════════════════════════════════════════════════════════════════════════

private val mockFilters = listOf(
  Filter("dog", "Dog", "🐶", Color(0x44FF9800)),
  Filter("crown", "Crown", "👑", Color(0x44FFD600)),
  Filter("glasses", "Glasses", "👓", Color(0x444FC3F7)),
  Filter("rainbow", "Rainbow", "🌈", Color(0x44E040FB)),
  Filter("retro", "Retro", "📺", Color(0x44FF5252)),
  Filter("heart", "Heart", "❤️", Color(0x44FF1744)),
  Filter("star", "Star", "⭐", Color(0x44FFD600)),
  Filter("fire", "Fire", "🔥", Color(0x44FF6D00)),
)

/** Mock gallery thumbnails (recent photos). */
private val mockGalleryThumbnails = listOf(
  "thumb_1", "thumb_2", "thumb_3", "thumb_4", "thumb_5",
)

/** Drawing colors palette. */
private val drawingColors = listOf(
  Color.White,
  Color.Red,
  Color(0xFFFF9800),
  Color(0xFFFFEB3B),
  Color(0xFF4CAF50),
  Color(0xFF2196F3),
  Color(0xFF9C27B0),
  Color.Black,
)

// ═════════════════════════════════════════════════════════════════════════════
// Public composable — entry point from NavGraph
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Full-screen camera screen inspired by Snapchat.
 *
 * @param onBack Navigate back to the previous screen.
 * @param onSnapTaken Called with the captured media URI and whether it's a video.
 */
@Composable
fun CameraScreen(
  onBack: () -> Unit,
  onSnapTaken: (mediaUri: String, isVideo: Boolean) -> Unit,
) {
  // ─── Core state ───────────────────────────────────────────────────────────
  var flashMode by remember { mutableStateOf(FlashMode.OFF) }
  var timerOption by remember { mutableStateOf(TimerOption.OFF) }
  var isFrontCamera by remember { mutableStateOf(true) }
  var selectedFilterId by remember { mutableStateOf<String?>(null) }
  var captureMode by remember { mutableStateOf(CaptureMode.PHOTO) }

  // Capture state
  var isCapturing by remember { mutableStateOf(false) }
  var isRecording by remember { mutableStateOf(false) }
  var recordingElapsedMs by remember { mutableLongStateOf(0L) }
  var capturedUri by remember { mutableStateOf<String?>(null) }
  var capturedIsVideo by remember { mutableStateOf(false) }

  // Text overlay state
  var showTextTool by remember { mutableStateOf(false) }
  var overlayText by remember { mutableStateOf("") }

  // Drawing state
  var isDrawingMode by remember { mutableStateOf(false) }
  var selectedDrawColor by remember { mutableStateOf(drawingColors[0]) }
  var drawPoints by remember { mutableStateOf(listOf<List<Offset>>()) }
  var currentStroke by remember { mutableStateOf(listOf<Offset>()) }

  // UI visibility
  var showSendSheet by remember { mutableStateOf(false) }

  // Timer countdown
  var timerCountdown by remember { mutableIntStateOf(0) }

  // Camera flip animation
  var flipRotation by remember { mutableFloatStateOf(0f) }

  val scope = rememberCoroutineScope()
  val density = LocalDensity.current

  // ─── Timer countdown effect ───────────────────────────────────────────────
  LaunchedEffect(timerCountdown) {
    if (timerCountdown > 0) {
      delay(1000L)
      timerCountdown--
      if (timerCountdown == 0 && !isRecording) {
        // Simulate photo capture
        isCapturing = true
        scope.launch {
          delay(300)
          capturedUri = "file:///nova_snap_${System.currentTimeMillis()}.jpg"
          capturedIsVideo = false
          isCapturing = false
        }
      }
    }
  }

  // ─── Recording timer effect ───────────────────────────────────────────────
  LaunchedEffect(isRecording) {
    if (isRecording) {
      recordingElapsedMs = 0L
      while (isRecording) {
        delay(1000L)
        recordingElapsedMs += 1000L
      }
    }
  }

  // ═════════════════════════════════════════════════════════════════════════
  // Root layout
  // ═════════════════════════════════════════════════════════════════════════
  Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

    // ── 1. Camera preview (full screen) ──────────────────────────────────
    CameraPreview(
      isFrontCamera = isFrontCamera,
      modifier = Modifier.fillMaxSize(),
    )

    // ── 2. AR filter overlay ─────────────────────────────────────────────
    if (selectedFilterId != null) {
      val filter = mockFilters.find { it.id == selectedFilterId }
      if (filter != null) {
        FilterOverlay(
          filter = filter,
          modifier = Modifier.fillMaxSize(),
        )
      }
    }

    // ── 3. Drawing overlay (when drawing mode is active) ──────────────────
    if (isDrawingMode) {
      DrawingCanvas(
        points = drawPoints,
        currentStroke = currentStroke,
        color = selectedDrawColor,
        onNewPoint = { currentStroke = currentStroke + it },
        onStrokeEnd = {
          drawPoints = drawPoints + listOf(currentStroke)
          currentStroke = emptyList()
        },
        modifier = Modifier.fillMaxSize(),
      )
    }

    // ── 4. Text overlay (when text tool is active) ────────────────────────
    if (showTextTool && overlayText.isNotBlank()) {
      TextOverlayDisplay(
        text = overlayText,
        modifier = Modifier.fillMaxSize(),
      )
    }

    // ── 5. Recording indicator (top center) ──────────────────────────────
    if (isRecording) {
      RecordingIndicator(
        elapsedMs = recordingElapsedMs,
        modifier = Modifier
          .align(Alignment.TopCenter)
          .padding(top = 60.dp),
      )
    }

    // ── 6. Timer countdown overlay (center) ──────────────────────────────
    if (timerCountdown > 0) {
      TimerCountdownOverlay(
        seconds = timerCountdown,
        modifier = Modifier.align(Alignment.Center),
      )
    }

    // ── 7. Top controls ──────────────────────────────────────────────────
    TopControls(
      flashMode = flashMode,
      onFlashClick = { flashMode = flashMode.next() },
      timerOption = timerOption,
      onTimerClick = { timerOption = timerOption.next() },
      showTextTool = showTextTool,
      onTextToolClick = { showTextTool = !showTextTool },
      isDrawingMode = isDrawingMode,
      onDrawingToolClick = { isDrawingMode = !isDrawingMode },
      onBack = onBack,
      modifier = Modifier.align(Alignment.TopStart),
    )

    // ── 8. Bottom controls (only when not in post-capture preview) ────────
    if (capturedUri == null) {
      BottomControls(
        galleryThumbnails = mockGalleryThumbnails,
        isRecording = isRecording,
        isFrontCamera = isFrontCamera,
        flipRotation = flipRotation,
        selectedDrawColor = selectedDrawColor,
        isDrawingMode = isDrawingMode,
        drawingColors = drawingColors,
        onGalleryClick = { /* TODO: open gallery */ },
        onCaptureClick = {
          if (timerOption.seconds > 0) {
            timerCountdown = timerOption.seconds
          } else {
            isCapturing = true
            scope.launch {
              delay(300)
              capturedUri = "file:///nova_snap_${System.currentTimeMillis()}.jpg"
              capturedIsVideo = false
              isCapturing = false
            }
          }
        },
        onCaptureLongPress = {
          // Start video recording
          isRecording = true
          captureMode = CaptureMode.VIDEO
        },
        onCaptureLongPressRelease = {
          // Stop video recording
          isRecording = false
          capturedUri = "file:///nova_video_${System.currentTimeMillis()}.mp4"
          capturedIsVideo = true
        },
        onFlipClick = {
          isFrontCamera = !isFrontCamera
          flipRotation += 180f
        },
        onDrawColorSelect = { selectedDrawColor = it },
        modifier = Modifier.align(Alignment.BottomCenter),
      )

      // ── 9. Filter carousel (above bottom controls) ─────────────────────
      FilterCarousel(
        filters = mockFilters,
        selectedFilterId = selectedFilterId,
        onFilterSelect = { selectedFilterId = it },
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .padding(bottom = 130.dp),
      )
    }

    // ── 10. Post-capture preview ─────────────────────────────────────────
    if (capturedUri != null) {
      CapturePreviewOverlay(
        onSendToChat = { onSnapTaken(capturedUri!!, capturedIsVideo) },
        onAddToStory = {
          // In a real app this would upload and add to story
          onSnapTaken(capturedUri!!, capturedIsVideo)
        },
        onRetake = {
          capturedUri = null
          capturedIsVideo = false
        },
        modifier = Modifier.align(Alignment.BottomCenter),
      )
    }

    // ── 11. Send-to bottom sheet ─────────────────────────────────────────
    if (showSendSheet) {
      SendToSheet(
        onDismiss = { showSendSheet = false },
        onSendToChat = { /* TODO: navigate to chat picker */ },
        onAddToStory = { /* TODO: add to story */ },
      )
    }
  }
}

// ═════════════════════════════════════════════════════════════════════════════
// Camera preview (AndroidView wrapping CameraX PreviewView)
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Full-screen camera preview using CameraX [PreviewView] wrapped in
 * [AndroidView]. When CameraX is unavailable (e.g. emulator without camera),
 * a mock dark preview is shown.
 */
@Composable
private fun CameraPreview(
  isFrontCamera: Boolean,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current

  AndroidView(
    factory = { ctx ->
      PreviewView(ctx).also { previewView ->
        previewView.layoutParams = ViewGroup.LayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT,
          ViewGroup.LayoutParams.MATCH_PARENT,
        )
        previewView.scaleType = PreviewView.ScaleType.FILL_CENTER
        previewView.implementationMode = PreviewView.ImplementationMode.COMPATIBLE

        // In production, bind CameraX here:
        //   val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
        //   cameraProviderFuture.addListener({
        //     val cameraProvider = cameraProviderFuture.get()
        //     val preview = Preview.Builder().build()
        //     preview.setSurfaceProvider(previewView.surfaceProvider)
        //     val cameraSelector = if (isFrontCamera)
        //       CameraSelector.DEFAULT_FRONT_CAMERA
        //     else
        //       CameraSelector.DEFAULT_BACK_CAMERA
        //     cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
        //   }, ContextCompat.getMainExecutor(ctx))
      }
    },
    modifier = modifier,
  )
}

// ═════════════════════════════════════════════════════════════════════════════
// Filter overlay (mock AR face filter)
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Draws a mock AR face filter overlay.
 *
 * In production this would use ML Kit face detection and render 3D assets.
 * Here we display the filter name, emoji, a decorative ring, and a subtle
 * tinted overlay colour.
 */
@Composable
private fun FilterOverlay(
  filter: Filter,
  modifier: Modifier = Modifier,
) {
  Box(modifier = modifier.background(filter.overlayColor)) {
    // Large emoji decoration (simulates a face filter)
    Text(
      text = filter.emoji,
      fontSize = 120.sp,
      modifier = Modifier
        .align(Alignment.Center)
        .offset(y = (-60).dp),
    )

    // Filter name chip at top
    Surface(
      shape = RoundedCornerShape(20.dp),
      color = Color(0x99000000),
      modifier = Modifier
        .align(Alignment.TopCenter)
        .padding(top = 120.dp),
    ) {
      Text(
        text = filter.name,
        style = MaterialTheme.typography.titleMedium,
        color = Color.White,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
      )
    }

    // Decorative animated rings
    val infiniteTransition = rememberInfiniteTransition(label = "filterRing")
    val ringAlpha by infiniteTransition.animateFloat(
      initialValue = 0.3f,
      targetValue = 0.8f,
      animationSpec = infiniteRepeatable(
        animation = tween(1500, easing = LinearEasing),
        repeatMode = RepeatMode.Reverse,
      ),
      label = "ringAlpha",
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
      val cx = size.width / 2
      val cy = size.height / 3
      val radius = size.width * 0.35f
      drawCircle(
        color = Color.White.copy(alpha = ringAlpha * 0.3f),
        radius = radius,
        center = Offset(cx, cy),
        style = Stroke(width = 3.dp.toPx()),
      )
      drawCircle(
        color = Color.White.copy(alpha = ringAlpha * 0.15f),
        radius = radius + 20.dp.toPx(),
        center = Offset(cx, cy),
        style = Stroke(
          width = 1.dp.toPx(),
          pathEffect = PathEffect.dashPathEffect(
            floatArrayOf(10f, 10f), 0f,
          ),
        ),
      )
    }
  }
}

// ═════════════════════════════════════════════════════════════════════════════
// Drawing canvas
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Full-screen drawing canvas that captures touch drag gestures and renders
 * strokes on a transparent overlay.
 */
@Composable
private fun DrawingCanvas(
  points: List<List<Offset>>,
  currentStroke: List<Offset>,
  color: Color,
  onNewPoint: (Offset) -> Unit,
  onStrokeEnd: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Canvas(
    modifier = modifier
      .pointerInput(Unit) {
        detectDragGestures(
          onDragStart = { offset -> onNewPoint(offset) },
          onDrag = { change, _ ->
            change.consume()
            onNewPoint(change.position)
          },
          onDragEnd = { onStrokeEnd() },
          onDragCancel = { onStrokeEnd() },
        )
      },
  ) {
    // Render completed strokes
    points.forEach { stroke ->
      if (stroke.size > 1) {
        for (i in 1 until stroke.size) {
          drawLine(
            color = color,
            start = stroke[i - 1],
            end = stroke[i],
            strokeWidth = 6.dp.toPx(),
            cap = androidx.compose.ui.graphics.StrokeCap.Round,
          )
        }
      }
    }
    // Render current (in-progress) stroke
    if (currentStroke.size > 1) {
      for (i in 1 until currentStroke.size) {
        drawLine(
          color = color,
          start = currentStroke[i - 1],
          end = currentStroke[i],
          strokeWidth = 6.dp.toPx(),
          cap = androidx.compose.ui.graphics.StrokeCap.Round,
        )
      }
    }
  }
}

// ═════════════════════════════════════════════════════════════════════════════
// Text overlay display
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Displays user-entered text centred on screen with a glassmorphism
 * background.
 */
@Composable
private fun TextOverlayDisplay(
  text: String,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier = modifier,
    contentAlignment = Alignment.Center,
  ) {
    Surface(
      shape = RoundedCornerShape(16.dp),
      color = Color(0x66000000),
      modifier = Modifier
        .padding(horizontal = 32.dp)
        .clickable { /* In production: edit text */ },
    ) {
      Text(
        text = text,
        style = TextStyle(
          color = Color.White,
          fontSize = 28.sp,
          fontWeight = FontWeight.Bold,
          textAlign = TextAlign.Center,
        ),
        modifier = Modifier.padding(24.dp),
      )
    }
  }
}

// ═════════════════════════════════════════════════════════════════════════════
// Recording indicator (red pulsing dot + elapsed time)
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Animated red recording indicator with elapsed time display.
 * Mimics Snapchat / Instagram video recording UI.
 */
@Composable
private fun RecordingIndicator(
  elapsedMs: Long,
  modifier: Modifier = Modifier,
) {
  val infiniteTransition = rememberInfiniteTransition(label = "recordingPulse")
  val pulseScale by infiniteTransition.animateFloat(
    initialValue = 1f,
    targetValue = 0.6f,
    animationSpec = infiniteRepeatable(
      animation = tween(600, easing = LinearEasing),
      repeatMode = RepeatMode.Reverse,
    ),
    label = "pulseScale",
  )

  Row(
    modifier = modifier
      .clip(RoundedCornerShape(20.dp))
      .background(Color(0x99000000))
      .padding(horizontal = 14.dp, vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    // Pulsing red dot
    Box(
      modifier = Modifier
        .size(12.dp)
        .scale(pulseScale)
        .clip(CircleShape)
        .background(Color(0xFFFF1744)),
    )

    Text(
      text = formatDuration(elapsedMs),
      style = MaterialTheme.typography.titleSmall,
      color = Color.White,
      fontWeight = FontWeight.Bold,
    )
  }
}

// ═════════════════════════════════════════════════════════════════════════════
// Timer countdown overlay
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Large centred countdown number shown when self-timer is active.
 */
@Composable
private fun TimerCountdownOverlay(
  seconds: Int,
  modifier: Modifier = Modifier,
) {
  val scale by animateFloatAsState(
    targetValue = if (seconds > 0) 1f else 0.5f,
    animationSpec = tween(200),
    label = "timerScale",
  )

  Text(
    text = "$seconds",
    style = MaterialTheme.typography.displayLarge.copy(
      fontWeight = FontWeight.Bold,
    ),
    color = Color.White,
    modifier = modifier.scale(scale),
  )
}

// ═════════════════════════════════════════════════════════════════════════════
// Top controls
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Top bar with back navigation, flash toggle, timer, text tool, and drawing
 * tool buttons.
 */
@Composable
private fun TopControls(
  flashMode: FlashMode,
  onFlashClick: () -> Unit,
  timerOption: TimerOption,
  onTimerClick: () -> Unit,
  showTextTool: Boolean,
  onTextToolClick: () -> Unit,
  isDrawingMode: Boolean,
  onDrawingToolClick: () -> Unit,
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .padding(start = 4.dp, end = 16.dp, top = 48.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    // ── Left: Back button ──
    IconButton(onClick = onBack) {
      Icon(
        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
        contentDescription = "Back",
        tint = Color.White,
      )
    }

    // ── Right: Tools ──
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
      // Drawing tool
      GlassIconButton(
        onClick = onDrawingToolClick,
        icon = Icons.Default.Brush,
        contentDescription = "Draw",
        isActive = isDrawingMode,
      )

      // Text overlay tool
      GlassIconButton(
        onClick = onTextToolClick,
        icon = Icons.Default.TextFields,
        contentDescription = "Add text",
        isActive = showTextTool,
      )

      // Timer button
      GlassIconButton(
        onClick = onTimerClick,
        icon = when (timerOption) {
          TimerOption.OFF -> Icons.Default.Timer
          TimerOption.THREE -> Icons.Default.Timer3
          TimerOption.TEN -> Icons.Default.Timer10
        },
        contentDescription = "Timer: ${timerOption.label}",
        isActive = timerOption != TimerOption.OFF,
      ) {
        if (timerOption != TimerOption.OFF) {
          Box(
            modifier = Modifier
              .align(Alignment.TopEnd)
              .offset(x = 4.dp, y = (-2).dp)
          ) {
            Text(
              text = timerOption.label,
              color = Color.White,
              fontSize = 9.sp,
              fontWeight = FontWeight.Bold,
            )
          }
        }
      }

      // Flash toggle
      GlassIconButton(
        onClick = onFlashClick,
        icon = when (flashMode) {
          FlashMode.OFF -> Icons.Default.FlashOff
          FlashMode.ON -> Icons.Default.FlashOn
          FlashMode.AUTO -> Icons.Default.FlashAuto
        },
        contentDescription = "Flash: $flashMode",
        isActive = flashMode != FlashMode.OFF,
      )
    }
  }
}

// ═════════════════════════════════════════════════════════════════════════════
// Glassmorphism icon button
// ═════════════════════════════════════════════════════════════════════════════

/**
 * A circular icon button with a semi-transparent glass background.
 * [badge] is an optional composable slot, e.g. for a timer label badge.
 */
@Composable
private fun GlassIconButton(
  onClick: () -> Unit,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  contentDescription: String,
  isActive: Boolean = false,
  modifier: Modifier = Modifier,
  badge: (@Composable androidx.compose.foundation.layout.BoxScope.() -> Unit)? = null,
) {
  val bgColor by animateColorAsState(
    targetValue = if (isActive) Color(0xCC7C4DFF) else Color(0x66000000),
    animationSpec = tween(200),
    label = "glassBtnBg",
  )

  Box(modifier = modifier) {
    IconButton(
      onClick = onClick,
      modifier = Modifier.size(44.dp),
      colors = IconButtonDefaults.iconButtonColors(
        containerColor = bgColor,
        contentColor = Color.White,
      ),
    ) {
      Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        modifier = Modifier.size(22.dp),
      )
    }
    badge?.invoke(this)
  }
}

// ═════════════════════════════════════════════════════════════════════════════
// Filter carousel
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Horizontal scrollable list of AR face filter thumbnails.
 * Each filter is displayed as a circular emoji with its name beneath.
 */
@Composable
private fun FilterCarousel(
  filters: List<Filter>,
  selectedFilterId: String?,
  onFilterSelect: (String?) -> Unit,
  modifier: Modifier = Modifier,
) {
  LazyRow(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 8.dp),
    horizontalArrangement = Arrangement.spacedBy(10.dp),
    contentPadding = PaddingValues(horizontal = 8.dp),
    state = rememberLazyListState(),
  ) {
    // "No filter" option
    item(key = "no_filter") {
      FilterThumbnail(
        emoji = "🙂",
        name = "None",
        isSelected = selectedFilterId == null,
        onClick = { onFilterSelect(null) },
      )
    }

    items(items = filters, key = { it.id }) { filter ->
      FilterThumbnail(
        emoji = filter.emoji,
        name = filter.name,
        isSelected = selectedFilterId == filter.id,
        onClick = {
          onFilterSelect(
            if (selectedFilterId == filter.id) null else filter.id,
          )
        },
      )
    }
  }
}

/**
 * Single filter thumbnail — a circular emoji avatar with a label below.
 * Glows with the accent colour when selected.
 */
@Composable
private fun FilterThumbnail(
  emoji: String,
  name: String,
  isSelected: Boolean,
  onClick: () -> Unit,
) {
  val borderColor by animateColorAsState(
    targetValue = if (isSelected) NovaPrimary else Color.Transparent,
    animationSpec = tween(200),
    label = "filterBorder",
  )

  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier
      .widthIn(min = 56.dp)
      .clickable(onClick = onClick),
  ) {
    Box(
      modifier = Modifier
        .size(52.dp)
        .clip(CircleShape)
        .background(if (isSelected) Color(0x337C4DFF) else Color(0x66000000))
        .then(
          if (isSelected) Modifier.border(2.dp, borderColor, CircleShape)
          else Modifier
        ),
      contentAlignment = Alignment.Center,
    ) {
      Text(
        text = emoji,
        fontSize = 24.sp,
      )
    }

    Spacer(modifier = Modifier.height(4.dp))

    Text(
      text = name,
      style = MaterialTheme.typography.labelSmall,
      color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
      fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      textAlign = TextAlign.Center,
    )
  }
}

// ═════════════════════════════════════════════════════════════════════════════
// Bottom controls
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Bottom bar containing gallery preview, capture button, flip toggle, and
 * drawing colour picker.
 */
@Composable
private fun BottomControls(
  galleryThumbnails: List<String>,
  isRecording: Boolean,
  isFrontCamera: Boolean,
  flipRotation: Float,
  selectedDrawColor: Color,
  isDrawingMode: Boolean,
  drawingColors: List<Color>,
  onGalleryClick: () -> Unit,
  onCaptureClick: () -> Unit,
  onCaptureLongPress: () -> Unit,
  onCaptureLongPressRelease: () -> Unit,
  onFlipClick: () -> Unit,
  onDrawColorSelect: (Color) -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(modifier = modifier.fillMaxWidth()) {
    // Drawing colour palette (visible only in drawing mode)
    AnimatedVisibility(
      visible = isDrawingMode,
      enter = fadeIn() + slideInVertically { it / 2 },
      exit = fadeOut() + slideOutVertically { it / 2 },
    ) {
      DrawingColorPicker(
        colors = drawingColors,
        selectedColor = selectedDrawColor,
        onColorSelect = onDrawColorSelect,
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 8.dp),
      )
    }

    // Main bottom row
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      // ── Left: Gallery thumbnail preview ──
      GalleryPreview(
        thumbnails = galleryThumbnails,
        onClick = onGalleryClick,
      )

      // ── Centre: Capture button ──
      CaptureButton(
        isRecording = isRecording,
        onClick = onCaptureClick,
        onLongPress = onCaptureLongPress,
        onLongPressRelease = onCaptureLongPressRelease,
      )

      // ── Right: Front/rear flip ──
      IconButton(
        onClick = onFlipClick,
        modifier = Modifier
          .size(48.dp)
          .clip(CircleShape)
          .background(Color(0x66000000)),
      ) {
        Icon(
          imageVector = if (isFrontCamera)
            Icons.Default.CameraFront
          else
            Icons.Default.CameraRear,
          contentDescription = "Flip camera",
          tint = Color.White,
          modifier = Modifier
            .size(26.dp)
            .rotate(flipRotation),
        )
      }
    }
  }
}

// ═════════════════════════════════════════════════════════════════════════════
// Gallery preview
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Shows the most recent gallery thumbnail as a small rounded square,
 * mimicking Snapchat's gallery shortcut.
 */
@Composable
private fun GalleryPreview(
  thumbnails: List<String>,
  onClick: () -> Unit,
) {
  Box(
    modifier = Modifier
      .size(48.dp)
      .clip(RoundedCornerShape(8.dp))
      .background(Color(0x66000000))
      .clickable(onClick = onClick),
    contentAlignment = Alignment.Center,
  ) {
    if (thumbnails.isNotEmpty()) {
      // Mock thumbnail — in production load from ContentResolver
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(Color(0xFF37474F)),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = Icons.Default.PhotoLibrary,
          contentDescription = "Gallery",
          tint = Color.White.copy(alpha = 0.7f),
          modifier = Modifier.size(24.dp),
        )
      }
    }
  }
}

// ═════════════════════════════════════════════════════════════════════════════
// Capture button
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Large circular capture button.
 *
 * - **Tap** → take a photo (with brief scale-down animation)
 * - **Long-press & hold** → record video (shows outer red ring)
 * - **Release after long-press** → stop recording
 */
@Composable
private fun CaptureButton(
  isRecording: Boolean,
  onClick: () -> Unit,
  onLongPress: () -> Unit,
  onLongPressRelease: () -> Unit,
) {
  var isPressed by remember { mutableStateOf(false) }
  var isLongPressing by remember { mutableStateOf(false) }

  val outerRingColor by animateColorAsState(
    targetValue = when {
      isRecording -> Color(0xFFFF1744)
      isPressed -> Color.White.copy(alpha = 0.5f)
      else -> Color.White.copy(alpha = 0.8f)
    },
    animationSpec = tween(150),
    label = "captureRingColor",
  )

  val innerScale by animateFloatAsState(
    targetValue = if (isPressed || isRecording) 0.85f else 1f,
    animationSpec = tween(150),
    label = "captureInnerScale",
  )

  val infiniteTransition = rememberInfiniteTransition(label = "capturePulse")
  val recordingOuterScale by infiniteTransition.animateFloat(
    initialValue = 1f,
    targetValue = 1.12f,
    animationSpec = infiniteRepeatable(
      animation = tween(800, easing = LinearEasing),
      repeatMode = RepeatMode.Reverse,
    ),
    label = "recordingOuterScale",
  )

  Box(
    contentAlignment = Alignment.Center,
    modifier = Modifier
      .size(80.dp)
      .scale(if (isRecording) recordingOuterScale else 1f)
      .pointerInput(Unit) {
        detectTapGestures(
          onTap = {
            onClick()
          },
          onLongPress = {
            isLongPressing = true
            isPressed = true
            onLongPress()
          },
        )
      },
  ) {
    // Outer ring
    Box(
      modifier = Modifier
        .size(76.dp)
        .border(4.dp, outerRingColor, CircleShape),
    )

    // Inner circle
    Box(
      modifier = Modifier
        .size(64.dp)
        .scale(innerScale)
        .clip(CircleShape)
        .background(
          if (isRecording) Color(0xFFFF1744)
          else Color.White,
        ),
      contentAlignment = Alignment.Center,
    ) {
      if (isRecording) {
        // Recording square (like Snapchat/Instagram)
        Box(
          modifier = Modifier
            .size(24.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Color.White),
        )
      }
    }
  }
}

// ═════════════════════════════════════════════════════════════════════════════
// Drawing colour picker
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Horizontal row of colour circles for the drawing tool.
 * The selected colour has a white outer ring.
 */
@Composable
private fun DrawingColorPicker(
  colors: List<Color>,
  selectedColor: Color,
  onColorSelect: (Color) -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier
      .padding(horizontal = 24.dp),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    colors.forEach { color ->
      val isSelected = color == selectedColor
      Box(
        modifier = Modifier
          .size(if (isSelected) 36.dp else 28.dp)
          .clip(CircleShape)
          .background(color)
          .then(
            if (isSelected) Modifier.border(2.dp, Color.White, CircleShape)
            else Modifier
          )
          .clickable { onColorSelect(color) },
      )
    }
  }
}

// ═════════════════════════════════════════════════════════════════════════════
// Capture preview overlay (post-capture)
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Bottom panel shown after a photo or video is captured.
 *
 * Displays actions: **Send to Chat**, **Add to My Story**, and **Retake**.
 */
@Composable
private fun CapturePreviewOverlay(
  onSendToChat: () -> Unit,
  onAddToStory: () -> Unit,
  onRetake: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier.fillMaxWidth(),
    color = Color(0xDD000000),
    tonalElevation = 0.dp,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 24.dp, vertical = 20.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      // ── Action buttons ──
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        // Send to Chat
        Button(
          onClick = onSendToChat,
          modifier = Modifier.weight(1f).height(52.dp),
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = NovaPrimary,
          ),
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.Send,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Send to Chat",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
          )
        }

        // Add to Story
        Button(
          onClick = onAddToStory,
          modifier = Modifier.weight(1f).height(52.dp),
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = NovaSecondary.copy(alpha = 0.2f),
          ),
        ) {
          Icon(
            imageVector = Icons.Default.AutoAwesome,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = NovaSecondary,
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "My Story",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = NovaSecondary,
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // ── Retake ──
      TextButton(onClick = onRetake) {
        Icon(
          imageVector = Icons.Default.RotateLeft,
          contentDescription = null,
          modifier = Modifier.size(16.dp),
          tint = Color.White.copy(alpha = 0.7f),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = "Retake",
          style = MaterialTheme.typography.labelLarge,
          color = Color.White.copy(alpha = 0.7f),
        )
      }
    }
  }
}

// ═════════════════════════════════════════════════════════════════════════════
// Send-to bottom sheet
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Modal bottom sheet with "Send to Chat" and "Add to My Story" options
 * that appears after a snap is captured.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SendToSheet(
  onDismiss: () -> Unit,
  onSendToChat: () -> Unit,
  onAddToStory: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 24.dp)
        .padding(bottom = 32.dp),
    ) {
      // ── Header ──
      Text(
        text = "Send Snap",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 16.dp),
      )

      // ── Send to Chat ──
      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .clickable {
            onSendToChat()
            onDismiss()
          },
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
      ) {
        Row(
          modifier = Modifier.padding(16.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.Send,
            contentDescription = null,
            tint = NovaPrimary,
            modifier = Modifier.size(24.dp),
          )
          Spacer(modifier = Modifier.width(16.dp))
          Column {
            Text(
              text = "Send to Chat",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.SemiBold,
            )
            Text(
              text = "Share privately with a friend",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // ── Add to My Story ──
      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .clickable {
            onAddToStory()
            onDismiss()
          },
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
      ) {
        Row(
          modifier = Modifier.padding(16.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(
            imageVector = Icons.Default.AutoAwesome,
            contentDescription = null,
            tint = NovaSecondary,
            modifier = Modifier.size(24.dp),
          )
          Spacer(modifier = Modifier.width(16.dp))
          Column {
            Text(
              text = "Add to My Story",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.SemiBold,
            )
            Text(
              text = "Share with all your friends for 24h",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // ── Cancel ──
      TextButton(
        onClick = onDismiss,
        modifier = Modifier.align(Alignment.CenterHorizontally),
      ) {
        Text(
          text = "Cancel",
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

// ═════════════════════════════════════════════════════════════════════════════
// Utility extensions
// ═════════════════════════════════════════════════════════════════════════════

/** Cycle to the next flash mode. */
private fun FlashMode.next(): FlashMode = when (this) {
  FlashMode.OFF -> FlashMode.ON
  FlashMode.ON -> FlashMode.AUTO
  FlashMode.AUTO -> FlashMode.OFF
}

/** Cycle to the next timer option. */
private fun TimerOption.next(): TimerOption = when (this) {
  TimerOption.OFF -> TimerOption.THREE
  TimerOption.THREE -> TimerOption.TEN
  TimerOption.TEN -> TimerOption.OFF
}

/**
 * Formats a duration in milliseconds as "m:ss" (e.g. "1:23").
 * Matches the formatter used in [com.novamesh.ui.screens.ChatDetailScreen].
 */
private fun formatDuration(durationMs: Long): String {
  val totalSeconds = durationMs / 1000
  val minutes = totalSeconds / 60
  val seconds = totalSeconds % 60
  return "%d:%02d".format(minutes, seconds)
}
