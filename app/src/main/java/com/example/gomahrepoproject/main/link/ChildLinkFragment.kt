package com.example.gomahrepoproject.main.link

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.gomahrepoproject.R
import com.example.gomahrepoproject.databinding.FragmentChildLinkBinding

class ChildLinkFragment : Fragment() {
    private var _binding: FragmentChildLinkBinding? = null
    private val binding get() = _binding!!
    private val linkViewModel: LinkViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChildLinkBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        linkViewModel.listenForLinkRequests()

        linkViewModel.pendingRequests.observe(viewLifecycleOwner) { requests ->
            if (requests.isNotEmpty()) {
                binding.tvPendingRequest.text = "Pending request from parent"
                binding.etVerificationCode.visibility = View.VISIBLE
                binding.btnAcceptRequest.visibility = View.VISIBLE
                binding.btnAcceptRequest.setOnClickListener {
                    val code = binding.etVerificationCode.text.toString().trim()
                    if (code.isEmpty()) {
                        showToast("Enter verification code")
                    } else {
                        linkViewModel.acceptLinkRequest(requests[0].first, code)
                    }
                }
            } else {
                binding.tvPendingRequest.text = "No pending requests"
                binding.etVerificationCode.visibility = View.GONE
                binding.btnAcceptRequest.visibility = View.GONE
            }
        }

        linkViewModel.linkStatus.observe(viewLifecycleOwner) { status ->
            showToast(status)
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