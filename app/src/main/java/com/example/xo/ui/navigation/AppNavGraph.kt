package com.example.xo.ui.navigation

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.xo.engine.Difficulty
import com.example.xo.engine.GameMode
import com.example.xo.ui.AppLanguage
import com.example.xo.ui.translate
import com.example.xo.ui.game.GameViewModel
import com.example.xo.ui.screens.BluetoothLobbyScreen
import com.example.xo.ui.screens.GameScreen
import com.example.xo.ui.screens.StartScreen
import com.example.xo.ui.screens.ResultScreen
import kotlinx.coroutines.delay

@Composable
fun AppNavGraph(vm: GameViewModel = viewModel()) {
    val context = LocalContext.current
    val preferences = remember(context) { context.getSharedPreferences("xo3d_settings", Context.MODE_PRIVATE) }
    var language by remember {
        mutableStateOf(runCatching {
            AppLanguage.valueOf(preferences.getString("language", AppLanguage.ENGLISH.name)!!)
        }.getOrDefault(AppLanguage.ENGLISH))
    }
    var playing by remember { mutableStateOf(false) }
    var showResult by remember { mutableStateOf(false) }
    var showBluetoothLobby by remember { mutableStateOf(false) }
    var showExitConfirmation by remember { mutableStateOf(false) }
    val state by vm.state.collectAsState()
    val activity = context.findActivity()

    fun changeLanguage(value: AppLanguage) {
        language = value
        preferences.edit().putString("language", value.name).commit()
    }

    BackHandler(enabled = !playing) {
        if (showBluetoothLobby) showBluetoothLobby = false else showExitConfirmation = true
    }

    CompositionLocalProvider(
        LocalLayoutDirection provides if (language == AppLanguage.ARABIC) LayoutDirection.Rtl else LayoutDirection.Ltr
    ) {
        if (showExitConfirmation && !playing && !showBluetoothLobby) {
            AlertDialog(
                onDismissRequest = { showExitConfirmation = false },
                title = { Text(translate("Exit game?", language)) },
                text = { Text(translate("Do you want to exit the game?", language)) },
                confirmButton = {
                    TextButton(onClick = {
                        showExitConfirmation = false
                        activity?.finish()
                    }) { Text(translate("Exit", language)) }
                },
                dismissButton = {
                    TextButton(onClick = { showExitConfirmation = false }) { Text(translate("Stay", language)) }
                }
            )
        }

        LaunchedEffect(playing, state.match.board.isFinished, state.match) {
            if (playing && state.match.board.isFinished) {
                delay(1200)
                if (playing && state.match.board.isFinished) showResult = true
            } else showResult = false
        }

        if (showBluetoothLobby && !playing) {
            BluetoothLobbyScreen(
                language = language,
                onConnected = { channel, isHost, matchId, reconnect ->
                    vm.attachBluetooth(channel, isHost, matchId, reconnect)
                    showBluetoothLobby = false
                    showResult = false
                    playing = true
                },
                onBack = { showBluetoothLobby = false }
            )
        } else if (playing && state.match.board.isFinished && showResult) {
            Box(Modifier.fillMaxSize()) {
                GameScreen(
                    state = state,
                    language = language,
                    onMove = vm::play,
                    onReconnect = vm::reconnect,
                    onRestart = { vm.restart(); showResult = false },
                    onBack = { vm.disconnectBluetooth(); playing = false; showResult = false }
                )
                ResultScreen(
                    state = state,
                    language = language,
                    onAgain = { vm.restart(); showResult = false },
                    onHome = { vm.disconnectBluetooth(); playing = false; showResult = false }
                )
            }
        } else if (playing) {
            GameScreen(
                state = state,
                language = language,
                onMove = vm::play,
                onReconnect = vm::reconnect,
                onRestart = { vm.restart(); showResult = false },
                onBack = { vm.disconnectBluetooth(); playing = false; showResult = false }
            )
        } else {
            StartScreen(
                language = language,
                onLanguageChange = ::changeLanguage,
                onStart = { mode: GameMode, difficulty: Difficulty ->
                    vm.start(mode, difficulty)
                    showResult = false
                    playing = true
                },
                onBluetooth = { showBluetoothLobby = true }
            )
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
