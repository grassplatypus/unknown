package com.break8t.unknown.ui.account

import androidx.compose.foundation.layout.requiredSize
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.break8t.unknown.R
import com.break8t.unknown.ui.components.BorderWidth
import com.break8t.unknown.ui.components.ControlShape
import com.break8t.unknown.ui.components.DrivingCover
import com.break8t.unknown.ui.components.drivingBlur
import com.break8t.unknown.ui.theme.AppColors

// Figma "1. 시작과 계정". 크기는 Figma px × 1.3이다(SideRail.kt 참고).
internal val WideWidth = 1200.dp
internal val SidePadding = 78.dp

@DrawableRes
internal fun characterRes(id: String): Int = when (id) {
    "ch_cat" -> R.drawable.account_ch_cat
    "ch_mush" -> R.drawable.account_ch_mush
    "ch_cloud" -> R.drawable.account_ch_cloud
    "ch_firefly" -> R.drawable.account_ch_firefly
    else -> R.drawable.account_ch_sprout
}

/** 로고와 앱 이름. */
@Composable
internal fun AppHeader(modifier: Modifier = Modifier) {
    Row(
        modifier.padding(start = 44.dp, top = 39.dp),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 런처 아이콘 앞면은 둘레에 투명 여백이 커서, 그림 부분이 62dp 높이를 채우도록 크게 그리고 가운데만 보인다.
        Box(Modifier.size(62.dp), contentAlignment = Alignment.Center) {
            Image(painterResource(R.mipmap.ic_launcher_foreground), contentDescription = null, Modifier.requiredSize(112.dp))
        }
        Text(stringResource(R.string.app_name), color = AppColors.Text, fontSize = 44.sp, fontWeight = FontWeight.Bold)
    }
}

/** 왼쪽 위 "< 계정 선택" 같은 돌아가기 칩. */
@Composable
internal fun BackChip(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .heightIn(min = 100.dp)
            .clip(ControlShape)
            .background(AppColors.Card)
            .border(BorderWidth, AppColors.Line, ControlShape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 31.dp),
        horizontalArrangement = Arrangement.spacedBy(13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(R.drawable.account_ic_back), contentDescription = null, Modifier.size(44.dp), tint = AppColors.Text)
        Text(text, color = AppColors.Text, fontSize = 44.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
internal fun Avatar(characterId: String, size: Dp, corner: Dp, modifier: Modifier = Modifier) {
    Image(
        painterResource(characterRes(characterId)),
        contentDescription = null,
        modifier.size(size).clip(RoundedCornerShape(corner)).background(AppColors.Accent),
        contentScale = ContentScale.Crop,
    )
}

/** 테두리가 있는 버튼. danger면 빨간 테두리(계정 삭제), 쓸 수 없으면 회색으로 칠한다. */
@Composable
internal fun OutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    danger: Boolean = false,
) {
    val tone = if (danger) AppColors.Error else AppColors.Text
    Box(
        modifier
            .heightIn(min = 100.dp)
            .clip(ControlShape)
            .background(if (enabled) AppColors.Card else AppColors.Disabled)
            .then(if (enabled) Modifier.border(BorderWidth, if (danger) tone else AppColors.Line, ControlShape) else Modifier)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 26.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = if (enabled) tone else AppColors.DisabledText, fontSize = 44.sp, fontWeight = FontWeight.Bold)
    }
}

/** PIN 칸. 다음에 채울 칸은 노란 테두리, 틀렸을 때는 모든 칸이 빨간 테두리다. */
@Composable
internal fun PinBoxes(input: PinInput, length: Int, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(26.dp)) {
        repeat(length) { i ->
            val active = !input.error && i == input.digits.length
            val shape = RoundedCornerShape(16.dp)
            Box(
                Modifier
                    .size(width = 120.dp, height = 146.dp)
                    .clip(shape)
                    .background(AppColors.Card)
                    .border(
                        if (active) 6.5.dp else BorderWidth,
                        when {
                            input.error -> AppColors.Error
                            active -> AppColors.Selected
                            else -> AppColors.Line
                        },
                        shape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (i < input.digits.length) Box(Modifier.size(34.dp).clip(CircleShape).background(AppColors.Text))
            }
        }
    }
}

