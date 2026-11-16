package com.example.xo.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.Closeable
import java.util.UUID

class BluetoothClientManager(private val adapter: BluetoothAdapter) : Closeable {
    private var activeSocket: BluetoothSocket? = null

    @SuppressLint("MissingPermission")
    suspend fun connect(device: BluetoothDevice, serviceId: UUID): BluetoothSocket =
        withContext(Dispatchers.IO) {
            close()
            adapter.cancelDiscovery()
            val socket = device.createRfcommSocketToServiceRecord(serviceId)
            try {
                socket.connect()
                activeSocket = socket
                socket
            } catch (error: Throwable) {
                runCatching { socket.close() }
                throw error
            }
        }

    override fun close() {
        runCatching { activeSocket?.close() }
        activeSocket = null
    }
}
