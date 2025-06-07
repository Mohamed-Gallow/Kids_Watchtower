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
    private val checkInterval: Long = 2000
    private lateinit var database: DatabaseReference
    private val auth = FirebaseAuth.getInstance()
    private var childId: String? = null
    private var blockedPackages: Set<String> = emptySet()

    override fun onCreate() {
        super.onCreate()
        database = FirebaseDatabase.getInstance().reference
        val userId = auth.currentUser?.uid ?: return

        val userRef = database.child("users").child(userId)
        userRef.child("role").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.getValue(String::class.java) == "child") {
                    childId = userId
                    listenForBlockedApps()
                    startMonitoring()
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun listenForBlockedApps() {
        val cid = childId ?: return
        database.child("users").child(cid).child("blockedApps")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    blockedPackages = snapshot.children.mapNotNull {
                        it.getValue(String::class.java)
                    }.toSet()
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("AppMonitoringService", "Failed to read blocked apps: ${error.message}")
                }
            })
    }

    private fun startMonitoring() {
        handler.post(object : Runnable {
            override fun run() {
                val foregroundApp = getForegroundApp()
                if (foregroundApp in blockedPackages) {
                    showBlockScreen(foregroundApp!!)
                }
                handler.postDelayed(this, checkInterval)
            }
        })
    }

    private fun getForegroundApp(): String? {
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val endTime = System.currentTimeMillis()
        val startTime = endTime - 5000

        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY, startTime, endTime
        )

        return stats.maxByOrNull { it.lastTimeUsed }?.packageName
    }

    private fun showBlockScreen(blockedApp: String) {
        val intent = Intent(this, BlockedAppActivity::class.java)
        intent.putExtra("BLOCKED_APP_NAME", blockedApp)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
    }


    override fun onBind(intent: Intent?): IBinder? = null
}
