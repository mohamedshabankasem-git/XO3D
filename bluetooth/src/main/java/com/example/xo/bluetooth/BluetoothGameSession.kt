package com.example.xo.bluetooth
data class BluetoothGameSession(val matchId:String,val isHost:Boolean,val lastAcceptedSequence:Long=0,val seenMessageIds:Set<String> = emptySet()) {
 fun accept(signal:BluetoothSignal):BluetoothGameSession? {
  if(signal.matchId!=matchId || signal.messageId in seenMessageIds) return null
  if(signal.type==SignalType.MOVE && signal.sequence!=lastAcceptedSequence+1) return null
  return copy(lastAcceptedSequence=if(signal.type==SignalType.MOVE) signal.sequence else lastAcceptedSequence,seenMessageIds=(seenMessageIds+signal.messageId).toList().takeLast(128).toSet())
 }
}
