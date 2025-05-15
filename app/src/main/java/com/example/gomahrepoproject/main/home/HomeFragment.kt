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
import android.os.BatteryManager
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
import androidx.core.content.ContextCompat.registerReceiver
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gomahrepoproject.ChildActivity
import com.example.gomahrepoproject.R
import com.example.gomahrepoproject.databinding.FragmentHomeBinding
import com.example.gomahrepoproject.main.data.Data
import com.example.gomahrepoproject.main.location.LocationService
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
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import java.util.Locale


@Suppress("DEPRECATION")
class HomeFragment : Fragment() , OnMapReadyCallback {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!


    private lateinit var batteryReceiver: BroadcastReceiver

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val allGranted = permissions.all { it.value }
            if (allGranted) {
                startLocationUpdates()
                startLocationService()
                showToast("Live location started")
            } else {
                showToast("All permissions must be granted")
            }
        }


    private lateinit var recentAdapter: RecentAdapter
    private val profileViewModel : ProfileViewModel by viewModels()
    private lateinit var googleMap: GoogleMap

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private var currentMarker: Marker? = null

    private val pathPoints = mutableListOf<LatLng>()
    private var polyline: Polyline? = null

    private val handler = Handler(Looper.getMainLooper())
    private var lastLatLng: LatLng? = null
    private var isStopped = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initSpinnerItem()
        initAdapters()
        manageTime()
        initViews()
        initCurrentLocation()

        val mapFragment =
            childFragmentManager.findFragmentById(R.id.fHomeLocation) as? com.google.android.gms.maps.SupportMapFragment
        mapFragment?.getMapAsync(this)



        binding.navToChildFragment.setOnClickListener {
            val intent = Intent(requireContext(), ChildActivity::class.java)
            requireContext().startActivity(intent)
        }

    }

    override fun onMapReady(p0: GoogleMap) {
        googleMap = p0
        checkPermissionsAndStart()
    }


    private fun initViews() {

        profileViewModel.user.observe(viewLifecycleOwner) { user ->
            binding.headerName.text = user?.displayName ?: ""
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
            startLocationUpdates()
            startLocationService()
            showToast("Live location started")
        }
    }

    private fun startLocationService() {
        val intent = Intent(requireContext(), LocationService::class.java)
        requireActivity().startService(intent)
    }


    private fun initCurrentLocation() {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())

        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {

            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val geocoder = Geocoder(requireContext(), Locale.getDefault())
                    val addressList =
                        geocoder.getFromLocation(location.latitude, location.longitude, 1)

                    binding.tvLocationName.text = if (!addressList.isNullOrEmpty()) {
                        val address = addressList[0]
                        val village = address.subLocality ?: ""
                        val city = address.locality ?: ""
                        val province = address.adminArea ?: ""
                        "$village, $city, $province"
                    } else {
                        "Unknown Location"
                    }
                } else {

                    "Unable to get location".also { binding.tvLocationName.text = it }
                }
            }
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1
            )
        }
    }

    private fun initSpinnerItem() {
        val devices = mutableListOf(DeviceModel("Mohammed Phone", 60)) // Initial data
        binding.userSpinner.adapter = SpinnerAdapter(requireContext(), devices)

        // Battery Receiver to update Spinner data
        batteryReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val batteryLevel = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
                val batteryScale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
                val batteryPct = if (batteryLevel != -1 && batteryScale != -1) {
                    (batteryLevel * 100 / batteryScale.toFloat()).toInt()
                } else {
                    0
                }

                // Update the first device's battery level (assuming it's the current phone)
                devices[0].batteryLevel = batteryPct
                (binding.userSpinner.adapter as SpinnerAdapter).notifyDataSetChanged()
            }
        }

        // Register receiver
        requireContext().registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    }

    private fun manageTime() {
        binding.icManageTime.setOnClickListener {


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

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
            .setIntervalMillis(1000)
            .build()

        fusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(requireContext())

        fusedLocationProviderClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            requireActivity().mainLooper
        )
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val location = locationResult.lastLocation ?: return
            val latLng = LatLng(location.latitude, location.longitude)

            // Check if googleMap is initialized
            if (::googleMap.isInitialized) {
                if (currentMarker == null) {
                    currentMarker = googleMap.addMarker(
                        MarkerOptions().position(latLng).title("My Location")
                    )
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f))

                    polyline = googleMap.addPolyline(
                        PolylineOptions()
                            .color(ContextCompat.getColor(requireContext(), R.color.blue))
                            .width(10f)
                            .geodesic(true)
                    )
                } else {
                    currentMarker?.position = latLng
                    googleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng))
                }

                pathPoints.add(latLng)
                polyline?.points = pathPoints

                detectMovement(latLng)
            }
        }
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
        currentMarker?.let { marker ->
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
            val colorTo =
                ContextCompat.getColor(requireContext(), R.color.red)

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
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
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

}