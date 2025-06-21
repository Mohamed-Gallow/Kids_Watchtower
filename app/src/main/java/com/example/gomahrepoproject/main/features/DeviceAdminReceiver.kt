package com.example.gomahrepoproject.main.features



import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class DeviceAdminReceiver : DeviceAdminReceiver() {
    override fun onEnabled(context: Context, intent: Intent) {
        Toast.makeText(context, "تم تفعيل إدارة الجهاز", Toast.LENGTH_SHORT).show()
    }

    override fun onDisabled(context: Context, intent: Intent) {
        Toast.makeText(context, "تم تعطيل إدارة الجهاز", Toast.LENGTH_SHORT).show()
    }
}