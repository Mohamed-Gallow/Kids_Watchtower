package com.example.gomahrepoproject.main.AppTimeRangeBlocker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.gomahrepoproject.databinding.FragmentAppTimeRangeBinding

/**
 * Placeholder fragment for App Time Range feature.
 */
class AppTimeRangeFragment : Fragment() {
    private var _binding: FragmentAppTimeRangeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAppTimeRangeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
