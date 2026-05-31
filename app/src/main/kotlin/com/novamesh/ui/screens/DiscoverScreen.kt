/**
 * DiscoverScreen — a social discovery feed for finding users, channels, and
 * communities on the NovaMesh network.
 *
 * This screen serves as the "Discover" tab and is organised into the following
 * sections:
 *
 * - **Search bar**: top text field to search users, channels, and groups
 * - **Quick actions**: horizontally scrollable chips (QR scan, Nearby,
 *   Trending Channels)
 * - **Suggested contacts**: horizontal row of user cards with avatar, name,
 *   and an "Add" button
 * - **Trending Channels**: section header + horizontal scrolling channel cards
 *   showing channel name, subscriber count, verified badge, and a follow
 *   button
 * - **Popular Communities**: section header + cards with community name, member
 *   count, channel count, and a join button
 * - **Nova Spotlight**: featured content card highlighting a channel or
 *   community
 *
 * All data is mocked for prototyping. In production a ViewModel would
 * observe the discovery API / directory service.
 */

@file:OptIn(ExperimentalMaterial3Api::class)

package com.novamesh.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material.icons.outlined.Whatshot
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novamesh.ui.theme.NovaPrimary
import com.novamesh.ui.theme.NovaSecondary
import com.novamesh.ui.theme.NovaSuccess

// ═════════════════════════════════════════════════════════════════════════════
// Internal data models
// ═════════════════════════════════════════════════════════════════════════════

/**
 * A suggested user / contact shown in the discover feed.
 */
private data class DiscoverUser(
  val id: String,
  val displayName: String,
  val username: String,
  val avatarEmoji: String,
  val isVerified: Boolean = false,
)

/**
 * A trending or featured channel.
 */
private data class DiscoverChannel(
  val id: String,
  val name: String,
  val description: String,
  val subscriberCount: Int,
  val emoji: String,
  val isVerified: Boolean = false,
  val isLive: Boolean = false,
)

/**
 * A popular community/group.
 */
private data class DiscoverCommunity(
  val id: String,
  val name: String,
  val description: String,
  val memberCount: Int,
  val channelCount: Int,
  val emoji: String,
  val category: String,
)

/**
 * A quick action chip option.
 */
private data class QuickAction(
  val id: String,
  val label: String,
  val icon: androidx.compose.ui.graphics.vector.ImageVector,
)

// ═════════════════════════════════════════════════════════════════════════════
// Mock data
// ═════════════════════════════════════════════════════════════════════════════

private val quickActions = listOf(
  QuickAction("qr", "QR Code", Icons.Filled.QrCodeScanner),
  QuickAction("nearby", "Nearby", Icons.Filled.LocationOn),
  QuickAction("trending", "Trending Channels", Icons.Filled.TrendingUp),
)

private val suggestedUsers = listOf(
  DiscoverUser("u1", "Sarah Chen", "sarah_codes", "👩‍💻", isVerified = true),
  DiscoverUser("u2", "Marcus Johnson", "marcus_j", "🎨"),
  DiscoverUser("u3", "Priya Patel", "priya_dev", "🌟", isVerified = true),
  DiscoverUser("u4", "Liam O'Brien", "liamob", "🎵"),
  DiscoverUser("u5", "Emma Wilson", "emma_w", "📸", isVerified = true),
  DiscoverUser("u6", "David Kim", "david_k", "🚀"),
)

private val trendingChannels = listOf(
  DiscoverChannel("c1", "TechTalk", "Latest in tech & programming", 45200, "💻", isVerified = true, isLive = true),
  DiscoverChannel("c2", "Creative Corner", "Art, design & photography", 32100, "🎨", isVerified = true),
  DiscoverChannel("c3", "Music Mix", "Daily music recommendations", 28700, "🎵", isVerified = false),
  DiscoverChannel("c4", "Gaming Hub", "Game reviews & live streams", 25400, "🎮", isLive = true),
  DiscoverChannel("c5", "Travel Diaries", "Explore the world virtually", 18900, "🌍", isVerified = true),
  DiscoverChannel("c6", "Fitness First", "Workouts & nutrition tips", 16200, "💪", isVerified = false),
)

