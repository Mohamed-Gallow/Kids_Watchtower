package com.example.gomahrepoproject.main.home

import android.Manifest
import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gomahrepoproject.ChildActivity
import com.example.gomahrepoproject.R
import com.example.gomahrepoproject.databinding.FragmentHomeBinding
import com.example.gomahrepoproject.main.data.Data
import com.example.gomahrepoproject.main.location.LocationViewModel
import com.example.gomahrepoproject.main.profile.ProfileViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.Locale


@Suppress("DEPRECATION")
class HomeFragment : Fragment() , OnMapReadyCallback {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var batteryReceiver: BroadcastReceiver
    private lateinit var recentAdapter: RecentAdapter
    private val profileViewModel: ProfileViewModel by viewModels()
    private val locationViewModel: LocationViewModel by viewModels()
    private lateinit var googleMap: GoogleMap

    private var childMarker: Marker? = null
    private var childId: String? = null
    private val pathPoints = mutableListOf<LatLng>()
    private var polyline: Polyline? = null
    private val handler = Handler(Looper.getMainLooper())
    private var lastLatLng: LatLng? = null
    private var isStopped = false
    private var isFirstLocationUpdate = true

    private var isChild = false
    private var isSharingLocation = false

    companion object {
        private var fusedLocationProviderClient: FusedLocationProviderClient? = null
        private var locationCallback: LocationCallback? = null
        private var currentChildId: String? = null
        private var isLocationSharingActive = false
        private const val TAG = "HomeFragment"
    }

