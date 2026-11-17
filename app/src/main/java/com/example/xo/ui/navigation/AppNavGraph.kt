package com.example.xo.ui.navigation

import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.xo.engine.Difficulty
import com.example.xo.engine.GameMode
import com.example.xo.ui.game.GameViewModel
import com.example.xo.ui.screens.GameScreen
import com.example.xo.ui.screens.StartScreen
import com.example.xo.ui.screens.ResultScreen
import kotlinx.coroutines.delay

@Composable
fun AppNavGraph(vm: GameViewModel = viewModel()) {
    var playing by remember { mutableStateOf(false) }
    var showResult by remember { mutableStateOf(false) }
    val state by vm.state.collectAsState()

    LaunchedEffect(playing, state.match.board.isFinished, state.match) {
        if (playing && state.match.board.isFinished) {
            // Keep the final move visible before presenting the result actions.
            delay(1200)
            if (playing && state.match.board.isFinished) showResult = true
        } else {
            showResult = false
        }
    }

    if (playing && state.match.board.isFinished && showResult) {
        ResultScreen(state, {
            vm.restart()
            showResult = false
        }, {
            playing = false
            showResult = false
        })
    } else if (playing) {
        GameScreen(
            state = state,
            onMove = vm::play,
            onRestart = {
                vm.restart()
                showResult = false
            },
            onBack = {
                playing = false
                showResult = false
            }
        )
    } else {
        StartScreen { mode: GameMode, difficulty: Difficulty ->
            vm.start(mode, difficulty)
            showResult = false
            playing = true
        }
    }
}
