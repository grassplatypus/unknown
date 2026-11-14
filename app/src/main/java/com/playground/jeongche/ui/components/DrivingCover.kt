package com.playground.jeongche.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.playground.jeongche.ui.theme.AppColors

/**
 * 주행 중에 화면 위를 덮는다. 아래 화면의 입력은 막아야 한다.
 * 자리만 잡아 둔 구현이다. components 담당이 S-11-b 모양(흐림, 아래쪽 안내 상자)으로 바꾼다.
 */
@Composable
fun DrivingCover(hint: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().background(AppColors.Background.copy(alpha = 0.85f)), Alignment.Center) {
        Text(hint, color = AppColors.Text)
    }
}
