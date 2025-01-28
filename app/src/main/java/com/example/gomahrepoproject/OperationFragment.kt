package com.example.gomahrepoproject

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.gomahrepoproject.databinding.FragmentOperationBinding


class OperationFragment : Fragment() {
    private var _binding: FragmentOperationBinding? = null
    private val binding get() = _binding!!

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
    }

    private fun initButtons() {
        binding.btnSignIn.setOnClickListener {
            this@OperationFragment.findNavController()
                .navigate(R.id.action_operationFragment_to_loginFragment)
        }
        binding.btnSignUp.setOnClickListener {
            this@OperationFragment.findNavController()
                .navigate(R.id.action_operationFragment_to_registerFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}