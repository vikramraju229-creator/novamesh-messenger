package com.novamesh.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.novamesh.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for [UserEntity].
 *
 * Manages the local cache of user profiles, contact list, presence
 * information, and block/mute states. Contacts are queried reactively
 * for display in the contact picker UI.
 */
@Dao
interface UserDao {

    /**
     * Observe all contacts (users where isContact = 1), sorted by
     * display name alphabetically.
     */
    @Query("SELECT * FROM users WHERE isContact = 1 ORDER BY displayName ASC")
    fun getContacts(): Flow<List<UserEntity>>

    /**
     * Get a single user by their ID.
     */
    @Query("SELECT * FROM users WHERE id = :userId")
    suspend fun getUserById(userId: String): UserEntity?

    /**
     * Insert or replace a single user profile.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    /**
     * Batch insert or replace multiple user profiles.
     * Used when syncing the contact list or resolving user info for a room.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<UserEntity>)

    /**
     * Delete a user from the local database.
     */
    @Query("DELETE FROM users WHERE id = :userId")
    suspend fun deleteUser(userId: String)

    /**
     * Update the presence status for a user.
     */
    @Query("UPDATE users SET presence = :presence WHERE id = :userId")
    suspend fun updatePresence(userId: String, presence: String)

    /**
     * Toggle the blocked state for a user.
     */
    @Query("UPDATE users SET isBlocked = :isBlocked WHERE id = :userId")
    suspend fun blockUser(userId: String, isBlocked: Boolean)

    /**
     * Search users by display name or username (partial match, case-insensitive).
     * @param query The search term
     * @return Flow emitting matching users
     */
    @Query("SELECT * FROM users WHERE displayName LIKE '%' || :query || '%' OR username LIKE '%' || :query || '%'")
    fun searchUsers(query: String): Flow<List<UserEntity>>
}
