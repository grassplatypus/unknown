package com.break8t.unknown.ui.home

import androidx.compose.ui.res.imageResource
import androidx.compose.ui.graphics.ImageBitmap
import com.break8t.unknown.ui.collection.artFor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.break8t.unknown.R
import com.break8t.unknown.config.BalanceConfig
import com.break8t.unknown.ui.components.BorderWidth
import com.break8t.unknown.ui.iso.IsoPalette
import com.break8t.unknown.ui.theme.AppColors
import kotlin.math.floor
import kotlin.math.min

// Figma px x 1.3 (app frame 1284x901 px vs window 1682x1210 dp), same scale as SideRail.kt.
internal val Int.fdp: Dp get() = (this * 1.3f).dp
internal val Int.fsp: TextUnit get() = (this * 1.3f).sp

internal val TagShape = RoundedCornerShape(12.fdp)

@Composable
internal fun HomeText(
    text: String,
    size: Int,
    modifier: Modifier = Modifier,
    color: Color = AppColors.Text,
    weight: FontWeight = FontWeight.Normal,
    align: TextAlign? = null,
    lineHeight: Int? = null,
) = Text(
    text,
    modifier,
    color = color,
    fontSize = size.fsp,
    fontWeight = weight,
    textAlign = align,
    // MaterialTheme gives bodyLarge (24sp line height) to every Text; Figma text boxes are about 1.2x the size.
    lineHeight = ((lineHeight?.toFloat() ?: (size * 1.2f)) * 1.3f).sp,
)

/** 누를 수 없는 상태 표시(홈 제목, P/D 칩, 코인). */
@Composable
internal fun Tag(fill: Color, modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) = Row(
    modifier
        .heightIn(min = 76.fdp)
        .clip(TagShape)
        .background(fill)
        .border(BorderWidth, AppColors.Line, TagShape)
        .padding(horizontal = 24.fdp),
    horizontalArrangement = Arrangement.spacedBy(10.fdp, Alignment.CenterHorizontally),
    verticalAlignment = Alignment.CenterVertically,
    content = content,
)

@Composable
internal fun Coin(size: Int, modifier: Modifier = Modifier) = Canvas(modifier.size(size.fdp)) {
    val stroke = 3.fdp.toPx()
    drawCircle(AppColors.Coin, radius = this.size.minDimension / 2 - stroke / 2)
    drawCircle(AppColors.CoinBorder, radius = this.size.minDimension / 2 - stroke / 2, style = Stroke(stroke))
}

private val AvatarColors = listOf(AppColors.Selected, AppColors.Sky, AppColors.SuccessBg)

/** 참가자 그림 자리. Figma의 프로필 그림은 다른 회사 캐릭터라서 첫 글자와 단색으로 대신한다. 테두리는 레벨 단계를 따른다. */
@Composable
internal fun Avatar(
    p: Participant,
    index: Int,
    textSize: Int,
    modifier: Modifier = Modifier,
    config: BalanceConfig = BalanceConfig.DEFAULT,
) {
    val fill = if (p.friend) AvatarColors[index % AvatarColors.size] else AppColors.Muted
    val frame = avatarFrame(p.level, config)
    Box(modifier.drawBehind { drawPixelFrame(frame, fill) }, contentAlignment = Alignment.Center) {
        // 대표 캐릭터 그림을 테두리 안쪽에 둔다. 그림이 없는 새 캐릭터만 예전처럼 첫 글자를 쓴다.
        val art = artFor(p.characterId)
        if (art != null) {
            Image(ImageBitmap.imageResource(art), p.name, Modifier.fillMaxSize(0.68f), filterQuality = FilterQuality.None)
        } else {
            HomeText(p.name.take(1), textSize, weight = FontWeight.Bold)
        }
    }
}

// 칸 크기를 정수 픽셀로 내려야 칸 사이에 틈이 생기지 않는다. 모서리는 FRAME_CUT 칸만큼 계단으로 깎는다.
private const val FRAME_CELLS = 16
private const val FRAME_CUT = 2

/** 금속 테두리의 세 단. 빛은 왼쪽 위에서 온다. */
private class Metal(val light: Color, val mid: Color, val dark: Color)

private fun metalOf(frame: AvatarFrame): Metal? = when (frame) {
    AvatarFrame.BASIC -> null
    AvatarFrame.SILVER -> Metal(IsoPalette.White, IsoPalette.RoadKerb, IsoPalette.StoneC)
    AvatarFrame.GOLD, AvatarFrame.JEWEL -> Metal(IsoPalette.Light, AppColors.Coin, AppColors.CoinBorder)
}

private fun DrawScope.drawPixelFrame(frame: AvatarFrame, fill: Color) {
    val n = FRAME_CELLS
    val cell = floor(size.minDimension / n)
    if (cell < 1f) return
    val ox = floor((size.width - cell * n) / 2)
    val oy = floor((size.height - cell * n) / 2)
    fun px(x: Int, y: Int, c: Color) = drawRect(c, Offset(ox + x * cell, oy + y * cell), Size(cell, cell))
    val metal = metalOf(frame)
    val band = when (frame) {
        AvatarFrame.BASIC -> 0
        AvatarFrame.SILVER -> 1
        AvatarFrame.GOLD, AvatarFrame.JEWEL -> 2
    }
    for (y in 0 until n) for (x in 0 until n) {
        val cx = min(x, n - 1 - x)
        val cy = min(y, n - 1 - y)
        val depth = minOf(cx, cy, cx + cy - FRAME_CUT)
        if (depth < 0) continue
        val c = when {
            depth == 0 -> AppColors.Line
            metal != null && depth == 1 -> if (x + y < n - 1) metal.light else metal.dark
            metal != null && depth <= band -> metal.mid
            else -> fill
        }
        px(x, y, c)
    }
    if (frame != AvatarFrame.JEWEL) return
    fun gem(x: Int, y: Int) {
        px(x, y, IsoPalette.White)
        px(x + 1, y, IsoPalette.Red)
        px(x, y + 1, IsoPalette.Red)
        px(x + 1, y + 1, IsoPalette.RoofDark)
    }
    val mid = n / 2 - 1
    gem(mid, 1); gem(mid, n - 3); gem(1, mid); gem(n - 3, mid)
    for ((x, y) in listOf(2 to 2, n - 3 to 2, 2 to n - 3, n - 3 to n - 3)) px(x, y, IsoPalette.Blue)
}

@Composable
internal fun durationText(sec: Long): String {
    val h = sec / 3600
    val m = sec % 3600 / 60
    val s = sec % 60
    return when {
        h > 0 -> stringResource(R.string.home_duration_hm, h, m)
        s > 0 -> stringResource(R.string.home_duration_ms, m, s)
        else -> stringResource(R.string.home_duration_m, m)
    }
}

/** 숫자 읽는 소리에 받침이 없으면(2 이, 4 사, 5 오, 9 구) 조사가 "가"다. */
@Composable
internal fun levelUpText(level: Int): String = stringResource(
    if (level % 10 in setOf(2, 4, 5, 9)) R.string.home_level_up_ga else R.string.home_level_up_i,
    level,
)
