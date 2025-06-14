package com.example.gomahrepoproject.main.blockapps

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class AppMonitoringService : AccessibilityService() {

    private val auth = FirebaseAuth.getInstance()
    private var childId: String? = null
    private var blockedPackages: Set<String> = emptySet()
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

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val packageName = event?.packageName?.toString() ?: return
        Log.d("AppMonitor", "Detected app: $packageName")

        if (blockedPackages.contains(packageName)) {
            Log.d("AppMonitor", "BLOCKED app launched: $packageName")
            launchBlockScreen(packageName)
        }
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
