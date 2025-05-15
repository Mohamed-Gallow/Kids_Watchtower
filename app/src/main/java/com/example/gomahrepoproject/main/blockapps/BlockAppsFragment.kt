package com.example.gomahrepoproject.main.blockapps

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.gomahrepoproject.R

class BlockAppsFragment : Fragment() {

    private val viewModel: BlockedAppsViewModel by viewModels()

    private lateinit var rvBlockedApps: RecyclerView
    private lateinit var rvUnblockedApps: RecyclerView

    private lateinit var blockedAppsAdapter: BlockAppAdapter
    private lateinit var unblockedAppsAdapter: BlockAppAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_block_apps, container, false)
        rvBlockedApps = view.findViewById(R.id.rvBlockedApps)
        rvUnblockedApps = view.findViewById(R.id.rvUnblockedApps)

        setupRecyclerViews()
        observeViewModel()

        return view
    }

    private fun setupRecyclerViews() {
        blockedAppsAdapter = BlockAppAdapter { app ->
            viewModel.unblockApp(app.packageName)
            Toast.makeText(requireContext(), "Unblocked: ${app.appName}", Toast.LENGTH_SHORT).show()
        }

        unblockedAppsAdapter = BlockAppAdapter { app ->
            viewModel.blockApp(app.packageName)
            Toast.makeText(requireContext(), "Blocked: ${app.appName}", Toast.LENGTH_SHORT).show()
        }

        rvBlockedApps.apply {
            layoutManager = GridLayoutManager(requireContext(), 3)
            adapter = blockedAppsAdapter
        }

        rvUnblockedApps.apply {
            layoutManager = GridLayoutManager(requireContext(), 3)
            adapter = unblockedAppsAdapter
        }
    }

    private fun observeViewModel() {
        viewModel.blockedApps.observe(viewLifecycleOwner, Observer { apps ->
            blockedAppsAdapter.submitList(apps)
        })

        viewModel.unblockedApps.observe(viewLifecycleOwner, Observer { apps ->
            unblockedAppsAdapter.submitList(apps)
        })
    }
}
