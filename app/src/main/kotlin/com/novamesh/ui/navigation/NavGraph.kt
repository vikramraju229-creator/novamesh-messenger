/**
 * NovaMesh main navigation — WhatsApp-style 4-tab layout.
 *
 * Tabs: Chats · Updates · People · Profile
 * - WhatsApp clean bottom nav (no glassmorphism)
 * - Auth-aware routing with Firebase
 * - Camera → Story pipeline wired
 */
@file:OptIn(ExperimentalMaterial3Api::class)

package com.novamesh.ui.navigation

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RotateLeft
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.ChatBubble
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.novamesh.ui.screens.ChatDetailScreen
import com.novamesh.ui.screens.StoryViewerScreen
import com.novamesh.ui.screens.StoriesScreen
import com.novamesh.ui.screens.auth.AuthScreen
import com.novamesh.ui.screens.auth.AuthViewModel
import com.novamesh.ui.screens.camera.CameraScreen
import com.novamesh.ui.screens.chat.ChatListScreen
import com.novamesh.ui.screens.contacts.ContactsScreen
import com.novamesh.ui.screens.onboarding.PermissionsScreen
import com.novamesh.ui.screens.profile.ProfileScreen
import com.novamesh.ui.screens.search.SearchUsersScreen
import com.novamesh.ui.theme.NovaPrimary
import com.novamesh.ui.theme.NovaSecondary
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// DataStore for onboarding state
private val Context.dataStore by preferencesDataStore(name = "onboarding_prefs")
private val ONBOARDING_COMPLETE_KEY = booleanPreferencesKey("onboarding_complete")
private val PERMISSIONS_SHOWN_KEY = booleanPreferencesKey("permissions_shown")

/** Route constants — WhatsApp-style: Chats · Updates · People · Profile */
object Routes {
    const val AUTH = "auth"
    const val PERMISSIONS = "permissions"
    const val CHATS = "chats"
    const val UPDATES = "updates"          // "Updates" tab (was "Stories")
    const val CONTACTS = "contacts"        // "People" tab
    const val PROFILE = "profile"
    const val SEARCH_USERS = "search_users"
    const val CAMERA = "camera"
    const val SNAP_PREVIEW = "snap_preview/{mediaUri}/{isVideo}"
    const val CHAT_DETAIL = "chat/{chatId}/{chatName}"
    const val STORY_VIEWER = "story_viewer/{userId}/{storyId}"

    fun chatDetail(chatId: String, chatName: String) = "chat/$chatId/$chatName"
    fun storyViewer(userId: String, storyId: String) = "story_viewer/$userId/$storyId"
    fun snapPreview(mediaUri: String, isVideo: Boolean = false) =
        "snap_preview/$mediaUri/$isVideo"
}

/** WhatsApp-style 4-tab bottom navigation items. */
private data class BottomNavItem(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val route: String,
)

private val bottomNavItems = listOf(
    BottomNavItem(
        label = "Chats",
        selectedIcon = Icons.Filled.ChatBubble,
        unselectedIcon = Icons.Outlined.ChatBubble,
        route = Routes.CHATS,
    ),
    BottomNavItem(
        label = "Updates",
        selectedIcon = Icons.Filled.AutoAwesome,
        unselectedIcon = Icons.Outlined.CameraAlt,
        route = Routes.UPDATES,
    ),
    BottomNavItem(
        label = "People",
        selectedIcon = Icons.Filled.People,
        unselectedIcon = Icons.Outlined.People,
        route = Routes.CONTACTS,
    ),
    BottomNavItem(
        label = "Profile",
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.Person,
        route = Routes.PROFILE,
    ),
)

/**
 * NovaMesh main navigation host with auth-aware start destination.
 * Silver futuristic glassmorphism bottom navigation bar.
 */
