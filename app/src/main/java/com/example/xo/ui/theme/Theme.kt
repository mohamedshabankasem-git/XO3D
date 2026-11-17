package com.example.xo.ui.theme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
private val Palette=darkColorScheme(primary=Color(0xFF8CE6D0),secondary=Color(0xFFFFC46B),background=Color(0xFF101923),surface=Color(0xFF1B2937),onPrimary=Color(0xFF09231F),onBackground=Color(0xFFF3F7FA),onSurface=Color(0xFFF3F7FA))
@Composable fun XO3DTheme(content:@Composable ()->Unit) { MaterialTheme(colorScheme=Palette,content=content) }
