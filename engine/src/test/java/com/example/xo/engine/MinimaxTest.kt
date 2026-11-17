package com.example.xo.engine
import org.junit.Assert.*
import org.junit.Test
class MinimaxTest {
 @Test fun takesWin() { assertEquals(2,Minimax.chooseMove(Board.from("OO-X-X---"),Difficulty.HARD)) }
 @Test fun blocksLoss() { assertEquals(2,Minimax.chooseMove(Board.from("XX-O-----"),Difficulty.HARD)) }
 @Test fun noMoveAfterWin() { assertNull(Minimax.chooseMove(Board.from("XXXOO----"),Difficulty.HARD)) }
}