private val popularCommunities = listOf(
  DiscoverCommunity("g1", "NovaMesh Developers", "Official community for NovaMesh developers", 12800, 42, "🛠️", "Technology"),
  DiscoverCommunity("g2", "Digital Nomads", "For remote workers & travellers", 9400, 28, "🌴", "Lifestyle"),
  DiscoverCommunity("g3", "Photography Club", "Share your best shots & get feedback", 8100, 35, "📷", "Creative"),
  DiscoverCommunity("g4", "Book Worms", "Monthly book club discussions", 5600, 12, "📚", "Education"),
)

/**
 * Featured spotlight item shown as a hero card.
 */
private val spotlightChannel = DiscoverChannel(
  id = "spotlight_1",
  name = "NovaMesh Official",
  description = "Stay up to date with the latest NovaMesh features, tips, " +
    "and community highlights. Subscribe for weekly updates!",
  subscriberCount = 128000,
  emoji = "✨",
  isVerified = true,
  isLive = true,
)

// ═════════════════════════════════════════════════════════════════════════════
// Public composable — entry point for the NavGraph
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Social discovery feed where users can search, browse suggested contacts,
 * trending channels, popular communities, and featured content.
 *
 * @param onSearchInvoked Called with the search query when the user submits.
 * @param onQrScanClick Navigate to QR scanner.
 * @param onNearbyClick Navigate to nearby users.
 * @param onUserClick Navigate to a user's profile.
 * @param onChannelClick Navigate to a channel detail.
 * @param onCommunityClick Navigate to a community detail.
 */
