package com.break8t.unknown.ui.shop

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.break8t.unknown.R
import com.break8t.unknown.ui.Route
import com.break8t.unknown.ui.components.AppToast
import com.break8t.unknown.ui.components.BorderWidth
import com.break8t.unknown.ui.components.ControlShape
import com.break8t.unknown.ui.components.SideRail
import com.break8t.unknown.ui.theme.AppColors
import com.break8t.unknown.ui.theme.AppTheme
import kotlinx.coroutines.delay

// Figma "4. 섬" S-18 and "5. 상점". Sizes are Figma px x 1.3, see SideRail.kt.
internal val ScreenPadding = 35.dp
internal val Gap = 35.dp
internal val TileShape = RoundedCornerShape(21.dp)
internal val BoxShape = RoundedCornerShape(13.dp)
private val DialogShape = RoundedCornerShape(26.dp)

// The toast has no timing in Figma; long enough to read one short line.
private const val ToastMillis = 2500L

/** 이 묶음이 내보낸 그림. 없는 것(ch_cat 등)은 null이고, 그 자리는 글자 원으로 그린다. */
@DrawableRes
internal fun shopImage(id: String): Int? = when (id) {
    "seed_carrot" -> R.drawable.shop_seed_carrot
    "seed_potato" -> R.drawable.shop_seed_potato
    "seed_wheat" -> R.drawable.shop_seed_wheat
    "it_sofa" -> R.drawable.shop_it_sofa
    "it_plant" -> R.drawable.shop_it_plant
    "it_picture" -> R.drawable.shop_it_picture
    "it_rug" -> R.drawable.shop_it_rug
    "it_lamp" -> R.drawable.shop_it_lamp
    "it_shelf" -> R.drawable.shop_it_shelf
    "ch_sprout", "voice_default", "voice_sprout" -> R.drawable.shop_ch_sprout
    "ch_cloud" -> R.drawable.shop_ch_cloud
    "pose_wave" -> R.drawable.shop_pose_wave
    "pose_jump" -> R.drawable.shop_pose_jump
    "pose_sit" -> R.drawable.shop_pose_sit
    "pose_side" -> R.drawable.shop_pose_side
    "pose_back" -> R.drawable.shop_pose_back
    else -> null
}

/** 상품 그림. 그림이 없으면 이름 첫 글자를 넣은 원을 그린다. */
@Composable
internal fun ItemImage(id: String, name: String, size: Dp, modifier: Modifier = Modifier) {
    val res = shopImage(id)
    if (res != null) {
        Image(painterResource(res), contentDescription = null, modifier.size(size))
    } else {
        Box(modifier.size(size).padding(size / 8).clip(CircleShape).background(AppColors.Selected), Alignment.Center) {
            Text(name.take(1), color = AppColors.Text, fontSize = (size.value * 0.35f).sp, fontWeight = FontWeight.Bold)
        }
    }
}

/** 상단 칩(섬으로, 상점, 보관함). onClick이 null이면 지금 화면이라 누를 수 없다. */
@Composable
internal fun HeaderChip(text: String, @DrawableRes icon: Int?, selected: Boolean, onClick: (() -> Unit)?) {
    Row(
        Modifier
            .heightIn(min = 100.dp)
            .clip(ControlShape)
            .background(if (selected) AppColors.Selected else AppColors.Card)
            .border(BorderWidth, AppColors.Line, ControlShape)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(horizontal = 31.dp),
        horizontalArrangement = Arrangement.spacedBy(13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) Icon(painterResource(icon), contentDescription = null, Modifier.size(44.dp), tint = AppColors.Text)
        Text(text, color = AppColors.Text, fontSize = 44.sp, fontWeight = FontWeight.Bold)
    }
}

/** Figma "coin". 그림 대신 원으로 그린다. */
@Composable
internal fun CoinDot(size: Dp) =
    Box(Modifier.size(size).clip(CircleShape).background(AppColors.Selected).border(size / 9, AppColors.CoinBorder, CircleShape))

@Composable
internal fun CoinAmount(amount: Int, fontSize: TextUnit, color: Color = AppColors.Text) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        CoinDot((fontSize.value * 0.95f).dp)
        Text(amount.toString(), color = color, fontSize = fontSize, fontWeight = FontWeight.Bold)
    }
}

/** 상단 줄. 좁은 창에서는 칩이 다음 줄로 넘어가고, 코인 칩은 그 줄의 오른쪽 끝에 붙는다. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun HeaderBar(coins: Int, chips: @Composable () -> Unit) {
    FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Gap),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        chips()
        Spacer(Modifier.weight(1f))
        CoinChip(coins)
    }
}

@Composable
private fun CoinChip(coins: Int) {
    Row(
        Modifier
            .heightIn(min = 100.dp)
            .clip(ControlShape)
            .background(AppColors.Card)
            .border(BorderWidth, AppColors.Line, ControlShape)
            .padding(horizontal = 31.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) { CoinAmount(coins, 44.sp) }
}

/** 빨간 테두리 버튼(버리기). */
@Composable
internal fun DangerButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) =
    FlatButton(text, AppColors.Card, AppColors.Error, AppColors.Error, modifier, onClick)

/** 누를 수 없는 버튼(코인이 모자랄 때의 구매). */
@Composable
internal fun DisabledButton(text: String, modifier: Modifier = Modifier) =
    FlatButton(text, AppColors.Disabled, AppColors.DisabledText, null, modifier, null)

