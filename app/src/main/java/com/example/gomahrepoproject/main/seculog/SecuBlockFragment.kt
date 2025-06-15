package com.example.gomahrepoproject.main.seculog

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gomahrepoproject.R
import com.example.gomahrepoproject.databinding.FragmentSecuBlockLogBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class SecuBlockFragment : Fragment() {
    private var _binding: FragmentSecuBlockLogBinding? = null
    private val binding get() = _binding!!

    private lateinit var blockedSitesAdapter: BlockedSitesAdapter
    private val blockedSitesList = mutableListOf<String>()
    private val database = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        listenForBlockedSites()
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentSecuBlockLogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize adapter with remove callback
        blockedSitesAdapter = BlockedSitesAdapter(blockedSitesList) { urlToRemove ->
            removeBlockedSite(urlToRemove)
        }

        binding.rvBlockedSites.layoutManager = LinearLayoutManager(requireContext())
        binding.rvBlockedSites.adapter = blockedSitesAdapter

        binding.btnBlockUrl.setOnClickListener {
            val url = binding.etWebsiteUrl.text.toString().trim()
            if (url.isNotEmpty()) {
                val formattedUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    "http://$url"
                } else {
                    url
                }

                if (!blockedSitesList.contains(formattedUrl)) {
                    addBlockedSite(formattedUrl)
                } else {
                    showToast("Website already blocked")
                }
            } else {
                showToast("Please enter a website URL")
            }
        }

        binding.btnTest.setOnClickListener {
            findNavController().navigate(R.id.action_secuLogFragment_to_testSecuFragment)
        }

    }

    private fun listenForBlockedSites() {
        val parentId = auth.currentUser?.uid ?: return
        database.getReference("users").child(parentId).child("blockedSites")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    blockedSitesList.clear()
                    for (site in snapshot.children) {
                        val url = site.getValue(String::class.java)
                        if (url != null) {
                            blockedSitesList.add(url)
                        }
                    }
                    blockedSitesAdapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    showToast("Error fetching blocked sites: ${error.message}")
                }
            })
    }

    private fun addBlockedSite(url: String) {
        val parentId = auth.currentUser?.uid ?: return
        val blockedSiteRef = database.getReference("users").child(parentId).child("blockedSites").push()
        blockedSiteRef.setValue(url).addOnCompleteListener {
            showToast("Website blocked successfully")
            binding.etWebsiteUrl.text.clear()
        }.addOnFailureListener {
            showToast("Failed to block website: ${it.message}")
        }
    }

    private fun removeBlockedSite(urlToRemove: String) {
        val parentId = auth.currentUser?.uid ?: return
        database.getReference("users").child(parentId).child("blockedSites")
            .orderByValue().equalTo(urlToRemove)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (site in snapshot.children) {
                        site.ref.removeValue().addOnCompleteListener {
                            showToast("Website unblocked successfully")
                        }.addOnFailureListener {
                            showToast("Failed to unblock website: ${it.message}")
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    showToast("Error removing blocked site: ${error.message}")
                }
            })
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}