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
 @Test fun acceptsResetWithoutResettingMoveSequence() {
  val first=BluetoothSignal(matchId="game",messageId="m1",type=SignalType.MOVE,sequence=1,payload="0")
  val reset=BluetoothSignal(matchId="game",messageId="reset",type=SignalType.GAME_START,payload="RESET")
  val second=BluetoothSignal(matchId="game",messageId="m2",type=SignalType.MOVE,sequence=2,payload="1")
  val afterFirst=BluetoothGameSession("game",true).accept(first)!!
  val afterReset=afterFirst.accept(reset)!!
  assertEquals(1L,afterReset.lastAcceptedSequence)
  assertNull(afterReset.accept(reset))
  assertEquals(2L,afterReset.accept(second)!!.lastAcceptedSequence)
 }
}
