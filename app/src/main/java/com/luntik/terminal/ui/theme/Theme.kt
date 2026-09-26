package com.luntik.terminal.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val TerminalColorScheme = darkColorScheme(
    primary = Color(0xFFCCCCCC),
    onPrimary = Color.Black,
    background = Color(0xFF0C0C0C),
    onBackground = Color(0xFFCCCCCC),
    surface = Color(0xFF0C0C0C),
    onSurface = Color(0xFFCCCCCC)
)

@Composable
fun LuntikTerminalTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TerminalColorScheme,
        content = content
    )
}