@Composable
fun DiscoverScreen(
  onSearchInvoked: (query: String) -> Unit = {},
  onQrScanClick: () -> Unit = {},
  onNearbyClick: () -> Unit = {},
  onUserClick: (userId: String) -> Unit = {},
  onChannelClick: (channelId: String) -> Unit = {},
  onCommunityClick: (communityId: String) -> Unit = {},
) {
  // ─── State ───────────────────────────────────────────────────────────────
  var searchQuery by remember { mutableStateOf("") }
  var isSearchExpanded by remember { mutableStateOf(false) }

  // Track which users/channels/communities the user has interacted with
  val addedUsers = remember { mutableStateOf(setOf<String>()) }
  val followedChannels = remember { mutableStateOf(setOf<String>()) }
  val joinedCommunities = remember { mutableStateOf(setOf<String>()) }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = "Discover",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
          )
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface,
        ),
      )
    },
    modifier = Modifier.fillMaxSize(),
  ) { paddingValues ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues),
      contentPadding = PaddingValues(bottom = 32.dp),
    ) {
      // ── Search Bar ─────────────────────────────────────────────────────
      item(key = "search_bar") {
        DiscoverSearchBar(
          query = searchQuery,
          onQueryChange = { searchQuery = it },
          onSearch = {
            isSearchExpanded = false
            if (searchQuery.isNotBlank()) {
              onSearchInvoked(searchQuery.trim())
            }
          },
          expanded = isSearchExpanded,
          onExpandedChange = { isSearchExpanded = it },
          onClear = {
            searchQuery = ""
            isSearchExpanded = false
          },
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        )
      }

      // ── Quick Actions Row ─────────────────────────────────────────────
      item(key = "quick_actions") {
        QuickActionsRow(
          actions = quickActions,
          onActionClick = { action ->
            when (action.id) {
              "qr" -> onQrScanClick()
              "nearby" -> onNearbyClick()
              "trending" -> { /* TODO: scroll to or navigate */ }
            }
          },
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        )
      }

      // ── Suggested Contacts ────────────────────────────────────────────
      item(key = "suggested_header") {
        SectionHeader(
          title = "Suggested Contacts",
          icon = Icons.Outlined.PersonAdd,
          modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
        )
      }
      item(key = "suggested_row") {
        SuggestedContactsRow(
          users = suggestedUsers,
          addedUsers = addedUsers.value,
          onAddUser = { userId ->
            addedUsers.value = addedUsers.value + userId
          },
          onUserClick = onUserClick,
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        )
      }

      // ── Trending Channels ─────────────────────────────────────────────
      item(key = "trending_header") {
        SectionHeader(
          title = "Trending Channels",
          icon = Icons.Outlined.Whatshot,
          actionLabel = "See all",
          onActionClick = { /* TODO: navigate to full list */ },
          modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
        )
      }
      item(key = "trending_row") {
        TrendingChannelsRow(
          channels = trendingChannels,
          followedChannels = followedChannels.value,
          onFollowClick = { channelId ->
            followedChannels.value = followedChannels.value + channelId
          },
          onChannelClick = onChannelClick,
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        )
      }

      // ── Nova Spotlight (featured) ─────────────────────────────────────
      item(key = "spotlight_section") {
        SpotlightCard(
          channel = spotlightChannel,
          isFollowed = spotlightChannel.id in followedChannels.value,
          onFollowClick = {
            followedChannels.value = followedChannels.value + spotlightChannel.id
          },
          onChannelClick = { onChannelClick(spotlightChannel.id) },
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        )
      }

      // ── Popular Communities ───────────────────────────────────────────
      item(key = "communities_header") {
        SectionHeader(
          title = "Popular Communities",
          icon = Icons.Outlined.Star,
          actionLabel = "Browse all",
          onActionClick = { /* TODO: navigate to full list */ },
          modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
        )
      }
      items(
        items = popularCommunities,
        key = { "community_${it.id}" },
      ) { community ->
        CommunityCard(
          community = community,
          isJoined = community.id in joinedCommunities.value,
          onJoinClick = {
            joinedCommunities.value = joinedCommunities.value + community.id
          },
          onCommunityClick = { onCommunityClick(community.id) },
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        )
      }
    }
  }
}

// ═════════════════════════════════════════════════════════════════════════════
// Search bar
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Search bar for discovering users, channels, and groups on the network.
 */
@Composable
private fun DiscoverSearchBar(
  query: String,
  onQueryChange: (String) -> Unit,
  onSearch: () -> Unit,
  expanded: Boolean,
  onExpandedChange: (Boolean) -> Unit,
  onClear: () -> Unit,
  modifier: Modifier = Modifier,
) {
  SearchBar(
    query = query,
    onQueryChange = onQueryChange,
    onSearch = { onSearch() },
    active = expanded,
    onActiveChange = onExpandedChange,
    placeholder = { Text("Search users, channels, groups…") },
    leadingIcon = {
      Icon(
        imageVector = Icons.Filled.Search,
        contentDescription = "Search",
      )
    },
    trailingIcon = {
      if (query.isNotEmpty()) {
        IconButton(onClick = onClear) {
          Icon(
            imageVector = Icons.Filled.Clear,
            contentDescription = "Clear search",
          )
        }
      }
    },
    modifier = modifier,
  ) {
    // Dropdown suggestions would be rendered here in a production app
    Text(
      text = "Try searching for \"NovaMesh\" or \"Photography\"",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(16.dp),
    )
  }
}

// ═════════════════════════════════════════════════════════════════════════════
// Quick actions row
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Horizontally scrollable row of quick-action chips (QR scan, Nearby,
 * Trending Channels).
 */
