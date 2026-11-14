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

/**
 * Figma "0. 공통 조각 / 색상표"의 값과 그 야간판(Figma "야간 색상표"). 이름도 색상표의 이름을 따른다.
 * [night]는 MainActivity가 화면을 만들기 전에 정한다. 시스템 야간 모드가 바뀌면 Activity가 다시 만들어진다.
 */
object AppColors {
    @Volatile var night = false
    private fun pick(day: Long, dark: Long) = Color(if (night) dark else day)
    val Background get() = pick(0xFFF4EEDF, 0xFF1E1711) // 화면 바탕
    val Card get() = pick(0xFFFFFAF0, 0xFF2A2018) // 카드, 다이얼로그, 기본 버튼
    val Accent get() = pick(0xFFF6E3B4, 0xFF4D3B1C) // P/D 칩, 번호 표시
    val Muted get() = pick(0xFFEBE0C8, 0xFF3A2D21) // 꺼짐 버튼, 불러오는 중 상자
    val Sky get() = pick(0xFF74BDF7, 0xFF223A55) // 섬과 텃밭 배경
    val Text get() = pick(0xFF3A2A1C, 0xFFF1E2BB) // 기본 글자
    val TextSub get() = pick(0xFF6F5B47, 0xFFC2AE8E) // 설명, 꺼짐 글자
    val TextOnPrimary get() = pick(0xFFF1E2BB, 0xFFF1E2BB) // 사이드 메뉴 글자
    val OnPrimary get() = pick(0xFFFFFAF0, 0xFFF1E2BB) // 주 버튼 위 글자. 낮에는 Card와 같다
    val Splash get() = pick(0xFFF1E2BB, 0xFF1E1711) // S-01 시작 화면 바탕
    val RailSelected get() = pick(0xFF5A3B22, 0xFFF1E2BB) // 사이드 메뉴에서 고른 칸의 아이콘과 글자
    val Line get() = pick(0xFF6B4A2E, 0xFF8A6A48) // 카드와 버튼 테두리
    val Primary get() = pick(0xFF5A3B22, 0xFF5A3B22) // 주 버튼, 사이드 메뉴, 토스트
    val Selected get() = pick(0xFFF2B33D, 0xFF8C5E14) // 고른 탭, 켜짐 버튼
    val Coin get() = pick(0xFFF2B33D, 0xFFF2B33D) // 코인은 그림이라 밤에도 같은 색
    val CoinBorder get() = pick(0xFFB9791A, 0xFFB9791A)
    val Divider get() = pick(0xFFDFD3B8, 0xFF3E3226)
    val Success get() = pick(0xFF4F8A2C, 0xFF8FC46A)
    val SuccessBg get() = pick(0xFFDCEBC8, 0xFF2E3D22)
    val Error get() = pick(0xFFB8452F, 0xFFE0745A)
    val Disabled get() = pick(0xFFE6DCC6, 0xFF2E251C)
    val DisabledText get() = pick(0xFFA39582, 0xFF7F705E)
    /** 대화상자 뒤를 어둡게 하는 막. 밤에도 어두워야 해서 낮의 글자색에 고정한다. */
    val Scrim = Color(0x803A2A1C)
}

private fun scheme() = lightColorScheme(
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
fun AppTheme(content: @Composable () -> Unit) = MaterialTheme(colorScheme = scheme()) {
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
