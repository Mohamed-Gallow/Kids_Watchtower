package com.example.gomahrepoproject.main.blockapps

import android.app.Application
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

data class AppModel(
    val appName: String = "",
    val packageName: String = "",
    var isBlocked: Boolean = false
)

class BlockedAppsViewModel(application: Application) : AndroidViewModel(application) {

    private val firebaseAuth = FirebaseAuth.getInstance()
    private val databaseRef = FirebaseDatabase.getInstance().reference
    private var currentUserId: String? = null
    private var childId: String? = null

    private val _blockedApps = MutableLiveData<List<AppModel>>()
    val blockedApps: LiveData<List<AppModel>> get() = _blockedApps

    private val _unblockedApps = MutableLiveData<List<AppModel>>()
    val unblockedApps: LiveData<List<AppModel>> get() = _unblockedApps

    init {
        currentUserId = firebaseAuth.currentUser?.uid
        fetchRoleAndLoadData()
    }

    private fun fetchRoleAndLoadData() {
        currentUserId?.let { userId ->
            databaseRef.child("users").child(userId).child("role")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        when (snapshot.getValue(String::class.java)) {
                            "child" -> {
                                childId = userId
                                uploadInstalledApps()
                                listenForBlockedApps()
                            }
                            "parent" -> {
                                databaseRef.child("users").child(userId).child("linkedAccounts").child("childId")
                                    .addListenerForSingleValueEvent(object : ValueEventListener {
                                        override fun onDataChange(childSnapshot: DataSnapshot) {
                                            childId = childSnapshot.getValue(String::class.java)
                                            childId?.let { listenForBlockedApps() }
                                        }

                                        override fun onCancelled(error: DatabaseError) {}
                                    })
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })
        }
    }

    private fun uploadInstalledApps() {
        val pm = getApplication<Application>().packageManager
        val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        val appList = packages.map {
            AppModel(
                appName = it.loadLabel(pm).toString(),
                packageName = it.packageName
            )
        }
        childId?.let { id ->
            databaseRef.child("users").child(id).child("installedApps").setValue(appList)
        }
    }

    private fun listenForBlockedApps() {
        childId?.let { id ->
            val appsRef = databaseRef.child("users").child(id).child("installedApps")
            val blockedRef = databaseRef.child("users").child(id).child("blockedApps")

            appsRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(appsSnapshot: DataSnapshot) {
                    val appList = mutableListOf<AppModel>()
                    for (appSnap in appsSnapshot.children) {
                        val app = appSnap.getValue(AppModel::class.java)
                        app?.let { appList.add(it) }
                    }

                    blockedRef.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(blockedSnapshot: DataSnapshot) {
                            val blockedPackages = blockedSnapshot.children.mapNotNull { it.getValue(String::class.java) }
                            val blocked = mutableListOf<AppModel>()
                            val unblocked = mutableListOf<AppModel>()

                            for (app in appList) {
                                if (blockedPackages.contains(app.packageName)) {
                                    app.isBlocked = true
                                    blocked.add(app)
                                } else {
                                    app.isBlocked = false
                                    unblocked.add(app)
                                }
                            }

                            _blockedApps.postValue(blocked)
                            _unblockedApps.postValue(unblocked)
                        }

                        override fun onCancelled(error: DatabaseError) {}
                    })
                }

                override fun onCancelled(error: DatabaseError) {}
            })
        }
    }

    fun blockApp(packageName: String) {
        childId?.let { id ->
            val ref = databaseRef.child("users").child(id).child("blockedApps")
            ref.orderByValue().equalTo(packageName)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (!snapshot.exists()) {
                            ref.push().setValue(packageName)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })
        }
    }

    fun unblockApp(packageName: String) {
        childId?.let { id ->
            val ref = databaseRef.child("users").child(id).child("blockedApps")
            ref.orderByValue().equalTo(packageName)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        for (child in snapshot.children) {
                            child.ref.removeValue()
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })
        }
    }
}
