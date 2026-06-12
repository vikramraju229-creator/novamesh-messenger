/**
 * Local profile cache using Jetpack DataStore.
 *
 * Saves the Firestore profile document locally so the ProfileScreen
 * can display data instantly on cold starts without waiting for a
 * network round-trip. Background refresh still updates from Firestore.
 */
package com.novamesh.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.novamesh.ui.screens.profile.ProfileData
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** DataStore extension property. */
private val Context.profileStore by preferencesDataStore(name = "profile_cache")

/** Keys used in the DataStore. */
private object ProfileCacheKeys {
    val CACHED_PROFILE = stringPreferencesKey("cached_profile")
}

/** JSON instance with lenient parsing for forward-compatibility. */
private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

/**
 * Serializable version of [ProfileData] for DataStore persistence.
 */
@Serializable
private data class CachedProfile(
    val name: String = "",
    val username: String = "",
    val phone: String = "",
    val email: String = "",
    val authMethod: String = "",
    val photoUrl: String = "",
    val bio: String = "",
    val lastSeen: Long = 0L,
    val isOnline: Boolean = false,
    val createdAt: Long = 0L,
    val snapStreak: Int = 0,
)

/**
 * Converts [ProfileData] → [CachedProfile] for serialization.
 */
private fun ProfileData.toCached() = CachedProfile(
    name = name,
    username = username,
    phone = phone,
    email = email,
    authMethod = authMethod,
    photoUrl = photoUrl,
    bio = bio,
    lastSeen = lastSeen,
    isOnline = isOnline,
    createdAt = createdAt,
    snapStreak = snapStreak,
)

/**
 * Converts [CachedProfile] → [ProfileData] for the UI layer.
 */
private fun CachedProfile.toProfileData() = ProfileData(
    name = name,
    username = username,
    phone = phone,
    email = email,
    authMethod = authMethod,
    photoUrl = photoUrl,
    bio = bio,
    lastSeen = lastSeen,
    isOnline = isOnline,
    createdAt = createdAt,
    snapStreak = snapStreak,
)

/**
 * Reads and writes the user profile to DataStore for instant offline load.
 */
class ProfileCache(private val context: Context) {

    /** Save profile to local cache. */
    suspend fun save(profile: ProfileData) {
        context.profileStore.edit { prefs ->
            prefs[ProfileCacheKeys.CACHED_PROFILE] = json.encodeToString(profile.toCached())
        }
    }

    /** Read cached profile — returns null if never cached. */
    suspend fun load(): ProfileData? {
        val raw = context.profileStore.data.map { prefs ->
            prefs[ProfileCacheKeys.CACHED_PROFILE]
        }.first()

        return raw?.let {
            try {
                json.decodeFromString<CachedProfile>(it).toProfileData()
            } catch (_: Exception) {
                null // Corrupt cache — ignore
            }
        }
    }

    /** Clear cached profile (e.g. on logout). */
    suspend fun clear() {
        context.profileStore.edit { prefs ->
            prefs.remove(ProfileCacheKeys.CACHED_PROFILE)
        }
    }
}
