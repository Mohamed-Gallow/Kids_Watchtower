package com.example.gomahrepoproject.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.UserProfileChangeRequest

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val auth = FirebaseAuth.getInstance()

    private var _authState = MutableLiveData<Boolean>()
    val authState: LiveData<Boolean> get() = _authState

    private var _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage


    fun login(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { loginTask ->
                if (loginTask.isSuccessful) {
                    _authState.value = true
                } else {
                    _authState.value = false
                }
            }
    }

    fun register(email: String, password: String, username: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { registerTask ->
                if (registerTask.isSuccessful) {
                    val profileUpdate = UserProfileChangeRequest.Builder()
                        .setDisplayName(username)
                        .build()

                    auth.currentUser?.updateProfile(profileUpdate)
                        ?.addOnCompleteListener { profileTask ->
                            if (profileTask.isSuccessful) {
                                _authState.value = true
                            } else {
                                _errorMessage.value = profileTask.exception?.localizedMessage
                            }
                        }
                } else {
                    if (registerTask.exception is FirebaseAuthUserCollisionException) {
                        _errorMessage.value = "already exist"
                    } else {
                        _errorMessage.value = registerTask.exception?.localizedMessage
                    }
                }
            }
    }

    fun resetPassword(email: String) {
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { passwordTask ->
                if (passwordTask.isSuccessful) {
                    _errorMessage.value = "password reset message sent successfully"

                } else {
                    _errorMessage.value = passwordTask.exception?.localizedMessage
                }
            }
    }

    fun logout() {
        auth.signOut()
        _authState.value = false
    }


}