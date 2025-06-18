package com.example.gomahrepoproject.main.blockapps

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.example.gomahrepoproject.main.AppTimeRangeBlocker.AppTimeRange
import com.example.gomahrepoproject.main.AppTimeRangeBlocker.TimeRangeBlockedActivity
import com.example.gomahrepoproject.main.blockapps.AppUploader
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class AppMonitoringService : AccessibilityService() {

    private val auth = FirebaseAuth.getInstance()
    private var childId: String? = null
    private var blockedPackages: Set<String> = emptySet()
    private var timeRangeRules: List<AppTimeRange> = emptyList()
    private lateinit var database: DatabaseReference

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d("AppMonitor", "Accessibility Service connected")

        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            packageNames = null  // Monitor all apps
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 100
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
        }
        serviceInfo = info

        val userId = auth.currentUser?.uid ?: return
        database = FirebaseDatabase.getInstance().reference

        // Only run on child devices
        database.child("users").child(userId).child("role")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.getValue(String::class.java) == "child") {
                        childId = userId
                        listenForBlockedApps()
                        listenForTimeRangeRules()
                        AppUploader.uploadInstalledAppsToFirebase(this@AppMonitoringService)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("AppMonitoringService", "Role check failed: ${error.message}")
                }
            })
    }

    private fun listenForBlockedApps() {
        val cid = childId ?: return
        database.child("users").child(cid).child("blockedApps")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    blockedPackages = snapshot.children.mapNotNull { child ->
                        val value = child.getValue()
                        if (value is Boolean && value) {
                            child.key?.replace(",", ".")
                        } else {
                            null
                        }
                    }.toSet()

                    Log.d("AppMonitor", "Blocked packages updated: $blockedPackages")
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("AppMonitoringService", "Failed to fetch blocked apps: ${error.message}")
                }
            })
    }

    private fun listenForTimeRangeRules() {
        val cid = childId ?: return
        database.child("users").child(cid).child("timeRangeRules")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    timeRangeRules = snapshot.children.mapNotNull { child ->
                        child.getValue(AppTimeRange::class.java)
                    }
                    Log.d("AppMonitor", "Time range rules updated: $timeRangeRules")
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("AppMonitoringService", "Failed to fetch time ranges: ${error.message}")
                }
            })
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val packageName = event?.packageName?.toString() ?: return
        Log.d("AppMonitor", "Detected app: $packageName")

        if (blockedPackages.contains(packageName)) {
            Log.d("AppMonitor", "BLOCKED app launched: $packageName")
            launchBlockScreen(packageName)
            return
        }

        val rule = timeRangeRules.firstOrNull { it.packageName == packageName }
        if (rule != null && !isWithinAllowedTime(rule)) {
            Log.d("AppMonitor", "App $packageName outside allowed time range")
            launchTimeRangeScreen(rule)
        }
    }

    private fun isWithinAllowedTime(range: AppTimeRange): Boolean {
        val now = java.util.Calendar.getInstance()
        val start = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, range.startHour)
            set(java.util.Calendar.MINUTE, range.startMinute)
            set(java.util.Calendar.SECOND, 0)
        }
        val end = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, range.endHour)
            set(java.util.Calendar.MINUTE, range.endMinute)
            set(java.util.Calendar.SECOND, 0)
        }
        return now.after(start) && now.before(end)
    }

    private fun launchTimeRangeScreen(rule: AppTimeRange) {
        val intent = Intent(this, TimeRangeBlockedActivity::class.java).apply {
            putExtra("APP_NAME", rule.appName)
            putExtra("START_TIME", String.format("%02d:%02d", rule.startHour, rule.startMinute))
            putExtra("END_TIME", String.format("%02d:%02d", rule.endHour, rule.endMinute))
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        startActivity(intent)
    }

    private fun launchBlockScreen(blockedApp: String) {
        val intent = Intent(this, BlockedAppActivity::class.java)
        intent.putExtra("BLOCKED_APP_NAME", blockedApp)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
    }

    override fun onInterrupt() {
        // Required method, can be left empty
    }
}