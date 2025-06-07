package com.example.gomahrepoproject.main.blockapps

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.example.gomahrepoproject.R

class BlockedAppActivity : ComponentActivity() {

    private lateinit var handler: Handler
    private lateinit var runnable: Runnable
    private var blockedPackageName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_blocked_app)

        val blockedAppName = intent.getStringExtra("BLOCKED_APP_NAME") ?: "This app"
        blockedPackageName = intent.getStringExtra("BLOCKED_PACKAGE_NAME")

        val textView = findViewById<TextView>(R.id.blockedAppText)
        val closeButton = findViewById<Button>(R.id.btnClose)

        textView.text = "$blockedAppName is blocked!"

        closeButton.setOnClickListener {
            closeApp()
        }

        // Start monitoring to prevent reopening the blocked app
        handler = Handler(Looper.getMainLooper())
        runnable = object : Runnable {
            override fun run() {
                if (isBlockedAppInForeground()) {
                    restartActivity()  // Bring this screen again
                }
                handler.postDelayed(this, 1000)
            }
        }
        handler.post(runnable)
    }

    private fun isBlockedAppInForeground(): Boolean {
        val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val tasks = am.runningAppProcesses
        val blockedPackage = blockedPackageName ?: return false

        return tasks.any {
            it.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND &&
                    it.processName == blockedPackage
        }
    }

    private fun restartActivity() {
        val intent = Intent(this, BlockedAppActivity::class.java)
        intent.putExtra("BLOCKED_APP_NAME", intent.getStringExtra("BLOCKED_APP_NAME"))
        intent.putExtra("BLOCKED_PACKAGE_NAME", blockedPackageName)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    private fun closeApp() {
        val homeIntent = Intent(Intent.ACTION_MAIN)
        homeIntent.addCategory(Intent.CATEGORY_HOME)
        homeIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(homeIntent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(runnable)
    }
}
