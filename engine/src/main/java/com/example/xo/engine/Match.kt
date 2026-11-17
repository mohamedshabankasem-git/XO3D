package com.example.xo.engine
data class Match(val board: Board = Board(), val currentPlayer: Cell = Cell.X, val mode: GameMode = GameMode.SOLO, val difficulty: Difficulty = Difficulty.MEDIUM) {
 fun play(index: Int): Match { val next = board.place(index, currentPlayer); return copy(board = next, currentPlayer = currentPlayer.opponent()) }
 fun reset() = copy(board = Board(), currentPlayer = Cell.X)
}
