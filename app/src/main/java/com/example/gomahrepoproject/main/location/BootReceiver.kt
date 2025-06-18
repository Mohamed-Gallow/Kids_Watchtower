package com.example.gomahrepoproject.main.location

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.example.gomahrepoproject.main.AppTimeRangeBlocker.TimeRangeMonitorService

class BootReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val user = FirebaseAuth.getInstance().currentUser
            if (user == null || !user.isEmailVerified) {
                Log.e(TAG, "No authenticated user, skipping service start")
                return
            }

            val userId = user.uid
            FirebaseDatabase.getInstance().getReference("users").child(userId).child("role")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val role = snapshot.getValue(String::class.java)
                        if (role == "child") {
                            val locIntent = Intent(context, LocationService::class.java)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                context.startForegroundService(locIntent)
                            } else {
                                context.startService(locIntent)
                            }
                            val rangeIntent = Intent(context, TimeRangeMonitorService::class.java)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                context.startForegroundService(rangeIntent)
                            } else {
                                context.startService(rangeIntent)
                            }
                            Log.d(TAG, "Started LocationService and TimeRangeMonitorService after boot")
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {
                        Log.e(TAG, "Error fetching role: ${error.message}")
                    }
                })
        }
    }
}