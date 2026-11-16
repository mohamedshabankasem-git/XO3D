package com.example.xo.engine
import org.junit.Assert.*
import org.junit.Test
class BoardTest {
 @Test fun horizontalWin() { assertEquals(Cell.X, Board.from("XXXOO----").winner) }
 @Test fun verticalWin() { assertEquals(Cell.X, Board.from("X-OX-OX--").winner) }
 @Test fun diagonalWin() { assertEquals(Cell.O, Board.from("O-X-O-X--").winner) }
 @Test fun draw() { assertTrue(Board.from("XOXOOXXXO").isDraw) }
 @Test(expected=IllegalArgumentException::class) fun occupiedCellRejected() { Board.from("X--------").place(0,Cell.O) }
 @Test fun encodeDecode() { assertEquals("XO-------",Board.from("XO-------").encode()) }
}
