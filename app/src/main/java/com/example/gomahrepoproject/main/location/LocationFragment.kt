package com.example.gomahrepoproject.main.location

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.gomahrepoproject.databinding.FragmentLocationBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions


class LocationFragment : Fragment() , OnMapReadyCallback{
    private var _binding  : FragmentLocationBinding?=null
    private val binding get() = _binding!!
    private val locationViewModel : LocationViewModel by viewModels()
    private lateinit var googleMap : GoogleMap

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val allGranted = permissions.all { it.value }
            if (allGranted) {
                val intent = Intent(requireContext(), LocationService::class.java)
                requireActivity().startService(intent)
                showToast("live location started")
            } else {
                showToast("all permissions must be granted")
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentLocationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
    }

    private fun initViews() {
        binding.btnStartLiveLocation.setOnClickListener {
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
                val intent = Intent(requireContext(), LocationService::class.java)
                requireActivity().startService(intent)
                showToast("live location started")
            }
        }

        binding.btnStopLiveLocation.setOnClickListener {
            val intent = Intent(requireContext(), LocationService::class.java)
            requireContext().startService(intent)
            showToast("live location stopped")
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }


    override fun onMapReady(p0: GoogleMap) {
        googleMap = p0

        val location = LatLng(30.033333, 31.233334)
        googleMap.addMarker(MarkerOptions().position(location).title("Cairo"))
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location,10f))
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}