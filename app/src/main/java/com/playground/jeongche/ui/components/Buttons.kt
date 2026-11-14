package com.playground.jeongche.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.playground.jeongche.R
import com.playground.jeongche.ui.theme.AppColors

// Figma "0. 공통 조각" Button, 상태 버튼, Chip. Sizes are Figma px x 1.3, see SideRail.kt.
internal val ControlShape = RoundedCornerShape(16.dp)
internal val BorderWidth = 3.dp

/** 주 버튼. Figma 노드의 글자색은 #FFFAF0(Card)이다. */
@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) =
    Label(text, AppColors.Primary, AppColors.Card, bordered = false, 44.sp, modifier, Modifier.clickable(role = Role.Button, onClick = onClick))

/** 보조 버튼(다이얼로그의 취소 등). */
@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) =
    Label(text, AppColors.Card, AppColors.Text, bordered = true, 44.sp, modifier, Modifier.clickable(role = Role.Button, onClick = onClick))

/** 켜짐/꺼짐 상태 버튼. 글자는 상태를 그대로 보인다. */
@Composable
fun StateButton(on: Boolean, onChange: (Boolean) -> Unit, modifier: Modifier = Modifier) =
    Label(
        stringResource(if (on) R.string.components_on else R.string.components_off),
        if (on) AppColors.Selected else AppColors.Muted,
        if (on) AppColors.Text else AppColors.TextSub,
        bordered = true,
        44.sp,
        modifier.widthIn(min = 208.dp),
        Modifier.toggleable(on, role = Role.Switch, onValueChange = onChange),
    )

/** 고를 수 있는 칩. 고른 칩은 선택 색으로 칠한다. */
@Composable
fun AppChip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) =
    Label(
        text,
        if (selected) AppColors.Selected else AppColors.Card,
        AppColors.Text,
        bordered = true,
        39.sp,
        modifier,
        Modifier.selectable(selected, onClick = onClick, role = Role.Tab),
    )

// action goes after clip so the press ripple follows the rounded shape.
@Composable
private fun Label(
    text: String,
    fill: Color,
    textColor: Color,
    bordered: Boolean,
    fontSize: TextUnit,
    modifier: Modifier,
    action: Modifier,
) {
    Box(
        modifier
            .heightIn(min = 100.dp)
            .clip(ControlShape)
            .background(fill)
            .then(if (bordered) Modifier.border(BorderWidth, AppColors.Line, ControlShape) else Modifier)
            .then(action)
            .padding(horizontal = 26.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = textColor, fontSize = fontSize, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}
