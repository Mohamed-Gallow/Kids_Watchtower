package com.example.gomahrepoproject.main.location

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.gomahrepoproject.R
import com.example.gomahrepoproject.auth.AuthViewModel
import com.example.gomahrepoproject.databinding.FragmentLocationBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth


class LocationFragment : Fragment(), OnMapReadyCallback {
    private var _binding: FragmentLocationBinding? = null
    private val binding get() = _binding!!
    private val locationViewModel: LocationViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()
    private lateinit var googleMap: GoogleMap
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
        initButtons()
    }

    private fun initButtons() {
        binding.ivLoginBackArrow.setOnClickListener {
            this@LocationFragment.findNavController().popBackStack()
        }
        val currentUser = FirebaseAuth.getInstance().currentUser
        ("${currentUser?.displayName}'s Device" ).also { binding.tvLocationUsername.text = it }
    }

    override fun onMapReady(p0: GoogleMap) {
        googleMap = p0

        val location = LatLng(30.033333, 31.233334)
        googleMap.addMarker(MarkerOptions().position(location).title("Cairo"))
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 10f))
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}