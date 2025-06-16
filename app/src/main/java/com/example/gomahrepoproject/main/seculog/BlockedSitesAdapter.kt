package com.example.gomahrepoproject.main.seculog

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.gomahrepoproject.databinding.ItemBlockedSiteBinding
import androidx.recyclerview.widget.DiffUtil

class BlockedSitesAdapter(
    private val blockedSites: MutableList<String> = mutableListOf(),
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

    fun updateSites(newSites: List<String>) {
        val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = blockedSites.size
            override fun getNewListSize() = newSites.size
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
                blockedSites[oldItemPosition] == newSites[newItemPosition]
            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) =
                blockedSites[oldItemPosition] == newSites[newItemPosition]
        })
        blockedSites.clear()
        blockedSites.addAll(newSites)
        diffResult.dispatchUpdatesTo(this)
    }
}




