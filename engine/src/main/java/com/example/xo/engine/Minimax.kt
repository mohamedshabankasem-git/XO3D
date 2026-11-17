package com.example.xo.engine
import kotlin.math.max
import kotlin.math.min
object Minimax {
 fun chooseMove(board: Board, difficulty: Difficulty, computer: Cell = Cell.O): Int? {
  val moves = board.availableMoves
  if (moves.isEmpty() || board.isFinished) return null
  if (difficulty == Difficulty.EASY || (difficulty == Difficulty.MEDIUM && kotlin.random.Random.nextInt(100) < 35)) return moves.random()
  return moves.maxByOrNull { minimax(board.place(it, computer), computer.opponent(), computer, 0, Int.MIN_VALUE, Int.MAX_VALUE) }
 }
 private fun minimax(board: Board, player: Cell, ai: Cell, depth: Int, alpha0: Int, beta0: Int): Int {
  board.winner?.let { return if (it == ai) 10-depth else depth-10 }
  if (board.isDraw) return 0
  var alpha = alpha0; var beta = beta0
  if (player == ai) {
   var best = Int.MIN_VALUE
   for (move in board.availableMoves) { best = max(best, minimax(board.place(move, player), player.opponent(), ai, depth+1, alpha, beta)); alpha = max(alpha,best); if (beta <= alpha) break }
   return best
  }
  var best = Int.MAX_VALUE
  for (move in board.availableMoves) { best = min(best, minimax(board.place(move, player), player.opponent(), ai, depth+1, alpha, beta)); beta = min(beta,best); if (beta <= alpha) break }
  return best
 }
}
