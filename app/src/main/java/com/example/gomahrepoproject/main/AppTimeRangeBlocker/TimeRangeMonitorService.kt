package com.example.gomahrepoproject.main.AppTimeRangeBlocker

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import com.example.gomahrepoproject.main.blockapps.AppUsageTracker
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

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        monitoredApps = intent?.let {
            val appName = it.getStringExtra("APP_NAME") ?: "YouTube"
            val pkg = it.getStringExtra("PACKAGE_NAME") ?: "com.google.android.youtube"
            val sh = it.getIntExtra("START_HOUR", 9)
            val sm = it.getIntExtra("START_MIN", 0)
            val eh = it.getIntExtra("END_HOUR", 17)
            val em = it.getIntExtra("END_MIN", 0)
            listOf(AppTimeRange(appName, pkg, sh, sm, eh, em))
        } ?: listOf(AppTimeRange("YouTube", "com.google.android.youtube", 9, 0, 17, 0))

        handler.post(checkRunnable)
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(checkRunnable)
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
