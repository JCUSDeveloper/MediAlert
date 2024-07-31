package com.example.medialert

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView

class DeviceListAdapter(context: Context, private val devices: List<String>) :
    ArrayAdapter<String>(context, 0, devices) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.list_item, parent, false)

        val deviceNameTextView = view.findViewById<TextView>(R.id.device_name)
        val deviceIconImageView = view.findViewById<ImageView>(R.id.device_icon)

        val deviceInfo = getItem(position) ?: "Unknown Device"

        deviceNameTextView.text = deviceInfo
        deviceIconImageView.setImageResource(R.drawable.blue_connect) // Cambia el ícono según sea necesario

        return view
    }
}
