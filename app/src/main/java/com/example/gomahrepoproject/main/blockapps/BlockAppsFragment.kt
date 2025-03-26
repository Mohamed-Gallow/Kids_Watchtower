package com.example.gomahrepoproject.main.blockapps

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.example.gomahrepoproject.R
import com.google.firebase.auth.FirebaseAuth

class BlockAppsFragment : Fragment(R.layout.fragment_block_apps) {

    private val viewModel: BlockedAppsViewModel by viewModels()
    private var blockedApps: List<String> = emptyList() // Store blocked apps list

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val userId = FirebaseAuth.getInstance().currentUser?.uid // Replace with actual user ID
        val childId = "CHILD_ID" // Replace with actual child ID

        viewModel.initialize(requireContext())

        // Observe blocked apps from ViewModel
        viewModel.blockedApps.observe(viewLifecycleOwner, Observer { apps ->
            blockedApps = apps // Update blocked apps list
            Log.d("BlockedApps", "Blocked Apps List: $apps")
        })

        // Load blocked apps for the given user & child
        if (userId != null) {
            viewModel.loadBlockedApps(userId, childId)
        }

        // Start monitoring running apps
        viewModel.startMonitoringApps { appPackage ->
            Log.d("BlockedApps", "Detected app: $appPackage")

            if (blockedApps.contains(appPackage)) {
                val appName = getAppNameFromPackage(appPackage) // Get app name
                showBlockScreen(appName) // Pass app name to block screen
            }
        }
    }

    private fun showBlockScreen(blockedApp: String) {
        val intent = Intent(requireContext(), BlockedAppActivity::class.java)
        intent.putExtra("BLOCKED_APP_NAME", blockedApp)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    private fun getAppNameFromPackage(packageName: String): String {
        val packageManager = requireContext().packageManager
        return try {
            val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(applicationInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName // Return package name if not found
        }
    }
}
