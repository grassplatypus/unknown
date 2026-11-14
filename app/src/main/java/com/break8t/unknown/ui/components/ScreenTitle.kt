package com.break8t.unknown.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.break8t.unknown.ui.theme.AppColors

/**
 * 지금 화면(메뉴)의 이름. 칩과 헷갈리지 않게 테두리와 바탕 없이 칩보다 큰 글자로 쓴다.
 * 섬처럼 그림 위에 놓여도 읽히게 밝은 외곽선을 두르는데, 흐린 그림자 대신 글자를 여덟 방향으로 밀어 찍는다.
 */
@Composable
fun ScreenTitle(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier.heightIn(min = 100.dp).padding(horizontal = OUTLINE_DP.dp).semantics(mergeDescendants = true) { heading() },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(Modifier.clearAndSetSemantics {}) {
            for ((dx, dy) in Outline) {
                Text(text, Modifier.offset((dx * OUTLINE_DP).dp, (dy * OUTLINE_DP).dp), color = AppColors.Card, style = TitleStyle, maxLines = 1)
            }
        }
        Text(text, color = AppColors.Text, style = TitleStyle, maxLines = 1)
    }
}

private const val OUTLINE_DP = 3
private val Outline = listOf(-1 to -1, 0 to -1, 1 to -1, -1 to 0, 1 to 0, -1 to 1, 0 to 1, 1 to 1)
private val TitleStyle = TextStyle(fontSize = 52.sp, fontWeight = FontWeight.Bold)
