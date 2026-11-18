package com.example.xo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.xo.engine.Difficulty
import com.example.xo.engine.GameMode
import com.example.xo.ui.AppLanguage
import com.example.xo.ui.translate
import com.example.xo.ui.audio.dynamicWaveBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartScreen(
    language: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    onStart: (GameMode, Difficulty) -> Unit,
    onBluetooth: () -> Unit,
    musicEnabled: Boolean,
    onMusicToggle: () -> Unit
) {
    var difficulty by remember { mutableStateOf(Difficulty.MEDIUM) }
    var languageExpanded by remember { mutableStateOf(false) }
    Surface(Modifier.fillMaxSize(), color = dynamicWaveBackground()) {
        Column(
            Modifier.fillMaxSize().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onMusicToggle) {
                    Text(if (musicEnabled) "♫" else "♪", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                }
                Box(Modifier.weight(1f), contentAlignment = Alignment.TopEnd) {
                ExposedDropdownMenuBox(
                    expanded = languageExpanded,
                    onExpandedChange = { languageExpanded = !languageExpanded }
                ) {
                    OutlinedTextField(
                        value = if (language == AppLanguage.ARABIC) "العربية" else "English",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(translate("Language", language)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = languageExpanded) },
                        modifier = Modifier.menuAnchor().width(170.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = languageExpanded,
                        onDismissRequest = { languageExpanded = false }
                    ) {
                        DropdownMenuItem(text = { Text("العربية") }, onClick = {
                            languageExpanded = false
                            onLanguageChange(AppLanguage.ARABIC)
                        })
                        DropdownMenuItem(text = { Text("English") }, onClick = {
                            languageExpanded = false
                            onLanguageChange(AppLanguage.ENGLISH)
                        })
                    }
                }
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("XO3D", style = MaterialTheme.typography.displayLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            Text(translate("A fresh take on the classic game", language))
            Spacer(Modifier.height(32.dp))
            Text(translate("Difficulty", language), style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Difficulty.values().forEach { d ->
                    FilterChip(
                        selected = difficulty == d,
                        onClick = { difficulty = d },
                        label = { Text(translate(d.name, language)) }
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
            Button(onClick = { onStart(GameMode.SOLO, difficulty) }, modifier = Modifier.fillMaxWidth()) {
                Text(translate("Play vs Computer", language))
            }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(onClick = { onStart(GameMode.LOCAL, difficulty) }, modifier = Modifier.fillMaxWidth()) {
                Text(translate("Two Players – Same Device", language))
            }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(onClick = onBluetooth, modifier = Modifier.fillMaxWidth()) {
                Text(translate("Bluetooth Multiplayer", language))
            }
        }
    }
}
