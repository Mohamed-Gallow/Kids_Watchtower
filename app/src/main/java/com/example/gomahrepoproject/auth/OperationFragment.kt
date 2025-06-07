package com.example.gomahrepoproject.auth

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.gomahrepoproject.R
import com.example.gomahrepoproject.databinding.FragmentOperationBinding
import com.example.gomahrepoproject.ui.MainActivity

class OperationFragment : Fragment() {
    private var _binding: FragmentOperationBinding? = null
    private val binding get() = _binding!!
    private val authViewModel : AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentOperationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initButtons()
        observeLogin()
    }

    private fun initButtons() {
        binding.btnSignIn.setOnClickListener {
            this@OperationFragment.findNavController()
                .navigate(R.id.action_operationFragment_to_loginFragment)
        }
        binding.btnSignUp.setOnClickListener {
            this@OperationFragment.findNavController()
                .navigate(R.id.action_opreationFragment_to_personTypeFragment2)
        }
    }

    private fun observeLogin() {
        authViewModel.authState.observe(viewLifecycleOwner) { user ->
            if (user != null) {
                if (user.displayName.isNullOrEmpty()) {
                    findNavController().navigate(R.id.action_loginFragment_to_setUsernameFragment)
                } else {
                    Intent(requireContext(), MainActivity::class.java).also { startActivity(it) }
                    requireActivity().finish()
                }
            }
        }

        authViewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}