@Composable
private fun FlatButton(text: String, fill: Color, textColor: Color, border: Color?, modifier: Modifier, onClick: (() -> Unit)?) {
    Box(
        modifier
            .heightIn(min = 100.dp)
            .clip(ControlShape)
            .background(fill)
            .then(if (border != null) Modifier.border(BorderWidth, border, ControlShape) else Modifier)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(horizontal = 26.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = textColor, fontSize = 44.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}

/** D-13, D-14의 개수 고르기. 끝에 닿은 쪽 버튼은 꺼진 색으로 바뀐다. */
@Composable
internal fun QtyStepper(qty: Int, min: Int, max: Int, onChange: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        StepButton(R.drawable.shop_ic_minus, R.string.shop_qty_less, qty > min) { onChange(qty - 1) }
        Text(
            qty.toString(),
            Modifier.weight(1f),
            color = AppColors.Text,
            fontSize = 57.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        StepButton(R.drawable.shop_ic_plus, R.string.shop_qty_more, qty < max) { onChange(qty + 1) }
    }
}

@Composable
private fun StepButton(@DrawableRes icon: Int, label: Int, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(width = 109.dp, height = 99.dp)
            .clip(ControlShape)
            .background(if (enabled) AppColors.Card else AppColors.Disabled)
            .then(if (enabled) Modifier.border(BorderWidth, AppColors.Line, ControlShape) else Modifier)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        val tint = if (enabled) AppColors.Text else AppColors.DisabledText
        Icon(painterResource(icon), contentDescription = stringResource(label), Modifier.size(52.dp), tint = tint)
    }
}

/** 다이얼로그 맨 위의 상품 줄: 그림 상자, 이름, 그 아래 한 줄. */
@Composable
internal fun ItemHeaderRow(id: String, name: String, sub: @Composable () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(31.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(156.dp).clip(RoundedCornerShape(16.dp)).background(AppColors.Accent), Alignment.Center) {
            ItemImage(id, name, 125.dp)
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(name, color = AppColors.Text, fontSize = 52.sp, fontWeight = FontWeight.Bold)
            sub()
        }
    }
}

/** 큰 그림 상자(D-15, D-17, D-18, D-19). */
@Composable
internal fun BigImageBox(id: String, name: String) {
    Box(Modifier.fillMaxWidth().heightIn(min = 195.dp).clip(RoundedCornerShape(16.dp)).background(AppColors.Accent), Alignment.Center) {
        ItemImage(id, name, 195.dp)
    }
}

/** 보유와 총액. 총액이 보유보다 크면 총액 쪽을 빨갛게 한다. */
@Composable
internal fun CoinSummary(coins: Int, total: Int) {
    val short = total > coins
    Row(
        Modifier.fillMaxWidth().clip(ControlShape).background(AppColors.Muted).padding(horizontal = 31.dp, vertical = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(R.string.shop_have), color = AppColors.TextSub, fontSize = 39.sp)
        Spacer(Modifier.size(13.dp))
        CoinAmount(coins, 44.sp)
        Spacer(Modifier.weight(1f))
        Text(stringResource(R.string.shop_total), color = if (short) AppColors.Error else AppColors.TextSub, fontSize = 39.sp)
        Spacer(Modifier.size(13.dp))
        CoinAmount(total, 44.sp, if (short) AppColors.Error else AppColors.Text)
    }
}

/** 이 묶음의 다이얼로그 창. 공용 AppDialog는 본문이 글 한 줄이라 그림과 개수 고르기를 넣지 못한다. */
@Composable
internal fun ShopDialog(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismiss, DialogProperties(usePlatformDefaultWidth = false)) {
        DialogBox(Modifier.padding(24.dp), content)
    }
}

@Composable
internal fun DialogBox(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .widthIn(max = 832.dp)
            .clip(DialogShape)
            .background(AppColors.Card)
            .border(BorderWidth, AppColors.Line, DialogShape)
            .padding(52.dp),
        verticalArrangement = Arrangement.spacedBy(31.dp),
        content = content,
    )
}

@Composable
internal fun DialogTitle(text: String) =
    Text(text, color = AppColors.Text, fontSize = 47.sp, lineHeight = 64.sp, fontWeight = FontWeight.Bold)

/** 짧은 알림을 화면 아래에 잠깐 띄운다. */
@Composable
internal fun BoxScope.ToastHost(message: String?, onDone: () -> Unit) {
    if (message == null) return
    LaunchedEffect(message) {
        delay(ToastMillis)
        onDone()
    }
    AppToast(message, Modifier.align(Alignment.BottomCenter).padding(bottom = 52.dp))
}

@Preview(name = "기본 창", widthDp = 1682, heightDp = 1210)
@Preview(name = "화면 나눔", widthDp = 841, heightDp = 1210)
internal annotation class ScreenPreviews

/** 미리보기 틀: 사이드 메뉴와 본문, 그 위에 다이얼로그 자리. */
@Composable
internal fun PreviewFrame(dialog: (@Composable ColumnScope.() -> Unit)? = null, body: @Composable () -> Unit) = AppTheme {
    Row(Modifier.fillMaxSize().background(AppColors.Background)) {
        SideRail(current = Route.ISLAND, onSelect = {})
        Box(Modifier.weight(1f).fillMaxSize()) {
            body()
            if (dialog != null) {
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)), Alignment.Center) {
                    DialogBox(Modifier.padding(24.dp), dialog)
                }
            }
        }
    }
}
