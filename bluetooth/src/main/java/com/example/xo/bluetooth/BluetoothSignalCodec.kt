package com.example.xo.bluetooth
object BluetoothSignalCodec {
 fun encode(s:BluetoothSignal):String {
  require(s.payload.none { it=='|' || it=='\n' || it=='\r' })
  return listOf(s.protocolVersion,s.matchId,s.messageId,s.type.name,s.sequence,s.payload).joinToString("|")
 }
 fun decode(raw:String):BluetoothSignal {
  require(raw.length<=768)
  val p=raw.split("|",limit=6); require(p.size==6)
  return BluetoothSignal(p[0].toInt(),p[1],p[2],SignalType.valueOf(p[3]),p[4].toLong(),p[5])
 }
}
