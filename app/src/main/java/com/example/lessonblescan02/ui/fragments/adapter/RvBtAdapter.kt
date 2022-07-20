package com.example.lessonblescan02.ui.fragments.adapter

import android.bluetooth.BluetoothDevice
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.lessonblescan02.R

class RvBtAdapter : RecyclerView.Adapter<RvBtHolder>() {
    private val bluetoothDevices = mutableListOf<BluetoothDevice>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RvBtHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.layout_device, parent, false)
        return RvBtHolder(view)
    }

    override fun onBindViewHolder(holder: RvBtHolder, position: Int) {
        holder.bind(bluetoothDevices[position])
    }

    override fun getItemCount(): Int = bluetoothDevices.size

    fun addDevice(bluetoothDevice: BluetoothDevice) {
        if (!bluetoothDevices.contains(bluetoothDevice)) {
            bluetoothDevices.add(bluetoothDevice)
            notifyItemInserted(bluetoothDevices.size - 1)
        }
    }

    fun clear() {
        val size = bluetoothDevices.size
        bluetoothDevices.clear()
        notifyItemRangeRemoved(0, size)
    }
}