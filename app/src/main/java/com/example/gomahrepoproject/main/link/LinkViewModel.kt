package com.example.gomahrepoproject.main.link

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlin.random.Random

class LinkViewModel(application: Application) : AndroidViewModel(application) {
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()
    private val usersRef = database.getReference("users")

    private val _linkStatus = MutableLiveData<String>()
    val linkStatus: LiveData<String> get() = _linkStatus

    private val _pendingRequests = MutableLiveData<List<Pair<String, String>>>() // Pair(parentId, verificationCode)
    val pendingRequests: LiveData<List<Pair<String, String>>> get() = _pendingRequests

    // Send a linking request from parent to child
    fun sendLinkRequest(parentEmail: String, childEmail: String) {
        val parentId = auth.currentUser?.uid
        if (parentId == null) {
            _linkStatus.value = "Parent not authenticated"
            Log.e(TAG, "Parent not authenticated")
            return
        }

        Log.d(TAG, "Searching for child with email: $childEmail")
        // Find child by email
        usersRef.orderByChild("email").equalTo(childEmail).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d(TAG, "Query snapshot: ${snapshot.childrenCount} children found")
                if (snapshot.exists()) {
                    val childNode = snapshot.children.first()
                    val childId = childNode.key ?: return
                    val childData = childNode.getValue(UserData::class.java)
                    Log.d(TAG, "Child found: ID=$childId, Data=$childData")

                    if (childData?.role != "child") {
                        _linkStatus.value = "User is not a child account"
                        Log.e(TAG, "User is not a child account")
                        return
                    }

                    // Generate a verification code
                    val verificationCode = Random.nextInt(100000, 999999).toString()
                    Log.d(TAG, "Generated verification code: $verificationCode")
                    // Store linking request in child's node
                    usersRef.child(childId).child("linkingRequests").child(parentId).setValue(
                        mapOf("verificationCode" to verificationCode, "parentEmail" to parentEmail)
                    ).addOnCompleteListener {
                        _linkStatus.value = "Linking request sent. Verification code: $verificationCode"
                        Log.d(TAG, "Linking request sent successfully")
                    }.addOnFailureListener {
                        _linkStatus.value = "Failed to send linking request: ${it.message}"
                        Log.e(TAG, "Failed to send linking request: ${it.message}")
                    }
                } else {
                    _linkStatus.value = "Child email not found"
                    Log.e(TAG, "Child email not found: $childEmail")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                _linkStatus.value = "Error querying child: ${error.message}"
                Log.e(TAG, "Query cancelled: ${error.message}")
            }
        })
    }

    // Child listens for pending linking requests
    fun listenForLinkRequests() {
        val childId = auth.currentUser?.uid ?: return
        Log.d(TAG, "Listening for linking requests for child: $childId")
        usersRef.child(childId).child("linkingRequests").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val requests = mutableListOf<Pair<String, String>>()
                for (child in snapshot.children) {
                    val parentId = child.key ?: continue
                    val verificationCode = child.child("verificationCode").getValue(String::class.java) ?: continue
                    requests.add(Pair(parentId, verificationCode))
                    Log.d(TAG, "Found linking request from parent: $parentId, code: $verificationCode")
                }
                _pendingRequests.value = requests
                if (requests.isEmpty()) {
                    Log.d(TAG, "No pending linking requests")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                _linkStatus.value = "Error fetching requests: ${error.message}"
                Log.e(TAG, "Error fetching requests: ${error.message}")
            }
        })
    }

    // Child accepts a linking request
    fun acceptLinkRequest(parentId: String, enteredCode: String) {
        val childId = auth.currentUser?.uid ?: return
        Log.d(TAG, "Child $childId attempting to accept request from parent $parentId with code $enteredCode")
        usersRef.child(childId).child("linkingRequests").child(parentId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val verificationCode = snapshot.child("verificationCode").getValue(String::class.java)
                if (verificationCode == enteredCode) {
                    // Link accounts
                    usersRef.child(childId).child("linkedAccounts").child("parentId").setValue(parentId)
                    usersRef.child(parentId).child("linkedAccounts").child("childId").setValue(childId)
                    // Remove linking request
                    usersRef.child(childId).child("linkingRequests").child(parentId).removeValue()
                    _linkStatus.value = "Accounts linked successfully"
                    Log.d(TAG, "Accounts linked successfully")
                } else {
                    _linkStatus.value = "Invalid verification code"
                    Log.e(TAG, "Invalid verification code")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                _linkStatus.value = "Error: ${error.message}"
                Log.e(TAG, "Error accepting request: ${error.message}")
            }
        })
    }

    companion object {
        const val TAG = "LinkViewModel"
    }
}

data class UserData(
    val email: String = "",
    val role: String = ""
)