package com.break8t.unknown.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalInspectionMode
import com.break8t.unknown.ui.iso.IsoPalette
import com.break8t.unknown.ui.theme.AppColors
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.sin
import kotlin.random.Random

/** 색종이 한 조각. 위치와 시각은 화면과 전체 시간에 대한 비율이다. */
internal class ConfettiPiece(val x: Float, val start: Float, val fall: Float, val sway: Float, val cells: Int, val color: Int)

private const val CONFETTI_COUNT = 70
private const val CONFETTI_MS = 2600
private const val CONFETTI_SEED = 8
private const val PREVIEW_PROGRESS = 0.35f

/** 늦게 출발한 조각도 progress 1 전에 바닥을 지나도록, 출발 시각과 낙하 시간의 합을 1 아래로 둔다. */
internal fun confettiPieces(colorCount: Int, count: Int = CONFETTI_COUNT, seed: Int = CONFETTI_SEED): List<ConfettiPiece> {
    val r = Random(seed)
    return List(count) {
        val start = r.nextFloat() * 0.45f
        val fall = 0.4f + r.nextFloat() * (1f - start - 0.4f)
        ConfettiPiece(r.nextFloat(), start, fall, sway = 0.01f + r.nextFloat() * 0.02f, cells = 1 + r.nextInt(2), r.nextInt(colorCount))
    }
}

/** [progress]에서 조각의 (x, y) 비율. 출발 전이거나 바닥을 지났으면 null이라, 다 떨어지면 아무것도 그리지 않는다. */
internal fun ConfettiPiece.at(progress: Float): Pair<Float, Float>? {
    val k = (progress - start) / fall
    if (k < 0f || k >= 1f) return null
    return x + sway * sin(k * 4 * PI.toFloat()) to -0.05f + 1.1f * k
}

/** D-03 레벨 업 위로 떨어지는 픽셀 색종이. P에서만 뜨는 알림이라 움직여도 된다. 한 번 떨어지고 멈춘다. */
@Composable
internal fun Confetti(modifier: Modifier = Modifier) {
    val still = LocalInspectionMode.current
    val progress = remember { Animatable(if (still) PREVIEW_PROGRESS else 0f) }
    LaunchedEffect(Unit) { if (!still) progress.animateTo(1f, tween(CONFETTI_MS, easing = LinearEasing)) }
    val colors = remember {
        listOf(AppColors.Coin, AppColors.Error, AppColors.Success, AppColors.Sky, IsoPalette.Pink, IsoPalette.Blue, IsoPalette.Sprout, IsoPalette.Carrot)
    }
    val pieces = remember { confettiPieces(colors.size) }
    Canvas(modifier) {
        // 위치를 칸에 맞춰 내려서 조각이 매끈하게 미끄러지지 않고 한 칸씩 내려온다.
        val pixel = floor(6.fdp.toPx()).coerceAtLeast(1f)
        val t = progress.value
        pieces.forEach { p ->
            val (fx, fy) = p.at(t) ?: return@forEach
            val x = floor(fx * size.width / pixel) * pixel
            val y = floor(fy * size.height / pixel) * pixel
            drawRect(colors[p.color], Offset(x, y), Size(pixel * p.cells, pixel * p.cells))
        }
    }
}
