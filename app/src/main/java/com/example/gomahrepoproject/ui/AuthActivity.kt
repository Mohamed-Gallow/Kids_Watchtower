package com.example.gomahrepoproject.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.gomahrepoproject.R
import com.example.gomahrepoproject.databinding.ActivityAuthBinding

class AuthActivity : AppCompatActivity() {
    private lateinit var _binding : ActivityAuthBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)

    }
}