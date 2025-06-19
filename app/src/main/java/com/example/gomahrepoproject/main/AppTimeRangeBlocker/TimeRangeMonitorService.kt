package com.example.gomahrepoproject.main.AppTimeRangeBlocker

import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import com.example.gomahrepoproject.main.blockapps.AppMonitoringService
import com.example.gomahrepoproject.main.blockapps.AppUsageTracker
import com.example.gomahrepoproject.main.blockapps.BlockingOverlay
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.util.Calendar

/**
 * Service that periodically checks the foreground application and blocks apps
 * that are launched outside of their allowed time range.
 */
class TimeRangeMonitorService : Service() {

    private val usageTracker by lazy { AppUsageTracker(this) }
    private val handler = Handler(Looper.getMainLooper())
    private val checkRunnable = object : Runnable {
        override fun run() {
            checkForegroundApp()
            handler.postDelayed(this, 5000)
        }
    }

    private var monitoredApps: List<AppTimeRange> = emptyList()
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance().reference
    private var listener: ValueEventListener? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        monitoredApps = intent?.getSerializableExtra("APP_LIST") as? ArrayList<AppTimeRange>
            ?: emptyList()

        ensurePermissions()
        listenForRules()
        handler.post(checkRunnable)
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(checkRunnable)
        listener?.let {
            val childId = auth.currentUser?.uid ?: return
            db.child("users").child(childId).child("timeRangeRules").removeEventListener(it)
        }
        if (BlockingOverlay.isShowing) {
            BlockingOverlay.hide()
        }
        super.onDestroy()
    }

    private fun checkForegroundApp() {
        val pkg = usageTracker.getForegroundApp() ?: return
        val appRange = monitoredApps.firstOrNull { it.packageName == pkg }
        if (appRange != null && !isWithinAllowedTime(appRange)) {
            val msg = "${appRange.appName} is allowed only between " +
                    timeToString(appRange.startHour, appRange.startMinute) + " and " +
                    timeToString(appRange.endHour, appRange.endMinute)
            BlockingOverlay.show(this, msg)
        } else {
            if (BlockingOverlay.isShowing) {
                BlockingOverlay.hide()
            }
        }
    }

    private fun listenForRules() {
        val childId = auth.currentUser?.uid ?: return
        listener?.let { db.child("users").child(childId).child("timeRangeRules").removeEventListener(it) }
        val l = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                monitoredApps = snapshot.children.mapNotNull { it.getValue(AppTimeRange::class.java) }
            }

            override fun onCancelled(error: DatabaseError) {}
        }
        db.child("users").child(childId).child("timeRangeRules").addValueEventListener(l)
        listener = l
    }

    private fun ensurePermissions() {
        if (!usageTracker.hasUsageAccessPermission()) {
            usageTracker.requestUsageAccessPermission()
        }
        if (!isAccessibilityServiceEnabled()) {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
            Toast.makeText(
                this,
                "Please enable Accessibility Service for monitoring",
                Toast.LENGTH_LONG
            ).show()
        }
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabledServices.contains(
            "$packageName/${AppMonitoringService::class.java.name}"
        )
    }

    private fun isWithinAllowedTime(range: AppTimeRange): Boolean {
        val now = Calendar.getInstance()
        val start = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, range.startHour)
            set(Calendar.MINUTE, range.startMinute)
            set(Calendar.SECOND, 0)
        }
        val end = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, range.endHour)
            set(Calendar.MINUTE, range.endMinute)
            set(Calendar.SECOND, 0)
        }
        return now.after(start) && now.before(end)
    }

    private fun timeToString(h: Int, m: Int): String = String.format("%02d:%02d", h, m)
}
