/**
 * MLKitFaceFilter — real-time AR face filter via ML Kit face detection.
 *
 * Detects facial landmarks and contours using Google ML Kit, then draws
 * AR overlay effects (dog ears, cat whiskers, crown, glasses, etc.)
 * using Android [Canvas] drawing primitives.
 *
 * Usage:
 * ```kotlin
 * val faceFilter = MLKitFaceFilter()
 * scope.launch {
 *     val faces = faceFilter.detectFace(bitmap)
 *     // ... render on Canvas using drawFilter()
 * }
 * faceFilter.release()
 * ```
 */

package com.novamesh.camera

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.RectF
import android.graphics.Typeface
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceContour
import com.google.mlkit.vision.face.FaceLandmark
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Integrates ML Kit face detection with Canvas-based AR filter rendering.
 *
 * Detects faces at high speed (FAST mode) with all landmarks, contours,
 * and classifications enabled. Each detected face is mapped to a
 * [FaceLandmarks] data class that holds the bounding box, Euler angles,
 * feature points, and contour points.
 */
class MLKitFaceFilter {

    // ──────────────────────────────────────────────────────────────────────────
    // ML Kit face detector
    // ──────────────────────────────────────────────────────────────────────────

    private val faceDetector: FaceDetector

    init {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setMinFaceSize(0.15f)
            .enableTracking()
            .build()

        faceDetector = FaceDetection.getClient(options)
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Data models
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Mapped facial landmarks extracted from an ML Kit [Face] object.
     *
     * All coordinates are in the original image's pixel space.
     */
    data class FaceLandmarks(
        /** Bounding box of the detected face. */
        val boundingBox: RectF,

        /** Head rotation about the vertical axis (yaw). */
        val headEulerAngleX: Float,
        /** Head rotation about the horizontal axis (pitch). */
        val headEulerAngleY: Float,
        /** Head rotation about the Z axis (roll). */
        val headEulerAngleZ: Float,

        /** Centre of the left eye (pixel coords), or null. */
        val leftEye: PointF?,
        /** Centre of the right eye (pixel coords), or null. */
        val rightEye: PointF?,
        /** Tip of the nose (pixel coords), or null. */
        val noseBase: PointF?,
        /** Left corner of the mouth (pixel coords), or null. */
        val mouthLeft: PointF?,
        /** Right corner of the mouth (pixel coords), or null. */
        val mouthRight: PointF?,
        /** Bottom centre of the mouth (pixel coords), or null. */
        val mouthBottom: PointF?,
        /** Left ear (pixel coords), or null. */
        val leftEar: PointF?,
        /** Right ear (pixel coords), or null. */
        val rightEar: PointF?,

        /** Map of [FaceContour] type → list of contour points. */
        val contours: Map<Int, List<PointF>> = emptyMap(),

        /** Width of the face bounding box. */
        val faceWidth: Float,
        /** Height of the face bounding box. */
        val faceHeight: Float,
    )

    // ──────────────────────────────────────────────────────────────────────────
    // AR filter types
    // ──────────────────────────────────────────────────────────────────────────

    /** Available AR face filter effects. */
    enum class FilterType {
        /** No filter. */
        NONE,
        /** Dog ears + dog nose. */
        DOG,
        /** Cat ears + whiskers. */
        CAT,
        /** Royal crown above head. */
        CROWN,
        /** Sunglasses over eyes. */
        GLASSES,
        /** Rainbow gradient above head. */
        RAINBOW,
        /** Fire effect around head. */
        FIRE,
        /** Floating hearts around face. */
        HEART,
        /** Floating stars around face. */
        STAR,
        /** Clown face overlay (red nose, makeup). */
        CLOWN,
        /** Pig nose + ears. */
        PIG,
        /** Robot face overlay (geometric). */
        ROBOT,
        /** Alien face overlay (large eyes, antenna). */
        ALIEN,
        /** Skeleton face overlay (skull). */
        SKELETON,
        /** Zombie face overlay (wounds, green tint). */
        ZOMBIE,
        /** Superhero mask overlay. */
        SUPERHERO,
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Public API
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Runs ML Kit face detection on the given [bitmap].
     *
     * @param bitmap The camera frame as an Android [Bitmap].
     * @return A list of [FaceLandmarks], one per detected face.
     */
    suspend fun detectFace(bitmap: Bitmap): List<FaceLandmarks> =
        withContext(Dispatchers.IO) {
            try {
                val inputImage = InputImage.fromBitmap(bitmap, 0)
                val faces = faceDetector.process(inputImage).await()
                faces.map { face -> mapToLandmarks(face) }
            } catch (e: Exception) {
                emptyList()
            }
        }

    /**
     * Runs ML Kit face detection on a byte array NV21 frame.
     *
     * @param data       NV21 encoded camera frame bytes.
     * @param width      Frame width in pixels.
     * @param height     Frame height in pixels.
     * @param rotation   Clockwise rotation of the frame (0, 90, 180, 270).
     * @return A list of [FaceLandmarks], one per detected face.
     */
    suspend fun detectFace(
        data: ByteArray,
        width: Int,
        height: Int,
        rotation: Int = 0,
    ): List<FaceLandmarks> = withContext(Dispatchers.IO) {
        try {
            val inputImage = InputImage.fromByteArray(
                data,
                width,
                height,
                rotation,
                InputImage.IMAGE_FORMAT_NV21,
            )
            val faces = faceDetector.process(inputImage).await()
            faces.map { face -> mapToLandmarks(face) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Draws an AR filter overlay on [canvas] for the given face.
     *
     * @param canvas     The Android [Canvas] to draw on.
     * @param landmarks  Detected [FaceLandmarks], or null if no face.
     * @param filterType The [FilterType] to render.
     */
    fun drawFilter(
        canvas: Canvas,
        landmarks: FaceLandmarks?,
        filterType: FilterType,
    ) {
        if (landmarks == null || filterType == FilterType.NONE) return

        canvas.save()

        // Apply head rotation so that overlays tilt with the user's head
        val cx = landmarks.boundingBox.centerX()
        val cy = landmarks.boundingBox.centerY()
        canvas.rotate(landmarks.headEulerAngleZ, cx, cy)

        // Scale factor based on face size (normalised to a 200px face)
        val faceSize = maxOf(landmarks.faceWidth, landmarks.faceHeight)
        val scale = faceSize / 200f

        // Base paint configuration
        val paint = Paint().apply {
            isAntiAlias = true
            isDither = true
        }

        when (filterType) {
            FilterType.DOG -> drawDogFilter(canvas, landmarks, scale, paint)
            FilterType.CAT -> drawCatFilter(canvas, landmarks, scale, paint)
            FilterType.CROWN -> drawCrownFilter(canvas, landmarks, scale, paint)
            FilterType.GLASSES -> drawGlassesFilter(canvas, landmarks, scale, paint)
            FilterType.RAINBOW -> drawRainbowFilter(canvas, landmarks, scale, paint)
            FilterType.FIRE -> drawFireFilter(canvas, landmarks, scale, paint)
            FilterType.HEART -> drawHeartFilter(canvas, landmarks, scale, paint)
            FilterType.STAR -> drawStarFilter(canvas, landmarks, scale, paint)
            FilterType.CLOWN -> drawClownFilter(canvas, landmarks, scale, paint)
            FilterType.PIG -> drawPigFilter(canvas, landmarks, scale, paint)
            FilterType.ROBOT -> drawRobotFilter(canvas, landmarks, scale, paint)
            FilterType.ALIEN -> drawAlienFilter(canvas, landmarks, scale, paint)
            FilterType.SKELETON -> drawSkeletonFilter(canvas, landmarks, scale, paint)
            FilterType.ZOMBIE -> drawZombieFilter(canvas, landmarks, scale, paint)
            FilterType.SUPERHERO -> drawSuperheroFilter(canvas, landmarks, scale, paint)
            FilterType.NONE -> { /* no-op */ }
        }

        canvas.restore()
    }

    /**
     * Returns the anchor position for a sticker-type filter, based on the
     * detected face.
     *
     * @param filter The [FilterType] to get the position for.
     * @return A [PointF] in image pixel coordinates, or (0,0) if no face
     *         is available (caller should handle this).
     */
    fun getFilterPosition(landmarks: FaceLandmarks?, filter: FilterType): PointF {
        if (landmarks == null) return PointF(0f, 0f)

        return when (filter) {
            FilterType.DOG, FilterType.CAT ->
                PointF(
                    landmarks.boundingBox.centerX(),
                    landmarks.boundingBox.top - landmarks.faceHeight * 0.3f,
                )
            FilterType.CROWN, FilterType.RAINBOW ->
                PointF(
                    landmarks.boundingBox.centerX(),
                    landmarks.boundingBox.top - landmarks.faceHeight * 0.5f,
                )
            FilterType.GLASSES -> {
                val left = landmarks.leftEye ?: return PointF(
                    landmarks.boundingBox.centerX(),
                    landmarks.boundingBox.centerY(),
                )
                val right = landmarks.rightEye ?: return left
                PointF(
                    (left.x + right.x) / 2f,
                    (left.y + right.y) / 2f,
                )
            }
            FilterType.FIRE, FilterType.HEART, FilterType.STAR ->
                PointF(
                    landmarks.boundingBox.centerX(),
                    landmarks.boundingBox.top - landmarks.faceHeight * 0.2f,
                )
            FilterType.CLOWN, FilterType.PIG ->
                landmarks.noseBase ?: PointF(
                    landmarks.boundingBox.centerX(),
                    landmarks.boundingBox.centerY(),
                )
            FilterType.ROBOT, FilterType.ALIEN ->
                PointF(
                    landmarks.boundingBox.centerX(),
                    landmarks.boundingBox.centerY(),
                )
            FilterType.SKELETON, FilterType.ZOMBIE, FilterType.SUPERHERO ->
                PointF(
                    landmarks.boundingBox.centerX(),
                    landmarks.boundingBox.centerY(),
                )
            FilterType.NONE -> PointF(
                landmarks.boundingBox.centerX(),
                landmarks.boundingBox.centerY(),
            )
        }
    }

    /**
     * Releases the underlying ML Kit face detector.
     *
     * Must be called when the filter is no longer needed to free native
     * resources.
     */
    fun release() {
        faceDetector.close()
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Face → FaceLandmarks mapping
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Maps an ML Kit [Face] object to our [FaceLandmarks] data class.
     */
    private fun mapToLandmarks(face: com.google.mlkit.vision.face.Face): FaceLandmarks {
        val box = face.boundingBox
        val contours = mutableMapOf<Int, List<PointF>>()

        // Extract all available contours
        FaceContour.ALL_CONTOURS.forEach { contourType ->
            face.getContour(contourType)?.let { contour ->
                contours[contourType] = contour.points.map { point ->
                    PointF(point.x, point.y)
                }
            }
        }

        return FaceLandmarks(
            boundingBox = RectF(box),
            headEulerAngleX = face.headEulerAngleX,
            headEulerAngleY = face.headEulerAngleY,
            headEulerAngleZ = face.headEulerAngleZ,
            leftEye = face.getLandmark(FaceLandmark.LEFT_EYE)?.position?.let {
                PointF(it.x, it.y)
            },
            rightEye = face.getLandmark(FaceLandmark.RIGHT_EYE)?.position?.let {
                PointF(it.x, it.y)
            },
            noseBase = face.getLandmark(FaceLandmark.NOSE_BASE)?.position?.let {
                PointF(it.x, it.y)
            },
            mouthLeft = face.getLandmark(FaceLandmark.MOUTH_LEFT)?.position?.let {
                PointF(it.x, it.y)
            },
            mouthRight = face.getLandmark(FaceLandmark.MOUTH_RIGHT)?.position?.let {
                PointF(it.x, it.y)
            },
            mouthBottom = face.getLandmark(FaceLandmark.MOUTH_BOTTOM)?.position?.let {
                PointF(it.x, it.y)
            },
            leftEar = face.getLandmark(FaceLandmark.LEFT_EAR)?.position?.let {
                PointF(it.x, it.y)
            },
            rightEar = face.getLandmark(FaceLandmark.RIGHT_EAR)?.position?.let {
                PointF(it.x, it.y)
            },
            contours = contours,
            faceWidth = box.width().toFloat(),
            faceHeight = box.height().toFloat(),
        )
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Filter drawing implementations
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Draws dog ears and a dog nose.
     *
     * - Ears: two large floppy triangles at the top of the head.
     * - Nose: a dark oval shape over the nose base.
     */
    private fun drawDogFilter(
        canvas: Canvas,
        lm: FaceLandmarks,
        scale: Float,
        paint: Paint,
    ) {
        val top = lm.boundingBox.top
        val centerX = lm.boundingBox.centerX()
        val earWidth = lm.faceWidth * 0.45f
        val earHeight = lm.faceHeight * 0.5f

        // Left ear
        val leftEarPath = Path().apply {
            moveTo(centerX - lm.faceWidth * 0.3f, top)
            lineTo(centerX - lm.faceWidth * 0.5f, top - earHeight)
            lineTo(centerX + lm.faceWidth * 0.1f, top - earHeight * 0.3f)
            close()
        }
        paint.color = Color.rgb(139, 90, 43) // Brown
        paint.style = Paint.Style.FILL
        canvas.drawPath(leftEarPath, paint)

        // Right ear
        val rightEarPath = Path().apply {
            moveTo(centerX + lm.faceWidth * 0.3f, top)
            lineTo(centerX + lm.faceWidth * 0.5f, top - earHeight)
            lineTo(centerX - lm.faceWidth * 0.1f, top - earHeight * 0.3f)
            close()
        }
        canvas.drawPath(rightEarPath, paint)

        // Dog nose
        val nose = lm.noseBase ?: return
        val noseSize = 20f * scale
        paint.color = Color.rgb(30, 20, 10)
        paint.style = Paint.Style.FILL
        canvas.drawOval(
            RectF(
                nose.x - noseSize,
                nose.y - noseSize * 0.6f,
                nose.x + noseSize,
                nose.y + noseSize * 0.6f,
            ),
            paint,
        )
    }

    /**
     * Draws cat ears and whiskers.
     *
     * - Ears: two pointed triangles on top of the head.
     * - Whiskers: three lines extending from each cheek.
     */
    private fun drawCatFilter(
        canvas: Canvas,
        lm: FaceLandmarks,
        scale: Float,
        paint: Paint,
    ) {
        val top = lm.boundingBox.top
        val centerX = lm.boundingBox.centerX()
        val earWidth = lm.faceWidth * 0.3f
        val earHeight = lm.faceHeight * 0.45f

        // Left ear
        paint.color = Color.rgb(255, 165, 0) // Orange
        paint.style = Paint.Style.FILL
        val leftEarPath = Path().apply {
            moveTo(centerX - lm.faceWidth * 0.25f, top)
            lineTo(centerX - lm.faceWidth * 0.35f, top - earHeight)
            lineTo(centerX - lm.faceWidth * 0.05f, top - earHeight * 0.2f)
            close()
        }
        canvas.drawPath(leftEarPath, paint)

        // Right ear
        val rightEarPath = Path().apply {
            moveTo(centerX + lm.faceWidth * 0.25f, top)
            lineTo(centerX + lm.faceWidth * 0.35f, top - earHeight)
            lineTo(centerX + lm.faceWidth * 0.05f, top - earHeight * 0.2f)
            close()
        }
        canvas.drawPath(rightEarPath, paint)

        // Inner ear (pink)
        val innerPath = Path().apply {
            moveTo(centerX - lm.faceWidth * 0.22f, top + 5f * scale)
            lineTo(centerX - lm.faceWidth * 0.28f, top - earHeight * 0.6f)
            lineTo(centerX - lm.faceWidth * 0.1f, top - earHeight * 0.1f)
            close()
        }
        paint.color = Color.rgb(255, 182, 193) // Light pink
        canvas.drawPath(innerPath, paint)

        val innerPath2 = Path().apply {
            moveTo(centerX + lm.faceWidth * 0.22f, top + 5f * scale)
            lineTo(centerX + lm.faceWidth * 0.28f, top - earHeight * 0.6f)
            lineTo(centerX + lm.faceWidth * 0.1f, top - earHeight * 0.1f)
            close()
        }
        canvas.drawPath(innerPath2, paint)

        // ── Whiskers ─────────────────────────────────────────────────────
        paint.color = Color.WHITE
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f * scale
        paint.strokeCap = Paint.Cap.ROUND

        val leftCheek = lm.mouthLeft ?: PointF(
            centerX - lm.faceWidth * 0.3f,
            lm.boundingBox.centerY(),
        )
        val rightCheek = lm.mouthRight ?: PointF(
            centerX + lm.faceWidth * 0.3f,
            lm.boundingBox.centerY(),
        )

        // Left whiskers
        for (i in -1..1) {
            val yOff = i * 10f * scale
            canvas.drawLine(
                leftCheek.x, leftCheek.y + yOff,
                leftCheek.x - 60f * scale, leftCheek.y + yOff + 10f * scale,
                paint,
            )
        }

        // Right whiskers
        for (i in -1..1) {
            val yOff = i * 10f * scale
            canvas.drawLine(
                rightCheek.x, rightCheek.y + yOff,
                rightCheek.x + 60f * scale, rightCheek.y + yOff + 10f * scale,
                paint,
            )
        }

        // Nose (small pink triangle)
        val nose = lm.noseBase ?: return
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(255, 105, 180) // Hot pink
        val noseSize = 12f * scale
        val nosePath = Path().apply {
            moveTo(nose.x, nose.y - noseSize)
            lineTo(nose.x - noseSize, nose.y + noseSize * 0.5f)
            lineTo(nose.x + noseSize, nose.y + noseSize * 0.5f)
            close()
        }
        canvas.drawPath(nosePath, paint)
    }

    /**
     * Draws a royal crown above the head.
     *
     * Features a golden crown with three points and jewels.
     */
    private fun drawCrownFilter(
        canvas: Canvas,
        lm: FaceLandmarks,
        scale: Float,
        paint: Paint,
    ) {
        val top = lm.boundingBox.top
        val centerX = lm.boundingBox.centerX()
        val crownWidth = lm.faceWidth * 1.1f
        val crownHeight = lm.faceHeight * 0.5f
        val crownTop = top - crownHeight

        // Crown body (gold)
        paint.color = Color.rgb(255, 215, 0) // Gold
        paint.style = Paint.Style.FILL

        val crownPath = Path().apply {
            // Base line
            moveTo(centerX - crownWidth / 2, top)
            // Left side
            lineTo(centerX - crownWidth / 2, crownTop + crownHeight * 0.4f)
            // Left spike
            lineTo(centerX - crownWidth * 0.3f, crownTop)
            lineTo(centerX - crownWidth * 0.15f, crownTop + crownHeight * 0.3f)
            // Centre spike (tallest)
            lineTo(centerX, crownTop - crownHeight * 0.2f)
            lineTo(centerX + crownWidth * 0.15f, crownTop + crownHeight * 0.3f)
            // Right spike
            lineTo(centerX + crownWidth * 0.3f, crownTop)
            lineTo(centerX + crownWidth / 2, crownTop + crownHeight * 0.4f)
            // Right side
            lineTo(centerX + crownWidth / 2, top)
            close()
        }
        canvas.drawPath(crownPath, paint)

        // Crown outline
        paint.color = Color.rgb(218, 165, 32) // Darker gold
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f * scale
        canvas.drawPath(crownPath, paint)

        // Jewels (coloured circles on the crown)
        paint.style = Paint.Style.FILL
        val jewelPositions = listOf(
            Pair(centerX - crownWidth * 0.25f, crownTop + crownHeight * 0.15f),
            Pair(centerX, crownTop + crownHeight * 0.1f),
            Pair(centerX + crownWidth * 0.25f, crownTop + crownHeight * 0.15f),
        )
        val jewelColors = listOf(
            Color.RED,
            Color.BLUE,
            Color.GREEN,
        )
        jewelPositions.forEachIndexed { index, (jx, jy) ->
            paint.color = jewelColors[index % jewelColors.size]
            canvas.drawCircle(jx, jy, 6f * scale, paint)
        }
    }

    /**
     * Draws sunglasses over the eyes.
     *
     * Features a black/navy frame bridging across both eyes.
     */
    private fun drawGlassesFilter(
        canvas: Canvas,
        lm: FaceLandmarks,
        scale: Float,
        paint: Paint,
    ) {
        val leftEye = lm.leftEye ?: return
        val rightEye = lm.rightEye ?: return

        val eyeWidth = lm.faceWidth * 0.25f
        val eyeHeight = lm.faceHeight * 0.18f
        val bridgeWidth = (rightEye.x - leftEye.x) * 0.3f

        // Frame colour
        paint.color = Color.rgb(20, 20, 40) // Dark navy
        paint.style = Paint.Style.FILL

        // Left lens
        canvas.drawRoundRect(
            leftEye.x - eyeWidth,
            leftEye.y - eyeHeight * 0.6f,
            leftEye.x + eyeWidth,
            leftEye.y + eyeHeight * 0.6f,
            12f * scale,
            12f * scale,
            paint,
        )

        // Right lens
        canvas.drawRoundRect(
            rightEye.x - eyeWidth,
            rightEye.y - eyeHeight * 0.6f,
            rightEye.x + eyeWidth,
            rightEye.y + eyeHeight * 0.6f,
            12f * scale,
            12f * scale,
            paint,
        )

        // Bridge
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 4f * scale
        canvas.drawLine(
            leftEye.x + eyeWidth,
            leftEye.y,
            rightEye.x - eyeWidth,
            rightEye.y,
            paint,
        )

        // Lens reflection (subtle highlight)
        paint.color = Color.argb(40, 255, 255, 255)
        paint.style = Paint.Style.FILL
        val highlightSize = eyeWidth * 0.3f
        canvas.drawOval(
            RectF(
                leftEye.x - highlightSize,
                leftEye.y - eyeHeight * 0.2f,
                leftEye.x - highlightSize * 0.3f,
                leftEye.y + eyeHeight * 0.1f,
            ),
            paint,
        )
        canvas.drawOval(
            RectF(
                rightEye.x - highlightSize,
                rightEye.y - eyeHeight * 0.2f,
                rightEye.x - highlightSize * 0.3f,
                rightEye.y + eyeHeight * 0.1f,
            ),
            paint,
        )
    }

    /**
     * Draws a rainbow gradient arc above the head.
     */
    private fun drawRainbowFilter(
        canvas: Canvas,
        lm: FaceLandmarks,
        scale: Float,
        paint: Paint,
    ) {
        val centerX = lm.boundingBox.centerX()
        val top = lm.boundingBox.top
        val arcHeight = lm.faceHeight * 0.6f
        val arcWidth = lm.faceWidth * 1.2f

        val rainbowColors = listOf(
            Color.RED,
            Color.rgb(255, 165, 0), // Orange
            Color.YELLOW,
            Color.GREEN,
            Color.BLUE,
            Color.rgb(75, 0, 130), // Indigo
            Color.rgb(148, 0, 211), // Violet
        )

        val bandHeight = arcHeight / rainbowColors.size
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = bandHeight * 0.9f
        paint.strokeCap = Paint.Cap.ROUND

        rainbowColors.forEachIndexed { index, color ->
            paint.color = color
            val radius = arcHeight - index * bandHeight
            canvas.drawArc(
                RectF(
                    centerX - arcWidth / 2,
                    top - radius,
                    centerX + arcWidth / 2,
                    top + radius * 0.3f,
                ),
                180f,   // Start angle (left)
                180f,   // Sweep angle (to right)
                false,
                paint,
            )
        }
    }

    /**
     * Draws a stylised fire effect around the top of the head.
     *
     * Draws flame-like shapes using orange, red, and yellow paths.
     */
    private fun drawFireFilter(
        canvas: Canvas,
        lm: FaceLandmarks,
        scale: Float,
        paint: Paint,
    ) {
        val centerX = lm.boundingBox.centerX()
        val top = lm.boundingBox.top
        val flameWidth = lm.faceWidth * 1.2f
        val flameHeight = lm.faceHeight * 0.6f

        paint.style = Paint.Style.FILL

        // Multiple flame layers (outer = dark red, mid = orange, inner = yellow)
        val flameLayers = listOf(
            Triple(Color.rgb(180, 0, 0), flameHeight, flameWidth),       // Outer
            Triple(Color.rgb(255, 69, 0), flameHeight * 0.75f, flameWidth * 0.8f), // Mid
            Triple(Color.rgb(255, 140, 0), flameHeight * 0.5f, flameWidth * 0.6f),  // Inner
            Triple(Color.YELLOW, flameHeight * 0.3f, flameWidth * 0.4f),           // Core
        )

        flameLayers.forEach { (color, height, width) ->
            paint.color = color
            val flamePath = Path().apply {
                moveTo(centerX - width / 2, top)
                // Left flame curve
                cubicTo(
                    centerX - width / 2, top - height * 0.5f,
                    centerX - width * 0.2f, top - height,
                    centerX, top - height,
                )
                // Right flame curve
                cubicTo(
                    centerX + width * 0.2f, top - height,
                    centerX + width / 2, top - height * 0.5f,
                    centerX + width / 2, top,
                )
                close()
            }
            canvas.drawPath(flamePath, paint)
        }
    }

    /**
     * Draws floating hearts around the face.
     *
     * Hearts are placed at randomised positions around the bounding box.
     */
    private fun drawHeartFilter(
        canvas: Canvas,
        lm: FaceLandmarks,
        scale: Float,
        paint: Paint,
    ) {
        paint.color = Color.rgb(255, 23, 68) // Red-pink
        paint.style = Paint.Style.FILL

        // Define heart positions around the face
        val heartPositions = listOf(
            PointF(lm.boundingBox.left - 20f * scale, lm.boundingBox.top - 10f * scale),
            PointF(lm.boundingBox.right + 20f * scale, lm.boundingBox.top + 20f * scale),
            PointF(lm.boundingBox.left - 10f * scale, lm.boundingBox.bottom - 20f * scale),
            PointF(lm.boundingBox.right + 15f * scale, lm.boundingBox.bottom - 30f * scale),
            PointF(lm.boundingBox.centerX(), lm.boundingBox.top - 40f * scale),
        )

        heartPositions.forEach { pos ->
            drawHeart(canvas, pos.x, pos.y, 15f * scale, paint)
        }
    }

    /**
     * Draws floating stars around the face.
     *
     * Five-pointed stars at positions around the bounding box.
     */
    private fun drawStarFilter(
        canvas: Canvas,
        lm: FaceLandmarks,
        scale: Float,
        paint: Paint,
    ) {
        paint.color = Color.rgb(255, 215, 0) // Gold
        paint.style = Paint.Style.FILL

        val starPositions = listOf(
            PointF(lm.boundingBox.left - 15f * scale, lm.boundingBox.top - 5f * scale),
            PointF(lm.boundingBox.right + 15f * scale, lm.boundingBox.top + 30f * scale),
            PointF(lm.boundingBox.left, lm.boundingBox.bottom + 10f * scale),
            PointF(lm.boundingBox.right + 5f * scale, lm.boundingBox.bottom),
            PointF(lm.boundingBox.centerX() - lm.faceWidth * 0.4f, lm.boundingBox.top - 30f * scale),
            PointF(lm.boundingBox.centerX() + lm.faceWidth * 0.4f, lm.boundingBox.top - 20f * scale),
        )

        starPositions.forEach { pos ->
            drawStar(canvas, pos.x, pos.y, 12f * scale, paint)
        }
    }

    /**
     * Draws a clown face overlay (red nose, smile, face paint).
     */
    private fun drawClownFilter(
        canvas: Canvas,
        lm: FaceLandmarks,
        scale: Float,
        paint: Paint,
    ) {
        // ── Red nose ─────────────────────────────────────────────────────
        val nose = lm.noseBase ?: return
        paint.color = Color.RED
        paint.style = Paint.Style.FILL
        canvas.drawCircle(nose.x, nose.y, 22f * scale, paint)

        // Nose highlight
        paint.color = Color.argb(80, 255, 255, 255)
        canvas.drawCircle(
            nose.x - 6f * scale,
            nose.y - 6f * scale,
            6f * scale,
            paint,
        )

        // ── Large smile ──────────────────────────────────────────────────
        paint.color = Color.RED
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 4f * scale
        paint.strokeCap = Paint.Cap.ROUND

        val mouthCenter = lm.mouthBottom ?: PointF(
            lm.boundingBox.centerX(),
            lm.boundingBox.centerY() + lm.faceHeight * 0.2f,
        )
        val smileWidth = lm.faceWidth * 0.6f
        val smileHeight = lm.faceHeight * 0.15f

        val smilePath = Path().apply {
            moveTo(mouthCenter.x - smileWidth / 2, mouthCenter.y)
            quadTo(mouthCenter.x, mouthCenter.y + smileHeight, mouthCenter.x + smileWidth / 2, mouthCenter.y)
        }
        canvas.drawPath(smilePath, paint)

        // ── Cheek circles (red blush) ────────────────────────────────────
        paint.color = Color.argb(100, 255, 100, 100)
        paint.style = Paint.Style.FILL
        canvas.drawCircle(
            lm.boundingBox.left + lm.faceWidth * 0.15f,
            lm.boundingBox.centerY(),
            18f * scale,
            paint,
        )
        canvas.drawCircle(
            lm.boundingBox.right - lm.faceWidth * 0.15f,
            lm.boundingBox.centerY(),
            18f * scale,
            paint,
        )

        // ── Eyebrows (exaggerated) ──────────────────────────────────────
        paint.color = Color.BLACK
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 5f * scale
        paint.strokeCap = Paint.Cap.ROUND

        val leftEye = lm.leftEye ?: return
        val rightEye = lm.rightEye ?: return

        canvas.drawLine(
            leftEye.x - 20f * scale,
            leftEye.y - 30f * scale,
            leftEye.x + 20f * scale,
            leftEye.y - 25f * scale,
            paint,
        )
        canvas.drawLine(
            rightEye.x - 20f * scale,
            rightEye.y - 25f * scale,
            rightEye.x + 20f * scale,
            rightEye.y - 30f * scale,
            paint,
        )
    }

    /**
     * Draws a pig nose and ears.
     */
    private fun drawPigFilter(
        canvas: Canvas,
        lm: FaceLandmarks,
        scale: Float,
        paint: Paint,
    ) {
        // ── Pig nose ─────────────────────────────────────────────────────
        val nose = lm.noseBase ?: return
        paint.color = Color.rgb(255, 192, 203) // Pink
        paint.style = Paint.Style.FILL
        canvas.drawOval(
            RectF(
                nose.x - 25f * scale,
                nose.y - 15f * scale,
                nose.x + 25f * scale,
                nose.y + 15f * scale,
            ),
            paint,
        )

        // Nostrils
        paint.color = Color.rgb(200, 100, 120)
        paint.style = Paint.Style.FILL
        canvas.drawOval(
            RectF(
                nose.x - 12f * scale,
                nose.y - 6f * scale,
                nose.x - 3f * scale,
                nose.y + 6f * scale,
            ),
            paint,
        )
        canvas.drawOval(
            RectF(
                nose.x + 3f * scale,
                nose.y - 6f * scale,
                nose.x + 12f * scale,
                nose.y + 6f * scale,
            ),
            paint,
        )

        // ── Pig ears ─────────────────────────────────────────────────────
        val top = lm.boundingBox.top
        val centerX = lm.boundingBox.centerX()

        paint.color = Color.rgb(255, 182, 193) // Light pink
        val earPath = Path().apply {
            moveTo(centerX - lm.faceWidth * 0.3f, top)
            quadTo(
                centerX - lm.faceWidth * 0.45f,
                top - lm.faceHeight * 0.3f,
                centerX - lm.faceWidth * 0.15f,
                top - lm.faceHeight * 0.15f,
            )
            close()
        }
        canvas.drawPath(earPath, paint)

        val earPath2 = Path().apply {
            moveTo(centerX + lm.faceWidth * 0.3f, top)
            quadTo(
                centerX + lm.faceWidth * 0.45f,
                top - lm.faceHeight * 0.3f,
                centerX + lm.faceWidth * 0.15f,
                top - lm.faceHeight * 0.15f,
            )
            close()
        }
        canvas.drawPath(earPath2, paint)
    }

    /**
     * Draws a geometric robot face overlay.
     */
    private fun drawRobotFilter(
        canvas: Canvas,
        lm: FaceLandmarks,
        scale: Float,
        paint: Paint,
    ) {
        val box = lm.boundingBox
        val centerX = box.centerX()
        val centerY = box.centerY()

        // ── Antenna ──────────────────────────────────────────────────────
        paint.color = Color.GRAY
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 4f * scale
        canvas.drawLine(centerX, box.top, centerX, box.top - lm.faceHeight * 0.3f, paint)

        // Antenna ball
        paint.style = Paint.Style.FILL
        paint.color = Color.RED
        canvas.drawCircle(centerX, box.top - lm.faceHeight * 0.35f, 8f * scale, paint)

        // ── Face panel (metallic rectangle) ──────────────────────────────
        paint.color = Color.rgb(180, 190, 200)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f * scale
        canvas.drawRoundRect(
            box.left + 10f * scale,
            box.top + 10f * scale,
            box.right - 10f * scale,
            box.bottom - 10f * scale,
            16f * scale,
            16f * scale,
            paint,
        )

        // ── Eyes (LED-style circles) ─────────────────────────────────────
        val leftEye = lm.leftEye ?: return
        val rightEye = lm.rightEye ?: return

        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(0, 255, 0) // Neon green
        canvas.drawCircle(leftEye.x, leftEye.y, 14f * scale, paint)
        canvas.drawCircle(rightEye.x, rightEye.y, 14f * scale, paint)

        // Eye glow
        paint.color = Color.argb(60, 0, 255, 0)
        canvas.drawCircle(leftEye.x, leftEye.y, 22f * scale, paint)
        canvas.drawCircle(rightEye.x, rightEye.y, 22f * scale, paint)

        // ── Mouth (LED grid) ─────────────────────────────────────────────
        val mouthY = centerY + lm.faceHeight * 0.2f
        paint.color = Color.rgb(0, 200, 255) // Cyan
        paint.style = Paint.Style.FILL
        val mouthWidth = lm.faceWidth * 0.4f
        val mouthHeight = lm.faceHeight * 0.08f
        canvas.drawRoundRect(
            centerX - mouthWidth / 2,
            mouthY - mouthHeight / 2,
            centerX + mouthWidth / 2,
            mouthY + mouthHeight / 2,
            4f * scale,
            4f * scale,
            paint,
        )

        // Grid lines in mouth
        paint.color = Color.rgb(100, 100, 110)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f * scale
        for (i in -1..1) {
            canvas.drawLine(
                centerX + i * mouthWidth * 0.25f,
                mouthY - mouthHeight / 2,
                centerX + i * mouthWidth * 0.25f,
                mouthY + mouthHeight / 2,
                paint,
            )
        }
    }

    /**
     * Draws an alien face overlay (large eyes, antenna).
     */
    private fun drawAlienFilter(
        canvas: Canvas,
        lm: FaceLandmarks,
        scale: Float,
        paint: Paint,
    ) {
        val box = lm.boundingBox
        val centerX = box.centerX()
        val top = box.top

        // ── Antennae ─────────────────────────────────────────────────────
        paint.color = Color.rgb(100, 200, 100)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f * scale
        canvas.drawLine(
            centerX - lm.faceWidth * 0.15f, top,
            centerX - lm.faceWidth * 0.2f, top - lm.faceHeight * 0.4f,
            paint,
        )
        canvas.drawLine(
            centerX + lm.faceWidth * 0.15f, top,
            centerX + lm.faceWidth * 0.2f, top - lm.faceHeight * 0.4f,
            paint,
        )

        // Antenna tips (glowing balls)
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(0, 255, 100)
        canvas.drawCircle(
            centerX - lm.faceWidth * 0.2f,
            top - lm.faceHeight * 0.4f,
            6f * scale,
            paint,
        )
        canvas.drawCircle(
            centerX + lm.faceWidth * 0.2f,
            top - lm.faceHeight * 0.4f,
            6f * scale,
            paint,
        )

        // ── Large alien eyes ─────────────────────────────────────────────
        val leftEye = lm.leftEye ?: return
        val rightEye = lm.rightEye ?: return

        // Eye sockets (dark)
        paint.color = Color.rgb(30, 60, 30)
        paint.style = Paint.Style.FILL
        canvas.drawOval(
            RectF(
                leftEye.x - 28f * scale,
                leftEye.y - 22f * scale,
                leftEye.x + 28f * scale,
                leftEye.y + 22f * scale,
            ),
            paint,
        )
        canvas.drawOval(
            RectF(
                rightEye.x - 28f * scale,
                rightEye.y - 22f * scale,
                rightEye.x + 28f * scale,
                rightEye.y + 22f * scale,
            ),
            paint,
        )

        // Pupils (black)
        paint.color = Color.BLACK
        canvas.drawCircle(leftEye.x, leftEye.y, 12f * scale, paint)
        canvas.drawCircle(rightEye.x, rightEye.y, 12f * scale, paint)

        // Eye glow (green)
        paint.color = Color.argb(80, 0, 255, 0)
        canvas.drawCircle(leftEye.x, leftEye.y, 18f * scale, paint)
        canvas.drawCircle(rightEye.x, rightEye.y, 18f * scale, paint)

        // ── Green skin tint (translucent overlay) ────────────────────────
        paint.color = Color.argb(40, 100, 220, 100)
        paint.style = Paint.Style.FILL
        canvas.drawOval(box, paint)
    }

    /**
     * Draws a skeleton / skull overlay.
     */
    private fun drawSkeletonFilter(
        canvas: Canvas,
        lm: FaceLandmarks,
        scale: Float,
        paint: Paint,
    ) {
        val box = lm.boundingBox

        // ── Skull outline (white bone shape) ─────────────────────────────
        paint.color = Color.rgb(230, 230, 230)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 4f * scale
        canvas.drawOval(box, paint)

        // ── Eye sockets (dark hollows) ───────────────────────────────────
        val leftEye = lm.leftEye ?: return
        val rightEye = lm.rightEye ?: return

        paint.color = Color.rgb(20, 20, 20)
        paint.style = Paint.Style.FILL
        canvas.drawOval(
            RectF(
                leftEye.x - 20f * scale,
                leftEye.y - 18f * scale,
                leftEye.x + 20f * scale,
                leftEye.y + 18f * scale,
            ),
            paint,
        )
        canvas.drawOval(
            RectF(
                rightEye.x - 20f * scale,
                rightEye.y - 18f * scale,
                rightEye.x + 20f * scale,
                rightEye.y + 18f * scale,
            ),
            paint,
        )

        // ── Nose cavity (inverted heart / triangle) ──────────────────────
        val nose = lm.noseBase ?: return
        val nosePath = Path().apply {
            moveTo(nose.x, nose.y - 10f * scale)
            lineTo(nose.x - 12f * scale, nose.y + 12f * scale)
            lineTo(nose.x + 12f * scale, nose.y + 12f * scale)
            close()
        }
        paint.color = Color.rgb(20, 20, 20)
        paint.style = Paint.Style.FILL
        canvas.drawPath(nosePath, paint)

        // ── Teeth (horizontal lines across mouth area) ───────────────────
        val mouthY = lm.mouthBottom?.y ?: (box.centerY() + box.height() * 0.2f)
        paint.color = Color.rgb(200, 200, 200)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f * scale

        val teethWidth = box.width() * 0.5f
        // Jaw line
        canvas.drawLine(
            box.centerX() - teethWidth / 2,
            mouthY,
            box.centerX() + teethWidth / 2,
            mouthY,
            paint,
        )

        // Vertical teeth lines
        paint.strokeWidth = 2f * scale
        for (i in -2..2) {
            val x = box.centerX() + i * teethWidth * 0.12f
            canvas.drawLine(x, mouthY, x, mouthY + 15f * scale, paint)
        }
    }

    /**
     * Draws a zombie face overlay (green tint, wounds).
     */
    private fun drawZombieFilter(
        canvas: Canvas,
        lm: FaceLandmarks,
        scale: Float,
        paint: Paint,
    ) {
        val box = lm.boundingBox

        // ── Green skin tint ──────────────────────────────────────────────
        paint.color = Color.argb(60, 100, 180, 100)
        paint.style = Paint.Style.FILL
        canvas.drawOval(box, paint)

        // ── Scar / wound on forehead ─────────────────────────────────────
        paint.color = Color.rgb(120, 30, 30) // Dark red
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 4f * scale
        paint.strokeCap = Paint.Cap.ROUND

        val foreheadY = box.top + box.height() * 0.15f
        val scarPath = Path().apply {
            moveTo(box.centerX() - 20f * scale, foreheadY)
            lineTo(box.centerX() - 5f * scale, foreheadY + 12f * scale)
            lineTo(box.centerX() + 15f * scale, foreheadY + 5f * scale)
        }
        canvas.drawPath(scarPath, paint)

        // Stitches across scar
        paint.color = Color.BLACK
        paint.strokeWidth = 2f * scale
        for (i in -1..1) {
            val sx = box.centerX() + i * 8f * scale
            canvas.drawLine(
                sx, foreheadY + 5f * scale,
                sx + 4f * scale, foreheadY + 2f * scale,
                paint,
            )
        }

        // ── Dark eye bags ────────────────────────────────────────────────
        val leftEye = lm.leftEye ?: return
        val rightEye = lm.rightEye ?: return

        paint.color = Color.argb(100, 40, 20, 20)
        paint.style = Paint.Style.FILL
        canvas.drawOval(
            RectF(
                leftEye.x - 22f * scale,
                leftEye.y + 8f * scale,
                leftEye.x + 22f * scale,
                leftEye.y + 24f * scale,
            ),
            paint,
        )
        canvas.drawOval(
            RectF(
                rightEye.x - 22f * scale,
                rightEye.y + 8f * scale,
                rightEye.x + 22f * scale,
                rightEye.y + 24f * scale,
            ),
            paint,
        )

        // ── Blood drip from mouth corner ────────────────────────────────
        val mouthLeft = lm.mouthLeft ?: return
        paint.color = Color.rgb(180, 20, 20)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f * scale
        canvas.drawLine(
            mouthLeft.x,
            mouthLeft.y,
            mouthLeft.x - 10f * scale,
            mouthLeft.y + 25f * scale,
            paint,
        )

        // Blood drop
        paint.style = Paint.Style.FILL
        canvas.drawCircle(
            mouthLeft.x - 10f * scale,
            mouthLeft.y + 30f * scale,
            4f * scale,
            paint,
        )
    }

    /**
     * Draws a superhero mask overlay.
     */
    private fun drawSuperheroFilter(
        canvas: Canvas,
        lm: FaceLandmarks,
        scale: Float,
        paint: Paint,
    ) {
        val box = lm.boundingBox
        val centerX = box.centerX()

        // ── Mask shape (domino mask covering eyes and bridge) ────────────
        paint.color = Color.rgb(30, 30, 80) // Dark navy
        paint.style = Paint.Style.FILL

        val maskWidth = lm.faceWidth * 1.1f
        val maskHeight = lm.faceHeight * 0.35f
        val maskTop = box.top + lm.faceHeight * 0.2f

        val maskPath = Path().apply {
            moveTo(centerX - maskWidth / 2, maskTop)
            // Upper curve
            quadTo(
                centerX,
                maskTop - maskHeight * 0.3f,
                centerX + maskWidth / 2,
                maskTop,
            )
            // Right side down
            quadTo(
                centerX + maskWidth / 2 + 10f * scale,
                maskTop + maskHeight * 0.3f,
                centerX + maskWidth * 0.35f,
                maskTop + maskHeight,
            )
            // Bottom curve (eye holes)
            quadTo(
                centerX,
                maskTop + maskHeight * 0.6f,
                centerX - maskWidth * 0.35f,
                maskTop + maskHeight,
            )
            // Left side up
            quadTo(
                centerX - maskWidth / 2 - 10f * scale,
                maskTop + maskHeight * 0.3f,
                centerX - maskWidth / 2,
                maskTop,
            )
            close()
        }
        canvas.drawPath(maskPath, paint)

        // ── Eye holes (skin-coloured cutouts) ────────────────────────────
        val leftEye = lm.leftEye ?: return
        val rightEye = lm.rightEye ?: return

        paint.color = Color.argb(180, 255, 220, 180) // Skin tone
        paint.style = Paint.Style.FILL
        canvas.drawOval(
            RectF(
                leftEye.x - 18f * scale,
                leftEye.y - 10f * scale,
                leftEye.x + 18f * scale,
                leftEye.y + 10f * scale,
            ),
            paint,
        )
        canvas.drawOval(
            RectF(
                rightEye.x - 18f * scale,
                rightEye.y - 10f * scale,
                rightEye.x + 18f * scale,
                rightEye.y + 10f * scale,
            ),
            paint,
        )

        // ── Eye hole outlines ────────────────────────────────────────────
        paint.color = Color.rgb(20, 20, 60)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f * scale
        canvas.drawOval(
            RectF(
                leftEye.x - 18f * scale,
                leftEye.y - 10f * scale,
                leftEye.x + 18f * scale,
                leftEye.y + 10f * scale,
            ),
            paint,
        )
        canvas.drawOval(
            RectF(
                rightEye.x - 18f * scale,
                rightEye.y - 10f * scale,
                rightEye.x + 18f * scale,
                rightEye.y + 10f * scale,
            ),
            paint,
        )

        // ── Cape attachment (small emblem on forehead) ───────────────────
        paint.color = Color.rgb(255, 215, 0) // Gold
        paint.style = Paint.Style.FILL
        val emblemSize = 10f * scale
        canvas.drawCircle(centerX, maskTop - maskHeight * 0.2f, emblemSize, paint)
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Utility drawing helpers
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Draws a heart shape centred at (cx, cy).
     */
    private fun drawHeart(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        size: Float,
        paint: Paint,
    ) {
        val path = Path().apply {
            moveTo(cx, cy + size * 0.3f)
            // Left lobe
            cubicTo(
                cx - size, cy - size * 0.5f,
                cx - size * 0.5f, cy - size,
                cx, cy - size * 0.3f,
            )
            // Right lobe
            cubicTo(
                cx + size * 0.5f, cy - size,
                cx + size, cy - size * 0.5f,
                cx, cy + size * 0.3f,
            )
            close()
        }
        canvas.drawPath(path, paint)
    }

    /**
     * Draws a five-pointed star centred at (cx, cy).
     */
    private fun drawStar(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        size: Float,
        paint: Paint,
    ) {
        val points = mutableListOf<Pair<Float, Float>>()
        for (i in 0 until 10) {
            val angle = Math.toRadians((i * 36.0) - 90.0)
            val r = if (i % 2 == 0) size else size * 0.4f
            points.add(
                Pair(
                    cx + r * cos(angle).toFloat(),
                    cy + r * sin(angle).toFloat(),
                ),
            )
        }

        val path = Path().apply {
            points.forEachIndexed { index, (x, y) ->
                if (index == 0) moveTo(x, y) else lineTo(x, y)
            }
            close()
        }
        canvas.drawPath(path, paint)
    }
}
