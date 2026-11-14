package com.break8t.unknown.ui.collection

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.break8t.unknown.R
import com.break8t.unknown.ui.Route
import com.break8t.unknown.ui.components.SideRail
import com.break8t.unknown.ui.theme.AppColors
import com.break8t.unknown.ui.theme.AppTheme

// Figma sections 6 and 7. Sizes are Figma px x 1.3, see SideRail.kt.
internal val ScreenPadding = 35.dp
internal val TileShape = RoundedCornerShape(21.dp)
internal val ArtShape = RoundedCornerShape(16.dp)
private val ChipShape = RoundedCornerShape(16.dp)
private val PanelShape = RoundedCornerShape(26.dp)

/** 모은 것 한 칸의 바탕. 고른 칸은 굵은 노란 테두리(Figma 5px)다. */
@Composable
internal fun Tile(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .clip(TileShape)
            .background(AppColors.Card)
            .border(if (selected) 6.5.dp else 3.dp, if (selected) AppColors.Selected else AppColors.Line, TileShape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(21.dp),
        content = content,
    )
}

/** 그림. 그림이 없는 항목은 단색 원과 첫 글자로 대신한다(40-safety.md). */
@Composable
internal fun Art(
    @DrawableRes art: Int?,
    name: String,
    modifier: Modifier = Modifier,
    scale: ContentScale = ContentScale.Fit,
    alpha: Float = 1f,
) {
    if (art != null) {
        Image(painterResource(art), contentDescription = null, modifier.alpha(alpha), contentScale = scale)
    } else {
        Box(modifier.alpha(alpha).clip(CircleShape).background(AppColors.Muted), Alignment.Center) {
            Text(name.take(1), color = AppColors.TextSub, fontSize = 52.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
internal fun LockIcon(modifier: Modifier = Modifier) =
    Icon(painterResource(R.drawable.myinfo_ic_lock), stringResource(R.string.collection_locked), modifier.size(73.dp), tint = AppColors.Text)

/** 화면 위쪽의 제목 칩. onBack이 있으면 뒤로 가는 칩(Figma S-21-c "< 내 정보")이 된다. */
@Composable
fun HeaderChip(text: String, modifier: Modifier = Modifier, selected: Boolean = true, onBack: (() -> Unit)? = null) {
    Row(
        modifier
            .heightIn(min = 100.dp)
            .clip(ChipShape)
            .background(if (selected) AppColors.Selected else AppColors.Card)
            .border(3.dp, AppColors.Line, ChipShape)
            .then(if (onBack != null) Modifier.clickable(role = Role.Button, onClick = onBack) else Modifier)
            .padding(horizontal = 34.dp),
        horizontalArrangement = Arrangement.spacedBy(13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) Icon(painterResource(R.drawable.myinfo_ic_back), null, Modifier.size(44.dp), tint = AppColors.Text)
        Text(text, color = AppColors.Text, fontSize = 44.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

/**
 * 화면 안에 그리는 다이얼로그 자리. 창을 따로 띄우는 AppDialog는 DrivingCover 위로 올라오므로 쓰지 않는다.
 * 바깥을 누르거나 뒤로 가기를 누르면 onDismiss를 부른다.
 */
@Composable
fun Overlay(onDismiss: () -> Unit, content: @Composable BoxScope.() -> Unit) {
    BackHandler(onBack = onDismiss)
    Box(
        Modifier
            .fillMaxSize()
            .background(AppColors.Scrim)
            .clickable(remember { MutableInteractionSource() }, indication = null, onClick = onDismiss)
            .padding(24.dp),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

/** 다이얼로그 판. 안쪽을 눌러도 바깥 누름(닫기)으로 새지 않게 막는다. */
@Composable
fun DialogFrame(maxWidth: Dp, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .widthIn(max = maxWidth)
            .fillMaxWidth()
            .clip(PanelShape)
            .background(AppColors.Card)
            .border(3.dp, AppColors.Line, PanelShape)
            .pointerInput(Unit) { detectTapGestures { } }
            .verticalScroll(rememberScrollState())
            .padding(55.dp),
        verticalArrangement = Arrangement.spacedBy(31.dp),
        content = content,
    )
}

@Composable
fun DialogTitle(text: String, color: androidx.compose.ui.graphics.Color = AppColors.Text) =
    Text(text, color = color, fontSize = 52.sp, lineHeight = 70.sp, fontWeight = FontWeight.Bold)

@Composable
fun DialogBody(text: String) = Text(text, color = AppColors.TextSub, fontSize = 36.sp, lineHeight = 55.sp)

/** D-20, D-23의 위쪽 그림 상자. */
@Composable
fun DialogArt(@DrawableRes art: Int?, name: String) {
    Box(
        Modifier.fillMaxWidth().heightIn(min = 234.dp).clip(ArtShape).background(AppColors.Accent),
        Alignment.Center,
    ) { Art(art, name, Modifier.padding(13.dp).size(208.dp)) }
}

/** 두 창 크기 미리보기(50-screens.md). */
@Preview(name = "기본 창", widthDp = 1682, heightDp = 1210)
@Preview(name = "화면 나눔", widthDp = 841, heightDp = 1210)
annotation class ScreenPreviews

/** 미리보기용 앱 틀: 사이드 메뉴와 본문. */
@Composable
fun PreviewShell(route: Route, content: @Composable BoxScope.() -> Unit) = AppTheme {
    Row(Modifier.fillMaxSize().background(AppColors.Background)) {
        SideRail(current = route, onSelect = {})
        Box(Modifier.weight(1f).fillMaxHeight(), content = content)
    }
}
