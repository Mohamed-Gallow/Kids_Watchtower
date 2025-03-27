package com.example.gomahrepoproject.main.blockapps

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.database.*

class BlockedAppsViewModel(application: Application) : AndroidViewModel(application) {

    private lateinit var database: DatabaseReference


    private val _blockedApps = MutableLiveData<List<String>>()
    val blockedApps: LiveData<List<String>> = _blockedApps

    fun initialize(context: Context) {
        database = FirebaseDatabase.getInstance().getReference("blocked_apps")
    }

    fun loadBlockedApps(userId: String, childId: String) {
        database.child(userId).child(childId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val appsList = snapshot.children.mapNotNull { it.getValue(String::class.java) }
                _blockedApps.value = appsList
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle possible errors
            }
        })
    }

    fun addBlockedApp(userId: String, childId: String, appPackage: String) {
        database.child(userId).child(childId).push().setValue(appPackage)
    }
    fun startMonitoringApps(onAppDetected: (String) -> Unit) {
        // Simulate monitoring logic, should be implemented with real logic
        onAppDetected("com.example.blockedapp")
    }

}