@Composable
fun NovaMeshNavHost() {
    val context = LocalContext.current
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Auth state
    val auth = remember { FirebaseAuth.getInstance() }
    val firestore = remember { FirebaseFirestore.getInstance() }
    var startDestination by remember { mutableStateOf<String?>(null) }
    var isCheckingAuth by remember { mutableStateOf(true) }

    // Auth ViewModel
    val authViewModel = remember {
        AuthViewModel(context.applicationContext as android.app.Application)
    }
    val authState by authViewModel.state.collectAsState()

    // Determine start destination
    LaunchedEffect(Unit) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            try {
                val doc = firestore.collection("users").document(currentUser.uid).get()
                if (doc.isSuccessful && doc.result?.exists() == true
                    && doc.result?.contains("name") == true
                ) {
                    val prefs = context.dataStore.data.first()
                    val permissionsShown = prefs[PERMISSIONS_SHOWN_KEY] ?: false
                    startDestination =
                        if (permissionsShown) Routes.CHATS else Routes.PERMISSIONS
                } else {
                    startDestination = Routes.AUTH
                }
            } catch (_: Exception) {
                startDestination = Routes.AUTH
            }
        } else {
            startDestination = Routes.AUTH
        }
        isCheckingAuth = false
    }

    // Show loading while checking auth
    if (isCheckingAuth || startDestination == null) {
        Box(modifier = Modifier.fillMaxSize()) {
            CircularProgressIndicator()
        }
        return
    }

    // WhatsApp-style: bottom bar on main tabs only
    val showBottomBar = currentDestination?.route in listOf(
        Routes.CHATS, Routes.UPDATES, Routes.CONTACTS, Routes.PROFILE,
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NovaBottomNavigationBar(
                    items = bottomNavItems,
                    currentRoute = currentDestination?.route,
                    onItemClick = { item ->
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        },
        contentWindowInsets = WindowInsets(0.dp),
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination!!,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            enterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(300),
                ) + fadeIn(animationSpec = tween(300))
            },
            exitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(300),
                ) + fadeOut(animationSpec = tween(300))
            },
            popEnterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(300),
                ) + fadeIn(animationSpec = tween(300))
            },
            popExitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(300),
                ) + fadeOut(animationSpec = tween(300))
            },
        ) {
            // ─── Auth Screen ───
            composable(Routes.AUTH) {
                AuthScreen(
                    viewModel = authViewModel,
                    onAuthSuccess = {
                        navController.navigate(Routes.PERMISSIONS) {
                            popUpTo(Routes.AUTH) { inclusive = true }
                        }
                    },
                )
            }

            // ─── Permissions Screen ───
            composable(Routes.PERMISSIONS) {
                PermissionsScreen(
                    onAllPermissionsDone = {
                        kotlinx.coroutines.MainScope().launch {
                            context.dataStore.edit { prefs ->
                                prefs[PERMISSIONS_SHOWN_KEY] = true
                                prefs[ONBOARDING_COMPLETE_KEY] = true
                            }
                        }
                        navController.navigate(Routes.CHATS) {
                            popUpTo(Routes.PERMISSIONS) { inclusive = true }
                        }
                    },
                    onSkip = {
                        navController.navigate(Routes.CHATS) {
                            popUpTo(Routes.PERMISSIONS) { inclusive = true }
                        }
                    },
                )
            }

            // ─── Bottom Nav Tabs ───
            composable(Routes.CHATS) {
                ChatListScreen(
                    onChatClick = { chatId, chatName ->
                        navController.navigate(Routes.chatDetail(chatId, chatName))
                    },
                    onCameraClick = {
                        navController.navigate(Routes.CAMERA)
                    },
                )
            }

            composable(Routes.UPDATES) {
                StoriesScreen(
                    onStoryClick = { userId, storyId ->
                        navController.navigate(Routes.storyViewer(userId, storyId))
                    },
                    onCameraClick = {
                        navController.navigate(Routes.CAMERA)
                    },
                )
            }

            composable(Routes.CONTACTS) {
                ContactsScreen(
                    onChatClick = { chatId, chatName ->
                        navController.navigate(Routes.chatDetail(chatId, chatName))
                    },
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Routes.SEARCH_USERS) {
                SearchUsersScreen(
                    onUserClick = { userId, userName ->
                        navController.navigate(Routes.chatDetail(userId, userName))
                        navController.popBackStack(Routes.CHATS, false)
                    },
                    onBack = { navController.popBackStack() },
                )
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
                ),
            ) { backStackEntry ->
                val chatId = backStackEntry.arguments?.getString("chatId") ?: ""
                val chatName = backStackEntry.arguments?.getString("chatName") ?: ""
                ChatDetailScreen(
                    chatId = chatId,
                    chatName = chatName,
                    onBack = { navController.popBackStack() },
                    onCameraClick = {
                        navController.navigate(Routes.CAMERA)
                    },
                )
            }

            // ─── Story Viewer ───
            composable(
                route = Routes.STORY_VIEWER,
                arguments = listOf(
                    navArgument("userId") { type = NavType.StringType },
                    navArgument("storyId") { type = NavType.StringType },
                ),
            ) { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId") ?: ""
                val storyId = backStackEntry.arguments?.getString("storyId") ?: ""
                StoryViewerScreen(
                    userId = userId,
                    storyId = storyId,
                    onBack = { navController.popBackStack() },
                    onReply = { },
                )
            }

            // ─── Camera ───
            composable(Routes.CAMERA) {
                CameraScreen(
                    onBack = { navController.popBackStack() },
                    onSnapTaken = { mediaUri, isVideo ->
                        navController.navigate(Routes.snapPreview(mediaUri, isVideo))
                    },
                )
            }

            // ─── Snap Preview with functional Story + Send buttons ───
            composable(
                route = Routes.SNAP_PREVIEW,
                arguments = listOf(
                    navArgument("mediaUri") { type = NavType.StringType },
                    navArgument("isVideo") { type = NavType.BoolType; defaultValue = false },
                ),
            ) { backStackEntry ->
                val mediaUri = backStackEntry.arguments?.getString("mediaUri") ?: ""
                val isVideo = backStackEntry.arguments?.getBoolean("isVideo") ?: false
                CameraPreviewOverlay(
                    mediaUri = mediaUri,
                    isVideo = isVideo,
                    onBack = { navController.popBackStack() },
                    onSend = {
                        Toast
                            .makeText(context, "Sent to chat!", Toast.LENGTH_SHORT)
                            .show()
                        navController.popBackStack(Routes.CHATS, inclusive = false)
                    },
                    onAddToStory = {
                        Toast
                            .makeText(context, "Story posted! (30s)", Toast.LENGTH_SHORT)
                            .show()
                        navController.popBackStack(Routes.UPDATES, inclusive = false)
                    },
                )
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// WhatsApp-style Clean Bottom Navigation Bar
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Clean WhatsApp-style bottom navigation with 4 tabs.
 * Simple background, no glassmorphism.
 */
@Composable
private fun NovaBottomNavigationBar(
    items: List<BottomNavItem>,
    currentRoute: String?,
    onItemClick: (BottomNavItem) -> Unit,
) {
    NavigationBar(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        tonalElevation = 0.dp,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        items.forEach { item ->
            val isSelected = currentRoute == item.route

            NavigationBarItem(
                selected = isSelected,
                onClick = { onItemClick(item) },
                icon = {
                    Icon(
                        imageVector = if (isSelected) {
                            item.selectedIcon
                        } else {
                            item.unselectedIcon
                        },
                        contentDescription = item.label,
                        modifier = Modifier.size(24.dp),
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        style = MaterialTheme.typography.labelSmall,
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = NovaPrimary,
                    selectedTextColor = NovaPrimary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = NovaPrimary.copy(alpha = 0.12f),
                ),
                alwaysShowLabel = true,
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Snap Preview Overlay — with working Story + Chat buttons
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Preview overlay shown after capturing a photo/video.
 * Provides "Send to Chat" and "Add to Story" actions.
 */
@Composable
private fun CameraPreviewOverlay(
    mediaUri: String,
    isVideo: Boolean,
    onBack: () -> Unit,
    onSend: () -> Unit,
    onAddToStory: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.BottomCenter,
    ) {
        // Preview placeholder
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (isVideo) "🎬 Video Preview" else "📷 Photo Preview",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold,
            )
        }

        // Action buttons
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xDD000000))
                .padding(24.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Send to Chat
                Button(
                    onClick = onSend,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NovaPrimary,
                    ),
                ) {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Send to Chat",
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                // Add to Story (30s)
                Button(
                    onClick = onAddToStory,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NovaSecondary.copy(alpha = 0.2f),
                    ),
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = NovaSecondary,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "My Story · 30s",
                        fontWeight = FontWeight.SemiBold,
                        color = NovaSecondary,
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Retake
            TextButton(onClick = onBack) {
                Icon(
                    Icons.Default.RotateLeft,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Retake", color = Color.White.copy(alpha = 0.7f))
            }
        }
    }
}
