package com.example.gomahrepoproject.auth

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.FirebaseDatabase

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    val auth = FirebaseAuth.getInstance()
    private val firebaseRef = FirebaseDatabase.getInstance().getReference("users")

    private val _authState = MutableLiveData<FirebaseUser?>()
    val authState: LiveData<FirebaseUser?> get() = _authState

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> get() = _errorMessage

    private val _resetPasswordStatus = MutableLiveData<Boolean>()
    val resetPasswordStatus: LiveData<Boolean> get() = _resetPasswordStatus

    init {
        checkIfLoggedIn()
    }

    fun login(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user?.isEmailVerified == true) {
                        _authState.value = user
                    } else {
                        _errorMessage.value = "Please verify your email before logging in."
                        auth.signOut()
                    }
                } else {
                    _errorMessage.value = task.exception?.message
                }
            }
    }

    fun register(email: String, password: String, role: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { registerTask ->
                if (registerTask.isSuccessful) {
                    val user = auth.currentUser
                    user?.sendEmailVerification()?.addOnCompleteListener { verificationTask ->
                        if (verificationTask.isSuccessful) {
                            val userId = user.uid
                            val userData = mapOf(
                                "email" to email,
                                "role" to role
                            )
                            firebaseRef.child(userId).setValue(userData)
                                .addOnCompleteListener {
                                    Log.d(TAG, "User data saved: $userData")
                                    _errorMessage.value = "Registered successfully, check your email for verification."
                                }
                                .addOnFailureListener {
                                    Log.e(TAG, "Failed to save user data: ${it.message}")
                                    _errorMessage.value = "Registration successful, but failed to save user data."
                                }
                        } else {
                            _errorMessage.value = "Registration successful, but failed to send verification email."
                        }
                    }
                } else {
                    if (registerTask.exception is FirebaseAuthUserCollisionException) {
                        _errorMessage.value = "Already Registered"
                    } else {
                        _errorMessage.value = registerTask.exception?.message
                    }
                }
            }
    }

    fun resetPassword(email: String) {
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                _resetPasswordStatus.value = task.isSuccessful
                if (!task.isSuccessful) {
                    _errorMessage.value = task.exception?.message
                }
            }
    }

    fun setUsername(username: String) {
        val user = auth.currentUser ?: return
        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(username)
            .build()

        user.updateProfile(profileUpdates)
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    _errorMessage.value = task.exception?.message
                }
            }
    }

    private fun updateProfilePicture(photoUrl: String) {
        val user = auth.currentUser ?: return
        val profileUpdates = UserProfileChangeRequest.Builder()
            .setPhotoUri(Uri.parse(photoUrl))
            .build()

        user.updateProfile(profileUpdates)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    firebaseRef.child(user.uid).child("photoUrl").setValue(photoUrl)
                } else {
                    _errorMessage.value = task.exception?.message
                }
            }
    }

    fun logout() {
        auth.signOut()
        _authState.value = null
    }

    private fun checkIfLoggedIn() {
        _authState.value = auth.currentUser?.takeIf { it.isEmailVerified }
    }

    companion object {
        const val TAG = "AuthViewModel"
    }
}