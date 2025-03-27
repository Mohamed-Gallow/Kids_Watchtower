package com.example.gomahrepoproject.main.blockapps

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.example.gomahrepoproject.R

class BlockedAppActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_blocked_app)

        val blockedAppName = intent.getStringExtra("BLOCKED_APP_NAME") ?: "This app"
        val textView = findViewById<TextView>(R.id.blockedAppText)
        val closeButton = findViewById<Button>(R.id.btnClose)

        textView.text = "$blockedAppName is blocked!"

        closeButton.setOnClickListener {
            closeApp()
        }
    }

    private fun closeApp() {
        val homeIntent = Intent(Intent.ACTION_MAIN)
        homeIntent.addCategory(Intent.CATEGORY_HOME)
        homeIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(homeIntent)
        finish()
    }
}
