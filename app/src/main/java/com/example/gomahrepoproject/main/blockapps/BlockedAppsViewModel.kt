package com.example.gomahrepoproject.main.blockapps

import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.gomahrepoproject.main.data.repository.FirebaseRepository

class BlockedAppsViewModel(application: Application) : AndroidViewModel(application) {


    private val repository = FirebaseRepository()
    private val _blockedApps = MutableLiveData<List<String>>()
    val blockedApps: LiveData<List<String>> get() = _blockedApps

    private lateinit var appUsageTracker: AppUsageTracker
    private val handler = Handler(Looper.getMainLooper())

    fun initialize(context: Context) {
        appUsageTracker = AppUsageTracker(context)
        if (!appUsageTracker.hasUsageAccessPermission()) {
            appUsageTracker.requestUsageAccessPermission()
        }
    }

    fun loadBlockedApps(userId: String, childId: String) {
        repository.getBlockedApps(userId, childId) { apps ->
            _blockedApps.postValue(apps)
        }
    }

    fun startMonitoringApps(onBlockedAppDetected: (String) -> Unit) {
        handler.post(object : Runnable {
            override fun run() {
                val foregroundApp = appUsageTracker.getForegroundApp()
                if (foregroundApp != null && _blockedApps.value?.contains(foregroundApp) == true) {
                    onBlockedAppDetected(foregroundApp)
                }
                handler.postDelayed(this, 1000) // Check every second
            }
        })
    }
}
