package com.example.gomahrepoproject.main.blockapps

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.gomahrepoproject.main.data.AppModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class BlockedAppsViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference

    private var childId: String? = null

    private val _blockedApps = MutableLiveData<List<AppModel>>()
    val blockedApps: LiveData<List<AppModel>> = _blockedApps

    private val _unblockedApps = MutableLiveData<List<AppModel>>()
    val unblockedApps: LiveData<List<AppModel>> = _unblockedApps

    init {
        fetchLinkedChildAndApps()
    }

    private fun fetchLinkedChildAndApps() {
        val parentId = auth.currentUser?.uid ?: return
        database.child("users").child(parentId).child("linkedAccounts").child("childId")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val linkedChildId = snapshot.getValue(String::class.java)
                    if (linkedChildId != null) {
                        childId = linkedChildId
                        fetchInstalledApps(linkedChildId)
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun fetchInstalledApps(childId: String) {
        val appsRef = database.child("users").child(childId).child("installedApps")
        val blockedRef = database.child("users").child(childId).child("blockedApps")

        appsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                blockedRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(blockedSnapshot: DataSnapshot) {
                        val blockedSet = blockedSnapshot.children
                            .filter { it.getValue(Boolean::class.java) == true }
                            .mapNotNull { it.key?.replace(",", ".") }
                            .toSet()

                        val allApps = snapshot.children.mapNotNull { appSnap ->
                            val app = appSnap.getValue(AppModel::class.java)
                            app?.copy(isBlocked = blockedSet.contains(app.packageName))
                        }

                        _blockedApps.value = allApps.filter { it.isBlocked }
                        _unblockedApps.value = allApps.filter { !it.isBlocked }
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    fun blockApp(packageName: String) {
        val child = childId ?: return
        val safeName = packageName.replace(".", ",")
        database.child("users").child(child).child("blockedApps").child(safeName).setValue(true)
    }

    fun unblockApp(packageName: String) {
        val child = childId ?: return
        val safeName = packageName.replace(".", ",")
        database.child("users").child(child).child("blockedApps").child(safeName).removeValue()
    }
}
