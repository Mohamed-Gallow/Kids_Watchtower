package com.example.gomahrepoproject.main.blockapps

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import com.example.gomahrepoproject.R

class BlockedAppActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_blocked_app)

        val appName = intent.getStringExtra("BLOCKED_APP_NAME") ?: "Unknown App"
        findViewById<TextView>(R.id.blockedAppText).text = "Access to \"$appName\" is blocked by your parent."
    }

    override fun onBackPressed() {
        // Prevent going back to the blocked app
    }
}