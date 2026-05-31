/**
 * Permission utility for NovaMesh Messenger.
 *
 * Provides helper functions for checking and requesting common Android
 * permissions used by the app (camera, microphone, storage, notifications).
 */
package com.novamesh.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

object PermissionUtils {

    /** Permission groups used by NovaMesh. */
    enum class PermissionType {
        CAMERA,
        MICROPHONE,
        STORAGE,
        NOTIFICATIONS,
        CONTACTS,
    }

    /** Get the Android permission string for a [PermissionType]. */
    fun androidPermission(type: PermissionType): String = when (type) {
        PermissionType.CAMERA -> Manifest.permission.CAMERA
        PermissionType.MICROPHONE -> Manifest.permission.RECORD_AUDIO
        PermissionType.STORAGE -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_IMAGES
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }
        }
        PermissionType.NOTIFICATIONS -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.POST_NOTIFICATIONS
            } else {
                "" // Not needed pre-Tiramisu
            }
        }
        PermissionType.CONTACTS -> Manifest.permission.READ_CONTACTS
    }

    /** Check if a given permission type is already granted. */
    fun isGranted(context: Context, type: PermissionType): Boolean {
        val permission = androidPermission(type)
        if (permission.isEmpty()) return true // Permission not required on this API level
        return ContextCompat.checkSelfPermission(context, permission) ==
                PackageManager.PERMISSION_GRANTED
    }

    /** Check if all required permissions for the camera feature are granted. */
    fun hasCameraPermissions(context: Context): Boolean =
        isGranted(context, PermissionType.CAMERA) &&
        isGranted(context, PermissionType.MICROPHONE) &&
        isGranted(context, PermissionType.STORAGE)

    /** Check if notification permission is granted (Android 13+). */
    fun hasNotificationPermission(context: Context): Boolean =
        isGranted(context, PermissionType.NOTIFICATIONS)
}
