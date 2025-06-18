package com.example.gomahrepoproject.main.AppTimeRangeBlocker

import android.widget.ArrayAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.gomahrepoproject.databinding.FragmentAppTimeRangeBinding
import androidx.fragment.app.viewModels
import com.example.gomahrepoproject.main.AppTimeRangeBlocker.TimeRangeViewModel
import com.example.gomahrepoproject.main.AppTimeRangeBlocker.AppRangeAdapter
import com.example.gomahrepoproject.main.AppTimeRangeBlocker.AppTimeRange
import android.provider.Settings
import android.content.Intent
import com.example.gomahrepoproject.main.blockapps.AppMonitoringService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

/**
 * Placeholder fragment for App Time Range feature.
 */
class AppTimeRangeFragment : Fragment() {
    private var _binding: FragmentAppTimeRangeBinding? = null
    private val binding get() = _binding!!
    private val appRanges = mutableListOf<AppTimeRange>()
    private val viewModel: TimeRangeViewModel by viewModels()
    private lateinit var adapter: AppRangeAdapter
    private var installedApps: List<Pair<String, String>> = emptyList()
    private var userRole: String? = null

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

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            FirebaseDatabase.getInstance().reference.child("users").child(userId).child("role")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        userRole = snapshot.getValue(String::class.java)
                        if (userRole == "child") {
                            setupChildUi()
                            checkAndPromptAccessibilityPermission()
                        } else {
                            setupParentUi()
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        setupParentUi()
                    }
                })
        } else {
            setupParentUi()
        }
    }

    private fun setupParentUi() {
        binding.startTimePicker.setIs24HourView(true)
        binding.endTimePicker.setIs24HourView(true)
        binding.startTimePicker.hour = 9
        binding.startTimePicker.minute = 0
        binding.endTimePicker.hour = 17
        binding.endTimePicker.minute = 0

        setupInstalledApps()

        adapter = AppRangeAdapter(appRanges)
        binding.appList.layoutManager = LinearLayoutManager(requireContext())
        binding.appList.adapter = adapter

        binding.btnAddApp.setOnClickListener {
            val pos = binding.appSpinner.selectedItemPosition
            if (pos in installedApps.indices) {
                val app = installedApps[pos]
                val range = AppTimeRange(
                    app.first,
                    app.second,
                    binding.startTimePicker.hour,
                    binding.startTimePicker.minute,
                    binding.endTimePicker.hour,
                    binding.endTimePicker.minute
                )
                val index = appRanges.indexOfFirst { it.packageName == app.second }
                if (index >= 0) {
                    appRanges[index] = range
                    Toast.makeText(requireContext(), "Updated ${app.first}", Toast.LENGTH_SHORT).show()
                } else {
                    appRanges.add(range)
                }
                adapter.updateData()
            }
        }

        binding.btnStartMonitor.setOnClickListener {
            if (appRanges.isEmpty()) {
                Toast.makeText(requireContext(), "Add at least one app", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.saveRulesToChild(appRanges)
            Toast.makeText(requireContext(), "Time ranges sent to child", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupChildUi() {
        binding.appSpinner.visibility = View.GONE
        binding.btnAddApp.visibility = View.GONE
        binding.startTimePicker.visibility = View.GONE
        binding.endTimePicker.visibility = View.GONE
        binding.btnStartMonitor.visibility = View.GONE

        adapter = AppRangeAdapter(appRanges)
        binding.appList.layoutManager = LinearLayoutManager(requireContext())
        binding.appList.adapter = adapter

        viewModel.rules.observe(viewLifecycleOwner) { rules ->
            appRanges.clear()
            appRanges.addAll(rules)
            adapter.updateData()
        }
        viewModel.listenForRules()
    }

    private fun setupInstalledApps() {
        val pm = requireContext().packageManager
        installedApps = pm.getInstalledApplications(0)
            .filter { pm.getLaunchIntentForPackage(it.packageName) != null }
            .map { it.loadLabel(pm).toString() to it.packageName }

        val appNames = installedApps.map { it.first }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, appNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.appSpinner.adapter = adapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun checkAndPromptAccessibilityPermission() {
        if (!isAccessibilityServiceEnabled()) {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
            Toast.makeText(
                requireContext(),
                "Please enable Accessibility Service for monitoring",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val enabledServices = Settings.Secure.getString(
            requireContext().contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabledServices.contains(
            "${requireContext().packageName}/${AppMonitoringService::class.java.name}"
        )
    }
}
