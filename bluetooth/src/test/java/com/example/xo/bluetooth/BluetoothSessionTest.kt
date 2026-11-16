package com.example.xo.bluetooth
import org.junit.Assert.*
import org.junit.Test
class BluetoothSessionTest {
 @Test fun rejectsOutOfOrderMove() {
  val session=BluetoothGameSession("game",true)
  val skipped=BluetoothSignal(matchId="game",messageId="m2",type=SignalType.MOVE,sequence=2)
  assertNull(session.accept(skipped))
 }
 @Test fun acceptsSequentialMoves() {
  val first=BluetoothSignal(matchId="game",messageId="m1",type=SignalType.MOVE,sequence=1)
  val second=BluetoothSignal(matchId="game",messageId="m2",type=SignalType.MOVE,sequence=2)
  val state=BluetoothGameSession("game",true).accept(first)!!
  assertEquals(2L,state.accept(second)!!.lastAcceptedSequence)
 }
}
