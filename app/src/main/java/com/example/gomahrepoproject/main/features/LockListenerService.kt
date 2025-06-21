
package com.example.gomahrepoproject.main.features

import android.app.Service
import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.IBinder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
class LockListenerService : Service() {

    private lateinit var databaseRef: DatabaseReference
    private lateinit var listener: ValueEventListener

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            stopSelf()
            return START_NOT_STICKY
        }


        databaseRef = FirebaseDatabase.getInstance().getReference("users/$userId/deviceStatus")

        listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val isLocked = snapshot.child("isLocked").getValue(Boolean::class.java) ?: false
                if (isLocked) {
                    lockDeviceNow()
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        }

        databaseRef.addValueEventListener(listener)

        return START_STICKY
    }

    private fun lockDeviceNow() {
        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val component = ComponentName(this, DeviceAdminReceiver::class.java)

        if (dpm.isAdminActive(component)) {
            dpm.lockNow()
        }
    }

    override fun onDestroy() {
        databaseRef.removeEventListener(listener)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}