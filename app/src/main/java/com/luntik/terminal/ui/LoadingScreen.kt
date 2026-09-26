package com.luntik.terminal.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun LoadingScreen(modifier: Modifier = Modifier) {
    val binaryChars = remember { List(120) { if (Random.nextBoolean()) "0" else "1" } }
    var visibleHands by remember { mutableFloatStateOf(0f) }
    var progress by remember { mutableFloatStateOf(0f) }

    val infiniteTransition = rememberInfiniteTransition(label = "binary")
    val offsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 40f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rain"
    )

    LaunchedEffect(Unit) {
        // Плавное появление рук
        for (i in 1..100) {
            delay(25)
            visibleHands = i / 100f
            progress = i / 100f
        }
    }

    Box(
        modifier = modifier.background(Color(0xFF0A0A0A)),
        contentAlignment = Alignment.Center
    ) {
        // Бинарный дождь на фоне
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cols = 18
            val rows = 28
            val cellW = size.width / cols
            val cellH = size.height / rows

            for (col in 0 until cols) {
                for (row in 0 until rows) {
                    val idx = (col * rows + row) % binaryChars.size
                    val y = (row * cellH + offsetY) % (size.height + cellH)
                    val alpha = 0.15f + (col % 5) * 0.05f
                    // Просто рисуем точки вместо текста для производительности
                    drawCircle(
                        color = Color(0xFF00FF41).copy(alpha = alpha * 0.6f),
                        radius = 1.5f,
                        center = Offset(col * cellW + cellW / 2, y)
                    )
                }
            }
        }

        // Центральный контент
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            // Имитация рук (текстовая версия, позже заменим на изображение)
            Text(
                text = "🤲",
                fontSize = (48 + visibleHands * 24).sp,
                modifier = Modifier.padding(bottom = 24.dp),
                style = TextStyle(
                    shadow = Shadow(
                        color = Color(0xFF00FF41).copy(alpha = visibleHands * 0.8f),
                        blurRadius = 24f
                    )
                )
            )

            Text(
                text = "LuntikTerminal",
                color = Color(0xFFCCCCCC).copy(alpha = 0.4f + visibleHands * 0.6f),
                fontSize = 22.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Инициализация системы...",
                color = Color(0xFF00FF41).copy(alpha = 0.7f),
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Прогресс
            Box(
                modifier = Modifier
                    .width(200.dp)
                    .height(4.dp)
                    .background(Color(0xFF222222))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress)
                        .background(Color(0xFF00FF41))
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${(progress * 100).toInt()}%",
                color = Color(0xFF666666),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
