package com.novamesh.ui.screens.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/** Profile data from Firestore. */
data class ProfileData(
    val name: String = "",
    val username: String = "",
    val phone: String = "",
    val photoUrl: String = "",
    val bio: String = "",
    val lastSeen: Long = 0L,
    val isOnline: Boolean = false,
    val createdAt: Long = 0L,
    val snapStreak: Int = 0,
)

/** Profile screen UI state. */
sealed interface ProfileState {
    data object Loading : ProfileState
    data class Loaded(val profile: ProfileData) : ProfileState
    data class Error(val message: String) : ProfileState
    data object Saving : ProfileState
    data object Saved : ProfileState
    data object UploadingPhoto : ProfileState
}

/**
 * ViewModel for the ProfileScreen.
 *
 * Loads profile data from Firestore, supports editing name,
 * username, bio, and uploading a new profile photo.
 */
class ProfileViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private val _state = MutableStateFlow<ProfileState>(ProfileState.Loading)
    val state: StateFlow<ProfileState> = _state.asStateFlow()

    private val currentUserId: String?
        get() = auth.currentUser?.uid

    init {
        loadProfile()
    }

    /** Load profile from Firestore. */
    fun loadProfile() {
        val uid = currentUserId ?: run {
            _state.value = ProfileState.Error("Not signed in")
            return
        }

        _state.value = ProfileState.Loading
        viewModelScope.launch {
            try {
                val doc = firestore.collection("users").document(uid).get().await()
                if (doc.exists()) {
                    val data = ProfileData(
                        name = doc.getString("name") ?: "",
                        username = doc.getString("username") ?: "",
                        phone = doc.getString("phone") ?: "",
                        photoUrl = doc.getString("photoUrl") ?: "",
                        bio = doc.getString("bio") ?: "",
                        lastSeen = doc.getLong("lastSeen") ?: 0L,
                        isOnline = doc.getBoolean("isOnline") ?: false,
                        createdAt = doc.getLong("createdAt") ?: 0L,
                        snapStreak = doc.getLong("snapStreak")?.toInt() ?: 0,
                    )
                    _state.value = ProfileState.Loaded(data)
                } else {
                    _state.value = ProfileState.Error("Profile not found")
                }
            } catch (e: Exception) {
                _state.value = ProfileState.Error(e.message ?: "Failed to load profile")
            }
        }
    }

    /** Update profile fields in Firestore. */
    fun updateProfile(
        name: String? = null,
        username: String? = null,
        bio: String? = null,
    ) {
        val uid = currentUserId ?: return
        _state.value = ProfileState.Saving

        viewModelScope.launch {
            try {
                val updates = hashMapOf<String, Any>()
                name?.let { updates["name"] = it }
                username?.let { updates["username"] = it }
                bio?.let { updates["bio"] = it }

                if (updates.isNotEmpty()) {
                    firestore.collection("users").document(uid).update(updates).await()
                }

                // Reload profile
                loadProfile()
                _state.value = ProfileState.Saved
            } catch (e: Exception) {
                _state.value = ProfileState.Error(e.message ?: "Failed to update profile")
            }
        }
    }

    /** Upload profile photo to Firebase Storage and update Firestore. */
    fun uploadProfilePhoto(uri: Uri) {
        val uid = currentUserId ?: return
        _state.value = ProfileState.UploadingPhoto

        viewModelScope.launch {
            try {
                val ref = storage.reference.child("profiles/$uid/photo.jpg")
                ref.putFile(uri).await()
                val downloadUrl = ref.downloadUrl.await().toString()

                // Update Firestore
                firestore.collection("users").document(uid)
                    .update("photoUrl", downloadUrl).await()

                // Reload profile
                loadProfile()
                _state.value = ProfileState.Saved
            } catch (e: Exception) {
                _state.value = ProfileState.Error(e.message ?: "Failed to upload photo")
            }
        }
    }

    /** Sign out the current user. */
    fun signOut() {
        auth.signOut()
    }
}
