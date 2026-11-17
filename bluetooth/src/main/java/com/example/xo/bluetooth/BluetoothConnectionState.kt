package com.example.xo.bluetooth
sealed interface BluetoothConnectionState {
 data object Disconnected:BluetoothConnectionState
 data object Listening:BluetoothConnectionState
 data object Connecting:BluetoothConnectionState
 data object Connected:BluetoothConnectionState
 data class Reconnecting(val attempt:Int):BluetoothConnectionState
 data class Failed(val reason:String):BluetoothConnectionState
}
