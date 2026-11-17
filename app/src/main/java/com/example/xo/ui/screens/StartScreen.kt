package com.example.xo.ui.screens
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.xo.engine.Difficulty
import com.example.xo.engine.GameMode
@Composable fun StartScreen(onStart:(GameMode,Difficulty)->Unit, onBluetooth:()->Unit) {
 var difficulty by remember { mutableStateOf(Difficulty.MEDIUM) }
 Surface(Modifier.fillMaxSize(),color=MaterialTheme.colorScheme.background) {
  Column(Modifier.fillMaxSize().padding(28.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center) {
   Text("XO3D",style=MaterialTheme.typography.displayLarge,color=MaterialTheme.colorScheme.primary)
   Spacer(Modifier.height(8.dp)); Text("A fresh take on the classic game")
   Spacer(Modifier.height(32.dp)); Text("Difficulty",style=MaterialTheme.typography.titleMedium)
   Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) { Difficulty.values().forEach { d -> FilterChip(selected=difficulty==d,onClick={difficulty=d},label={Text(d.name)}) } }
   Spacer(Modifier.height(24.dp))
   Button(onClick={onStart(GameMode.SOLO,difficulty)},modifier=Modifier.fillMaxWidth()) { Text("Play vs Computer") }
   Spacer(Modifier.height(10.dp))
   OutlinedButton(onClick={onStart(GameMode.LOCAL,difficulty)},modifier=Modifier.fillMaxWidth()) { Text("Two Players – Same Device") }
   Spacer(Modifier.height(10.dp))
   OutlinedButton(onClick=onBluetooth,modifier=Modifier.fillMaxWidth()) { Text("Bluetooth Multiplayer") }
  }
 }
}
