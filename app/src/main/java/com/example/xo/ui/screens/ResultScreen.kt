package com.example.xo.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.xo.engine.Cell
import com.example.xo.ui.AppLanguage
import com.example.xo.ui.translate
import com.example.xo.ui.game.GameUiState

@Composable
fun ResultScreen(
    state: GameUiState,
    language: AppLanguage,
    onAgain: () -> Unit,
    onHome: () -> Unit
) {
    BackHandler(enabled = true, onBack = onHome)
    val result = when (state.match.board.winner) {
        Cell.X -> translate("X wins", language)
        Cell.O -> translate("O wins", language)
        else -> translate("It's a draw", language)
    }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background.copy(alpha = 0.60f)) {
        Column(
            Modifier.fillMaxSize().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(translate("MATCH COMPLETE", language), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.height(16.dp))
            Text(result, style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(32.dp))
            Button(onClick = onAgain, modifier = Modifier.fillMaxWidth()) { Text(translate("Play Again", language)) }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onHome, modifier = Modifier.fillMaxWidth()) { Text(translate("Main Menu", language)) }
        }
    }
}
