package com.luntik.terminal.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luntik.terminal.ui.theme.Glass
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun LoadingScreen(modifier: Modifier = Modifier) {
    var progress by remember { mutableFloatStateOf(0f) }
    var handsAlpha by remember { mutableFloatStateOf(0f) }

    val infiniteTransition = rememberInfiniteTransition(label = "load")
    val rainOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 60f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rain"
    )
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    val binaryGrid = remember {
        List(24) { col ->
            List(36) { if (Random.nextBoolean()) 0 else 1 }
        }
    }

    LaunchedEffect(Unit) {
        for (i in 1..100) {
            delay(28)
            progress = i / 100f
            handsAlpha = (i / 100f).coerceIn(0f, 1f)
        }
    }

    Box(
        modifier = modifier.background(
            Brush.verticalGradient(
                listOf(Glass.BgDeep, Glass.BgMid, Glass.BgDeep)
            )
        ),
        contentAlignment = Alignment.Center
    ) {
        // Binary rain + hands drawn entirely on Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cols = binaryGrid.size
            val rows = binaryGrid[0].size
            val cellW = w / cols
            val cellH = h / rows

            // Binary matrix rain
            for (c in 0 until cols) {
                for (r in 0 until rows) {
                    val bit = binaryGrid[c][r]
                    val y = (r * cellH + rainOffset) % (h + cellH)
                    val alpha = (0.08f + (c % 7) * 0.02f) * (0.5f + handsAlpha * 0.5f)
                    val color = if (bit == 0)
                        Color(0xFF8B9CFF).copy(alpha = alpha)
                        else Color(0xFF7CFFB2).copy(alpha = alpha * 0.85f)

                    // Draw "0" or "1" as small rounded rects / lines (code-only glyphs)
                    val cx = c * cellW + cellW / 2
                    if (bit == 0) {
                        drawRoundRect(
                            color = color,
                            topLeft = Offset(cx - 4f, y - 7f),
                            size = Size(8f, 14f),
                            cornerRadius = CornerRadius(3f, 3f),
                            style = Stroke(width = 1.4f)
                        )
                    } else {
                        drawLine(
                            color = color,
                            start = Offset(cx, y - 7f),
                            end = Offset(cx, y + 7f),
                            strokeWidth = 1.6f,
                            cap = StrokeCap.Round
                        )
                    }
                }
            }

            // --- Two hands reaching toward center (vector, no emoji) ---
            val centerX = w / 2f
            val centerY = h / 2f - 40f
            val reach = 90f + handsAlpha * 28f
            val handAlpha = handsAlpha * glowPulse

            // Soft glow between hands
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Glass.Accent.copy(alpha = 0.25f * handAlpha),
                        Color.Transparent
                    ),
                    center = Offset(centerX, centerY),
                    radius = 70f
                ),
                radius = 70f,
                center = Offset(centerX, centerY)
            )

            // Core orb
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.9f * handAlpha),
                        Glass.Accent.copy(alpha = 0.6f * handAlpha),
                        Glass.Accent.copy(alpha = 0.1f * handAlpha)
                    ),
                    center = Offset(centerX, centerY),
                    radius = 18f + handsAlpha * 6f
                ),
                radius = 14f + handsAlpha * 5f,
                center = Offset(centerX, centerY)
            )

            fun drawHand(fromLeft: Boolean) {
                val dir = if (fromLeft) 1f else -1f
                val baseX = centerX - dir * reach
                val baseY = centerY + 20f
                val tipX = centerX - dir * (22f)
                val tipY = centerY + 4f

                val strokeColor = Color.White.copy(alpha = 0.75f * handAlpha)
                val fillColor = Color.White.copy(alpha = 0.12f * handAlpha)

                // Palm
                val palmPath = Path().apply {
                    moveTo(baseX, baseY)
                    cubicTo(
                        baseX + dir * 25f, baseY - 35f,
                        tipX - dir * 15f, tipY - 25f,
                        tipX, tipY
                    )
                    cubicTo(
                        tipX + dir * 8f, tipY + 18f,
                        baseX + dir * 30f, baseY + 25f,
                        baseX, baseY
                    )
                    close()
                }
                drawPath(palmPath, fillColor)
                drawPath(palmPath, strokeColor, style = Stroke(width = 2.2f, cap = StrokeCap.Round, join = StrokeJoin.Round))

                // Fingers (4 simple strokes reaching toward orb)
                for (i in 0..3) {
                    val spread = (i - 1.5f) * 11f
                    val fx = tipX + dir * 2f
                    val fy = tipY + spread * 0.6f - 6f
                    val ex = centerX - dir * (10f + i * 1.5f)
                    val ey = centerY + spread * 0.35f
                    drawLine(
                        color = strokeColor,
                        start = Offset(fx, fy),
                        end = Offset(ex, ey),
                        strokeWidth = 2.4f - i * 0.15f,
                        cap = StrokeCap.Round
                    )
                }

                // Wrist line
                drawLine(
                    color = strokeColor.copy(alpha = 0.5f * handAlpha),
                    start = Offset(baseX - dir * 8f, baseY + 8f),
                    end = Offset(baseX + dir * 18f, baseY + 8f),
                    strokeWidth = 1.5f,
                    cap = StrokeCap.Round
                )
            }

            drawHand(fromLeft = true)
            drawHand(fromLeft = false)
        }

        // Title + glass progress at bottom third
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp, start = 32.dp, end = 32.dp)
        ) {
            Text(
                text = "LuntikTerminal",
                color = Color.White.copy(alpha = 0.3f + handsAlpha * 0.7f),
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.SansSerif,
                letterSpacing = 1.5.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "инициализация",
                color = Glass.TextMuted.copy(alpha = handsAlpha),
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(28.dp))

            // Liquid glass progress track
            Box(
                modifier = Modifier
                    .width(220.dp)
                    .height(6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.08f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress)
                        .clip(RoundedCornerShape(50))
                        .background(
                            Brush.horizontalGradient(
                                listOf(Glass.Accent, Glass.TerminalGreen)
                            )
                        )
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "${(progress * 100).toInt()}%",
                color = Glass.TextMuted,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
