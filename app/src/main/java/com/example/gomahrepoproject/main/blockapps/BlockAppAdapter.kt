package com.example.gomahrepoproject.main.blockapps

import android.content.pm.PackageManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.gomahrepoproject.R
import com.example.gomahrepoproject.main.data.AppModel

class BlockAppAdapter(
    private val onAppClick: (AppModel) -> Unit
) : ListAdapter<AppModel, BlockAppAdapter.AppViewHolder>(AppDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.block_apps_item, parent, false)
        return AppViewHolder(view, parent.context.packageManager)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val app = getItem(position)
        holder.bind(app)
    }

    inner class AppViewHolder(itemView: View, private val packageManager: PackageManager) :
        RecyclerView.ViewHolder(itemView) {

        private val appIconImageView: ImageView = itemView.findViewById(R.id.ivAppIcon)
        private val appNameTextView: TextView = itemView.findViewById(R.id.tvAppName)
        private val statusIndicator: ImageView = itemView.findViewById(R.id.ivAddBtn)

        fun bind(app: AppModel) {
            appNameTextView.text = app.appName

            try {
                val appIcon = packageManager.getApplicationIcon(app.packageName)
                appIconImageView.setImageDrawable(appIcon)
            } catch (e: PackageManager.NameNotFoundException) {
                appIconImageView.setImageResource(R.drawable.ic_android) // fallback icon
            }

            // Optional: change icon color to indicate status
            val indicatorRes = if (app.isBlocked) R.drawable.ic_blocked else R.drawable.circle_bg
            statusIndicator.setImageResource(indicatorRes)

            itemView.setOnClickListener {
                onAppClick(app)
            }
        }
    }

    class AppDiffCallback : DiffUtil.ItemCallback<AppModel>() {
        override fun areItemsTheSame(oldItem: AppModel, newItem: AppModel): Boolean {
            return oldItem.packageName == newItem.packageName
        }

        override fun areContentsTheSame(oldItem: AppModel, newItem: AppModel): Boolean {
            return oldItem == newItem
        }
    }
}