/**
 * ViewModel for the DiscoverScreen — social discovery feed for finding users,
 * channels, and communities on the NovaMesh network.
 */
package com.novamesh.ui.viewmodel

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.TrendingUp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novamesh.domain.usecase.GetContactsUseCase
import com.novamesh.domain.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** A suggested contact in the discover feed. */
data class DiscoverUser(
    val id: String,
    val displayName: String,
    val username: String,
    val avatarEmoji: String,
    val isVerified: Boolean = false,
)

/** A trending channel. */
data class DiscoverChannel(
    val id: String,
    val name: String,
    val description: String,
    val subscriberCount: Int,
    val emoji: String,
    val isVerified: Boolean = false,
    val isLive: Boolean = false,
)

/** A popular community. */
data class DiscoverCommunity(
    val id: String,
    val name: String,
    val description: String,
    val memberCount: Int,
    val channelCount: Int,
    val emoji: String,
    val category: String,
)

/** A quick action chip. */
data class QuickAction(
    val id: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
)

/** UI state for the discover screen. */
data class DiscoverUiState(
    val searchQuery: String = "",
    val isSearchExpanded: Boolean = false,
    val suggestedUsers: List<DiscoverUser> = emptyList(),
    val trendingChannels: List<DiscoverChannel> = emptyList(),
    val popularCommunities: List<DiscoverCommunity> = emptyList(),
    val spotlightChannel: DiscoverChannel? = null,
    val quickActions: List<QuickAction> = emptyList(),
    val addedUserIds: Set<String> = emptySet(),
    val followedChannelIds: Set<String> = emptySet(),
    val joinedCommunityIds: Set<String> = emptySet(),
    val isLoading: Boolean = false,
)

/**
 * @param getContactsUseCase For searching contacts.
 */
class DiscoverViewModel(
    private val getContactsUseCase: GetContactsUseCase? = null,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiscoverUiState())
    val uiState: StateFlow<DiscoverUiState> = _uiState.asStateFlow()

    init {
        loadMockData()
    }

    private fun loadMockData() {
        _uiState.update {
            it.copy(
                suggestedUsers = defaultSuggestedUsers,
                trendingChannels = defaultTrendingChannels,
                popularCommunities = defaultCommunities,
                spotlightChannel = defaultSpotlight,
                quickActions = defaultQuickActions,
            )
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onSearchExpandedChange(expanded: Boolean) {
        _uiState.update { it.copy(isSearchExpanded = expanded) }
    }

    fun onSearchClear() {
        _uiState.update { it.copy(searchQuery = "", isSearchExpanded = false) }
    }

    fun onAddUser(userId: String) {
        _uiState.update { it.copy(addedUserIds = it.addedUserIds + userId) }
    }

    fun onFollowChannel(channelId: String) {
        _uiState.update { it.copy(followedChannelIds = it.followedChannelIds + channelId) }
    }

    fun onJoinCommunity(communityId: String) {
        _uiState.update { it.copy(joinedCommunityIds = it.joinedCommunityIds + communityId) }
    }

    companion object {
        private val defaultQuickActions = listOf(
            QuickAction("qr", "QR Code", Icons.Filled.QrCodeScanner),
            QuickAction("nearby", "Nearby", Icons.Filled.LocationOn),
            QuickAction("trending", "Trending Channels", Icons.Filled.TrendingUp),
        )

        private val defaultSuggestedUsers = listOf(
            DiscoverUser("u1", "Sarah Chen", "sarah_codes", "👩‍💻", true),
            DiscoverUser("u2", "Marcus Johnson", "marcus_j", "🎨"),
            DiscoverUser("u3", "Priya Patel", "priya_dev", "🌟", true),
            DiscoverUser("u4", "Liam O'Brien", "liamob", "🎵"),
            DiscoverUser("u5", "Emma Wilson", "emma_w", "📸", true),
            DiscoverUser("u6", "David Kim", "david_k", "🚀"),
        )

        private val defaultTrendingChannels = listOf(
            DiscoverChannel("c1", "TechTalk", "Latest in tech & programming", 45200, "💻", true, true),
            DiscoverChannel("c2", "Creative Corner", "Art, design & photography", 32100, "🎨", true),
            DiscoverChannel("c3", "Music Mix", "Daily music recommendations", 28700, "🎵"),
            DiscoverChannel("c4", "Gaming Hub", "Game reviews & live streams", 25400, "🎮", isLive = true),
            DiscoverChannel("c5", "Travel Diaries", "Explore the world virtually", 18900, "🌍", true),
            DiscoverChannel("c6", "Fitness First", "Workouts & nutrition tips", 16200, "💪"),
        )

        private val defaultCommunities = listOf(
            DiscoverCommunity("g1", "NovaMesh Developers", "Official community for NovaMesh devs", 12800, 42, "🛠️", "Technology"),
            DiscoverCommunity("g2", "Digital Nomads", "For remote workers & travellers", 9400, 28, "🌴", "Lifestyle"),
            DiscoverCommunity("g3", "Photography Club", "Share your best shots & get feedback", 8100, 35, "📷", "Creative"),
            DiscoverCommunity("g4", "Book Worms", "Monthly book club discussions", 5600, 12, "📚", "Education"),
        )

        private val defaultSpotlight = DiscoverChannel(
            id = "spotlight_1", name = "NovaMesh Official",
            description = "Stay up to date with the latest NovaMesh features, tips, and community highlights.",
            subscriberCount = 128000, emoji = "✨", isVerified = true, isLive = true,
        )
    }
}