    private val locationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == LocationManager.PROVIDERS_CHANGED_ACTION) {
                if (!isLocationEnabled()) {
                    stopLocationUpdates()
                    isSharingLocation = false
                    isLocationSharingActive = false
                    currentChildId?.let { childId ->
                        FirebaseDatabase.getInstance().getReference("users").child(childId)
                            .child("locationSharing").child("isSharing").setValue(false)
                    }
                    showToast("Location services disabled, sharing stopped")
                }
            }
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val allGranted = permissions.all { it.value }
            if (allGranted) {
                if (isChild) {
                    startLocationUpdates()
                    showToast("Sharing your location")
                } else if (childId != null) {
                    locationViewModel.requestChildLocationSharing(childId!!)
                    showToast("Child location tracking started")
                }
            } else {
                showToast("All permissions must be granted")
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initSpinnerItem()
        initAdapters()
        manageTime()
        initViews()

        val mapFragment =
            childFragmentManager.findFragmentById(R.id.fHomeLocation) as? com.google.android.gms.maps.SupportMapFragment
        mapFragment?.getMapAsync(this)

        // Initialize FusedLocationProviderClient if not already
        if (fusedLocationProviderClient == null) {
            fusedLocationProviderClient =
                LocationServices.getFusedLocationProviderClient(requireContext())
        }

        // Register location services broadcast receiver
        val filter = IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION)
        requireContext().registerReceiver(locationReceiver, filter)

        // Check authentication and email verification
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null || !user.isEmailVerified) {
            showToast("Please log in and verify your email")
            return
        }

        val userId = user.uid
        // Get user role from Firebase
        FirebaseDatabase.getInstance().getReference("users").child(userId).child("role")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val role = snapshot.getValue(String::class.java)
                    if (role == "child") {
                        isChild = true
                        childId = userId
                        currentChildId = userId
                        // Check if already sharing
                        FirebaseDatabase.getInstance().getReference("users").child(userId)
                            .child("locationSharing").child("isSharing")
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(sharingSnapshot: DataSnapshot) {
                                    isSharingLocation =
                                        sharingSnapshot.getValue(Boolean::class.java) ?: false
                                    isLocationSharingActive = isSharingLocation
                                    if (!isSharingLocation && isLocationEnabled()) {
                                        FirebaseDatabase.getInstance().getReference("users")
                                            .child(userId)
                                            .child("locationSharing").child("isSharing")
                                            .setValue(true)
                                        isSharingLocation = true
                                        isLocationSharingActive = true
                                    }
                                    locationViewModel.listenForChildLocation(childId!!)
                                    checkPermissionsAndStart()
                                }
                                override fun onCancelled(error: DatabaseError) {
                                    showToast("Error checking sharing status: ${error.message}")
                                }
                            })
                    } else if (role == "parent") {
                        // Get linked child ID
                        FirebaseDatabase.getInstance().getReference("users").child(userId)
                            .child("linkedAccounts").child("childId")
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(snapshot: DataSnapshot) {
                                    childId = snapshot.getValue(String::class.java)
                                    if (childId != null) {
                                        locationViewModel.listenForChildLocation(childId!!)
                                        checkPermissionsAndStart()
                                    } else {
                                        showToast("No linked child found. Please link a child account.")
                                        findNavController().navigate(R.id.action_homeFragment_to_connectPhoneFragment)
                                    }
                                }
                                override fun onCancelled(error: DatabaseError) {
                                    showToast("Error fetching child ID: ${error.message}")
                                }
                            })
                    } else {
                        showToast("Invalid user role. Please register with a valid role.")
                        FirebaseAuth.getInstance().signOut()
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    showToast("Error fetching role: ${error.message}")
                }
            })

        binding.navToChildFragment.setOnClickListener {
            val intent = Intent(requireContext(), ChildActivity::class.java)
            requireContext().startActivity(intent)
        }
    }

    private fun initViews() {
        profileViewModel.user.observe(viewLifecycleOwner) { user ->
            binding.headerName.text = user?.displayName ?: ""
        }

        if (isChild) {

            if (isSharingLocation) {
                isSharingLocation = false
                isLocationSharingActive = false
                FirebaseDatabase.getInstance().getReference("users").child(childId!!)
                    .child("locationSharing").child("isSharing").setValue(false)
                stopLocationUpdates()
                showToast("Stopped sharing location")
            } else {
                if (isLocationEnabled()) {
                    isSharingLocation = true
                    isLocationSharingActive = true
                    FirebaseDatabase.getInstance().getReference("users").child(childId!!)
                        .child("locationSharing").child("isSharing").setValue(true)
                    checkPermissionsAndStart()
                    showToast("Started sharing location")
                } else {
                    showToast("Please enable location services in device settings")
                }
            }

        }
    }

    private fun checkPermissionsAndStart() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val notGrantedPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(
                requireContext(),
                it
            ) != PackageManager.PERMISSION_GRANTED
        }

        if (notGrantedPermissions.isNotEmpty()) {
            requestPermissionLauncher.launch(notGrantedPermissions.toTypedArray())
        } else {
            if (isChild && isSharingLocation && isLocationEnabled()) {
                startLocationUpdates()
                showToast("Sharing your location")
            } else if (!isChild && childId != null) {
                locationViewModel.requestChildLocationSharing(childId!!)
                showToast("Child location tracking started")
            }
            initCurrentLocation()
        }
    }

    private fun startLocationUpdates() {
        if (fusedLocationProviderClient == null || !isLocationEnabled()) return

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
            .setMinUpdateIntervalMillis(2000)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    val latLng = LatLng(location.latitude, location.longitude)
                    if (isChild && isLocationSharingActive && isLocationEnabled()) {
                        Log.d(TAG, "Updating location: $latLng")
                        FirebaseDatabase.getInstance().getReference("locations")
                            .child(currentChildId!!)
                            .setValue(
                                mapOf(
                                    "latitude" to location.latitude.toString(),
                                    "longitude" to location.longitude.toString(),
                                    "timestamp" to System.currentTimeMillis()
                                )
                            ).addOnFailureListener {
                                showToast("Failed to update location: ${it.message}")
                            }
                    } else if (!isLocationEnabled()) {
                        stopLocationUpdates()
                        isSharingLocation = false
                        isLocationSharingActive = false
                        currentChildId?.let { childId ->
                            FirebaseDatabase.getInstance().getReference("users").child(childId)
                                .child("locationSharing").child("isSharing").setValue(false)
                        }
                        showToast("Location services disabled, sharing stopped")
                    } else {
                        showToast("not working")
                    }
                }
            }
        }

        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationProviderClient!!.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                Looper.getMainLooper()
            )
        }
    }

    private fun stopLocationUpdates() {
        locationCallback?.let { callback ->
            fusedLocationProviderClient?.removeLocationUpdates(callback)
        }
        Log.d(TAG, "Location updates stopped")
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager =
            requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val enabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        Log.d(TAG, "Location enabled: $enabled")
        return enabled
    }

    private fun initCurrentLocation() {
        locationViewModel.childLocation.observe(viewLifecycleOwner) { location ->
            val lat = location.latitude.toDoubleOrNull() ?: run {
                binding.tvLocationName.text = "Child location unavailable"
                return@observe
            }
            val lng = location.longitude.toDoubleOrNull() ?: run {
                binding.tvLocationName.text = "Child location unavailable"
                return@observe
            }
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                val geocoder = Geocoder(requireContext(), Locale.getDefault())
                try {
                    val addressList = geocoder.getFromLocation(lat, lng, 1)
                    binding.tvLocationName.text = if (!addressList.isNullOrEmpty()) {
                        val address = addressList[0]
                        val village = address.subLocality ?: ""
                        val city = address.locality ?: ""
                        val province = address.adminArea ?: ""
                        "$village, $city, $province"
                    } else {
                        "Unknown Location"
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Geocoding failed: ${e.message}")
                    binding.tvLocationName.text = "Unable to get address"
                }
            } else {
                binding.tvLocationName.text = "Location permission denied"
            }
        }
    }

    private fun initSpinnerItem() {
        val devices = mutableListOf(DeviceModel("Mohammed Phone", 60)) // Initial data
        binding.userSpinner.adapter = SpinnerAdapter(requireContext(), devices)

        batteryReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val batteryLevel = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
                val batteryScale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
                val batteryPct = if (batteryLevel != -1 && batteryScale != -1) {
                    (batteryLevel * 100 / batteryScale.toFloat()).toInt()
                } else {
                    0
                }
                devices[0].batteryLevel = batteryPct
                (binding.userSpinner.adapter as SpinnerAdapter).notifyDataSetChanged()
            }
        }

        requireContext().registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    }

    private fun manageTime() {
        binding.icManageTime.setOnClickListener {
            // Implement time management logic
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        setupChildLocationObserver()
    }

    private fun setupChildLocationObserver() {
        locationViewModel.childLocation.observe(viewLifecycleOwner) { location ->
            val lat = location.latitude.toDoubleOrNull() ?: return@observe
            val lng = location.longitude.toDoubleOrNull() ?: return@observe
            val childLatLng = LatLng(lat, lng)
            if (childMarker == null) {
                childMarker = googleMap.addMarker(
                    MarkerOptions().position(childLatLng).title("Child's Location")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                )
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(childLatLng, 16f))
                polyline = googleMap.addPolyline(
                    PolylineOptions()
                        .color(ContextCompat.getColor(requireContext(), R.color.blue))
                        .width(15f)
                        .geodesic(true)
                )
                isFirstLocationUpdate = false
            } else {
                childMarker?.position = childLatLng
                if (isFirstLocationUpdate) {
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(childLatLng, 16f))
                    isFirstLocationUpdate = false
                }
            }
            pathPoints.add(childLatLng)
            polyline?.points = pathPoints
            detectMovement(childLatLng)
        }
    }

    private fun detectMovement(newLatLng: LatLng) {
        if (lastLatLng == null) {
            lastLatLng = newLatLng
            startStopTimer()
            return
        }

        val distance = FloatArray(1)
        android.location.Location.distanceBetween(
            lastLatLng!!.latitude,
            lastLatLng!!.longitude,
            newLatLng.latitude,
            newLatLng.longitude,
            distance
        )

        if (distance[0] < 2) {
            if (!isStopped) {
                isStopped = true
                startStopTimer()
            }
        } else {
            isStopped = false
            handler.removeCallbacksAndMessages(null)
        }

        lastLatLng = newLatLng
    }

    private fun startStopTimer() {
        handler.postDelayed({
            if (isStopped) {
                animateMarker()
                animatePolylineColor()
            }
        }, 5000)
    }

    private fun animateMarker() {
        childMarker?.let { marker ->
            val animator = ValueAnimator.ofFloat(0f, 20f, 0f)
            animator.duration = 1000
            animator.addUpdateListener {
                val value = it.animatedValue as Float
                marker.setAnchor(0.5f, 1f + value / 100)
            }
            animator.start()
        }
    }

    private fun animatePolylineColor() {
        polyline?.let { polyline ->
            val colorFrom = ContextCompat.getColor(requireContext(), R.color.blue)
            val colorTo = ContextCompat.getColor(requireContext(), R.color.red)
            val colorAnimation = ValueAnimator.ofObject(ArgbEvaluator(), colorFrom, colorTo)
            colorAnimation.duration = 2000
            colorAnimation.addUpdateListener { animator ->
                polyline.color = animator.animatedValue as Int
            }
            colorAnimation.start()
        }
    }

    private fun initAdapters() {
        recentAdapter = RecentAdapter(
            listOf(
                Data(R.drawable.facebook_icon, "Facebook", "10:15", "35 Minute"),
                Data(R.drawable.twitter, "X", "00:32", "18 Minute"),
                Data(R.drawable.insta, "Instagram", "08:12", "2 Hours"),
            )
        )
        val layoutManager = LinearLayoutManager(requireContext())
        binding.recentRecyclerview.apply {
            this.layoutManager = layoutManager
            adapter = recentAdapter
        }
    }

    private fun showToast(message: String) {
        if (message.isNotEmpty()) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        try {
            requireContext().unregisterReceiver(batteryReceiver)
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        }
        _binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        requireContext().unregisterReceiver(locationReceiver)
    }
}