@Composable
private fun QuickActionsRow(
  actions: List<QuickAction>,
  onActionClick: (QuickAction) -> Unit,
  modifier: Modifier = Modifier,
) {
  LazyRow(
    modifier = modifier,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    contentPadding = PaddingValues(vertical = 4.dp),
  ) {
    items(items = actions, key = { it.id }) { action ->
      FilterChip(
        selected = false,
        onClick = { onActionClick(action) },
        label = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = action.label)
          }
        },
        leadingIcon = {
          Icon(
            imageVector = action.icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
        },
        colors = FilterChipDefaults.filterChipColors(
          containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        ),
        border = FilterChipDefaults.filterChipBorder(
          borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
          selectedBorderColor = MaterialTheme.colorScheme.primary,
          enabled = true,
          selected = false,
        ),
      )
    }
  }
}

// ═════════════════════════════════════════════════════════════════════════════
// Section header
// ═════════════════════════════════════════════════════════════════════════════

/**
 * A section header with an optional icon, title, and trailing action link.
 */
@Composable
private fun SectionHeader(
  title: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
  actionLabel: String? = null,
  onActionClick: (() -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      if (icon != null) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          modifier = Modifier.size(20.dp),
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }

    if (actionLabel != null && onActionClick != null) {
      Surface(
        onClick = onActionClick,
        shape = RoundedCornerShape(8.dp),
        color = Color.Transparent,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
        ) {
          Text(
            text = actionLabel,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium,
          )
          Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.primary,
          )
        }
      }
    }
  }
}

// ═════════════════════════════════════════════════════════════════════════════
// Suggested contacts row
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Horizontal scrolling row of suggested contact cards.
 *
 * Each card shows the user's emoji avatar, display name, @handle, a verified
 * badge if applicable, and an "Add" or "Added" button.
 */
@Composable
private fun SuggestedContactsRow(
  users: List<DiscoverUser>,
  addedUsers: Set<String>,
  onAddUser: (String) -> Unit,
  onUserClick: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  LazyRow(
    modifier = modifier,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    contentPadding = PaddingValues(horizontal = 16.dp),
  ) {
    items(items = users, key = { it.id }) { user ->
      val isAdded = user.id in addedUsers
      SuggestedUserCard(
        user = user,
        isAdded = isAdded,
        onAddClick = { onAddUser(user.id) },
        onClick = { onUserClick(user.id) },
      )
    }
  }
}

/**
 * A single suggested contact card with a vertical layout (avatar, name,
 * handle, action button).
 */
@Composable
private fun SuggestedUserCard(
  user: DiscoverUser,
  isAdded: Boolean,
  onAddClick: () -> Unit,
  onClick: () -> Unit,
) {
  Card(
    onClick = onClick,
    modifier = Modifier
      .width(130.dp),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      // Avatar with optional verified badge
      Box(
        modifier = Modifier.size(52.dp),
      ) {
        Box(
          modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = user.avatarEmoji,
            fontSize = 24.sp,
          )
        }

        if (user.isVerified) {
          Icon(
            imageVector = Icons.Filled.Verified,
            contentDescription = "Verified",
            tint = NovaSuccess,
            modifier = Modifier
              .align(Alignment.BottomEnd)
              .size(18.dp),
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Display name
      Text(
        text = user.displayName,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
      )

      // Username
      Text(
        text = "@${user.username}",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
      )

      Spacer(modifier = Modifier.height(8.dp))

      // Add / Added button
      if (isAdded) {
        FilledTonalButton(
          onClick = onAddClick,
          modifier = Modifier.fillMaxWidth(),
          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
          enabled = false,
        ) {
          Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = "Added",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
          )
        }
      } else {
        Button(
          onClick = onAddClick,
          modifier = Modifier.fillMaxWidth(),
          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
          shape = RoundedCornerShape(8.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
          ),
        ) {
          Icon(
            imageVector = Icons.Filled.PersonAdd,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = "Add",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
          )
        }
      }
    }
  }
}

// ═════════════════════════════════════════════════════════════════════════════
// Trending channels row
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Horizontal scrolling row of trending channel cards.
 *
 * Each card shows the channel emoji, name, subscriber count, a verified badge
 * if applicable, a live indicator, and a follow button.
 */
