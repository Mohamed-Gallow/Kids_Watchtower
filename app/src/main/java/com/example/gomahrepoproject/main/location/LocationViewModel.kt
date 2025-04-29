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
    private val firebaseRef =
        userId?.let { FirebaseDatabase.getInstance().getReference("current_location").child(it) }

    private var _currentLocation = MutableLiveData<List<LocationModel>>()
    val currentLocation: LiveData<List<LocationModel>> get() = _currentLocation


     fun sendCurrentLocationToFirebase(location: LocationModel) {
        if (firebaseRef == null) return

        firebaseRef.push().setValue(location).addOnCompleteListener {
            Log.e(TAG, "sendCurrentLocationToFirebase:Success $it ")
        }.addOnFailureListener {
            Log.e(TAG, "sendCurrentLocationToFirebase: Fail $it")
        }

    }

     fun loadCurrentLocationFromFirebase() {
        if (firebaseRef == null) return

        firebaseRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val locationList = mutableListOf<LocationModel>()

                for (child in snapshot.children) {
                    val location = child.getValue(LocationModel::class.java)
                    if (location != null) {
                        locationList.add(location)
                    }
                }

                _currentLocation.value = locationList
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "onCancelled: error is $error")
            }
        })

    }

    companion object {
        const val TAG = "LocationViewModel"
    }

}