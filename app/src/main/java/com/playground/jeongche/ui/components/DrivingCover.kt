package com.playground.jeongche.ui.components

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.rotary.onPreRotaryScrollEvent
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.playground.jeongche.ui.theme.AppColors

// Figma S-11-b. Sizes are Figma px x 1.3, see SideRail.kt.
private val PillShape = RoundedCornerShape(16.dp)

/** Figma S-11-b: 반투명 덮개와 아래쪽 안내. 덮개 아래로 가는 터치는 모두 여기서 소비한다. */
@Composable
fun DrivingCover(hint: String, modifier: Modifier = Modifier) {
    // Below API 31 the content is not blurred, so the Figma 0.6 scrim would leave it readable.
    val scrim = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) 0.6f else 0.92f
    Box(
        modifier
            .fillMaxSize()
            .background(AppColors.Background.copy(alpha = scrim))
            .pointerInput(Unit) {
                awaitPointerEventScope { while (true) awaitPointerEvent().changes.forEach { it.consume() } }
            }
            .padding(start = 24.dp, end = 24.dp, bottom = 72.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Box(
            Modifier
                .widthIn(max = 780.dp)
                .fillMaxWidth()
                .heightIn(min = 100.dp)
                .clip(PillShape)
                .background(AppColors.Card.copy(alpha = 0.92f))
                .padding(horizontal = 24.dp, vertical = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(hint, color = AppColors.Text, fontSize = 39.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }
    }
}

/**
 * DrivingCover 아래 본문에 건다. 덮개는 형제를 흐리게 할 수 없어서 본문 쪽에서 흐린다.
 * 흐림은 API 31 이상에서만 보인다. 로터리, 키, 접근성 입력은 버전과 상관없이 막는다.
 */
fun Modifier.drivingBlur(active: Boolean): Modifier =
    if (!active) {
        this
    } else {
        this
            .blur(18.dp)
            .clearAndSetSemantics {}
            .onPreviewKeyEvent { true }
            .onPreRotaryScrollEvent { true }
            .focusProperties { onEnter = { cancelFocusChange() } }
            .focusGroup()
    }