@Composable
private fun TrendingChannelsRow(
  channels: List<DiscoverChannel>,
  followedChannels: Set<String>,
  onFollowClick: (String) -> Unit,
  onChannelClick: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  LazyRow(
    modifier = modifier,
    horizontalArrangement = Arrangement.spacedBy(10.dp),
    contentPadding = PaddingValues(horizontal = 16.dp),
  ) {
    items(items = channels, key = { it.id }) { channel ->
      val isFollowed = channel.id in followedChannels
      TrendingChannelCard(
        channel = channel,
        isFollowed = isFollowed,
        onFollowClick = { onFollowClick(channel.id) },
        onClick = { onChannelClick(channel.id) },
      )
    }
  }
}

/**
 * A single trending channel card with a compact horizontal layout.
 */
@Composable
private fun TrendingChannelCard(
  channel: DiscoverChannel,
  isFollowed: Boolean,
  onFollowClick: () -> Unit,
  onClick: () -> Unit,
) {
  Card(
    onClick = onClick,
    modifier = Modifier
      .width(180.dp),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surface,
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
    ) {
      // Top row: emoji + verified badge
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Box(
          modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primaryContainer),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = channel.emoji,
            fontSize = 22.sp,
          )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          if (channel.isLive) {
            Surface(
              shape = RoundedCornerShape(4.dp),
              color = Color(0xFFE53935),
              modifier = Modifier.padding(end = 4.dp),
            ) {
              Text(
                text = "LIVE",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                fontSize = 9.sp,
              )
            }
          }

          if (channel.isVerified) {
            Icon(
              imageVector = Icons.Filled.Verified,
              contentDescription = "Verified",
              tint = NovaSuccess,
              modifier = Modifier.size(18.dp),
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Channel name
      Text(
        text = channel.name,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )

      // Description
      Text(
        text = channel.description,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(top = 2.dp, bottom = 8.dp),
      )

      // Subscriber count + follow button
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "${formatCount(channel.subscriberCount)} subs",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          fontWeight = FontWeight.Medium,
        )

        if (isFollowed) {
          FilledTonalButton(
            onClick = onFollowClick,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            enabled = false,
          ) {
            Text(
              text = "Followed",
              style = MaterialTheme.typography.labelSmall,
            )
          }
        } else {
          FilledTonalButton(
            onClick = onFollowClick,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
          ) {
            Icon(
              imageVector = Icons.Outlined.BookmarkAdd,
              contentDescription = null,
              modifier = Modifier.size(14.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "Follow",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Medium,
            )
          }
        }
      }
    }
  }
}

// ═════════════════════════════════════════════════════════════════════════════
// Spotlight / featured card
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Hero-style "Nova Spotlight" card featuring a highlighted channel or
 * community. Uses a prominent layout with a colourful container background.
 */
