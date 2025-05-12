package com.example.gomahrepoproject.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.gomahrepoproject.databinding.FragmentPersonTypeBinding


class PersonTypeFragment : Fragment() {
    private var _binding: FragmentPersonTypeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentPersonTypeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
    }

    private fun initViews() {
        binding.btnParent.setOnClickListener {
            val action =
                PersonTypeFragmentDirections.actionPersonTypeFragmentToRegisterFragment("parent")
            this@PersonTypeFragment.findNavController().navigate(action)
        }
        binding.btnChild.setOnClickListener {
            val action =
                PersonTypeFragmentDirections.actionPersonTypeFragmentToRegisterFragment("child")
            this@PersonTypeFragment.findNavController().navigate(action)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}