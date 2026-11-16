package com.example.xo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.xo.ui.navigation.AppNavGraph
import com.example.xo.ui.theme.XO3DTheme
class MainActivity:ComponentActivity() {
 override fun onCreate(savedInstanceState:Bundle?) { super.onCreate(savedInstanceState); setContent { XO3DTheme { AppNavGraph() } } }
}
