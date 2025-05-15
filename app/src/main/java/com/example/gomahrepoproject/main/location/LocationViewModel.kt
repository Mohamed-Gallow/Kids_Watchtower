package com.example.gomahrepoproject.main.location

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

class LocationViewModel(application: Application) : AndroidViewModel(application) {
    private val userId = FirebaseAuth.getInstance().currentUser?.uid
    private val database = FirebaseDatabase.getInstance()
    private val usersRef = database.getReference("users")
    private val locationsRef = database.getReference("locations")

    private var _childLocation = MutableLiveData<LocationModel>()
    val childLocation: LiveData<LocationModel> get() = _childLocation

    private var _isSharingLocation = MutableLiveData<Boolean>()
    val isSharingLocation: LiveData<Boolean> get() = _isSharingLocation

    fun sendCurrentLocationToFirebase(location: LocationModel) {
        if (userId == null) return
        locationsRef.child(userId).setValue(location).addOnCompleteListener {
            Log.d(TAG, "Location sent successfully")
        }.addOnFailureListener {
            Log.e(TAG, "Failed to send location: ${it.message}")
        }
    }

    fun listenForChildLocation(childId: String) {
        locationsRef.child(childId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val location = snapshot.getValue(LocationModel::class.java)
                if (location != null) {
                    _childLocation.value = location!!
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error fetching child location: ${error.message}")
            }
        })
    }

    fun requestChildLocationSharing(childId: String) {
        if (userId == null) return
        usersRef.child(childId).child("locationSharing").setValue(
            mapOf(
                "isSharing" to true,
                "targetUserId" to userId
            )
        ).addOnCompleteListener {
            Log.d(TAG, "Location sharing requested")
        }.addOnFailureListener {
            Log.e(TAG, "Failed to request location sharing: ${it.message}")
        }
    }

    fun stopChildLocationSharing(childId: String) {
        usersRef.child(childId).child("locationSharing").setValue(
            mapOf(
                "isSharing" to false,
                "targetUserId" to ""
            )
        ).addOnCompleteListener {
            Log.d(TAG, "Location sharing stopped")
        }
    }

    fun listenForLocationSharing() {
        if (userId == null) return
        usersRef.child(userId).child("locationSharing").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val isSharing = snapshot.child("isSharing").getValue(Boolean::class.java) ?: false
                _isSharingLocation.value = isSharing
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error checking location sharing: ${error.message}")
            }
        })
    }

    companion object {
        const val TAG = "LocationViewModel"
    }
}