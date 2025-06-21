package com.example.gomahrepoproject.main.features

import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.gomahrepoproject.R
import com.example.gomahrepoproject.databinding.FragmentFeaturesBinding
import com.example.gomahrepoproject.main.features.LockListenerService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class FeaturesFragment : Fragment() {

    private var _binding: FragmentFeaturesBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFeaturesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        initLockDevice()
        startLockListenerServiceIfChild()
    }

    private fun initViews() {
        binding.btnLinkDevice.setOnClickListener {
            findNavController().navigate(R.id.action_featuresFragment_to_connectPhoneFragment)
        }
        binding.btnChildLink.setOnClickListener {
            findNavController().navigate(R.id.action_featuresFragment_to_childLinkFragment)
        }
        binding.btnNavToLocation.setOnClickListener {
            findNavController().navigate(R.id.action_featuresFragment_to_locationFragment)
        }
        binding.btnNavToAppBlock.setOnClickListener {
            findNavController().navigate(R.id.action_featuresFragment_to_blockAppsFragment)
        }
        binding.btnNavToSecurityLog.setOnClickListener {
            findNavController().navigate(R.id.action_featuresFragment_to_secuLogFragment)
        }
        binding.btnNavToAppBlockTime.setOnClickListener {
            findNavController().navigate(R.id.action_featuresFragment_to_appTimeRangeFragment)
        }
    }

    private fun initLockDevice() {
        val user = FirebaseAuth.getInstance().currentUser ?: return

        val currentUid = user.uid
        val database = FirebaseDatabase.getInstance().reference

        database.child("users").child(currentUid).child("role").get().addOnSuccessListener { snapshot ->
            val userRole = snapshot.getValue(String::class.java)

            binding.btnLockDevice.setOnClickListener {
                if (userRole == "parent") {
                    database.child("users").child(currentUid).child("linkedAccounts")
                        .get().addOnSuccessListener { linkedSnapshot ->
                            val childId = linkedSnapshot.child("childId").getValue(String::class.java)
                            if (childId != null) {
                                val lockSignal = mapOf(
                                    "isLocked" to true,
                                    "lockedBy" to currentUid,
                                    "lockTime" to System.currentTimeMillis()
                                )

                                database.child("users").child(childId).child("deviceStatus")
                                    .setValue(lockSignal)
                                    .addOnSuccessListener {
                                        Toast.makeText(requireContext(), "Child device locked successfully", Toast.LENGTH_SHORT).show()
                                    }
                                    .addOnFailureListener { error ->
                                        Toast.makeText(requireContext(), "Failed to lock device: ${error.message}", Toast.LENGTH_SHORT).show()
                                    }
                            } else {
                                Toast.makeText(requireContext(), "No child account linked to this user", Toast.LENGTH_SHORT).show()
                            }
                        }.addOnFailureListener { error ->
                            Toast.makeText(requireContext(), "Failed to retrieve child data: ${error.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(requireContext(), "This button is only available for parents", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun startLockListenerServiceIfChild() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val userId = user.uid
        val database = FirebaseDatabase.getInstance().reference

        database.child("users").child(userId).child("role").get().addOnSuccessListener { snapshot ->
            val role = snapshot.getValue(String::class.java)
            if (role == "child") {
                val intent = Intent(requireContext(), LockListenerService::class.java)
                requireContext().startService(intent)

                // التحقق من صلاحية Device Admin
                val dpm = requireContext().getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
                val component = ComponentName(requireContext(), DeviceAdminReceiver::class.java)
                if (!dpm.isAdminActive(component)) {
                    requestDeviceAdminPermission()
                }
            }
        }
    }

    private fun requestDeviceAdminPermission() {
        val componentName = ComponentName(requireContext(), DeviceAdminReceiver::class.java)
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName)
            putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,  "Required to allow remote device locking")
        }
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}