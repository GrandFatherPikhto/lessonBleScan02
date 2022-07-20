package com.example.lessonblescan02.ui.fragments.adapter

import android.bluetooth.BluetoothDevice
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.example.lessonblescan02.R
import com.example.lessonblescan02.databinding.LayoutDeviceBinding

class RvBtHolder (view: View) : RecyclerView.ViewHolder(view) {
    private val binding = LayoutDeviceBinding.bind(view)

    fun bind(bluetoothDevice: BluetoothDevice) {
        binding.apply {
            tvName.text = bluetoothDevice.name ?: itemView.context.getString(R.string.default_ble_name)
            tvAddress.text = bluetoothDevice.address
        }
    }
}