package com.novamesh

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import com.novamesh.ui.navigation.NovaMeshNavHost
import com.novamesh.ui.theme.NovaMeshTheme

/**
 * Main entry point for NovaMesh Messenger.
 *
 * - Applies [FLAG_SECURE] to prevent screenshots (privacy by default)
 * - Uses edge-to-edge display with Material You theming
 * - Sets [WindowCompat.setDecorFitsSystemWindows] to false for proper
 *   bottom navigation bar rendering
 * - Hosts the Compose NavHost for all screens
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ─── Prevent screenshots / screen recording ───
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE,
        )

        enableEdgeToEdge()

        // Fix bottom nav bar cut-off: content draws behind system bars
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            NovaMeshTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    NovaMeshNavHost()
                }
            }
        }
    }
}
