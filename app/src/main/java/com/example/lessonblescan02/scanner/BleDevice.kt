package com.example.lessonblescan02.scanner

import android.bluetooth.BluetoothDevice

data class BleDevice (val address: String, val name: String? = null, val bondState:Int = 0) {
    constructor(bluetoothDevice: BluetoothDevice) : this (bluetoothDevice.address,
        bluetoothDevice.name, bluetoothDevice.bondState)
}