package com.luntik.terminal.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.luntik.terminal.ui.theme.Glass

@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.11f),
                        Color.White.copy(alpha = 0.04f)
                    )
                )
            )
            .border(1.dp, Glass.Border, shape)
            .padding(1.dp)
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(cornerRadius - 1.dp))
                .background(Color(0xFF14141C).copy(alpha = 0.72f))
                .padding(16.dp)
        ) {
            content()
        }
    }
}
