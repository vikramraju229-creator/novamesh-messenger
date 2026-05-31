/**
 * Use case for observing the contact list.
 */
package com.novamesh.domain.usecase

import com.novamesh.data.local.dao.UserDao
import com.novamesh.data.local.entity.UserEntity
import com.novamesh.domain.model.Presence
import com.novamesh.domain.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * @param userDao Local DAO for user profiles.
 */
class GetContactsUseCase(
    private val userDao: UserDao,
) {
    /**
     * Observe all contacts, sorted alphabetically by display name.
     */
    operator fun invoke(): Flow<List<User>> {
        return userDao.getContacts().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    /**
     * Search contacts by name or username.
     */
    fun search(query: String): Flow<List<User>> {
        return userDao.searchUsers(query).map { entities ->
            entities.filter { it.isContact }.map { it.toDomain() }
        }
    }

    /** Map [UserEntity] to domain [User]. */
    private fun UserEntity.toDomain(): User = User(
        id = id,
        username = username,
        displayName = displayName,
        avatarUri = avatarUri,
        bio = bio,
        phoneNumber = phoneNumber,
        presence = try { Presence.valueOf(presence) } catch (_: Exception) { Presence.OFFLINE },
        lastSeen = lastSeen,
        isBlocked = isBlocked,
        isMuted = isMuted,
        isContact = isContact,
    )
}
