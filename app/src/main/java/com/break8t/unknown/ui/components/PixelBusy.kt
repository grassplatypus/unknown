package com.break8t.unknown.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.break8t.unknown.ui.theme.AppColors

/** 픽셀 아트용 진행 표시. 매끈한 원 대신 세 칸이 차례로 켜진다. */
@Composable
fun PixelBusy(modifier: Modifier = Modifier, block: Dp = 12.dp) {
    val t by rememberInfiniteTransition(label = "busy").animateFloat(
        0f, 3f, infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart), label = "step",
    )
    Canvas(modifier.size(block * 5, block)) {
        val b = block.toPx()
        for (i in 0 until 3) {
            drawRect(
                if (t.toInt() == i) AppColors.Primary else AppColors.Muted,
                Offset(i * 2 * b, 0f), Size(b, b),
            )
        }
    }
}