@Composable
private fun SpotlightCard(
  channel: DiscoverChannel,
  isFollowed: Boolean,
  onFollowClick: () -> Unit,
  onChannelClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Card(
    onClick = onChannelClick,
    modifier = modifier,
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(
      containerColor = NovaPrimary.copy(alpha = 0.1f),
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(20.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      // Large emoji icon
      Box(
        modifier = Modifier
          .size(64.dp)
          .clip(RoundedCornerShape(16.dp))
          .background(NovaPrimary.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = channel.emoji,
          fontSize = 32.sp,
        )
      }

      Spacer(modifier = Modifier.width(16.dp))

      Column(modifier = Modifier.weight(1f)) {
        // Badge
        Surface(
          shape = RoundedCornerShape(6.dp),
          color = NovaPrimary.copy(alpha = 0.15f),
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
          ) {
            Icon(
              imageVector = Icons.Filled.AutoAwesome,
              contentDescription = null,
              modifier = Modifier.size(12.dp),
              tint = NovaPrimary,
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "Nova Spotlight",
              style = MaterialTheme.typography.labelSmall,
              color = NovaPrimary,
              fontWeight = FontWeight.SemiBold,
              fontSize = 10.sp,
            )
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Channel name
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = channel.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
          )
          if (channel.isVerified) {
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
              imageVector = Icons.Filled.Verified,
              contentDescription = "Verified",
              tint = NovaSuccess,
              modifier = Modifier.size(16.dp),
            )
          }
          if (channel.isLive) {
            Spacer(modifier = Modifier.width(6.dp))
            Surface(
              shape = RoundedCornerShape(4.dp),
              color = Color(0xFFE53935),
            ) {
              Text(
                text = "LIVE",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                fontSize = 9.sp,
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
          text = channel.description,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          // Subscriber count
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Filled.Person,
              contentDescription = null,
              modifier = Modifier.size(14.dp),
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "${formatCount(channel.subscriberCount)} subscribers",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              fontWeight = FontWeight.Medium,
            )
          }

          // Follow button
          if (isFollowed) {
            FilledTonalButton(
              onClick = onFollowClick,
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
              enabled = false,
            ) {
              Text(
                text = "Followed",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
              )
            }
          } else {
            Button(
              onClick = onFollowClick,
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
              shape = RoundedCornerShape(8.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = NovaPrimary,
              ),
            ) {
              Icon(
                imageVector = Icons.Outlined.BookmarkAdd,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Follow",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
              )
            }
          }
        }
      }
    }
  }
}

// ═════════════════════════════════════════════════════════════════════════════
// Community card
// ═════════════════════════════════════════════════════════════════════════════

/**
 * A card for a popular community, showing its emoji, name, category,
 * member/channel counts, description, and a join button.
 */
@Composable
private fun CommunityCard(
  community: DiscoverCommunity,
  isJoined: Boolean,
  onJoinClick: () -> Unit,
  onCommunityClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Card(
    onClick = onCommunityClick,
    modifier = modifier,
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surface,
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      // Emoji avatar
      Box(
        modifier = Modifier
          .size(48.dp)
          .clip(RoundedCornerShape(12.dp))
          .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = community.emoji,
          fontSize = 22.sp,
        )
      }

      Spacer(modifier = Modifier.width(12.dp))

      // Content
      Column(modifier = Modifier.weight(1f)) {
        // Name + category
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = community.name,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
          ) {
            Text(
              text = community.category,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSecondaryContainer,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
              fontSize = 10.sp,
            )
          }
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Description
        Text(
          text = community.description,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Stats row (members + channels)
        Row(
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Filled.Person,
              contentDescription = null,
              modifier = Modifier.size(12.dp),
              tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
              text = formatCount(community.memberCount),
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
              fontWeight = FontWeight.Medium,
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text(
              text = "members",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            )
          }

          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Menu,
              contentDescription = null,
              modifier = Modifier.size(12.dp),
              tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
              text = "${community.channelCount}",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
              fontWeight = FontWeight.Medium,
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text(
              text = "channels",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            )
          }
        }
      }

      Spacer(modifier = Modifier.width(8.dp))

      // Join / Joined button
      if (isJoined) {
        FilledTonalButton(
          onClick = onJoinClick,
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
          enabled = false,
        ) {
          Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = "Joined",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
          )
        }
      } else {
        OutlinedButton(
          onClick = onJoinClick,
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
          shape = RoundedCornerShape(8.dp),
        ) {
          Icon(
            imageVector = Icons.Outlined.AddCircleOutline,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = "Join",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
          )
        }
      }
    }
  }
}

// ═════════════════════════════════════════════════════════════════════════════
// Utility formatters
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Formats a large number into a short human-readable string (e.g. 12800 → "12.8K").
 */
private fun formatCount(count: Int): String {
  return when {
    count >= 1_000_000 -> "%.1fM".format(count / 1_000_000.0)
    count >= 1_000 -> "%.1fK".format(count / 1_000.0)
    else -> count.toString()
  }
}
