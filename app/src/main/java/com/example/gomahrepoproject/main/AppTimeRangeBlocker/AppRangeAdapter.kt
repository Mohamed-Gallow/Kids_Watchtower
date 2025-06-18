package com.example.gomahrepoproject.main.AppTimeRangeBlocker

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.gomahrepoproject.R

class AppRangeAdapter(private val apps: MutableList<AppTimeRange>) :
    RecyclerView.Adapter<AppRangeAdapter.RangeViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RangeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app_range, parent, false)
        return RangeViewHolder(view)
    }

    override fun getItemCount(): Int = apps.size

    override fun onBindViewHolder(holder: RangeViewHolder, position: Int) {
        holder.bind(apps[position])
    }

    fun updateData() {
        notifyDataSetChanged()
    }

    inner class RangeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val name: TextView = itemView.findViewById(R.id.tvAppName)
        private val range: TextView = itemView.findViewById(R.id.tvAppRange)

        fun bind(app: AppTimeRange) {
            name.text = app.appName
            range.text = String.format("%02d:%02d - %02d:%02d", app.startHour, app.startMinute, app.endHour, app.endMinute)
        }
    }
}
