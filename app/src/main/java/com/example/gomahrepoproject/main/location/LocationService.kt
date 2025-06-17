package com.example.gomahrepoproject.main.location

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.gomahrepoproject.R
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import android.content.pm.PackageManager
import android.Manifest

class LocationService : Service() {
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var childId: String? = null
    private var isLocationEnabled = false

    companion object {
        private const val TAG = "LocationService"
        private const val NOTIFICATION_ID = 1
    }

    private val locationRequest by lazy {
        LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
            .setMinUpdateIntervalMillis(2000)
            .build()
    }

    private val locationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == LocationManager.PROVIDERS_CHANGED_ACTION) {
                val wasEnabled = isLocationEnabled
                isLocationEnabled = isLocationEnabled()
                if (!isLocationEnabled && wasEnabled) {
                    stopLocationUpdates()
                    updateLocationStatus(false)
                    Log.d(TAG, "Location services disabled, updates stopped")
                } else if (isLocationEnabled && !wasEnabled) {
                    startLocationUpdates()
                    updateLocationStatus(true)
                    Log.d(TAG, "Location services enabled, updates started")
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        registerReceiver(locationReceiver, IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Start foreground service immediately
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.launcher_ic)
            .setContentTitle("Location Sharing Active")
            .setContentText("Sending your location")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Notification permission not granted")
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        initializeService()
        return START_STICKY
    }

    private fun initializeService() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null || !user.isEmailVerified) {
            Log.e(TAG, "User not authenticated or email not verified")
            stopSelf()
            return
        }

        val userId = user.uid
        FirebaseDatabase.getInstance().getReference("users").child(userId).child("role")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val role = snapshot.getValue(String::class.java)
                    if (role == "child") {
                        childId = userId
                        isLocationEnabled = isLocationEnabled()
                        if (isLocationEnabled) {
                            startLocationUpdates()
                        } else {
                            Log.d(TAG, "Location services disabled, waiting for enable")
                        }
                    } else {
                        Log.d(TAG, "Not a child user, stopping service")
                        stopSelf()
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Error fetching role: ${error.message}")
                    stopSelf()
                }
            })
    }

    private fun startLocationUpdates() {
        if (!isLocationEnabled || childId == null) return

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    val locationModel = LocationModel(
                        latitude = location.latitude.toString(),
                        longitude = location.longitude.toString(),
                        timestamp = System.currentTimeMillis(),
                        locationEnabled = true
                    )
                    FirebaseDatabase.getInstance().getReference("locations").child(childId!!)
                        .setValue(locationModel)
                        .addOnSuccessListener {
                            Log.d(TAG, "Location updated: ${location.latitude}, ${location.longitude}")
                        }
                        .addOnFailureListener {
                            Log.e(TAG, "Failed to update location: ${it.message}")
                        }
                }
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        } else {
            Log.e(TAG, "Location permissions not granted")
            stopSelf()
        }
    }

    private fun stopLocationUpdates() {
        if (::locationCallback.isInitialized) {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        }
        Log.d(TAG, "Location updates stopped")
    }

    private fun updateLocationStatus(enabled: Boolean) {
        childId?.let {
            FirebaseDatabase.getInstance().getReference("locations").child(it)
                .child("locationEnabled").setValue(enabled)
        }
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopLocationUpdates()
        unregisterReceiver(locationReceiver)
        Log.d(TAG, "LocationService destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}