package com.example.xo.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.Closeable
import java.util.UUID

class BluetoothHostManager(private val adapter: BluetoothAdapter) : Closeable {
    private var server: BluetoothServerSocket? = null
    private var activeSocket: BluetoothSocket? = null

    suspend fun awaitPlayer(serviceName: String, serviceId: UUID): BluetoothSocket =
        withContext(Dispatchers.IO) {
            close()
            val listening = adapter.listenUsingRfcommWithServiceRecord(serviceName, serviceId)
            server = listening
            try {
                listening.accept().also { socket ->
                    activeSocket = socket
                    server = null
                }
            } finally {
                runCatching { listening.close() }
            }
        }

    override fun close() {
        runCatching { activeSocket?.close() }
        runCatching { server?.close() }
        activeSocket = null
        server = null
    }
}
