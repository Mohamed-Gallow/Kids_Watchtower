package com.example.gomahrepoproject.main.AppTimeRangeBlocker

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.gomahrepoproject.databinding.FragmentAppTimeRangeBinding
import com.example.gomahrepoproject.main.blockapps.AppUsageTracker

/**
 * Placeholder fragment for App Time Range feature.
 */
class AppTimeRangeFragment : Fragment() {
    private var _binding: FragmentAppTimeRangeBinding? = null
    private val binding get() = _binding!!
    private val usageTracker by lazy { AppUsageTracker(requireContext()) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAppTimeRangeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.startTimePicker.setIs24HourView(true)
        binding.endTimePicker.setIs24HourView(true)
        binding.startTimePicker.hour = 9
        binding.startTimePicker.minute = 0
        binding.endTimePicker.hour = 17
        binding.endTimePicker.minute = 0

        binding.btnStartMonitor.setOnClickListener {
            if (!usageTracker.hasUsageAccessPermission()) {
                usageTracker.requestUsageAccessPermission()
                Toast.makeText(requireContext(), "Grant Usage Access and try again", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val intent = Intent(requireContext(), TimeRangeMonitorService::class.java).apply {
                putExtra("APP_NAME", "YouTube")
                putExtra("PACKAGE_NAME", "com.google.android.youtube")
                putExtra("START_HOUR", binding.startTimePicker.hour)
                putExtra("START_MIN", binding.startTimePicker.minute)
                putExtra("END_HOUR", binding.endTimePicker.hour)
                putExtra("END_MIN", binding.endTimePicker.minute)
            }
            requireContext().startService(intent)
            Toast.makeText(requireContext(), "Time range monitoring started", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
