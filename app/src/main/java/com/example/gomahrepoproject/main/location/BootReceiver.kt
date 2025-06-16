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
                            val serviceIntent = Intent(context, LocationService::class.java)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                context.startForegroundService(serviceIntent)
                            } else {
                                context.startService(serviceIntent)
                            }
                            Log.d(TAG, "Started LocationService after boot")
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {
                        Log.e(TAG, "Error fetching role: ${error.message}")
                    }
                })
        }
    }
}