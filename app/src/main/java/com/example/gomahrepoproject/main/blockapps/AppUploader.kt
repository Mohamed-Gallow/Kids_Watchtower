package com.example.gomahrepoproject.main.blockapps

import android.content.Context
import android.content.pm.PackageManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

object AppUploader {
    fun uploadInstalledAppsToFirebase(context: Context) {
        val packageManager = context.packageManager
        val packages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)

        val appsList = mutableListOf<Map<String, Any>>()

        for (appInfo in packages) {
            val appName = packageManager.getApplicationLabel(appInfo).toString()
            val packageName = appInfo.packageName

            // تجاهل التطبيقات غير القابلة للتشغيل
            if (packageManager.getLaunchIntentForPackage(packageName) == null) continue

            val appData = mapOf(
                "appName" to appName,
                "packageName" to packageName,
                "isBlocked" to false
            )

            appsList.add(appData)
        }

        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseDatabase.getInstance().reference
            .child("users")
            .child(userId)
            .child("installedApps")
            .setValue(appsList)
    }
}
