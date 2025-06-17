package com.example.gomahrepoproject.main.seculog

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Patterns
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
        _binding = FragmentSecuBlockLogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        blockedSitesAdapter = BlockedSitesAdapter(blockedSitesList) { urlToRemove ->
            removeBlockedSite(urlToRemove)
        }

        binding.rvBlockedSites.layoutManager = LinearLayoutManager(requireContext())
        binding.rvBlockedSites.adapter = blockedSitesAdapter

        binding.btnBlockUrl.setOnClickListener {
            val url = binding.etWebsiteUrl.text.toString().trim()
            if (isValidUrl(url)) {
                val formattedUrl = formatUrl(url)
                if (!blockedSitesList.contains(formattedUrl)) {
                    addBlockedSite(formattedUrl)
                } else {
                    showToast("Website already blocked")
                }
            } else {
                showToast("Please enter a valid website URL")
            }
        }

        initViews()

    }

    private fun initViews(){
        findNavController().navigate(R.id.action_secuLogFragment_to_testSecuFragment)
    }

    private fun listenForBlockedSites() {
        val userId = auth.currentUser?.uid ?: run {
            showToast("User not authenticated")
            return
        }
        database.getReference("users").child(userId).child("blockedSites")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val newSites = mutableListOf<String>()
                    for (site in snapshot.children) {
                        site.getValue(String::class.java)?.let { newSites.add(it) }
                    }
                    blockedSitesAdapter.updateSites(newSites)
                }

                override fun onCancelled(error: DatabaseError) {
                    showToast("Error fetching blocked sites: ${error.message}")
                }
            })
    }

    private fun addBlockedSite(url: String) {
        val userId = auth.currentUser?.uid ?: run {
            showToast("User not authenticated")
            return
        }
        val blockedSiteRef = database.getReference("users").child(userId).child("blockedSites").push()
        blockedSiteRef.setValue(url).addOnCompleteListener {
            showToast("Website blocked successfully")
            binding.etWebsiteUrl.text.clear()
        }.addOnFailureListener {
            showToast("Failed to block website: ${it.message}")
        }
    }

    private fun removeBlockedSite(urlToRemove: String) {
        val userId = auth.currentUser?.uid ?: run {
            showToast("User not authenticated")
            return
        }
        database.getReference("users").child(userId).child("blockedSites")
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

    private fun isValidUrl(url: String): Boolean {
        return url.isNotEmpty() && Patterns.WEB_URL.matcher(url).matches()
    }

    private fun formatUrl(url: String): String {
        return when {
            url.startsWith("http://") || url.startsWith("https://") -> url
            else -> "https://$url"
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}