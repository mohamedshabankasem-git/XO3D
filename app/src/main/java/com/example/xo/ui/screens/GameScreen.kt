package com.example.xo.ui.screens
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.xo.engine.Cell
import com.example.xo.engine.GameMode
import com.example.xo.ui.game.GameUiState
@Composable fun GameScreen(state:GameUiState,onMove:(Int)->Unit,onRestart:()->Unit,onBack:()->Unit) {
 Surface(Modifier.fillMaxSize(),color=MaterialTheme.colorScheme.background) {
  Column(Modifier.fillMaxSize().padding(20.dp),verticalArrangement=Arrangement.SpaceBetween) {
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) { TextButton(onClick=onBack){Text("Back")}; Text("XO3D",style=MaterialTheme.typography.titleLarge); TextButton(onClick=onRestart){Text("Restart")} }
   Column { Text(state.status,style=MaterialTheme.typography.headlineSmall,color=MaterialTheme.colorScheme.primary); Text(if(state.match.mode==GameMode.SOLO) "You: X    Computer: O" else "Local two-player match") }
   GameBoard3D(state.match.board.cells,!state.match.board.isFinished && !state.isThinking,onMove,Modifier.fillMaxWidth().aspectRatio(1f))
   Column { if(state.match.board.isFinished) Button(onClick=onRestart,modifier=Modifier.fillMaxWidth()){Text("Play Again")}; Text("Tap an empty tile to make your move.",style=MaterialTheme.typography.bodySmall) }
  }
 }
}
@Composable private fun GameBoard3D(cells:List<Cell>,enabled:Boolean,onMove:(Int)->Unit,modifier:Modifier=Modifier) {
 Canvas(modifier.clip(RoundedCornerShape(26.dp)).background(Color(0xFF172635)).graphicsLayer { rotationX=10f; shadowElevation=18.dp.toPx(); shape=RoundedCornerShape(26.dp); clip=true }.pointerInput(cells,enabled) {
  detectTapGestures { p -> if(enabled) { val col=(p.x/(size.width/3f)).toInt().coerceIn(0,2); val row=(p.y/(size.height/3f)).toInt().coerceIn(0,2); onMove(row*3+col) } }
 }) {
  val w=size.width/3f; val h=size.height/3f
  drawRoundRect(Color(0xFF0B121B),topLeft=Offset(7.dp.toPx(),12.dp.toPx()),size=Size(size.width-14.dp.toPx(),size.height-20.dp.toPx()),cornerRadius=androidx.compose.ui.geometry.CornerRadius(24.dp.toPx()))
  for(r in 0..2) for(c in 0..2) {
   val left=c*w+7.dp.toPx(); val top=r*h+7.dp.toPx()
   drawRoundRect(Brush.linearGradient(listOf(Color(0xFF395365),Color(0xFF1D3040))),Offset(left,top),Size(w-14.dp.toPx(),h-14.dp.toPx()),cornerRadius=androidx.compose.ui.geometry.CornerRadius(17.dp.toPx()))
   val x=c*w+w/2; val y=r*h+h/2; val rad=minOf(w,h)*0.24f
   when(cells[r*3+c]) {
    Cell.X -> { drawLine(Color(0xFF8CE6D0),Offset(x-rad,y-rad),Offset(x+rad,y+rad),13.dp.toPx(),cap=StrokeCap.Round); drawLine(Color(0xFF8CE6D0),Offset(x+rad,y-rad),Offset(x-rad,y+rad),13.dp.toPx(),cap=StrokeCap.Round) }
    Cell.O -> drawCircle(Color(0xFFFFC46B),rad,Offset(x,y),style=Stroke(13.dp.toPx()))
    Cell.EMPTY -> Unit
   }
  }
 }
}
