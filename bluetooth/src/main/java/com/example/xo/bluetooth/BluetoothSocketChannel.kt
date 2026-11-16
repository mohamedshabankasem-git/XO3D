package com.example.xo.bluetooth

import android.bluetooth.BluetoothSocket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.Closeable
import java.io.ByteArrayOutputStream
import java.io.IOException

class BluetoothSocketChannel(private val socket: BluetoothSocket) : Closeable {
    private val writeMutex = Mutex()
    suspend fun send(signal: BluetoothSignal) = withContext(Dispatchers.IO) {
        val bytes = (BluetoothSignalCodec.encode(signal) + "
").toByteArray(Charsets.UTF_8)
        socket.outputStream.write(bytes)
        socket.outputStream.flush()
    }

    suspend fun receive(): BluetoothSignal = withContext(Dispatchers.IO) {
        val output = ByteArrayOutputStream()
        while (true) {
            val next = socket.inputStream.read()
            if (next < 0) throw IOException("Bluetooth connection closed.")
            if (next == 10) break
            if (next != 13) {
                if (output.size() >= 768) throw IOException("Bluetooth message exceeds the protocol limit.")
                output.write(next)
            }
        }
        BluetoothSignalCodec.decode(output.toString("UTF-8"))
    }

    override fun close() {
        runCatching { socket.close() }
    }
}
