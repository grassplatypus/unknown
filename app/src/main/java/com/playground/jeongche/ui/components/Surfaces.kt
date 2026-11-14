package com.playground.jeongche.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.playground.jeongche.R
import com.playground.jeongche.ui.theme.AppColors

// Figma "0. 공통 조각" Card, Dialog, Toast. Sizes are Figma px x 1.3, see SideRail.kt.
private val DialogShape = RoundedCornerShape(26.dp)

/** 카드 바탕. 안쪽 여백과 배치는 내용 쪽에서 정한다. */
@Composable
fun AppCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) =
    Surface(
        modifier,
        shape = RoundedCornerShape(21.dp),
        color = AppColors.Card,
        contentColor = AppColors.Text,
        border = BorderStroke(BorderWidth, AppColors.Line),
        content = content,
    )

/** 제목, 본문, 취소/확인 버튼이 있는 다이얼로그. 바깥을 누르거나 취소하면 onDismiss를 부른다. */
@Composable
fun AppDialog(title: String, body: String, confirmText: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    // Platform default width is too narrow for 44sp buttons; the panel caps its own width.
    Dialog(onDismiss, DialogProperties(usePlatformDefaultWidth = false)) {
        DialogPanel(title, body, confirmText, onConfirm, onDismiss, Modifier.padding(24.dp))
    }
}

/** AppDialog의 창 안쪽. 미리보기에서 창 없이 보이려고 따로 둔다. */
@Composable
internal fun DialogPanel(
    title: String,
    body: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .widthIn(max = 832.dp)
            .clip(DialogShape)
            .background(AppColors.Card)
            .border(BorderWidth, AppColors.Line, DialogShape)
            .padding(52.dp),
        verticalArrangement = Arrangement.spacedBy(31.dp),
    ) {
        Text(title, color = AppColors.Text, fontSize = 52.sp, lineHeight = 70.sp, fontWeight = FontWeight.Bold)
        Text(body, color = AppColors.TextSub, fontSize = 36.sp, lineHeight = 55.sp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(35.dp)) {
            SecondaryButton(stringResource(R.string.components_cancel), onDismiss, Modifier.weight(1f))
            PrimaryButton(confirmText, onConfirm, Modifier.weight(1f))
        }
    }
}

/** 짧은 알림 모양만 그린다. 언제 띄우고 언제 지울지, 어디에 둘지는 부르는 쪽이 정한다. */
@Composable
fun AppToast(message: String, modifier: Modifier = Modifier) {
    Row(
        modifier
            .heightIn(min = 100.dp)
            .clip(RoundedCornerShape(percent = 50))
            .background(AppColors.Text)
            .semantics { liveRegion = LiveRegionMode.Polite }
            .padding(horizontal = 47.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(R.drawable.ic_check), contentDescription = null, Modifier.size(44.dp), tint = AppColors.Card)
        Text(message, color = AppColors.Card, fontSize = 39.sp, fontWeight = FontWeight.Bold)
    }
}
