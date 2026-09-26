package com.luntik.terminal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.luntik.terminal.ui.LoadingScreen
import com.luntik.terminal.ui.TerminalScreen
import com.luntik.terminal.ui.theme.LuntikTerminalTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.isAppearanceLightStatusBars = false
        controller.isAppearanceLightNavigationBars = false

        setContent {
            LuntikTerminalTheme {
                var showLoading by remember { mutableStateOf(true) }

                LaunchedEffect(Unit) {
                    delay(3500) // время показа загрузочного экрана
                    showLoading = false
                }

                if (showLoading) {
                    LoadingScreen(modifier = Modifier.fillMaxSize())
                } else {
                    TerminalScreen(modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}
