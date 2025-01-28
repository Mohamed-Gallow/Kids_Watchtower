package com.example.gomahrepoproject.main.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.gomahrepoproject.R
import com.example.gomahrepoproject.data.Data

class RecentAdapter(private val recentSocialList: List<Data>) :
    RecyclerView.Adapter<RecentAdapter.ItemViewHolder>() {
    inner class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val socialImage : ImageView = itemView.findViewById(R.id.ivSocialImage)
        val socialName : TextView = itemView.findViewById(R.id.tvSocialName)
        val spentTime : TextView = itemView.findViewById(R.id.tvSpentTime)
        val time : TextView = itemView.findViewById(R.id.tvTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.home_recent_item, parent, false)
        return ItemViewHolder(view)
    }

    override fun getItemCount(): Int {
        return recentSocialList.size
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val recentSocial = recentSocialList[position]
        holder.apply {
            socialName.text = recentSocial.socialName
            spentTime.text = recentSocial.spentTime
            time.text = recentSocial.time
            socialImage.setImageResource(recentSocial.socialImage)
        }
    }

}