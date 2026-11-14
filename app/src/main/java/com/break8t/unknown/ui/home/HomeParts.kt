package com.break8t.unknown.ui.home

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.break8t.unknown.R
import com.break8t.unknown.ui.components.BorderWidth
import com.break8t.unknown.ui.theme.AppColors

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
    lineHeight = lineHeight?.fsp ?: TextUnit.Unspecified,
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
    drawCircle(AppColors.Selected, radius = this.size.minDimension / 2 - stroke / 2)
    drawCircle(AppColors.CoinBorder, radius = this.size.minDimension / 2 - stroke / 2, style = Stroke(stroke))
}

private val AvatarColors = listOf(AppColors.Selected, AppColors.Sky, AppColors.SuccessBg)

/** 참가자 그림 자리. Figma의 프로필 그림은 다른 회사 캐릭터라서 첫 글자와 단색으로 대신한다. */
@Composable
internal fun Avatar(p: Participant, index: Int, textSize: Int, modifier: Modifier = Modifier) = Box(
    modifier
        .clip(RoundedCornerShape(8.fdp))
        .background(if (p.friend) AvatarColors[index % AvatarColors.size] else AppColors.Muted),
    contentAlignment = Alignment.Center,
) {
    HomeText(p.name.take(1), textSize, weight = FontWeight.Bold)
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
