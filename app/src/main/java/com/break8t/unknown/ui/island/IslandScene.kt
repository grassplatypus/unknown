package com.break8t.unknown.ui.island

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.break8t.unknown.ui.theme.AppColors
import kotlin.math.roundToInt
import kotlin.math.sqrt

/** 장면이 어느 곳을 비추는지. */
enum class SceneFocus { MAIN, FARM }

/** 장면이 그려야 하는 한 칸. progress는 0에서 1. */
data class PlotView(val seedId: String?, val grown: Boolean, val progress: Float)

/** 장면에 넘기는 것 전부. 장면은 이것만 보고 그린다. */
data class IslandSceneState(val night: Boolean, val focus: SceneFocus, val plots: List<PlotView>, val carId: String)

fun plotViews(plots: List<Plot>, now: Long): List<PlotView> =
    plots.map { PlotView(it.seedId, it.isGrown(now), it.progress(now)) }

private val NightSky = Color(0xFF010F43)
private val DayGround = Color(0xFF8CC63F)
private val NightGround = Color(0xFF2F5A3A)
private val CropColor = mapOf(
    "seed_carrot" to Color(0xFFF08A24),
    "seed_potato" to Color(0xFFB4479A),
    "seed_wheat" to AppColors.Selected,
)

internal fun sceneSky(night: Boolean) = if (night) NightSky else AppColors.Sky

/**
 * 섬 장면을 그리는 자리. 렌더링 방식이 정해지면 이 함수의 몸통만 바꾼다(지금은 단색 자리표시).
 * @param onPlot 텃밭 칸을 눌렀을 때 칸 번호(위에서부터 줄 순서)
 * @param plotOverlay 칸 위에 얹는 남은 시간, 다 자람, 고른 칸 표시. 장면이 칸 위치에 맞춰 놓는다.
 */
@Composable
fun IslandScene(
    state: IslandSceneState,
    modifier: Modifier = Modifier,
    onPlot: (Int) -> Unit = {},
    plotOverlay: @Composable BoxScope.(Int) -> Unit = {},
) {
    when (state.focus) {
        SceneFocus.MAIN -> Canvas(modifier.background(sceneSky(state.night))) {
            val ground = if (state.night) NightGround else DayGround
            drawOval(AppColors.Line, Offset(size.width * 0.15f, size.height * 0.5f), Size(size.width * 0.7f, size.height * 0.34f))
            drawOval(ground, Offset(size.width * 0.15f, size.height * 0.44f), Size(size.width * 0.7f, size.height * 0.34f))
        }
        SceneFocus.FARM -> PlaceholderFarm(state, modifier, onPlot, plotOverlay)
    }
}

@Composable
private fun PlaceholderFarm(state: IslandSceneState, modifier: Modifier, onPlot: (Int) -> Unit, plotOverlay: @Composable BoxScope.(Int) -> Unit) {
    val n = sqrt(state.plots.size.toDouble()).roundToInt().coerceAtLeast(1)
    BoxWithConstraints(modifier.background(sceneSky(state.night)), contentAlignment = Alignment.Center) {
        val tile = minOf(maxWidth, maxHeight) / n
        Column {
            for (r in 0 until n) Row {
                for (c in 0 until n) {
                    val i = r * n + c
                    val plot = state.plots.getOrNull(i) ?: PlotView(null, false, 0f)
                    Box(Modifier.size(tile).clickable(role = Role.Button) { onPlot(i) }) {
                        Canvas(Modifier.fillMaxSize()) {
                            val pad = size.minDimension * 0.04f
                            drawRoundRect(AppColors.Line, Offset(pad, pad), Size(size.width - 2 * pad, size.height - 2 * pad), CornerRadius(pad * 2))
                            val color = CropColor[plot.seedId] ?: return@Canvas
                            val r0 = size.minDimension * (0.12f + 0.18f * if (plot.grown) 1f else plot.progress)
                            drawCircle(color, r0, Offset(size.width / 2, size.height * 0.4f))
                        }
                        plotOverlay(i)
                    }
                }
            }
        }
    }
}
