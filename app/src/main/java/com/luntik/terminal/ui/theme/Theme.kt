package com.luntik.terminal.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val TerminalColorScheme = darkColorScheme(
    primary = Color(0xFF8B9CFF),
    onPrimary = Color.White,
    secondary = Color(0xFFB8C0FF),
    background = Color(0xFF0A0A0F),
    onBackground = Color(0xFFE8E8F0),
    surface = Color(0xFF14141C),
    onSurface = Color(0xFFE8E8F0)
)

@Composable
fun LuntikTerminalTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TerminalColorScheme,
        content = content
    )
}

/** Liquid Glass palette — same language as Lumina */
object Glass {
    val Border = Color.White.copy(alpha = 0.14f)
    val Fill = Color.White.copy(alpha = 0.07f)
    val FillStrong = Color.White.copy(alpha = 0.11f)
    val Highlight = Color.White.copy(alpha = 0.22f)
    val TextPrimary = Color.White
    val TextSecondary = Color.White.copy(alpha = 0.62f)
    val TextMuted = Color.White.copy(alpha = 0.38f)
    val Accent = Color(0xFF8B9CFF)
    val AccentSoft = Color(0xFF8B9CFF).copy(alpha = 0.35f)
    val Success = Color(0xFF5CFFB0)
    val Warning = Color(0xFFFFC857)
    val Error = Color(0xFFFF6B7A)
    val TerminalGreen = Color(0xFF7CFFB2)
    val BgDeep = Color(0xFF0A0A0F)
    val BgMid = Color(0xFF12121A)
}
