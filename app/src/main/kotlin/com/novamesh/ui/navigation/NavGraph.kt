/**
 * NovaMesh main navigation host with string-based routes.
 *
 * Uses string routes for maximum compatibility with navigation-compose 2.7.x.
 */
package com.novamesh.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.novamesh.ui.components.NovaBottomNavBar
import com.novamesh.ui.screens.CameraScreen
import com.novamesh.ui.screens.ChatDetailScreen
import com.novamesh.ui.screens.ChatListScreen
import com.novamesh.ui.screens.DiscoverScreen
import com.novamesh.ui.screens.ProfileScreen
import com.novamesh.ui.screens.StoryViewerScreen
import com.novamesh.ui.screens.StoriesScreen

/** Route constants. */
object Routes {
    const val CHATS = "chats"
    const val STORIES = "stories"
    const val DISCOVER = "discover"
    const val PROFILE = "profile"
    const val CAMERA = "camera"
    const val SNAP_PREVIEW = "snap_preview/{mediaUri}/{isVideo}"
    const val CHAT_DETAIL = "chat/{chatId}/{chatName}"
    const val STORY_VIEWER = "story_viewer/{userId}/{storyId}"

    fun chatDetail(chatId: String, chatName: String) = "chat/$chatId/$chatName"
    fun storyViewer(userId: String, storyId: String) = "story_viewer/$userId/$storyId"
    fun snapPreview(mediaUri: String, isVideo: Boolean = false) =
        "snap_preview/$mediaUri/$isVideo"
}

/**
 * NovaMesh main navigation host.
 */
@Composable
fun NovaMeshNavHost() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Determine if bottom bar should be visible
    val showBottomBar = currentDestination?.route in listOf(
        Routes.CHATS, Routes.STORIES, Routes.DISCOVER, Routes.PROFILE
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NovaBottomNavBar(
                    currentDestination = currentDestination,
                    onNavigate = { screen ->
                        navController.navigate(screen) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.CHATS,
            modifier = Modifier.padding(innerPadding),
            enterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(300)
                ) + fadeIn(animationSpec = tween(300))
            },
            exitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(300)
                ) + fadeOut(animationSpec = tween(300))
            },
            popEnterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(300)
                ) + fadeIn(animationSpec = tween(300))
            },
            popExitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(300)
                ) + fadeOut(animationSpec = tween(300))
            },
        ) {
            // ─── Bottom Nav Tabs ───
            composable(Routes.CHATS) {
                ChatListScreen(
                    onChatClick = { chatId, chatName ->
                        navController.navigate(Routes.chatDetail(chatId, chatName))
                    },
                    onCameraClick = {
                        navController.navigate(Routes.CAMERA)
                    }
                )
            }

            composable(Routes.STORIES) {
                StoriesScreen(
                    onStoryClick = { userId, storyId ->
                        navController.navigate(Routes.storyViewer(userId, storyId))
                    },
                    onCameraClick = {
                        navController.navigate(Routes.CAMERA)
                    }
                )
            }

            composable(Routes.DISCOVER) {
                DiscoverScreen()
            }

            composable(Routes.PROFILE) {
                ProfileScreen()
            }

            // ─── Chat Detail ───
            composable(
                route = Routes.CHAT_DETAIL,
                arguments = listOf(
                    navArgument("chatId") { type = NavType.StringType },
                    navArgument("chatName") { type = NavType.StringType; defaultValue = "" },
                )
            ) { backStackEntry ->
                val chatId = backStackEntry.arguments?.getString("chatId") ?: ""
                val chatName = backStackEntry.arguments?.getString("chatName") ?: ""
                ChatDetailScreen(
                    chatId = chatId,
                    chatName = chatName,
                    onBack = { navController.popBackStack() },
                    onCameraClick = {
                        navController.navigate(Routes.CAMERA)
                    }
                )
            }

            // ─── Story Viewer ───
            composable(
                route = Routes.STORY_VIEWER,
                arguments = listOf(
                    navArgument("userId") { type = NavType.StringType },
                    navArgument("storyId") { type = NavType.StringType },
                )
            ) { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId") ?: ""
                val storyId = backStackEntry.arguments?.getString("storyId") ?: ""
                StoryViewerScreen(
                    userId = userId,
                    storyId = storyId,
                    onBack = { navController.popBackStack() },
                    onReply = { /* TODO: navigate to chat */ }
                )
            }

            // ─── Camera ───
            composable(Routes.CAMERA) {
                CameraScreen(
                    onBack = { navController.popBackStack() },
                    onSnapTaken = { mediaUri, isVideo ->
                        navController.navigate(Routes.snapPreview(mediaUri, isVideo))
                    }
                )
            }

            // ─── Snap Preview ───
            composable(
                route = Routes.SNAP_PREVIEW,
                arguments = listOf(
                    navArgument("mediaUri") { type = NavType.StringType },
                    navArgument("isVideo") { type = NavType.BoolType; defaultValue = false },
                )
            ) { backStackEntry ->
                val mediaUri = backStackEntry.arguments?.getString("mediaUri") ?: ""
                val isVideo = backStackEntry.arguments?.getBoolean("isVideo") ?: false
                CameraPreviewOverlay(
                    mediaUri = mediaUri,
                    isVideo = isVideo,
                    onBack = { navController.popBackStack() },
                    onSend = { /* TODO: send snap */ },
                    onAddToStory = { /* TODO: add to story */ }
                )
            }
        }
    }
}

/**
 * Placeholder for SnapPreviewScreen.
 */
@Composable
private fun CameraPreviewOverlay(
    mediaUri: String,
    isVideo: Boolean,
    onBack: () -> Unit,
    onSend: () -> Unit,
    onAddToStory: () -> Unit,
) {
    androidx.compose.foundation.layout.Box(
        modifier = androidx.compose.ui.Modifier.fillMaxSize()
    ) {
        androidx.compose.material3.Text("Preview: $mediaUri")
    }
}
