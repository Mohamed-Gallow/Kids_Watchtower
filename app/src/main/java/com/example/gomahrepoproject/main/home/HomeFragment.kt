package com.example.gomahrepoproject.main.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gomahrepoproject.R
import com.example.gomahrepoproject.auth.AuthViewModel
import com.example.gomahrepoproject.databinding.FragmentHomeBinding
import com.example.gomahrepoproject.main.data.Data
import com.example.gomahrepoproject.main.profile.ProfileViewModel
import com.google.android.gms.maps.GoogleMap


class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var recentAdapter: RecentAdapter
    private val profileViewModel : ProfileViewModel by viewModels()
    private val homeViewModel: HomeViewModel by viewModels()
    private lateinit var mGoogleMap: GoogleMap

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prepareRecentAdapter()
        manageTime()
        setupUserData()
    }

    private fun setupUserData(){
        profileViewModel.user.observe(viewLifecycleOwner) { user ->
            binding.headerName.text = user?.displayName ?: ""
        }
    }

    private fun manageTime() {
        binding.icManageTime.setOnClickListener {


        }
    }

    private fun prepareRecentAdapter() {
        recentAdapter = RecentAdapter(
            listOf(
                Data(R.drawable.facebook_icon, "Facebook", "10:15", "35 Minute"),
                Data(R.drawable.twitter, "X", "00:32", "18 Minute"),
                Data(R.drawable.insta, "Instagram", "08:12", "2 Hours"),
            )
        )

        val layoutManager = LinearLayoutManager(requireContext())
        binding.recentRecyclerview.apply {
            this.layoutManager = layoutManager
            adapter = recentAdapter
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}