package com.example.gomahrepoproject.main.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.gomahrepoproject.R
import com.example.gomahrepoproject.auth.AuthViewModel
import com.example.gomahrepoproject.databinding.FragmentProfileBinding
import com.example.gomahrepoproject.ui.AuthActivity


class ProfileFragment : Fragment() {
    private var _binding : FragmentProfileBinding ?=null
    private val binding get() = _binding!!
    private val authViewModel : AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupLogout()
        initViews()
    }

    private fun initViews(){
        binding.tvProfileEmail.text=authViewModel.auth.currentUser?.email ?: "not found yet"
    }

    private fun setupLogout(){
        binding.apply {
            ivLogout.setOnClickListener {
                authViewModel.logout()
                val intent = Intent(requireContext(),AuthActivity::class.java)
                requireContext().startActivity(intent)
            }
            tvLogout.setOnClickListener {
                authViewModel.logout()
                val intent = Intent(requireContext(),AuthActivity::class.java)
                requireContext().startActivity(intent)
                requireActivity().finish()
            }
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}