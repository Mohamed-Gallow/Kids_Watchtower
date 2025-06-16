package com.example.gomahrepoproject.main.blockapps

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.gomahrepoproject.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class BlockAppsFragment : Fragment() {

    private val viewModel: BlockedAppsViewModel by viewModels()

    private lateinit var rvBlockedApps: RecyclerView
    private lateinit var rvUnblockedApps: RecyclerView

    private lateinit var blockedAppsAdapter: BlockAppAdapter
    private lateinit var unblockedAppsAdapter: BlockAppAdapter

    private var userRole: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_block_apps, container, false)
        rvBlockedApps = view.findViewById(R.id.rvBlockedApps)
        rvUnblockedApps = view.findViewById(R.id.rvUnblockedApps)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            FirebaseDatabase.getInstance().reference
                .child("users").child(userId).child("role")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val value = snapshot.value
                        if (value is String) {
                            userRole = value
                            if (userRole == "parent") {
                                setupRecyclerViewsForParent()
                            } else if (userRole == "child") {
                                AppUploader.uploadInstalledAppsToFirebase(requireContext())
                                checkAndPromptAccessibilityPermission()
                            }
                        }
                        observeViewModel()
                    }

                    override fun onCancelled(error: DatabaseError) {
                        observeViewModel()
                    }
                })
        } else {
            observeViewModel()
        }
    }

    private fun checkAndPromptAccessibilityPermission() {
        if (!isAccessibilityServiceEnabled()) {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
            Toast.makeText(
                requireContext(),
                "Please enable Accessibility Service to block apps",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun setupRecyclerViewsForParent() {
        blockedAppsAdapter = BlockAppAdapter { app ->
            viewModel.unblockApp(app.packageName)
            Toast.makeText(requireContext(), "Unblocked: ${app.appName}", Toast.LENGTH_SHORT).show()
        }

        unblockedAppsAdapter = BlockAppAdapter { app ->
            viewModel.blockApp(app.packageName)
            Toast.makeText(requireContext(), "Blocked: ${app.appName}", Toast.LENGTH_SHORT).show()
        }

        rvBlockedApps.layoutManager = GridLayoutManager(requireContext(), 3)
        rvBlockedApps.adapter = blockedAppsAdapter

        rvUnblockedApps.layoutManager = GridLayoutManager(requireContext(), 3)
        rvUnblockedApps.adapter = unblockedAppsAdapter
    }

    private fun observeViewModel() {
        viewModel.blockedApps.observe(viewLifecycleOwner, Observer { apps ->
            if (::blockedAppsAdapter.isInitialized) {
                blockedAppsAdapter.submitList(apps)
            }
        })

        viewModel.unblockedApps.observe(viewLifecycleOwner, Observer { apps ->
            if (::unblockedAppsAdapter.isInitialized) {
                unblockedAppsAdapter.submitList(apps)
            }
        })
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val context = requireContext()
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        return enabledServices.contains("${context.packageName}/${AppMonitoringService::class.java.name}")
    }
}
