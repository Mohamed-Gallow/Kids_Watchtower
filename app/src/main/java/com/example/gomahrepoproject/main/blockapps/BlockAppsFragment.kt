package com.example.gomahrepoproject.main.blockapps

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.gomahrepoproject.R
import com.example.gomahrepoproject.databinding.FragmentBlockAppsBinding

class BlockAppsFragment : Fragment() {
    private var _binding: FragmentBlockAppsBinding? = null
    private val binding get() = _binding!!
    private lateinit var blockAppAdapter:

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_block_apps, container, false)
    }


}