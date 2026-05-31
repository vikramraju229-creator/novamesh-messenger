/**
 * Broadcast receiver for the BOOT_COMPLETED event.
 *
 * After the device reboots, this receiver restarts the [SyncService]
 * so that Matrix sync resumes automatically without user intervention.
 *
 * Requires the `android.permission.RECEIVE_BOOT_COMPLETED` permission
 * (already declared in AndroidManifest.xml).
 */
package com.novamesh.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import timber.log.Timber

/**
 * Receives BOOT_COMPLETED broadcast to restart background services
 * after device reboot.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Timber.i("Boot completed — restarting SyncService")

            try {
                context.startForegroundService(
                    SyncService.startIntent(context),
                )
                Timber.i("SyncService started successfully after boot")
            } catch (e: Exception) {
                Timber.e(e, "Failed to start SyncService after boot")
            }
        }
    }
}
