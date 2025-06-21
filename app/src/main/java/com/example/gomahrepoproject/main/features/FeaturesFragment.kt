package com.example.gomahrepoproject.main.features

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.gomahrepoproject.R
import com.example.gomahrepoproject.auth.AuthViewModel
import com.example.gomahrepoproject.databinding.FragmentFeaturesBinding
import com.example.gomahrepoproject.main.location.UserDataModel
import com.google.firebase.database.FirebaseDatabase

class FeaturesFragment : Fragment() {
    private var _binding: FragmentFeaturesBinding? = null
    private val binding get() = _binding!!
    private val authViewModel: AuthViewModel by viewModels()
    private lateinit var userDataModel: UserDataModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentFeaturesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
    }

    private fun initViews() {
        binding.btnLinkDevice.setOnClickListener {
            this@FeaturesFragment.findNavController()
                .navigate(R.id.action_featuresFragment_to_connectPhoneFragment)
        }
        binding.btnChildLink.setOnClickListener {
            this@FeaturesFragment.findNavController()
                .navigate(R.id.action_featuresFragment_to_childLinkFragment)
        }
        binding.btnNavToLocation.setOnClickListener {
            this@FeaturesFragment.findNavController()
                .navigate(R.id.action_featuresFragment_to_locationFragment)
        }
        binding.btnNavToAppBlock.setOnClickListener {
            this@FeaturesFragment.findNavController()
                .navigate(R.id.action_featuresFragment_to_blockAppsFragment)
        }
        binding.btnNavToAppBlockTime.setOnClickListener {
            this@FeaturesFragment.findNavController()
                .navigate(R.id.action_featuresFragment_to_appTimeRangeFragment)
        }
        binding.btnNavToSecurityLog.setOnClickListener {
            // Get the current user from AuthViewModel
            authViewModel.authState.observe(viewLifecycleOwner) { firebaseUser ->
                if (firebaseUser != null) {
                    // Fetch the user's role from Firebase Realtime Database
                    val userId = firebaseUser.uid
                    FirebaseDatabase.getInstance().getReference("users").child(userId).child("role")
                        .get()
                        .addOnSuccessListener { dataSnapshot ->
                            val role = dataSnapshot.getValue(String::class.java)
                            when (role) {
                                "parent" -> {
                                    findNavController().navigate(R.id.action_featuresFragment_to_secuLogFragment)
                                }

                                "child" -> {
                                    findNavController().navigate(R.id.action_featuresFragment_to_testSecuFragment3)
                                }

                                else -> {
                                    Toast.makeText(
                                        requireContext(),
                                        "Role not recognized",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                        .addOnFailureListener {
                            Toast.makeText(
                                requireContext(),
                                "Failed to fetch role",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                } else {
                    Toast.makeText(
                        requireContext(),
                        "User not logged in",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }



    private fun initLockDevice() {
        binding.btnLockDevice.setOnClickListener {

        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}
