package com.example.gomahrepoproject.main.AppTimeRangeBlocker

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class TimeRangeViewModel(application: Application) : AndroidViewModel(application) {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance().reference

    private val _rules = MutableLiveData<List<AppTimeRange>>()
    val rules: LiveData<List<AppTimeRange>> = _rules

    fun saveRulesToChild(ranges: List<AppTimeRange>) {
        val parentId = auth.currentUser?.uid ?: return
        db.child("users").child(parentId).child("linkedAccounts").child("childId")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val childId = snapshot.getValue(String::class.java) ?: return
                    val childRef = db.child("users").child(childId).child("timeRangeRules")
                    val map = ranges.associate { range ->
                        val key = range.packageName.replace('.', ',')
                        key to range
                    }
                    childRef.setValue(map)
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    fun listenForRules() {
        val childId = auth.currentUser?.uid ?: return
        db.child("users").child(childId).child("timeRangeRules")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val list = snapshot.children.mapNotNull { it.getValue(AppTimeRange::class.java) }
                    _rules.value = list
                    startMonitorService(list)
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun startMonitorService(list: List<AppTimeRange>) {
        if (list.isEmpty()) return
        val intent = Intent(getApplication(), TimeRangeMonitorService::class.java).apply {
            putExtra("APP_LIST", ArrayList(list))
        }
        getApplication<Application>().startService(intent)
    }
}
