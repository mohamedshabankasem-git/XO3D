package com.example.xo.ui.screens

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import kotlin.random.Random
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.xo.engine.Cell
import com.example.xo.engine.GameMode
import com.example.xo.ui.AppLanguage
import com.example.xo.ui.translate
import com.example.xo.ui.game.GameUiState

@Composable
fun GameScreen(
    state: GameUiState,
    language: AppLanguage,
    onMove: (Int) -> Unit,
    onReconnect: () -> Unit,
    onRestart: () -> Unit,
    onBack: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var showExitConfirmation by remember { mutableStateOf(false) }

    // Intercept the Android system Back action while a match is open.
    BackHandler(enabled = true) {
        menuExpanded = false
        showExitConfirmation = true
    }

    if (showExitConfirmation) {
        AlertDialog(
            onDismissRequest = { showExitConfirmation = false },
            title = { Text(translate("Exit game?", language)) },
            text = { Text(translate("Do you want to exit the game?", language)) },
            confirmButton = {
                TextButton(onClick = {
                    showExitConfirmation = false
                    onBack()
                }) {
                    Text(translate("Exit", language))
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitConfirmation = false }) {
                    Text(translate("Stay", language))
                }
            }
        )
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.fillMaxSize().padding(start = 18.dp, end = 18.dp, top = 28.dp, bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("XO3D", style = MaterialTheme.typography.titleLarge)
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        ThreeDMenuIcon("⋮")
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) { ThreeDMenuIcon("↻"); Text(translate("Restart", language)) } },
                            onClick = {
                                menuExpanded = false
                                onRestart()
                            }
                        )
                        DropdownMenuItem(
                            text = { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) { ThreeDMenuIcon("⌂"); Text(translate("Main Menu", language)) } },
                            onClick = {
                                menuExpanded = false
                                showExitConfirmation = true
                            }
                        )
                    }
                }
            }

            Text(
                "${translate("Wins", language)}  X: ${state.winsX}  •  O: ${state.winsO}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(Modifier.height(10.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    translate(state.status, language),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(5.dp))
                if (state.match.mode == GameMode.BLUETOOTH && state.connectionStatus.contains("Connection lost")) {
                    OutlinedButton(onClick = onReconnect) { Text(translate("Reconnect", language)) }
                }
                if (state.match.mode != GameMode.LOCAL) {
                    Text(
                        if (state.match.mode == GameMode.SOLO) translate("You: X    •    Computer: O", language)
                        else translate("Player X    •    Player O", language),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            if (state.match.mode == GameMode.LOCAL) {
                // Two players sit on opposite sides of the same device:
                // keep the board centered and orient each name toward its player.
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        translate("Player O", language),
                        modifier = Modifier.align(Alignment.TopCenter)
                            .padding(top = 12.dp)
                            .graphicsLayer { rotationZ = 180f },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    GameBoard3D(
                        state.match.board.cells,
                        !state.match.board.isFinished && !state.isThinking,
                        onMove,
                        isFlat = true,
                        modifier = Modifier.fillMaxWidth().aspectRatio(1f)
                    )
                    Text(
                        translate("Player X", language),
                        modifier = Modifier.align(Alignment.BottomCenter)
                            .padding(bottom = 12.dp),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                Spacer(Modifier.weight(0.22f))
                GameBoard3D(
                    state.match.board.cells,
                    !state.match.board.isFinished && !state.isThinking && state.connectionStatus != "Connection lost" && state.connectionStatus != "Reconnecting…",
                    onMove,
                    isFlat = false,
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f)
                )
                Spacer(Modifier.weight(0.12f))
            }
            Text(
                if (state.match.board.isFinished) translate("Match finished", language)
                else translate("Tap an empty tile to make your move.", language),
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun GameBoard3D(
    cells: List<Cell>,
    enabled: Boolean,
    onMove: (Int) -> Unit,
    isFlat: Boolean,
    modifier: Modifier = Modifier
) {
    val clickTone = remember { ToneGenerator(AudioManager.STREAM_SYSTEM, 18) }
    DisposableEffect(clickTone) { onDispose { clickTone.release() } }
    val transition = rememberInfiniteTransition(label = "boardColorWave")
    val wavePhase by transition.animateFloat(initialValue = 0f, targetValue = (2f * Math.PI).toFloat(), animationSpec = infiniteRepeatable(tween(durationMillis = 12000, easing = LinearEasing)), label = "wavePhase")
    val phases = remember { List(9) { Random.nextFloat() * (2f * Math.PI).toFloat() } }
    Canvas(
        modifier.clip(RoundedCornerShape(26.dp))
            .background(Color(0xFF172635))
            .graphicsLayer {
                // A flat, straight-on board is easier to read from both sides
                // when two players share the same device.
                rotationX = if (isFlat) 0f else 10f
                shadowElevation = if (isFlat) 8.dp.toPx() else 18.dp.toPx()
                shape = RoundedCornerShape(26.dp)
                clip = true
            }
            .pointerInput(cells, enabled) {
                detectTapGestures { p ->
                    if (enabled) {
                        val col = (p.x / (size.width / 3f)).toInt().coerceIn(0, 2)
                        val row = (p.y / (size.height / 3f)).toInt().coerceIn(0, 2)
                        val index = row * 3 + col
                        if (cells[index] == Cell.EMPTY) {
                            clickTone.startTone(ToneGenerator.TONE_PROP_ACK, 22)
                            onMove(index)
                        }
                    }
                }
            }
    ) {
        val w = size.width / 3f
        val h = size.height / 3f
        drawRoundRect(
            animatedWaveColor(wavePhase, 0.13f, 0.58f),
            topLeft = Offset(7.dp.toPx(), 12.dp.toPx()),
            size = Size(size.width - 14.dp.toPx(), size.height - 20.dp.toPx()),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(24.dp.toPx())
        )
        for (r in 0..2) for (c in 0..2) {
            val left = c * w + 7.dp.toPx()
            val top = r * h + 7.dp.toPx()
            drawRoundRect(
                Brush.linearGradient(listOf(animatedWaveColor(wavePhase + phases[r * 3 + c], 0.08f, 0.46f), animatedWaveColor(wavePhase + phases[r * 3 + c] + 2.1f, 0.18f, 0.33f))),
                Offset(left, top),
                Size(w - 14.dp.toPx(), h - 14.dp.toPx()),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(17.dp.toPx())
            )
            val x = c * w + w / 2
            val y = r * h + h / 2
            val rad = minOf(w, h) * 0.24f
            when (cells[r * 3 + c]) {
                Cell.X -> {
                    drawLine(animatedWaveColor(wavePhase + phases[r * 3 + c], 0.34f, 0.84f), Offset(x-rad,y-rad), Offset(x+rad,y+rad), 13.dp.toPx(), cap=StrokeCap.Round)
                    drawLine(Color(0xFF8CE6D0), Offset(x+rad,y-rad), Offset(x-rad,y+rad), 13.dp.toPx(), cap=StrokeCap.Round)
                }
                Cell.O -> drawCircle(animatedWaveColor(wavePhase + phases[r * 3 + c] + 2.4f, 0.30f, 0.88f), rad, Offset(x,y), style=Stroke(13.dp.toPx()))
                Cell.EMPTY -> Unit
            }
        }
    }
}


private fun animatedWaveColor(phase: Float, saturation: Float, value: Float): Color {
    val hue = ((phase / (2f * Math.PI).toFloat() * 360f) % 360f + 360f) % 360f
    return Color.hsv(hue, saturation, value)
}


@Composable
private fun ThreeDMenuIcon(symbol: String) {
    Surface(
        modifier = Modifier.size(38.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color.Transparent,
        shadowElevation = 7.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF8DAFC4))
    ) {
        Box(
            modifier = Modifier.background(Brush.linearGradient(listOf(Color(0xFF5B829B), Color(0xFF172D43)))),
            contentAlignment = Alignment.Center
        ) {
            Text(symbol, color = Color.White, style = MaterialTheme.typography.titleLarge)
        }
    }
}
