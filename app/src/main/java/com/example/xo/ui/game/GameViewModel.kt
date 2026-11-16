package com.example.xo.ui.game
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xo.engine.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
data class GameUiState(val match:Match=Match(),val isThinking:Boolean=false) {
 val status:String get()=when { match.board.winner!=null -> "${match.board.winner} wins!"; match.board.isDraw -> "Draw"; isThinking -> "Computer is thinking…"; else -> "Player ${match.currentPlayer}'s turn" }
}
class GameViewModel:ViewModel() {
 private val _state=MutableStateFlow(GameUiState())
 val state=_state.asStateFlow()
 fun start(mode:GameMode,difficulty:Difficulty=Difficulty.MEDIUM) { _state.value=GameUiState(Match(mode=mode,difficulty=difficulty)) }
 fun play(index:Int) {
  val current=_state.value
  if(current.isThinking || current.match.board.isFinished) return
  val updated=runCatching { current.match.play(index) }.getOrNull() ?: return
  _state.value=current.copy(match=updated)
  if(updated.mode==GameMode.SOLO && !updated.board.isFinished && updated.currentPlayer==Cell.O) {
   _state.value=_state.value.copy(isThinking=true)
   viewModelScope.launch {
    val move=withContext(Dispatchers.Default) { Minimax.chooseMove(updated.board,updated.difficulty,Cell.O) }
    val latest=_state.value
    val next=move?.let { runCatching { latest.match.play(it) }.getOrNull() }
    _state.value=if(next!=null) GameUiState(next) else latest.copy(isThinking=false)
   }
  }
 }
 fun restart() { _state.value=GameUiState(_state.value.match.reset()) }
}
