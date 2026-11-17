package com.example.xo.ui.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xo.bluetooth.BluetoothGameSession
import com.example.xo.bluetooth.BluetoothSignal
import com.example.xo.bluetooth.BluetoothSignalCodec
import com.example.xo.bluetooth.BluetoothSocketChannel
import com.example.xo.bluetooth.SignalType
import com.example.xo.engine.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

data class GameUiState(
    val match: Match = Match(),
    val isThinking: Boolean = false,
    val connectionStatus: String = ""
) {
    val status: String get() = when {
        match.board.winner != null -> "${match.board.winner} wins!"
        match.board.isDraw -> "Draw"
        isThinking -> "Computer is thinking…"
        match.mode == GameMode.BLUETOOTH && connectionStatus.isNotBlank() -> connectionStatus
        else -> "Player ${match.currentPlayer}'s turn"
    }
}

class GameViewModel : ViewModel() {
    private val _state = MutableStateFlow(GameUiState())
    val state = _state.asStateFlow()

    private var bluetoothChannel: BluetoothSocketChannel? = null
    private var bluetoothMatchId: String = ""
    private var bluetoothIsHost = true
    private var bluetoothSequence = 0L
    private var bluetoothSession: BluetoothGameSession? = null
    private var receiveJob: Job? = null

    fun start(mode: GameMode, difficulty: Difficulty = Difficulty.MEDIUM) {
        disconnectBluetooth()
        _state.value = GameUiState(Match(mode = mode, difficulty = difficulty))
    }

    fun attachBluetooth(channel: BluetoothSocketChannel, isHost: Boolean, matchId: String) {
        disconnectBluetooth()
        bluetoothChannel = channel
        bluetoothMatchId = matchId
        bluetoothIsHost = isHost
        bluetoothSequence = 0L
        bluetoothSession = BluetoothGameSession(matchId, isHost)
        _state.value = GameUiState(Match(mode = GameMode.BLUETOOTH), connectionStatus = "Connected • You are ${if (isHost) "X" else "O"}")
        receiveJob = viewModelScope.launch {
            try {
                while (true) {
                    val signal = channel.receive()
                    if (signal.matchId != bluetoothMatchId || signal.type != SignalType.MOVE) continue
                    val accepted = bluetoothSession?.accept(signal) ?: break
                    bluetoothSession = accepted
                    val index = signal.payload.toIntOrNull() ?: continue
                    if (index !in 0..8) continue
                    val current = _state.value
                    val remoteMark = if (bluetoothIsHost) Cell.O else Cell.X
                    if (current.match.currentPlayer != remoteMark || current.match.board.isFinished) continue
                    val updated = runCatching { current.match.play(index) }.getOrNull() ?: continue
                    _state.value = current.copy(match = updated, connectionStatus = "Connected • Player ${updated.currentPlayer}'s turn")
                }
            } catch (error: Exception) {
                _state.value = _state.value.copy(connectionStatus = "Connection lost")
            }
        }
    }

    fun play(index: Int) {
        val current = _state.value
        if (current.isThinking || current.match.board.isFinished) return
        if (current.match.mode == GameMode.BLUETOOTH) {
            val localMark = if (bluetoothIsHost) Cell.X else Cell.O
            if (current.match.currentPlayer != localMark) return
        }
        val updated = runCatching { current.match.play(index) }.getOrNull() ?: return
        _state.value = current.copy(match = updated)
        if (updated.mode == GameMode.BLUETOOTH) {
            val sequence = ++bluetoothSequence
            val signal = BluetoothSignal(
                matchId = bluetoothMatchId,
                messageId = UUID.randomUUID().toString(),
                type = SignalType.MOVE,
                sequence = sequence,
                payload = index.toString()
            )
            bluetoothSession = bluetoothSession?.accept(signal)
            viewModelScope.launch {
                runCatching { bluetoothChannel?.send(signal) }.onFailure {
                    _state.value = _state.value.copy(connectionStatus = "Connection lost")
                }
            }
            return
        }
        if (updated.mode == GameMode.SOLO && !updated.board.isFinished && updated.currentPlayer == Cell.O) {
            _state.value = _state.value.copy(isThinking = true)
            viewModelScope.launch {
                val move = withContext(Dispatchers.Default) { Minimax.chooseMove(updated.board, updated.difficulty, Cell.O) }
                val latest = _state.value
                if (latest.match != updated) return@launch
                val next = move?.let { runCatching { latest.match.play(it) }.getOrNull() }
                _state.value = if (next != null) GameUiState(next) else latest.copy(isThinking = false)
            }
        }
    }

    fun restart() {
        val current = _state.value
        _state.value = current.copy(match = current.match.reset(), isThinking = false)
    }

    fun disconnectBluetooth() {
        receiveJob?.cancel()
        receiveJob = null
        bluetoothChannel?.close()
        bluetoothChannel = null
        bluetoothSession = null
        bluetoothMatchId = ""
        bluetoothSequence = 0L
    }

    override fun onCleared() {
        disconnectBluetooth()
        super.onCleared()
    }
}
