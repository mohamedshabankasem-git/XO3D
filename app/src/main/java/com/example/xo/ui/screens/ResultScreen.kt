package com.example.xo.ui.screens
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.xo.engine.Cell
import com.example.xo.ui.game.GameUiState
@Composable fun ResultScreen(state:GameUiState,onAgain:()->Unit,onHome:()->Unit) {
 BackHandler(enabled = true, onBack = onHome)
 val result=when(state.match.board.winner) { Cell.X -> "X wins"; Cell.O -> "O wins"; else -> "It's a draw" }
 Surface(Modifier.fillMaxSize(),color=MaterialTheme.colorScheme.background.copy(alpha = 0.40f)) {
  Column(Modifier.fillMaxSize().padding(28.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center) {
   Text("MATCH COMPLETE",style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.secondary)
   Spacer(Modifier.height(16.dp))
   Text(result,style=MaterialTheme.typography.displaySmall,color=MaterialTheme.colorScheme.primary)
   Spacer(Modifier.height(32.dp))
   Button(onClick=onAgain,modifier=Modifier.fillMaxWidth()) { Text("Play Again") }
   Spacer(Modifier.height(12.dp))
   OutlinedButton(onClick=onHome,modifier=Modifier.fillMaxWidth()) { Text("Main Menu") }
  }
 }
}
