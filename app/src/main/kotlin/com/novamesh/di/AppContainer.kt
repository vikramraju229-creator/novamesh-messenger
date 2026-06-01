/**
 * Manual dependency injection container for NovaMesh Messenger.
 *
 * Provides singleton instances for the data layer, domain use cases,
 * and ViewModel factories. This is a lightweight alternative to Hilt/Dagger
 * that avoids annotation processing overhead and build complexity.
 *
 * Usage:
 * ```
 * class MyApplication : Application() {
 *     lateinit var container: AppContainer
 *     override fun onCreate() {
 *         super.onCreate()
 *         container = AppContainer(this)
 *     }
 * }
 *
 * // In Activity or Fragment:
 * val container = (application as MyApplication).container
 * val viewModel = container.chatListViewModel()
 * ```
 */
package com.novamesh.di

import android.content.Context
import com.novamesh.data.local.AppDatabase
import com.novamesh.data.local.dao.ChatDao
import com.novamesh.data.local.dao.MessageDao
import com.novamesh.data.local.dao.UserDao
import com.novamesh.data.remote.MatrixRepository
import com.novamesh.data.repository.MessageRepository
import com.novamesh.domain.usecase.GetChatsUseCase
import com.novamesh.domain.usecase.GetContactsUseCase
import com.novamesh.domain.usecase.GetMessagesUseCase
import com.novamesh.domain.usecase.GetStoriesUseCase
import com.novamesh.domain.usecase.MarkAsReadUseCase
import com.novamesh.domain.usecase.SendMessageUseCase
import com.novamesh.domain.usecase.SyncMessagesUseCase
import com.novamesh.domain.usecase.ToggleReactionUseCase
import com.novamesh.ui.viewmodel.CameraViewModel
import com.novamesh.ui.viewmodel.ChatDetailViewModel
import com.novamesh.ui.viewmodel.ChatListViewModel
import com.novamesh.ui.viewmodel.DiscoverViewModel
import com.novamesh.ui.viewmodel.ProfileViewModel
import com.novamesh.ui.viewmodel.StoriesViewModel
import com.novamesh.ui.viewmodel.StoryViewerViewModel

/**
 * Application-level dependency container.
 *
 * Initializes the database, repositories, use cases, and
 * provides factory methods for all ViewModels.
 *
 * @param context Application context (for DB and Matrix repository).
 * @param userId Current logged-in user ID (default: "user_self" for dev).
 */
class AppContainer(
    private val context: Context,
    private val userId: String = "user_self",
) {
    // ─── Database ───────────────────────────────────────────────────────

    private val database: AppDatabase by lazy {
        AppDatabase.getInstance(context)
    }

    // ─── DAOs ───────────────────────────────────────────────────────────

    val chatDao: ChatDao by lazy { database.chatDao() }
    val messageDao: MessageDao by lazy { database.messageDao() }
    val userDao: UserDao by lazy { database.userDao() }

    // ─── Remote / Sync ──────────────────────────────────────────────────

    val matrixRepository: MatrixRepository by lazy {
        MatrixRepository(
            context = context,
            messageDao = messageDao,
            chatDao = chatDao,
            userDao = userDao,
        )
    }

    private val signalProtocolManager: com.novamesh.security.SignalProtocolManager by lazy {
        com.novamesh.security.SignalProtocolManager(context)
    }

    val messageRepository: MessageRepository by lazy {
        MessageRepository(
            messageDao = messageDao,
            chatDao = chatDao,
            matrixRepository = matrixRepository,
            signalProtocolManager = signalProtocolManager,
            userId = userId,
        )
    }

    // ─── Use Cases ──────────────────────────────────────────────────────

    val getChatsUseCase: GetChatsUseCase by lazy {
        GetChatsUseCase(chatDao, userDao)
    }

    val getMessagesUseCase: GetMessagesUseCase by lazy {
        GetMessagesUseCase(messageDao)
    }

    val sendMessageUseCase: SendMessageUseCase by lazy {
        SendMessageUseCase(messageDao, chatDao, matrixRepository, userId)
    }

    val toggleReactionUseCase: ToggleReactionUseCase by lazy {
        ToggleReactionUseCase(messageDao, matrixRepository, userId)
    }

    val markAsReadUseCase: MarkAsReadUseCase by lazy {
        MarkAsReadUseCase(messageDao, chatDao, matrixRepository)
    }

    val syncMessagesUseCase: SyncMessagesUseCase by lazy {
        SyncMessagesUseCase(messageRepository)
    }

    val getStoriesUseCase: GetStoriesUseCase by lazy {
        GetStoriesUseCase(userDao)
    }

    val getContactsUseCase: GetContactsUseCase by lazy {
        GetContactsUseCase(userDao)
    }

    // ─── ViewModel Factories ────────────────────────────────────────────

    fun chatListViewModel(): ChatListViewModel = ChatListViewModel(getChatsUseCase)

    fun chatDetailViewModel(): ChatDetailViewModel = ChatDetailViewModel(
        getMessagesUseCase = getMessagesUseCase,
        sendMessageUseCase = sendMessageUseCase,
        toggleReactionUseCase = toggleReactionUseCase,
        markAsReadUseCase = markAsReadUseCase,
    )

    fun cameraViewModel(): CameraViewModel = CameraViewModel(sendMessageUseCase)

    fun storiesViewModel(): StoriesViewModel = StoriesViewModel(getStoriesUseCase)

    fun storyViewerViewModel(): StoryViewerViewModel = StoryViewerViewModel()

    fun discoverViewModel(): DiscoverViewModel = DiscoverViewModel(getContactsUseCase)

    fun profileViewModel(): ProfileViewModel = ProfileViewModel()
}
