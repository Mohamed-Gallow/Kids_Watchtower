package com.example.gomahrepoproject.auth

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import com.example.gomahrepoproject.databinding.FragmentSetUsernameBinding
import com.example.gomahrepoproject.ui.MainActivity


class SetUsernameFragment : Fragment() {
    private var _binding: FragmentSetUsernameBinding? = null
    private val binding get() = _binding!!
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentSetUsernameBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initSetUsernameButton()
    }

    private fun initSetUsernameButton() {
        binding.btnSetUsername.setOnClickListener {
            val username = binding.etSetUsername.text.toString().trim()
            if (username.isEmpty()) {
                Toast.makeText(requireContext(), "set your username!", Toast.LENGTH_SHORT).show()
            } else {
                authViewModel.setUsername(username)
                Toast.makeText(requireContext(), "username set successfully", Toast.LENGTH_SHORT)
                    .show()
                val intent = Intent(requireContext(), MainActivity::class.java)
                startActivity(intent)
                requireActivity().finish()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}