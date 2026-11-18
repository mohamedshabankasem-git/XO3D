package com.example.xo.ui.game

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.xo.bluetooth.BluetoothGameSession
import com.example.xo.bluetooth.BluetoothSignal
import com.example.xo.bluetooth.BluetoothSocketChannel
import com.example.xo.bluetooth.SignalType
import com.example.xo.engine.*
import kotlinx.coroutines.CancellationException
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
    val connectionStatus: String = "",
    val winsX: Int = 0,
    val winsO: Int = 0
) {
    val status: String get() = when {
        match.board.winner != null -> "${match.board.winner} wins!"
        match.board.isDraw -> "Draw"
        isThinking -> "Computer is thinking…"
        match.mode == GameMode.BLUETOOTH && connectionStatus.isNotBlank() -> connectionStatus
        else -> "Player ${match.currentPlayer}'s turn"
    }
}

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences = application.getSharedPreferences("xo3d_scores", Context.MODE_PRIVATE)
    private var winsX = preferences.getInt("wins_x", 0)
    private var winsO = preferences.getInt("wins_o", 0)
    private val _state = MutableStateFlow(GameUiState(winsX = winsX, winsO = winsO))
    val state = _state.asStateFlow()

    private fun updateMatch(
        current: GameUiState,
        next: Match,
        connectionStatus: String = current.connectionStatus
    ): GameUiState {
        if (!current.match.board.isFinished && next.board.isFinished) {
            when (next.board.winner) {
                Cell.X -> winsX++
                Cell.O -> winsO++
                else -> Unit
            }
            // Commit synchronously so the latest score is safely on disk
            // even if the user immediately leaves or closes the app.
            check(preferences.edit()
                .putInt("wins_x", winsX)
                .putInt("wins_o", winsO)
                .commit()) { "Could not save win totals." }
        }
        return current.copy(
            match = next,
            connectionStatus = connectionStatus,
            winsX = winsX,
            winsO = winsO
        )
    }

    private var bluetoothChannel: BluetoothSocketChannel? = null
    private var bluetoothMatchId: String = ""
    private var bluetoothIsHost = true
    private var bluetoothSequence = 0L
    private var bluetoothSession: BluetoothGameSession? = null
    private var receiveJob: Job? = null
    private var reconnectAction: (suspend () -> BluetoothSocketChannel)? = null

    fun start(mode: GameMode, difficulty: Difficulty = Difficulty.MEDIUM) {
        disconnectBluetooth()
        _state.value = GameUiState(Match(mode = mode, difficulty = difficulty), winsX = winsX, winsO = winsO)
    }

    fun attachBluetooth(
        channel: BluetoothSocketChannel,
        isHost: Boolean,
        matchId: String,
        reconnect: suspend () -> BluetoothSocketChannel
    ) {
        disconnectBluetooth()
        bluetoothMatchId = matchId
        bluetoothIsHost = isHost
        bluetoothSequence = 0L
        bluetoothSession = BluetoothGameSession(matchId, isHost)
        reconnectAction = reconnect
        _state.value = GameUiState(
            Match(mode = GameMode.BLUETOOTH),
            connectionStatus = if (isHost) "Your turn" else "Opponent turn",
            winsX = winsX,
            winsO = winsO
        )
        bluetoothChannel = channel
        listenForBluetoothMessages(channel)
    }

    private fun listenForBluetoothMessages(channel: BluetoothSocketChannel) {
        receiveJob?.cancel()
        receiveJob = viewModelScope.launch {
            try {
                while (true) {
                    val signal = channel.receive()
                    if (signal.matchId != bluetoothMatchId) continue
                    val accepted = bluetoothSession?.accept(signal) ?: continue
                    bluetoothSession = accepted
                    if (signal.type == SignalType.GAME_START && signal.payload == "RESET") {
                        val current = _state.value
                        _state.value = current.copy(
                            match = current.match.reset(),
                            isThinking = false,
                            connectionStatus = if (bluetoothIsHost) "Your turn" else "Opponent turn"
                        )
                        continue
                    }
                    if (signal.type != SignalType.MOVE) continue
                    val index = signal.payload.toIntOrNull() ?: continue
                    if (index !in 0..8) continue
                    val current = _state.value
                    val remoteMark = if (bluetoothIsHost) Cell.O else Cell.X
                    if (current.match.currentPlayer != remoteMark || current.match.board.isFinished) continue
                    val updated = runCatching { current.match.play(index) }.getOrNull() ?: continue
                    val localMark = if (bluetoothIsHost) Cell.X else Cell.O
                    _state.value = updateMatch(
                        current,
                        updated,
                        if (updated.currentPlayer == localMark) "Your turn" else "Opponent turn"
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                channel.close()
                if (bluetoothChannel === channel) {
                    _state.value = _state.value.copy(connectionStatus = "Connection lost")
                }
            }
        }
    }

    fun reconnect() {
        val reconnect = reconnectAction ?: return
        if (_state.value.connectionStatus == "Reconnecting…") return
        _state.value = _state.value.copy(connectionStatus = "Reconnecting…")
        viewModelScope.launch {
            try {
                val channel = reconnect()
                if (bluetoothIsHost) {
                    val match = _state.value.match
                    channel.send(BluetoothSignal(
                        matchId = bluetoothMatchId,
                        messageId = UUID.randomUUID().toString(),
                        type = SignalType.GAME_STATE,
                        payload = "${match.currentPlayer.name}:${match.board.encode()}:${bluetoothSession?.lastAcceptedSequence ?: bluetoothSequence}"
                    ))
                } else {
                    val snapshot = channel.receive()
                    check(snapshot.type == SignalType.GAME_STATE && snapshot.matchId == bluetoothMatchId) {
                        "The host did not send the current match state."
                    }
                    val parts = snapshot.payload.split(":", limit = 3)
                    check(parts.size == 3 && parts[0] in listOf("X", "O")) { "Invalid match state." }
                    val restoredSequence = parts[2].toLongOrNull()
                    check(restoredSequence != null && restoredSequence >= 0L) { "Invalid match sequence." }
                    bluetoothSequence = restoredSequence
                    bluetoothSession = BluetoothGameSession(
                        matchId = bluetoothMatchId,
                        isHost = false,
                        lastAcceptedSequence = restoredSequence
                    )
                    _state.value = _state.value.copy(
                        match = Match(board = Board.from(parts[1]), currentPlayer = Cell.valueOf(parts[0]), mode = GameMode.BLUETOOTH)
                    )
                }
                bluetoothChannel = channel
                val localMark = if (bluetoothIsHost) Cell.X else Cell.O
                _state.value = _state.value.copy(
                    connectionStatus = if (_state.value.match.currentPlayer == localMark) "Your turn" else "Opponent turn"
                )
                listenForBluetoothMessages(channel)
            } catch (error: Exception) {
                _state.value = _state.value.copy(connectionStatus = "Connection lost • Tap reconnect")
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
        _state.value = updateMatch(current, updated)
        if (updated.mode == GameMode.BLUETOOTH) {
            val localMark = if (bluetoothIsHost) Cell.X else Cell.O
            _state.value = _state.value.copy(connectionStatus = if (updated.currentPlayer == localMark) "Your turn" else "Opponent turn")
            val sequence = (bluetoothSession?.lastAcceptedSequence ?: bluetoothSequence) + 1L
            bluetoothSequence = sequence
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
                _state.value = if (next != null) updateMatch(latest, next).copy(isThinking = false) else latest.copy(isThinking = false)
            }
        }
    }

    fun restart() {
        val current = _state.value
        _state.value = current.copy(
            match = current.match.reset(),
            isThinking = false,
            connectionStatus = if (current.match.mode == GameMode.BLUETOOTH) {
                if (bluetoothIsHost) "Your turn" else "Opponent turn"
            } else current.connectionStatus
        )
        if (current.match.mode == GameMode.BLUETOOTH) {
            val signal = BluetoothSignal(
                matchId = bluetoothMatchId,
                messageId = UUID.randomUUID().toString(),
                type = SignalType.GAME_START,
                payload = "RESET"
            )
            bluetoothSession = bluetoothSession?.accept(signal)
            viewModelScope.launch {
                runCatching { bluetoothChannel?.send(signal) }.onFailure {
                    _state.value = _state.value.copy(connectionStatus = "Connection lost")
                }
            }
        }
    }

    fun disconnectBluetooth() {
        receiveJob?.cancel()
        receiveJob = null
        bluetoothChannel?.close()
        bluetoothChannel = null
        bluetoothSession = null
        reconnectAction = null
        bluetoothMatchId = ""
        bluetoothSequence = 0L
    }

    override fun onCleared() {
        disconnectBluetooth()
        super.onCleared()
    }
}
