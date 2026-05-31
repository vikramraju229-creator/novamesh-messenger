/**
 * Date and time formatting utilities for NovaMesh Messenger.
 *
 * Provides consistent relative/absolute time formatting used across
 * all screens (chat list, message bubbles, story timestamps, etc.).
 */
package com.novamesh.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateUtils {

    private const val MINUTE_MS = 60_000L
    private const val HOUR_MS = 60 * MINUTE_MS
    private const val DAY_MS = 24 * HOUR_MS

    /**
     * Format a timestamp as a relative time string for chat list items.
     *
     * Examples: "Just now", "2m ago", "1h ago", "Yesterday", "Wed", "Jan 5"
     */
    fun formatRelativeTime(timestampMillis: Long): String {
        if (timestampMillis <= 0L) return ""
        val now = System.currentTimeMillis()
        val diff = now - timestampMillis
        return when {
            diff < MINUTE_MS -> "Just now"
            diff < 2 * MINUTE_MS -> "1m ago"
            diff < HOUR_MS -> "${diff / MINUTE_MS}m ago"
            diff < 2 * HOUR_MS -> "1h ago"
            diff < DAY_MS -> "${diff / HOUR_MS}h ago"
            diff < 2 * DAY_MS -> "Yesterday"
            diff < 7 * DAY_MS -> "${diff / DAY_MS}d ago"
            else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestampMillis))
        }
    }

    /**
     * Format a timestamp as a short time string (e.g. "10:30 AM").
     */
    fun formatMessageTime(timestampMillis: Long): String {
        return SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(timestampMillis))
    }

    /**
     * Format a timestamp as a date separator label.
     *
     * - Today → "Today"
     * - Yesterday → "Yesterday"
     * - This year → "Mon DD" (e.g. "Jun 1")
     * - Other years → "Mon DD, YYYY"
     */
    fun formatDateSeparator(timestampMillis: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestampMillis
        return when {
            diff < DAY_MS -> "Today"
            diff < 2 * DAY_MS -> "Yesterday"
            else -> SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(Date(timestampMillis))
        }
    }

    /**
     * Format a story timestamp (e.g. "5h ago", "Yesterday", "2d ago").
     */
    fun formatStoryTimestamp(timestampMillis: Long): String {
        if (timestampMillis <= 0L) return ""
        val now = System.currentTimeMillis()
        val diff = now - timestampMillis
        val minutes = diff / MINUTE_MS
        val hours = diff / HOUR_MS
        val days = hours / 24L
        return when {
            minutes < 1 -> "Just now"
            minutes < 60 -> "${minutes}m ago"
            hours < 24 -> "${hours}h ago"
            days == 1L -> "Yesterday"
            days < 7 -> "${days}d ago"
            else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestampMillis))
        }
    }

    /**
     * Format a duration in milliseconds as "m:ss" (e.g. "1:23").
     */
    fun formatDuration(durationMs: Long): String {
        val totalSeconds = durationMs / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "%d:%02d".format(minutes, seconds)
    }
}
