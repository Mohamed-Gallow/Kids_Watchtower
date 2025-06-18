package com.example.gomahrepoproject.main.AppTimeRangeBlocker

import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import com.example.gomahrepoproject.main.blockapps.AppUsageTracker
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
            ?: monitoredApps

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
        super.onDestroy()
    }

    private fun checkForegroundApp() {
        val pkg = usageTracker.getForegroundApp() ?: return
        val appRange = monitoredApps.firstOrNull { it.packageName == pkg } ?: return
        if (!isWithinAllowedTime(appRange)) {
            val overlay = Intent(this, TimeRangeBlockedActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra("APP_NAME", appRange.appName)
                putExtra("START_TIME", timeToString(appRange.startHour, appRange.startMinute))
                putExtra("END_TIME", timeToString(appRange.endHour, appRange.endMinute))
            }
            startActivity(overlay)
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
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }
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
