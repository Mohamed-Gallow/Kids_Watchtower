package com.example.gomahrepoproject.main.features

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.gomahrepoproject.R
import com.example.gomahrepoproject.databinding.FragmentFeaturesBinding


class FeaturesFragment : Fragment() {
    private var _binding: FragmentFeaturesBinding? = null
    private val binding get() = _binding!!

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
                .navigate(R.id.action_featuresFragment_to_locationFragment2)
        }
        binding.btnNavToAppBlock.setOnClickListener {
            this@FeaturesFragment.findNavController()
                .navigate(R.id.action_featuresFragment_to_blockAppsFragment)
        }
        binding.btnNavToSecurityLog.setOnClickListener {
            this@FeaturesFragment.findNavController()
                .navigate(R.id.action_featuresFragment_to_secuLogFragment)
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