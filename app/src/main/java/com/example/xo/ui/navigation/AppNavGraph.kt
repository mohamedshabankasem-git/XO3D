package com.example.xo.ui.navigation

import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.xo.engine.Difficulty
import com.example.xo.engine.GameMode
import com.example.xo.ui.game.GameViewModel
import com.example.xo.ui.screens.BluetoothLobbyScreen
import com.example.xo.ui.screens.GameScreen
import com.example.xo.ui.screens.StartScreen
import com.example.xo.ui.screens.ResultScreen
import kotlinx.coroutines.delay

@Composable
fun AppNavGraph(vm: GameViewModel = viewModel()) {
    var playing by remember { mutableStateOf(false) }
    var showResult by remember { mutableStateOf(false) }
    var showBluetoothLobby by remember { mutableStateOf(false) }
    val state by vm.state.collectAsState()

    LaunchedEffect(playing, state.match.board.isFinished, state.match) {
        if (playing && state.match.board.isFinished) {
            delay(1200)
            if (playing && state.match.board.isFinished) showResult = true
        } else {
            showResult = false
        }
    }

    if (showBluetoothLobby && !playing) {
        BluetoothLobbyScreen(
            onConnected = { channel, isHost, matchId, reconnect ->
                vm.attachBluetooth(channel, isHost, matchId, reconnect)
                showBluetoothLobby = false
                showResult = false
                playing = true
            },
            onBack = { showBluetoothLobby = false }
        )
    } else if (playing && state.match.board.isFinished && showResult) {
        ResultScreen(state, {
            vm.restart()
            showResult = false
        }, {
            vm.disconnectBluetooth()
            playing = false
            showResult = false
        })
    } else if (playing) {
        GameScreen(
            state = state,
            onMove = vm::play,
            onReconnect = vm::reconnect,
            onRestart = {
                vm.restart()
                showResult = false
            },
            onBack = {
                vm.disconnectBluetooth()
                playing = false
                showResult = false
            }
        )
    } else {
        StartScreen(
            onStart = { mode: GameMode, difficulty: Difficulty ->
                vm.start(mode, difficulty)
                showResult = false
                playing = true
            },
            onBluetooth = { showBluetoothLobby = true }
        )
    }
}
