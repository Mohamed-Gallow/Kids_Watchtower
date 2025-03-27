package com.example.gomahrepoproject.main.blockapps

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.util.Log
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import com.google.firebase.database.*

class AppMonitoringService : Service() {

    private lateinit var database: DatabaseReference
    private val handler = Handler()
    private val checkInterval: Long = 2000 // Check every 2 seconds

    override fun onCreate() {
        super.onCreate()
        database = FirebaseDatabase.getInstance().getReference("blocked_apps")

        startMonitoring()
    }

    private fun startMonitoring() {
        handler.post(object : Runnable {
            override fun run() {
                val foregroundApp = getForegroundApp()
                Log.d("AppMonitoringService", "Current App: $foregroundApp")

                checkBlockedApp(foregroundApp)
                handler.postDelayed(this, checkInterval)
            }
        })
    }

    private fun getForegroundApp(): String? {
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val time = System.currentTimeMillis()
        val usageStatsList = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY, time - 1000 * 10, time
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
        val userId = "USER_ID" // Replace with actual user ID
        val childId = "CHILD_ID" // Replace with actual child ID

        database.child(userId).child(childId).addListenerForSingleValueEvent(object :
            ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val blockedApps = snapshot.children.mapNotNull { it.getValue(String::class.java) }

                if (packageName in blockedApps) {
                    Log.d("AppMonitoringService", "Blocked app detected: $packageName")
                    showBlockScreen(packageName ?: "Unknown App")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("AppMonitoringService", "Error checking blocked apps: ${error.message}")
            }
        })
    }

    private fun showBlockScreen(blockedApp: String) {
        val intent = Intent(this, BlockedAppActivity::class.java)
        intent.putExtra("BLOCKED_APP_NAME", blockedApp)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
