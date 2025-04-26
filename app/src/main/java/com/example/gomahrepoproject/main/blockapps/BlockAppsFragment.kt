package com.example.gomahrepoproject.main.blockapps

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.gomahrepoproject.R
import com.example.gomahrepoproject.databinding.FragmentBlockAppsBinding
import com.google.firebase.auth.FirebaseAuth

class BlockAppsFragment : Fragment() {
    private var _binding: FragmentBlockAppsBinding? = null
    private val binding get() = _binding!!


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_block_apps, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val user = FirebaseAuth.getInstance().currentUser

    }
}