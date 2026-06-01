@file:OptIn(ExperimentalMaterial3Api::class)

package com.novamesh.ui.screens.camera

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.view.ViewGroup
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.video.VideoCapture
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraFront
import androidx.compose.material.icons.filled.CameraRear
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.RotateLeft
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/** Flash mode state machine. */
private enum class FlashMode { OFF, ON, AUTO }

/**
 * Full-screen camera screen using CameraX.
 *
 * @param onBack Navigate back.
 * @param onSnapTaken Called with captured media URI and whether it's video.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen(
    onBack: () -> Unit,
    onSnapTaken: (mediaUri: String, isVideo: Boolean) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    // Permissions
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)
    val audioPermission = rememberPermissionState(Manifest.permission.RECORD_AUDIO)

    // State
    var isFrontCamera by remember { mutableStateOf(false) }
    var flashMode by remember { mutableStateOf(FlashMode.OFF) }
    var isRecording by remember { mutableStateOf(false) }
    var recordingElapsedMs by remember { mutableLongStateOf(0L) }
    var capturedUri by remember { mutableStateOf<String?>(null) }
    var capturedIsVideo by remember { mutableStateOf(false) }
    var showPreview by remember { mutableStateOf(false) }

    // CameraX references
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var videoCapture by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var activeRecording by remember { mutableStateOf<Recording?>(null) }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    // Request permissions if not granted
    LaunchedEffect(Unit) {
        if (!cameraPermission.status.isGranted) {
            cameraPermission.launchPermissionRequest()
        }
        if (!audioPermission.status.isGranted) {
            audioPermission.launchPermissionRequest()
        }
    }

    // Recording timer
    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordingElapsedMs = 0L
            while (isRecording) {
                delay(1000L)
                recordingElapsedMs += 1000L
            }
        }
    }

    // Cleanup
    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    // UI
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        // Camera preview
        if (cameraPermission.status.isGranted) {
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).also { view ->
                        view.layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        )
                        view.scaleType = PreviewView.ScaleType.FILL_CENTER
                        view.implementationMode = PreviewView.ImplementationMode.COMPATIBLE

                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraProviderFuture.addListener({
                            val provider = cameraProviderFuture.get()
                            cameraProvider = provider

                            // Preview
                            val preview = Preview.Builder()
                                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                                .build()
                                .also { it.setSurfaceProvider(view.surfaceProvider) }

                            // Image capture
                            val imageCap = ImageCapture.Builder()
                                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                                .build()
                            imageCapture = imageCap

                            // Video capture
                            val recorder = Recorder.Builder()
                                .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                                .build()
                            val videoCap = VideoCapture.Builder(recorder)
                                .build()
                            videoCapture = videoCap

                            // Camera selector
                            val cameraSelector = if (isFrontCamera) {
                                CameraSelector.DEFAULT_FRONT_CAMERA
                            } else {
                                CameraSelector.DEFAULT_BACK_CAMERA
                            }

                            try {
                                provider.unbindAll()
                                provider.bindToLifecycle(
                                    lifecycleOwner,
                                    cameraSelector,
                                    preview,
                                    imageCap,
                                    videoCap,
                                )
                            } catch (e: Exception) {
                                // Camera in use
                            }
                        }, ContextCompat.getMainExecutor(ctx))
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            // Placeholder when no permission
            Box(
                modifier = Modifier.fillMaxSize().background(Color(0xFF1A1A1A)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Camera permission required",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }

        // Recording indicator
        if (isRecording) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 60.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0x99000000))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFF1744)),
                    )
                    Text(
                        text = formatDuration(recordingElapsedMs),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        // Top controls
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .padding(start = 4.dp, end = 16.dp, top = 48.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = {
                if (isRecording) {
                    activeRecording?.stop()
                    isRecording = false
                } else {
                    onBack()
                }
            }) {
                Icon(
                    imageVector = if (isRecording) Icons.Default.Stop else Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = if (isRecording) "Stop recording" else "Back",
                    tint = Color.White,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                // Flash toggle
                IconButton(
                    onClick = {
                        flashMode = when (flashMode) {
                            FlashMode.OFF -> FlashMode.ON
                            FlashMode.ON -> FlashMode.AUTO
                            FlashMode.AUTO -> FlashMode.OFF
                        }
                        imageCapture?.flashMode = when (flashMode) {
                            FlashMode.OFF -> ImageCapture.FLASH_MODE_OFF
                            FlashMode.ON -> ImageCapture.FLASH_MODE_ON
                            FlashMode.AUTO -> ImageCapture.FLASH_MODE_AUTO
                        }
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0x66000000)),
                ) {
                    Icon(
                        imageVector = when (flashMode) {
                            FlashMode.OFF -> Icons.Default.FlashOff
                            FlashMode.ON -> Icons.Default.FlashOn
                            FlashMode.AUTO -> Icons.Default.FlashAuto
                        },
                        contentDescription = "Flash: $flashMode",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }

        // Bottom controls
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // Gallery
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0x66000000))
                    .clickable { /* Open gallery */ },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoLibrary,
                    contentDescription = "Gallery",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(24.dp),
                )
            }

            // Capture button
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .border(4.dp, Color.White.copy(alpha = 0.8f), CircleShape)
                    .clickable {
                        if (isRecording) {
                            activeRecording?.stop()
                            isRecording = false
                        } else {
                            // Take photo
                            imageCapture?.let { capture ->
                                val photoFile = createFile(context, "jpg")
                                val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
                                capture.takePicture(
                                    outputOptions,
                                    ContextCompat.getMainExecutor(context),
                                    object : ImageCapture.OnImageSavedCallback {
                                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                            capturedUri = photoFile.absolutePath
                                            capturedIsVideo = false
                                            showPreview = true
                                        }

                                        override fun onError(exception: ImageCaptureException) {
                                            // Handle error
                                        }
                                    },
                                )
                            }
                        }
                    },
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(if (isRecording) Color(0xFFFF1744) else Color.White),
                )
            }

            // Flip camera
            IconButton(
                onClick = {
                    isFrontCamera = !isFrontCamera
                    // Rebind camera - the AndroidView will recreate
                    cameraProvider?.unbindAll()
                    cameraExecutor.execute {
                        // Camera will rebind on recomposition
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0x66000000)),
            ) {
                Icon(
                    imageVector = if (isFrontCamera) Icons.Default.CameraRear else Icons.Default.CameraFront,
                    contentDescription = "Flip camera",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp),
                )
            }
        }

        // Post-capture preview overlay
        AnimatedVisibility(
            visible = showPreview && capturedUri != null,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color(0xDD000000)),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Button(
                            onClick = {
                                capturedUri?.let { onSnapTaken(it, capturedIsVideo) }
                                showPreview = false
                            },
                            modifier = Modifier.weight(1f).height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Text("Send to Chat", fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = {
                                capturedUri?.let { onSnapTaken(it, capturedIsVideo) }
                                showPreview = false
                            },
                            modifier = Modifier.weight(1f).height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            ),
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("My Story", fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    TextButton(onClick = {
                        showPreview = false
                        capturedUri = null
                    }) {
                        Icon(Icons.Default.RotateLeft, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Retake", color = Color.White.copy(alpha = 0.7f))
                    }
                }
            }
        }
    }
}

/** Create a unique file for photo/video capture. */
private fun createFile(context: Context, extension: String): File {
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    val dir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: context.filesDir
    return File(dir, "NOVA_$timestamp.$extension")
}

/** Format milliseconds as mm:ss. */
private fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
