/**
 * File and media utility functions for NovaMesh Messenger.
 *
 * Provides helpers for file size formatting, MIME type detection,
 * cache directory management, and file extension extraction.
 */
package com.novamesh.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File

object FileUtils {

    /**
     * Format a file size in bytes to a human-readable string.
     *
     * Examples: "512 B", "2.5 KB", "1.2 MB", "3.0 GB"
     */
    fun formatFileSize(bytes: Long): String = when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
        bytes < 1024L * 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
        else -> "%.1f GB".format(bytes / (1024.0 * 1024.0 * 1024.0))
    }

    /**
     * Format a count of items into a short human-readable string.
     *
     * Examples: 1234 → "1.2K", 1280000 → "1.3M"
     */
    fun formatCount(count: Int): String = when {
        count >= 1_000_000 -> "%.1fM".format(count / 1_000_000.0)
        count >= 1_000 -> "%.1fK".format(count / 1_000.0)
        else -> count.toString()
    }

    /**
     * Get the file name from a content URI.
     */
    fun getFileName(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0 && cursor.moveToFirst()) {
                    cursor.getString(nameIndex)
                } else null
            }
        } catch (_: Exception) {
            uri.lastPathSegment
        }
    }

    /**
     * Get the file extension from a URI or file path.
     */
    fun getExtension(uri: String): String {
        val dotIndex = uri.lastIndexOf('.')
        return if (dotIndex >= 0) uri.substring(dotIndex + 1).lowercase() else ""
    }

    /**
     * Infer MIME type from a file extension.
     */
    fun mimeTypeFromExtension(extension: String): String = when (extension.lowercase()) {
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        "gif" -> "image/gif"
        "webp" -> "image/webp"
        "mp4" -> "video/mp4"
        "3gp" -> "video/3gpp"
        "webm" -> "video/webm"
        "mp3" -> "audio/mpeg"
        "ogg" -> "audio/ogg"
        "wav" -> "audio/wav"
        "m4a" -> "audio/mp4"
        "pdf" -> "application/pdf"
        "doc", "docx" -> "application/msword"
        "txt" -> "text/plain"
        else -> "application/octet-stream"
    }

    /**
     * Get the app's cache directory size in bytes.
     */
    fun getCacheSize(context: Context): Long {
        val cacheDir = context.cacheDir
        return cacheDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    /**
     * Clear the app's cache directory.
     */
    fun clearCache(context: Context): Boolean {
        return try {
            val cacheDir = context.cacheDir
            cacheDir.deleteRecursively()
            cacheDir.mkdirs()
            true
        } catch (_: Exception) {
            false
        }
    }
}
