package com.break8t.unknown.ui.house

import com.break8t.unknown.ui.theme.PixelIndicationOnDark
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.break8t.unknown.ui.Route
import com.break8t.unknown.ui.components.AppToast
import com.break8t.unknown.ui.components.BorderWidth
import com.break8t.unknown.ui.components.ControlShape
import com.break8t.unknown.ui.components.PrimaryButton
import com.break8t.unknown.ui.components.SecondaryButton
import com.break8t.unknown.ui.components.SideRail
import com.break8t.unknown.ui.theme.AppColors
import com.break8t.unknown.ui.theme.AppTheme
import kotlinx.coroutines.delay

// Figma px x 1.3 (see SideRail.kt). Shared by the house and photo screens.
internal val ScreenPad = 35.dp
internal val ChipGap = 24.dp

/** 위쪽 칩. onClick이 없으면 지금 화면 이름을 보이는 칩이다(Figma의 노란 칩). */
@Composable
internal fun TopChip(text: String, onClick: (() -> Unit)?, modifier: Modifier = Modifier, @DrawableRes icon: Int? = null) {
    val selected = onClick == null
    Row(
        modifier
            .heightIn(min = 100.dp)
            .clip(ControlShape)
            .background(if (selected) AppColors.Selected else AppColors.Card)
            .border(BorderWidth, AppColors.Line, ControlShape)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(horizontal = 31.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) Icon(painterResource(icon), null, Modifier.size(44.dp), tint = AppColors.Text)
        Text(text, color = AppColors.Text, fontSize = 39.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

/** 아이콘만 있는 네모 버튼(돌리기, 보관함에 넣기, 크기, 뒤집기). */
@Composable
internal fun SquareIconButton(
    @DrawableRes icon: Int,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 109.dp,
    iconSize: Dp = 52.dp,
    iconRotation: Float = 0f,
) {
    Box(
        modifier
            .size(size)
            .clip(ControlShape)
            .background(AppColors.Card)
            .border(BorderWidth, AppColors.Line, ControlShape)
            .clickable(role = Role.Button, onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(icon), label, Modifier.size(iconSize).rotate(iconRotation), tint = AppColors.Text)
    }
}

/** 진한 바탕의 주 버튼에 아이콘을 붙인 것(촬영). */
@Composable
internal fun PrimaryIconButton(text: String, @DrawableRes icon: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .heightIn(min = 100.dp)
            .clip(ControlShape)
            .background(AppColors.Primary)
            .clickable(null, PixelIndicationOnDark, role = Role.Button, onClick = onClick)
            .padding(horizontal = 31.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(13.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(icon), null, Modifier.size(49.dp), tint = AppColors.OnPrimary)
        Text(text, color = AppColors.OnPrimary, fontSize = 44.sp, fontWeight = FontWeight.Bold)
    }
}

/**
 * 버튼 두 개에 글자를 따로 줄 수 있는 확인 창. AppDialog는 취소 글자가 고정이고
 * 바깥을 누르면 취소와 같아서, "저장 안 함"에 쓰면 실수로 누른 바깥 터치가 배치를 버린다.
 */
@Composable
internal fun ChoiceDialog(
    title: String,
    body: String,
    dismissText: String,
    confirmText: String,
    onDismissButton: () -> Unit,
    onConfirm: () -> Unit,
    onOutside: () -> Unit,
) {
    Dialog(onOutside, DialogProperties(usePlatformDefaultWidth = false)) {
        ChoicePanel(title, body, dismissText, confirmText, onDismissButton, onConfirm, Modifier.padding(24.dp))
    }
}

@Composable
internal fun ChoicePanel(
    title: String,
    body: String,
    dismissText: String,
    confirmText: String,
    onDismissButton: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(26.dp)
    Column(
        modifier
            .widthIn(max = 936.dp)
            .clip(shape)
            .background(AppColors.Card)
            .border(BorderWidth, AppColors.Line, shape)
            .padding(55.dp),
        verticalArrangement = Arrangement.spacedBy(31.dp),
    ) {
        Text(title, color = AppColors.Text, fontSize = 52.sp, lineHeight = 70.sp, fontWeight = FontWeight.Bold)
        Text(body, color = AppColors.TextSub, fontSize = 36.sp, lineHeight = 55.sp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(35.dp)) {
            SecondaryButton(dismissText, onDismissButton, Modifier.weight(1f))
            PrimaryButton(confirmText, onConfirm, Modifier.weight(1f))
        }
    }
}

/** message가 있으면 잠깐 보였다가 onShown으로 지운다. 주차 중에만 쓰는 화면이라 움직임이 있어도 된다. */
@Composable
internal fun TimedToast(message: String?, onShown: () -> Unit, modifier: Modifier = Modifier) {
    if (message == null) return
    LaunchedEffect(message) {
        delay(TOAST_MS)
        onShown()
    }
    AppToast(message, modifier)
}

private const val TOAST_MS = 2_000L

@Preview(name = "기본 창", widthDp = 1682, heightDp = 1210)
@Preview(name = "화면 나눔", widthDp = 841, heightDp = 1210)
internal annotation class WindowPreviews

/** 미리보기에서 사이드 메뉴 옆에 본문을 그린다. */
@Composable
internal fun PreviewFrame(background: Color = AppColors.Background, content: @Composable () -> Unit) = AppTheme {
    Row(Modifier.fillMaxSize().background(background)) {
        SideRail(current = Route.ISLAND, onSelect = {})
        Box(Modifier.weight(1f).fillMaxHeight()) { content() }
    }
}
