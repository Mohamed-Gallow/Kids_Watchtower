package com.example.gomahrepoproject.main.AppTimeRangeBlocker

import android.app.Activity
import android.os.Bundle
import android.widget.TextView
import com.example.gomahrepoproject.R

/**
 * Simple overlay shown when an app is launched outside its allowed time range.
 */
class TimeRangeBlockedActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_time_range_blocked)

        val appName = intent.getStringExtra("APP_NAME") ?: "This app"
        val start = intent.getStringExtra("START_TIME") ?: ""
        val end = intent.getStringExtra("END_TIME") ?: ""
        findViewById<TextView>(R.id.blockedAppTimeText).text =
            "$appName is allowed only between $start and $end."
    }

    override fun onBackPressed() {
        // Prevent navigating back to the blocked application
    }
}
