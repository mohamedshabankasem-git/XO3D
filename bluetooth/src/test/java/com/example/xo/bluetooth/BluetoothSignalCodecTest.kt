package com.example.xo.bluetooth
import org.junit.Assert.*
import org.junit.Test
class BluetoothSignalCodecTest {
 @Test fun roundTrip() { val s=BluetoothSignal(matchId="m1",messageId="id1",type=SignalType.MOVE,sequence=1,payload="4"); assertEquals(s,BluetoothSignalCodec.decode(BluetoothSignalCodec.encode(s))) }
 @Test(expected=IllegalArgumentException::class) fun malformedRejected() { BluetoothSignalCodec.decode("bad") }
 @Test fun duplicateMoveRejected() { val s=BluetoothSignal(matchId="m",messageId="i",type=SignalType.MOVE,sequence=1); val session=BluetoothGameSession("m",true).accept(s)!!; assertNull(session.accept(s)) }
}
