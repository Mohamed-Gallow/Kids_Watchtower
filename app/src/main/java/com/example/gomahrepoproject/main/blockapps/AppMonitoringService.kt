package com.example.gomahrepoproject.main.blockapps

import android.app.Service
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class AppMonitoringService : Service() {

    private val handler = Handler()
    private val checkInterval: Long = 2000 // 2 seconds
    private lateinit var database: DatabaseReference
    private val auth = FirebaseAuth.getInstance()
    private var childId: String? = null

    override fun onCreate() {
        super.onCreate()
        database = FirebaseDatabase.getInstance().reference
        fetchChildIdAndStartMonitoring()
    }

    private fun fetchChildIdAndStartMonitoring() {
        val userId = auth.currentUser?.uid ?: return
        val userRef = database.child("users").child(userId)

        userRef.child("role").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                when (snapshot.getValue(String::class.java)) {
                    "child" -> {
                        childId = userId
                        startMonitoring()
                    }
                    "parent" -> {
                        userRef.child("linkedAccounts").child("childId")
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(snapshot: DataSnapshot) {
                                    childId = snapshot.getValue(String::class.java)
                                    startMonitoring()
                                }

                                override fun onCancelled(error: DatabaseError) {}
                            })
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun startMonitoring() {
        handler.post(object : Runnable {
            override fun run() {
                val foregroundApp = getForegroundApp()
                checkBlockedApp(foregroundApp)
                handler.postDelayed(this, checkInterval)
            }
        })
    }

    private fun getForegroundApp(): String? {
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val endTime = System.currentTimeMillis()
        val startTime = endTime - 5000

        val usageStatsList = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY, startTime, endTime
        )

        var currentApp: UsageStats? = null
        for (usageStats in usageStatsList) {
            if (currentApp == null || usageStats.lastTimeUsed > currentApp.lastTimeUsed) {
                currentApp = usageStats
            }
        }

        return currentApp?.packageName
    }

    private fun checkBlockedApp(packageName: String?) {
        if (packageName == null || childId == null) return

        database.child("users").child(childId!!).child("blockedApps")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val blockedApps = snapshot.children.mapNotNull {
                        it.getValue(String::class.java)
                    }

                    if (blockedApps.contains(packageName)) {
                        showBlockScreen(packageName)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("AppMonitoringService", "Error reading blocked apps: ${error.message}")
                }
            })
    }

    private fun showBlockScreen(blockedApp: String) {
        val intent = Intent(this, BlockedAppActivity::class.java)
        intent.putExtra("BLOCKED_APP_NAME", blockedApp)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