/** 숫자판. 플랫폼이 숫자판을 막으면 흐리게 덮고 안내를 띄운다. */
@Composable
internal fun Keypad(onDigit: (Char) -> Unit, onDelete: () -> Unit, blocked: Boolean, modifier: Modifier = Modifier) {
    Box(modifier) {
        Column(Modifier.drivingBlur(blocked), verticalArrangement = Arrangement.spacedBy(35.dp)) {
            listOf("123", "456", "789", " 0<").forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(35.dp)) {
                    row.forEach { c ->
                        val key = Modifier.size(width = 208.dp, height = 125.dp)
                        when (c) {
                            ' ' -> Spacer(key)
                            '<' -> KeyBox(key, onDelete) {
                                Icon(
                                    painterResource(R.drawable.account_ic_del),
                                    contentDescription = stringResource(R.string.account_key_delete),
                                    Modifier.size(57.dp),
                                    tint = AppColors.Text,
                                )
                            }
                            else -> KeyBox(key, { onDigit(c) }) {
                                Text(c.toString(), color = AppColors.Text, fontSize = 57.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
        if (blocked) DrivingCover(stringResource(R.string.account_park_hint_pin), Modifier.matchParentSize())
    }
}

@Composable
private fun KeyBox(modifier: Modifier, onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier
            .clip(ControlShape)
            .background(AppColors.Card)
            .border(BorderWidth, AppColors.Line, ControlShape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}

/** 이름과 안내 한 줄, 그 아래 PIN 칸과 오류 문구. PIN 화면 셋이 함께 쓴다. */
@Composable
internal fun PinSide(
    account: Account,
    prompt: String,
    input: PinInput,
    length: Int,
    errorText: String,
    showAvatar: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
        if (showAvatar) Avatar(account.characterId, 156.dp, 31.dp)
        Text(account.nickname, color = AppColors.Text, fontSize = 52.sp, fontWeight = FontWeight.Bold)
        Text(prompt, color = AppColors.Text, fontSize = 44.sp, textAlign = TextAlign.Center)
        PinBoxes(input, length, Modifier.padding(top = 16.dp))
        // 빈 문자열은 한글과 줄 높이가 달라 화면이 밀린다. 같은 문구를 늘 그리고 오류가 없을 때만 투명하게 둔다.
        Text(
            errorText,
            Modifier.alpha(if (input.error) 1f else 0f),
            color = AppColors.Error,
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false,
        )
    }
}

// Figma "v4/QR 견본 (스캔되지 않음)" 29×29 칸. 둘레에 두 칸 여백을 둔다.
private val QrRows = listOf(
    "11111110000101011000101111111",
    "10000010101100100010101000001",
    "10111010110100001010001011101",
    "10111010010000010111001011101",
    "10111010100000000101001011101",
    "10000010110110111110001000001",
    "11111110101000001101101111111",
    "00000000111100111101000000000",
    "00010010100000111001100001000",
    "11111101100111000010100110001",
    "10001100110010000011110011100",
    "00000000101000110110111010000",
    "11011010011100011111110100100",
    "11110001010111111110100011100",
    "10111001100100001110110010001",
    "10000010011101110110101011111",
    "01000001010111000010010111111",
    "10001010111111000101011100101",
    "00011101100000101001010010100",
    "00100011111010110011111001100",
    "00110000101000010100001100001",
    "00000000001001010001001100110",
    "11111110100001111101010000000",
    "10000010101000100110001000000",
    "10111010011010101110110100011",
    "10111010100001010111011010010",
    "10111010011101000000010111110",
    "10000010001101110011110111001",
    "11111110001001101010100111110",
)

/** 스캔되지 않는 견본 QR. 백엔드가 없어서 실제 코드를 만들지 않는다. */
@Composable
internal fun QrPattern(modifier: Modifier = Modifier) {
    Canvas(modifier.clip(RoundedCornerShape(14.dp)).background(Color.White)) {
        val cell = size.minDimension / (QrRows.size + 4)
        QrRows.forEachIndexed { y, row ->
            row.forEachIndexed { x, c ->
                if (c == '1') drawRect(AppColors.Text, Offset((x + 2) * cell, (y + 2) * cell), Size(cell, cell))
            }
        }
    }
}
