package com.break8t.unknown.ui.fielddrop

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.break8t.unknown.ui.iso.IsoPalette

/**
 * 지도는 위에서 내려다본 모습이라 차도 위에서 본 픽셀 그림으로 그린다. 앞이 위(북쪽)다.
 * 섬의 복셀 차를 위에서 찍으면 전조등과 미등이 차체에 가려 앞뒤를 알 수 없어서 따로 그린다.
 */
private val TOP_CAR = listOf(
    "...KKKKKKKK...",
    "..KLBBBBBBLK..",
    "..KBBBBBBBBK..",
    ".TKBBBBBBBBKT.",
    ".TKbGGGGGGbKT.",
    "..KGggGGGGGK..",
    "..KGGGGGGGGK..",
    "..KbBBBBBBbK..",
    "..KbBBBBBBbK..",
    "..KbBBBBBBbK..",
    "..KbBBBBBBbK..",
    "..KbBBBBBBbK..",
    "..KbGGGGGGbK..",
    ".TKbGGGGGGbKT.",
    ".TKBBBBBBBBKT.",
    "..KBBBBBBBBK..",
    "..KRBBBBBBRK..",
    "...KKKKKKKK...",
)

/** 그림 한 변의 칸 수. 돌려 찍는 rotatedCar가 정사각형을 받아서 가로를 세로에 맞춰 비워 둔다. */
val TOP_CAR_CELLS = TOP_CAR.size

/** 위에서 본 차의 ARGB 칸들(한 변 [TOP_CAR_CELLS]). 가로 가운데에 놓는다. */
fun topCarCells(body: Color): IntArray {
    val n = TOP_CAR_CELLS
    val pad = (n - TOP_CAR[0].length) / 2
    val dark = Color(body.red * 0.75f, body.green * 0.75f, body.blue * 0.75f)
    val colors = mapOf(
        'K' to IsoPalette.Ink, 'B' to body, 'b' to dark, 'G' to IsoPalette.Glass, 'g' to IsoPalette.GlassLight,
        'L' to IsoPalette.Light, 'R' to TAIL, 'T' to IsoPalette.Tire,
    ).mapValues { it.value.toArgb() }
    val out = IntArray(n * n)
    TOP_CAR.forEachIndexed { y, row -> row.forEachIndexed { x, c -> colors[c]?.let { out[y * n + x + pad] = it } } }
    return out
}

private val TAIL = Color(0xFFB0302A)

fun topCarBody(carIcon: String): Color = when (carIcon) {
    "fd_car_blue" -> IsoPalette.Blue
    "fd_car_yellow" -> IsoPalette.Yellow
    else -> IsoPalette.Red
}
