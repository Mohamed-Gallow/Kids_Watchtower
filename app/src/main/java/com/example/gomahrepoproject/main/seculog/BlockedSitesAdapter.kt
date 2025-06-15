package com.example.gomahrepoproject.main.seculog

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.gomahrepoproject.databinding.ItemBlockedSiteBinding

class BlockedSitesAdapter(
    private val blockedSites: List<String>,
    private val onRemoveClicked: (String) -> Unit
) : RecyclerView.Adapter<BlockedSitesAdapter.BlockedSitesViewHolder>() {

    inner class BlockedSitesViewHolder(val binding: ItemBlockedSiteBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BlockedSitesViewHolder {
        val binding = ItemBlockedSiteBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return BlockedSitesViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BlockedSitesViewHolder, position: Int) {
        val url = blockedSites[position]
        holder.binding.tvUrl.text = url
        holder.binding.btnRemove.setOnClickListener {
            onRemoveClicked(url)
        }
    }

    override fun getItemCount() = blockedSites.size
}