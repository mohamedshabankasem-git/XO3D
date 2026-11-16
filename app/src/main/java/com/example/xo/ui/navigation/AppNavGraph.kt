package com.example.xo.ui.navigation
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.xo.engine.Difficulty
import com.example.xo.engine.GameMode
import com.example.xo.ui.game.GameViewModel
import com.example.xo.ui.screens.GameScreen
import com.example.xo.ui.screens.StartScreen
@Composable fun AppNavGraph(vm:GameViewModel=viewModel()) {
 var playing by remember { mutableStateOf(false) }
 val state by vm.state.collectAsState()
 if(playing) GameScreen(state,vm::play,vm::restart) { playing=false }
 else StartScreen { mode,difficulty -> vm.start(mode,difficulty); playing=true }
}
