package com.break8t.unknown.ui.theme

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Figma "0. 공통 조각 / 색상표"의 값. 이름도 색상표의 이름을 따른다. */
object AppColors {
    val Background = Color(0xFFF4EEDF) // 화면 바탕
    val Card = Color(0xFFFFFAF0) // 카드, 다이얼로그, 기본 버튼
    val Accent = Color(0xFFF6E3B4) // P/D 칩, 번호 표시
    val Muted = Color(0xFFEBE0C8) // 꺼짐 버튼, 불러오는 중 상자
    val Sky = Color(0xFF74BDF7) // 섬과 텃밭 배경
    val Text = Color(0xFF3A2A1C) // 기본 글자
    val TextSub = Color(0xFF6F5B47) // 설명, 꺼짐 글자
    val TextOnPrimary = Color(0xFFF1E2BB) // 사이드 메뉴, 주 버튼 위 글자
    val Line = Color(0xFF6B4A2E) // 카드와 버튼 테두리
    val Primary = Color(0xFF5A3B22) // 주 버튼, 사이드 메뉴, 토스트
    val Selected = Color(0xFFF2B33D) // 고른 탭, 켜짐 버튼, 코인
    val CoinBorder = Color(0xFFB9791A)
    val Divider = Color(0xFFDFD3B8)
    val Success = Color(0xFF4F8A2C)
    val SuccessBg = Color(0xFFDCEBC8)
    val Error = Color(0xFFB8452F)
    val Disabled = Color(0xFFE6DCC6)
    val DisabledText = Color(0xFFA39582)
}

private val scheme = lightColorScheme(
    primary = AppColors.Primary,
    onPrimary = AppColors.TextOnPrimary,
    secondary = AppColors.Selected,
    background = AppColors.Background,
    onBackground = AppColors.Text,
    surface = AppColors.Card,
    onSurface = AppColors.Text,
    onSurfaceVariant = AppColors.TextSub,
    outline = AppColors.Line,
    error = AppColors.Error,
)

/** Material 물결 대신 PixelIndication을 앱 전체 누름 표시로 쓴다. 좁은 창에서는 전체를 같은 비율로 줄인다. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTheme(content: @Composable () -> Unit) = MaterialTheme(colorScheme = scheme) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val base = LocalDensity.current
        val k = windowScale(maxWidth.value)
        CompositionLocalProvider(
            LocalDensity provides Density(base.density * k, base.fontScale),
            LocalIndication provides PixelIndication,
            LocalRippleConfiguration provides null,
            content = content,
        )
    }
}

// 841dp 창에서 0.75배: 섬 위쪽 칩 넷이 원래 크기로 한 줄에 들어가고,
// 사이드 메뉴를 뺀 본문 폭은 1000dp 아래라 화면들은 좁은 배치를 그대로 쓴다.
internal fun windowScale(widthDp: Float): Float = (widthDp / 1121f).coerceIn(0.75f, 1f)
