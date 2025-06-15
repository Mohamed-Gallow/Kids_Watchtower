package com.example.gomahrepoproject.main.location

import android.Manifest
import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.gomahrepoproject.R
import com.example.gomahrepoproject.auth.AuthViewModel
import com.example.gomahrepoproject.databinding.FragmentLocationBinding
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
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class LocationFragment : Fragment(), OnMapReadyCallback {
    private var _binding: FragmentLocationBinding? = null
    private val binding get() = _binding!!
    private val authViewModel: AuthViewModel by viewModels()
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

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var isChild = false
    private var isSharingLocation = false

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
        _binding = FragmentLocationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as? com.google.android.gms.maps.SupportMapFragment
        mapFragment?.getMapAsync(this)

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireContext())

        // Check authentication and email verification
        val user = authViewModel.auth.currentUser
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
                        childId = userId // Child tracks their own location
                        // Check if already sharing
                        FirebaseDatabase.getInstance().getReference("users").child(userId)
                            .child("locationSharing").child("isSharing")
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(sharingSnapshot: DataSnapshot) {
                                    isSharingLocation = sharingSnapshot.getValue(Boolean::class.java) ?: false
                                    if (!isSharingLocation) {
                                        FirebaseDatabase.getInstance().getReference("users").child(userId)
                                            .child("locationSharing").child("isSharing").setValue(true)
                                        isSharingLocation = true
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
                                        findNavController().navigate(R.id.action_locationFragment_to_connectPhoneFragment)
                                    }
                                }
                                override fun onCancelled(error: DatabaseError) {
                                    showToast("Error fetching child ID: ${error.message}")
                                }
                            })
                    } else {
                        showToast("Invalid user role. Please register with a valid role.")
                        authViewModel.logout()
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    showToast("Error fetching role: ${error.message}")
                }
            })

        initViews()
    }

    private fun initViews() {
        binding.ivLoginBackArrow.setOnClickListener {
            this@LocationFragment.findNavController().popBackStack()
        }

        val firstName = authViewModel.auth.currentUser?.displayName
            ?.split(" ")
            ?.firstOrNull()
            ?.replaceFirstChar { it.uppercase() }
        if (isChild) {
            "$firstName's Phone".also { binding.tvLocationUsername.text = it }
            // Add toggle button for location sharing

                if (isSharingLocation) {
                    isSharingLocation = false
                    FirebaseDatabase.getInstance().getReference("users").child(childId!!)
                        .child("locationSharing").child("isSharing").setValue(false)
                    stopLocationUpdates()
                    showToast("Stopped sharing location")
                } else {
                    isSharingLocation = true
                    FirebaseDatabase.getInstance().getReference("users").child(childId!!)
                        .child("locationSharing").child("isSharing").setValue(true)
                    checkPermissionsAndStart()
                    showToast("Started sharing location")
                }

        } else {
            // For parent, show child's name if available
            childId?.let { cid ->
                FirebaseDatabase.getInstance().getReference("users").child(cid).child("displayName")
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val childName = snapshot.getValue(String::class.java)
                            "${childName?.replaceFirstChar { it.uppercase() } ?: "Child"}'s Phone".also {
                                binding.tvLocationUsername.text = it
                            }
                        }
                        override fun onCancelled(error: DatabaseError) {
                            "$firstName's Phone".also { binding.tvLocationUsername.text = it }
                        }
                    })
            } ?: "$firstName's Phone".also { binding.tvLocationUsername.text = it }
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
            if (isChild && isSharingLocation) {
                startLocationUpdates()
                showToast("Sharing your location")
            } else if (!isChild && childId != null) {
                locationViewModel.requestChildLocationSharing(childId!!)
                showToast("Child location tracking started")
            }
        }
    }

    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
            .setMinUpdateIntervalMillis(2000)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    val latLng = LatLng(location.latitude, location.longitude)
                    if (isChild && isSharingLocation) {
                        FirebaseDatabase.getInstance().getReference("locations").child(childId!!)
                            .setValue(mapOf(
                                "latitude" to location.latitude.toString(),
                                "longitude" to location.longitude.toString(),
                                "timestamp" to System.currentTimeMillis()
                            )).addOnFailureListener {
                                showToast("Failed to update location: ${it.message}")
                            }
                    }
                }
            }
        }

        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        }
    }

    private fun stopLocationUpdates() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
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

    override fun onDestroyView() {
        super.onDestroyView()
        // Do not stop location updates for child to keep sharing in background
        _binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        // Only stop updates if the app is fully closed (optional, can be removed for persistent sharing)
        if (isChild && !isSharingLocation) {
            stopLocationUpdates()
        }
    }
}