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
import com.example.gomahrepoproject.R
import com.example.gomahrepoproject.auth.AuthViewModel
import com.example.gomahrepoproject.databinding.FragmentLocationBinding
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

    private var googleMap: GoogleMap? = null
    private var childMarker: Marker? = null
    private var childId: String? = null

    private val pathPoints = mutableListOf<LatLng>()
    private var polyline: Polyline? = null
    private val handler = Handler(Looper.getMainLooper())
    private var lastLatLng: LatLng? = null
    private var isStopped = false
    private var isFirstLocationUpdate = true

    private var isChild = false

    companion object {
        private const val TAG = "LocationFragment"
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val allGranted = permissions.all { it.value }
            if (allGranted) {
                if (isChild) {
                    startLocationService()
                    showToast("Location service started")
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

        // Check authentication and email verification
        val user = authViewModel.auth.currentUser
        if (user == null || !user.isEmailVerified) {
            showToast("Please log in and verify your email")
            findNavController().navigate(R.id.action_locationFragment_to_connectPhoneFragment)
            return
        }

        val userId = user.uid
        FirebaseDatabase.getInstance().getReference("users").child(userId).child("role")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val role = snapshot.getValue(String::class.java)
                    if (role == "child") {
                        isChild = true
                        childId = userId
                        binding.tvLocationUsername.visibility = View.GONE
                        checkPermissionsAndStart()
                    } else if (role == "parent") {
                        isChild = false
                        // Initialize map for parents
                        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as? com.google.android.gms.maps.SupportMapFragment
                        mapFragment?.getMapAsync(this@LocationFragment)
                        // Get linked child ID
                        FirebaseDatabase.getInstance().getReference("users").child(userId)
                            .child("linkedAccounts").child("childId")
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(snapshot: DataSnapshot) {
                                    childId = snapshot.getValue(String::class.java)
                                    if (childId != null) {
                                        locationViewModel.listenForChildLocation(childId!!)
                                        setupChildName()
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
                        findNavController().navigate(R.id.action_locationFragment_to_connectPhoneFragment)
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
            findNavController().popBackStack()
        }
    }

    private fun setupChildName() {
        childId?.let { cid ->
            FirebaseDatabase.getInstance().getReference("users").child(cid).child("displayName")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val childName = snapshot.getValue(String::class.java)
                        binding.tvLocationUsername.text = "${childName?.replaceFirstChar { it.uppercase() } ?: "Child"}'s Phone"
                    }
                    override fun onCancelled(error: DatabaseError) {
                        binding.tvLocationUsername.text = "Child's Phone"
                    }
                })
        }
    }

    private fun checkPermissionsAndStart() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val notGrantedPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(requireContext(), it) != PackageManager.PERMISSION_GRANTED
        }

        if (notGrantedPermissions.isNotEmpty()) {
            requestPermissionLauncher.launch(notGrantedPermissions.toTypedArray())
        } else {
            if (isChild) {
                startLocationService()
            } else if (childId != null) {
                locationViewModel.requestChildLocationSharing(childId!!)
            }
        }
    }

    private fun startLocationService() {
        val intent = Intent(requireContext(), LocationService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            requireContext().startForegroundService(intent)
        } else {
            requireContext().startService(intent)
        }
    }

    private fun showToast(message: String) {
        if (message.isNotEmpty()) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        if (!isChild) {
            setupChildLocationObserver()
        }
    }

    private fun setupChildLocationObserver() {
        locationViewModel.childLocation.observe(viewLifecycleOwner) { location ->
            val lat = location.latitude.toDoubleOrNull() ?: return@observe
            val lng = location.longitude.toDoubleOrNull() ?: return@observe
            val childLatLng = LatLng(lat, lng)
            if (childMarker == null) {
                childMarker = googleMap?.addMarker(
                    MarkerOptions().position(childLatLng).title("Child's Location")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                )
                googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(childLatLng, 16f))
                polyline = googleMap?.addPolyline(
                    PolylineOptions()
                        .color(ContextCompat.getColor(requireContext(), R.color.blue))
                        .width(15f)
                        .geodesic(true)
                )
                isFirstLocationUpdate = false
            } else {
                childMarker?.position = childLatLng
                if (isFirstLocationUpdate) {
                    googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(childLatLng, 16f))
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
        _binding = null
        googleMap = null
    }
}