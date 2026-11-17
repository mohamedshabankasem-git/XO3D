package com.example.xo.bluetooth
enum class SignalType { CONNECT, CONNECTED, GAME_START, MOVE, MOVE_ACK, GAME_STATE, RECONNECT, DISCONNECT, ERROR, PING, PONG }
data class BluetoothSignal(val protocolVersion:Int=1,val matchId:String,val messageId:String,val type:SignalType,val sequence:Long=0,val payload:String="") {
 init { require(protocolVersion==1); require(matchId.isNotBlank() && matchId.length<=64); require(messageId.isNotBlank() && messageId.length<=64); require(sequence>=0); require(payload.length<=512) }
}
