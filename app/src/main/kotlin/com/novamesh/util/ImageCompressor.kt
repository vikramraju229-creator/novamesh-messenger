/**
 * ImageCompressor — reduces image file size before Firebase Storage upload.
 *
 * This saves Firebase Storage bandwidth (free tier: 20K uploads/day, 5GB stored).
 * By compressing to ~100-200KB per image, storage costs stay near zero.
 *
 * Usage:
 *   val compressedUri = ImageCompressor.compress(context, originalUri, maxWidth = 1024)
 *   // then upload compressedUri to Firebase Storage
 */
@file:Suppress("unused")

package com.novamesh.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Compresses images to fit within Firebase Storage free tier quotas.
 *
 * Default settings compress to ~150KB with good visual quality
 * (80% JPEG, 1024px max dimension).
 */
object ImageCompressor {

    /** Max JPEG quality (80% is visually lossless for photos). */
    private const val DEFAULT_QUALITY = 80

    /** Max image dimension (px). Images wider/taller are downscaled. */
    private const val DEFAULT_MAX_WIDTH = 1024

    /** Max image height proportionally scaled. */
    private const val DEFAULT_MAX_HEIGHT = 1024

    /**
     * Compress an image from a URI and return the compressed file URI.
     *
     * @param context  Application context.
     * @param sourceUri  The original image URI (content:// or file://).
     * @param maxWidth   Max image width (default 1024px).
     * @param maxHeight  Max image height (default 1024px).
     * @param quality    JPEG quality 1-100 (default 80).
     * @return Compressed file URI, or the original URI if compression fails.
     */
    suspend fun compress(
        context: Context,
        sourceUri: Uri,
        maxWidth: Int = DEFAULT_MAX_WIDTH,
        maxHeight: Int = DEFAULT_MAX_HEIGHT,
        quality: Int = DEFAULT_QUALITY,
    ): Uri = withContext(Dispatchers.IO) {
        try {
            // 1. Decode bounds without loading full image
            val bounds = context.contentResolver.openInputStream(sourceUri)?.use { input ->
                val opts = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeStream(input, null, opts)
                Pair(opts.outWidth, opts.outHeight)
            } ?: return@withContext sourceUri

            val (origWidth, origHeight) = bounds

            // 2. Calculate sample size to reduce memory
            val sampleSize = calculateSampleSize(origWidth, origHeight, maxWidth, maxHeight)

            // 3. Decode scaled bitmap
            val bitmap = context.contentResolver.openInputStream(sourceUri)?.use { input ->
                val opts = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                }
                BitmapFactory.decodeStream(input, null, opts)
            } ?: return@withContext sourceUri

            // 4. Correct orientation from EXIF
            val correctedBitmap = correctOrientation(context, sourceUri, bitmap)
            if (correctedBitmap !== bitmap) {
                bitmap.recycle()
            }

            // 5. Scale down if still too large
            val scaledBitmap = scaleDown(correctedBitmap, maxWidth, maxHeight)

            // 6. Compress to JPEG
            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            val compressedBytes = outputStream.toByteArray()

            // 7. Save to cache file
            val cacheFile = File(context.cacheDir, "compressed_images")
            if (!cacheFile.exists()) cacheFile.mkdirs()

            val outputFile = File(cacheFile, "IMG_${UUID.randomUUID()}.jpg")
            FileOutputStream(outputFile).use { fos ->
                fos.write(compressedBytes)
                fos.flush()
            }

            // Cleanup
            if (scaledBitmap !== correctedBitmap) {
                scaledBitmap.recycle()
            }
            if (correctedBitmap !== bitmap) {
                correctedBitmap.recycle()
            }
            bitmap.recycle()

            // Return URI
            Uri.fromFile(outputFile)
        } catch (e: Exception) {
            // Fallback: return original URI
            e.printStackTrace()
            sourceUri
        }
    }

    /**
     * Compress a Bitmap directly and return byte array.
     * Useful for in-memory compression before upload.
     */
    fun compressBitmap(
        bitmap: Bitmap,
        maxWidth: Int = DEFAULT_MAX_WIDTH,
        maxHeight: Int = DEFAULT_MAX_HEIGHT,
        quality: Int = DEFAULT_QUALITY,
    ): ByteArray {
        val scaled = scaleDown(bitmap, maxWidth, maxHeight)
        val outputStream = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        if (scaled !== bitmap) scaled.recycle()
        return outputStream.toByteArray()
    }

    /** Get estimated file size after compression (without actually compressing). */
    fun estimateCompressedSize(originalSizeBytes: Long): Long {
        // JPEG at 80% quality typically reduces size by 80-90%
        return (originalSizeBytes * 0.15).toLong().coerceAtLeast(50_000L) // at least 50KB
    }

    // ─── Private helpers ─────────────────────────────────────────────────

    private fun calculateSampleSize(
        origWidth: Int, origHeight: Int,
        maxWidth: Int, maxHeight: Int,
    ): Int {
        var sampleSize = 1
        while (origWidth / sampleSize > maxWidth * 1.5f ||
            origHeight / sampleSize > maxHeight * 1.5f
        ) {
            sampleSize *= 2
        }
        return sampleSize
    }

    private fun scaleDown(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxWidth && height <= maxHeight) return bitmap

        val ratio = minOf(maxWidth.toFloat() / width, maxHeight.toFloat() / height)
        val newWidth = (width * ratio).toInt()
        val newHeight = (height * ratio).toInt()

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    private fun correctOrientation(
        context: Context,
        sourceUri: Uri,
        bitmap: Bitmap,
    ): Bitmap {
        try {
            val inputStream = context.contentResolver.openInputStream(sourceUri) ?: return bitmap

            val exif = if (sourceUri.scheme == "content") {
                // For content URIs, try to read EXIF from the input stream directly
                val bytes = inputStream.readBytes()
                ExifInterface(ByteArrayInputStream(bytes))
            } else {
                ExifInterface(sourceUri.path ?: "")
            }
            inputStream.close()

            val rotation = when (exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL,
            )) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }

            if (rotation != 0f) {
                val matrix = Matrix().apply { postRotate(rotation) }
                val rotated = Bitmap.createBitmap(
                    bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true,
                )
                if (rotated !== bitmap) bitmap.recycle()
                return rotated
            }
        } catch (_: Exception) {
            // Ignore EXIF errors
        }
        return bitmap
    }
}

/** Minimal ByteArrayInputStream for EXIF reading. */
private class ByteArrayInputStream(private val bytes: ByteArray) : java.io.InputStream() {
    private var pos = 0
    override fun read(): Int {
        return if (pos < bytes.size) (bytes[pos++].toInt() and 0xFF) else -1
    }
    override fun read(b: ByteArray, off: Int, len: Int): Int {
        if (pos >= bytes.size) return -1
        val count = minOf(len, bytes.size - pos)
        System.arraycopy(bytes, pos, b, off, count)
        pos += count
        return count
    }
}
