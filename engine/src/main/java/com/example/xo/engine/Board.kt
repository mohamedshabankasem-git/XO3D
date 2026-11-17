package com.example.xo.engine

data class Board(val cells: List<Cell> = List(9) { Cell.EMPTY }) {
 init { require(cells.size == 9) }
 fun place(index: Int, mark: Cell): Board {
  require(index in 0..8); require(mark != Cell.EMPTY); require(cells[index] == Cell.EMPTY); require(!isFinished)
  return copy(cells = cells.toMutableList().also { it[index] = mark })
 }
 val winner: Cell? get() {
  val lines = listOf(listOf(0,1,2),listOf(3,4,5),listOf(6,7,8),listOf(0,3,6),listOf(1,4,7),listOf(2,5,8),listOf(0,4,8),listOf(2,4,6))
  return lines.firstNotNullOfOrNull { line -> cells[line[0]].takeIf { it != Cell.EMPTY && line.all { i -> cells[i] == it } } }
 }
 val isDraw get() = winner == null && cells.none { it == Cell.EMPTY }
 val isFinished get() = winner != null || isDraw
 val availableMoves get() = cells.indices.filter { cells[it] == Cell.EMPTY }
 fun encode() = cells.joinToString("") { when(it) { Cell.X -> "X"; Cell.O -> "O"; Cell.EMPTY -> "-" } }
 companion object {
  fun from(value: String): Board { require(value.length == 9); return Board(value.map { when(it) { 'X' -> Cell.X; 'O' -> Cell.O; '-' -> Cell.EMPTY; else -> error("Invalid cell") } }) }
 }
}
