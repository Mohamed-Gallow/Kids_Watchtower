package com.example.gomahrepoproject.main.home

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.example.gomahrepoproject.R

class SpinnerAdapter(
    context: Context,
    private val devices: List<DeviceModel>
) : ArrayAdapter<DeviceModel>(context, R.layout.spinner_item, devices) { // Use your spinner_item layout

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.spinner_item, parent, false)

        val device = devices[position]
        view.findViewById<TextView>(R.id.tvCurrentCharge).text = "${device.batteryLevel}%"

        return view
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return getView(position, convertView, parent) // Reuse the same layout for dropdown
    }
}