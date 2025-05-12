package com.example.gomahrepoproject.main.link

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.gomahrepoproject.databinding.FragmentConnectPhoneBinding
import com.google.firebase.auth.FirebaseAuth

class ConnectPhoneFragment : Fragment() {
    private var _binding: FragmentConnectPhoneBinding? = null
    private val binding get() = _binding!!
    private val linkViewModel: LinkViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentConnectPhoneBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnLinkParentWithChild.setOnClickListener {
            val childEmail = binding.etChildEmailField.text.toString().trim()
            val parentEmail = FirebaseAuth.getInstance().currentUser?.email ?: ""
            if (childEmail.isEmpty()) {
                showToast("Please enter child's email")
            } else {
                linkViewModel.sendLinkRequest(parentEmail, childEmail)
            }
        }

        linkViewModel.linkStatus.observe(viewLifecycleOwner) { status ->
            showToast(status)
        }

        binding.ivRegisterBackArrow.setOnClickListener {
            requireActivity().onBackPressed()
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}