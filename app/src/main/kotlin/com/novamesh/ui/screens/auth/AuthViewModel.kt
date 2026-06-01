package com.novamesh.ui.screens.auth

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

/** Possible authentication states. */
sealed interface AuthState {
    data object Idle : AuthState
    data object Loading : AuthState
    data class OTPSent(val verificationId: String, val forceResendingToken: PhoneAuthProvider.ForceResendingToken) : AuthState
    data object ProfileExists : AuthState
    data object NeedsProfile : AuthState
    data class Error(val message: String) : AuthState
    data object Success : AuthState
}

/**
 * ViewModel for the phone authentication flow.
 *
 * Manages OTP sending, verification, and profile creation
 * using Firebase Auth + Firestore (free tier).
 */
class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private val _state = MutableStateFlow<AuthState>(AuthState.Idle)
    val state: StateFlow<AuthState> = _state.asStateFlow()

    private val _currentUser = MutableStateFlow<FirebaseUser?>(null)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    /** Phone number entered by the user (with country code). */
    private var phoneNumber: String = ""

    private val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
            // Auto-retrieval or instant verification
            signInWithCredential(credential)
        }

        override fun onVerificationFailed(exception: FirebaseException) {
            val msg = when (exception) {
                is FirebaseAuthInvalidCredentialsException -> "Invalid phone number format"
                is FirebaseTooManyRequestsException -> "Too many attempts. Please try again later"
                else -> exception.message ?: "Verification failed"
            }
            _state.value = AuthState.Error(msg)
        }

        override fun onCodeSent(
            verificationId: String,
            token: PhoneAuthProvider.ForceResendingToken,
        ) {
            _state.value = AuthState.OTPSent(verificationId, token)
        }

        override fun onCodeAutoRetrievalTimeOut(verificationId: String) {
            // Let user manually enter OTP
        }
    }

    init {
        checkCurrentUser()
    }

    /** Check if user is already signed in. */
    private fun checkCurrentUser() {
        val user = auth.currentUser
        _currentUser.value = user
        if (user != null) {
            checkProfileExists(user.uid)
        }
    }

    /** Send OTP to the given phone number. */
    fun sendOTP(phone: String, activity: Activity) {
        phoneNumber = phone
        _state.value = AuthState.Loading

        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phone)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    /** Verify the OTP code entered by the user. */
    fun verifyOTP(verificationId: String, otp: String) {
        _state.value = AuthState.Loading
        val credential = PhoneAuthProvider.getCredential(verificationId, otp)
        signInWithCredential(credential)
    }

    /** Resend OTP with the same phone number. */
    fun resendOTP(token: PhoneAuthProvider.ForceResendingToken, activity: Activity) {
        if (phoneNumber.isBlank()) {
            _state.value = AuthState.Error("No phone number to resend to")
            return
        }
        _state.value = AuthState.Loading

        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .setForceResendingToken(token)
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    /** Sign in with phone auth credential. */
    private fun signInWithCredential(credential: PhoneAuthCredential) {
        viewModelScope.launch {
            try {
                val result = auth.signInWithCredential(credential).await()
                val user = result.user
                _currentUser.value = user
                if (user != null) {
                    checkProfileExists(user.uid)
                } else {
                    _state.value = AuthState.Error("Sign in failed")
                }
            } catch (e: Exception) {
                val msg = when (e) {
                    is FirebaseAuthInvalidCredentialsException -> "Invalid OTP code"
                    else -> e.message ?: "Verification failed"
                }
                _state.value = AuthState.Error(msg)
            }
        }
    }

    /** Check if user profile exists in Firestore. */
    private fun checkProfileExists(uid: String) {
        viewModelScope.launch {
            try {
                val doc = firestore.collection("users").document(uid).get().await()
                if (doc.exists() && doc.contains("name")) {
                    _state.value = AuthState.ProfileExists
                } else {
                    _state.value = AuthState.NeedsProfile
                }
            } catch (e: Exception) {
                // Firestore might be unavailable; assume new user
                _state.value = AuthState.NeedsProfile
            }
        }
    }

    /**
     * Create user profile in Firestore.
     *
     * @param name Display name
     * @param username @handle
     * @param photoUri Optional local URI for profile photo
     * @param onComplete Called with success/failure
     */
    fun createProfile(
        name: String,
        username: String,
        photoUri: String? = null,
        onComplete: (Boolean) -> Unit = {},
    ) {
        val user = auth.currentUser ?: run {
            _state.value = AuthState.Error("Not signed in")
            onComplete(false)
            return
        }

        _state.value = AuthState.Loading
        viewModelScope.launch {
            try {
                var photoUrl: String? = null

                // Upload photo if provided
                if (photoUri != null) {
                    val ref = storage.reference.child("profiles/${user.uid}/photo.jpg")
                    val uploadTask = ref.putFile(android.net.Uri.parse(photoUri)).await()
                    photoUrl = ref.downloadUrl.await().toString()
                }

                // Create user document in Firestore
                val userData = hashMapOf(
                    "name" to name,
                    "username" to username,
                    "phone" to (user.phoneNumber ?: ""),
                    "photoUrl" to (photoUrl ?: user.photoUrl?.toString() ?: ""),
                    "bio" to "",
                    "lastSeen" to System.currentTimeMillis(),
                    "isOnline" to true,
                    "fcmToken" to "",
                    "createdAt" to System.currentTimeMillis(),
                    "snapStreak" to 0,
                )

                firestore.collection("users").document(user.uid).set(userData).await()

                _state.value = AuthState.Success
                onComplete(true)
            } catch (e: Exception) {
                _state.value = AuthState.Error(e.message ?: "Failed to create profile")
                onComplete(false)
            }
        }
    }

    /** Sign out the current user. */
    fun signOut() {
        auth.signOut()
        _currentUser.value = null
        _state.value = AuthState.Idle
        phoneNumber = ""
    }

    /** Reset state to Idle (e.g., after error dismissal). */
    fun resetState() {
        _state.value = AuthState.Idle
    }
}
