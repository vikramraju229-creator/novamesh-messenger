/**
 * Use case for observing stories — the user's own story and contacts' stories.
 *
 * Currently returns mock data since stories are ephemeral (24h TTL) and
 * managed via the Matrix media repository. In production, this would query
 * a dedicated stories table or the Matrix room timeline for story events.
 */
package com.novamesh.domain.usecase

import com.novamesh.data.local.dao.UserDao
import com.novamesh.domain.model.Story
import com.novamesh.domain.model.StoryType
import com.novamesh.domain.model.StoryView
import com.novamesh.domain.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * @param userDao Local DAO for resolving user profiles.
 */
class GetStoriesUseCase(
    private val userDao: UserDao,
) {
    /**
     * Observe the current user's stories ("My Story").
     */
    fun getMyStories(userId: String): Flow<List<Story>> {
        // TODO: Query stories table or Matrix timeline for self stories
        return flowOf(mockMyStories(userId))
    }

    /**
     * Observe contacts' stories for the feed.
     *
     * Returns a list of [User] paired with their active [Story] list.
     */
    fun getContactStories(): Flow<List<Pair<User, List<Story>>>> {
        // TODO: Query stories table or Matrix room timeline for story events
        return flowOf(mockContactStories())
    }

    /**
     * Mark a story as viewed by the current user.
     */
    suspend fun markAsViewed(storyId: String, userId: String) {
        // TODO: Persist view receipt to local DB + sync to Matrix
    }

    // ─── Mock data (temporary) ───────────────────────────────────────────

    private fun mockMyStories(userId: String): List<Story> {
        val now = System.currentTimeMillis()
        val hour = 3_600_000L
        return listOf(
            Story(
                id = "my_story_1", userId = userId, userName = "You",
                mediaUri = "https://picsum.photos/1080/1920?random=1",
                type = StoryType.IMAGE, caption = "Good morning! ☀️",
                durationSeconds = 10, createdAt = now - 2 * hour,
                expiresAt = now + 22 * hour,
                viewedBy = listOf(
                    StoryView("user_1", "Alice", now - hour),
                    StoryView("user_2", "Bob", now - 30 * 60_000),
                ),
            ),
        )
    }

    private fun mockContactStories(): List<Pair<User, List<Story>>> {
        val now = System.currentTimeMillis()
        val hour = 3_600_000L
        val alice = User(id = "user_1", username = "alice", displayName = "Alice Wonderland")
        val bob = User(id = "user_2", username = "bob", displayName = "Bob Builder")

        return listOf(
            alice to listOf(
                Story(id = "story_alice_1", userId = "user_1", userName = "Alice Wonderland",
                    mediaUri = "https://picsum.photos/1080/1920?random=10",
                    type = StoryType.IMAGE, caption = "Sunset vibes 🌅",
                    durationSeconds = 10, createdAt = now - 3 * hour,
                    expiresAt = now + 21 * hour, viewedBy = emptyList()),
            ),
            bob to listOf(
                Story(id = "story_bob_1", userId = "user_2", userName = "Bob Builder",
                    mediaUri = "https://picsum.photos/1080/1920?random=20",
                    type = StoryType.IMAGE, caption = "New bike! 🚴",
                    durationSeconds = 10, createdAt = now - 5 * hour,
                    expiresAt = now + 19 * hour, viewedBy = emptyList()),
            ),
        )
    }
}
