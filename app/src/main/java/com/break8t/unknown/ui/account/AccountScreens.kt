package com.break8t.unknown.ui.account

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.break8t.unknown.R
import com.break8t.unknown.ui.components.AppCard
import com.break8t.unknown.ui.components.AppChip
import com.break8t.unknown.ui.components.BorderWidth
import com.break8t.unknown.ui.components.DrivingCover
import com.break8t.unknown.ui.components.PrimaryButton
import com.break8t.unknown.ui.components.SecondaryButton
import com.break8t.unknown.ui.components.drivingBlur
import com.break8t.unknown.ui.theme.AppColors

// 화면별 모양만 그린다. 상태와 이동은 AccountFlow가 정한다.

/** S-01: 안전 안내와 입장 준비 막대. */
@Composable
internal fun SplashContent(progress: Float) = BoxWithConstraints(Modifier.fillMaxSize()) {
    // 세로 창에서는 문구가 줄바꿈되어 길어지므로 여백을 줄여 진행 막대가 한 화면에 들어오게 한다.
    val wide = maxWidth >= WideWidth
    // Figma S-01의 바탕은 다른 계정 화면과 달리 #F1E2BB다.
    Column(
        Modifier.fillMaxSize().background(AppColors.TextOnPrimary).verticalScroll(rememberScrollState()),
    ) {
        AppHeader()
        val shape = RoundedCornerShape(21.dp)
        Column(
            Modifier
                .align(Alignment.CenterHorizontally)
                .padding(start = 24.dp, end = 24.dp, top = 30.dp, bottom = 24.dp)
                .widthIn(max = 1144.dp)
                .fillMaxWidth()
                .clip(shape)
                .background(AppColors.Card)
                .border(5.dp, AppColors.Line, shape)
                .padding(start = if (wide) 73.dp else 36.dp, end = if (wide) 73.dp else 36.dp, top = 47.dp, bottom = 62.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val boxShape = RoundedCornerShape(10.dp)
            Box(
                Modifier.size(99.dp).clip(boxShape).background(AppColors.Selected).border(BorderWidth, AppColors.Line, boxShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(R.drawable.account_ic_warn), contentDescription = null, Modifier.size(62.dp), tint = AppColors.Text)
            }
            Spacer(Modifier.height(23.dp))
            Text(stringResource(R.string.account_safety_label), color = AppColors.Error, fontSize = 34.sp, fontWeight = FontWeight.Bold)
            Text(
                stringResource(R.string.account_safety_title),
                color = AppColors.Text,
                fontSize = 62.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.account_safety_body), color = AppColors.Text, fontSize = 42.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(31.dp))
            Column(
                Modifier.fillMaxWidth().clip(boxShape).background(AppColors.Muted).padding(horizontal = 44.dp, vertical = 34.dp),
                verticalArrangement = Arrangement.spacedBy(26.dp),
            ) {
                Text("•  " + stringResource(R.string.account_safety_item1), color = AppColors.Text, fontSize = 36.sp)
                Text("•  " + stringResource(R.string.account_safety_item2), color = AppColors.Text, fontSize = 36.sp)
            }
            Spacer(Modifier.height(if (wide) 109.dp else 40.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(boxShape)
                    .background(AppColors.Accent)
                    .border(BorderWidth, AppColors.Line, boxShape)
                    .padding(horizontal = 36.dp, vertical = 34.dp),
                verticalArrangement = Arrangement.spacedBy(28.dp),
            ) {
                Box(Modifier.fillMaxWidth().height(44.dp).background(AppColors.Muted).border(BorderWidth, AppColors.Line)) {
                    Box(
                        Modifier.fillMaxWidth(progress).height(44.dp).background(AppColors.Success).border(BorderWidth, AppColors.Line),
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.account_loading), color = AppColors.Text, fontSize = 39.sp, fontWeight = FontWeight.Bold)
                    Text(
                        stringResource(R.string.account_percent, (progress * 100).toInt()),
                        color = AppColors.Text,
                        fontSize = 39.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

/** 자동 로그인 카운트다운에 보일 값. */
internal data class Countdown(val nickname: String, val seconds: Int)

/** S-02 계정 선택. countdown이 있으면 SC-00 자동 로그인 안내를 함께 보인다. */
@Composable
internal fun SelectContent(
    accounts: List<Account>,
    lastUsedId: String?,
    countdown: Countdown?,
    onPick: (Account) -> Unit,
    onProfiles: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        AppHeader()
        Column(Modifier.fillMaxWidth().padding(horizontal = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(80.dp))
            Text(stringResource(R.string.account_select_title), color = AppColors.Text, fontSize = 62.sp, fontWeight = FontWeight.Bold)
            // 카운트다운이 멈춰도 자리는 남긴다. 누르는 순간 카드가 손가락 아래에서 올라가 버리면 눌리지 않는다.
            var lastCountdown by remember { mutableStateOf(countdown) }
            if (countdown != null) lastCountdown = countdown
            lastCountdown?.let { shown ->
                Spacer(Modifier.height(31.dp))
                Column(
                    Modifier
                        .alpha(if (countdown != null) 1f else 0f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(AppColors.Accent)
                        .padding(horizontal = 36.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        stringResource(R.string.account_auto_login, shown.seconds, shown.nickname),
                        color = AppColors.Text,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                    Text(stringResource(R.string.account_auto_login_hint), color = AppColors.TextSub, fontSize = 31.sp)
                }
            }
            Spacer(Modifier.height(52.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(35.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(35.dp),
            ) {
                accounts.forEach { AccountCard(it, highlighted = it.id == lastUsedId) { onPick(it) } }
            }
            Spacer(Modifier.height(91.dp))
            SecondaryButton(stringResource(R.string.account_profiles), onProfiles, Modifier.width(468.dp))
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun AccountCard(account: Account, highlighted: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(21.dp)
    Box(
        Modifier
            .size(width = 286.dp, height = 377.dp)
            .clip(shape)
            .background(AppColors.Card)
            .border(if (highlighted) 6.5.dp else BorderWidth, if (highlighted) AppColors.Selected else AppColors.Line, shape)
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        Column(
            Modifier.fillMaxSize().padding(top = 36.dp, start = 8.dp, end = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Avatar(account.characterId, 156.dp, 31.dp)
            Text(
                account.nickname,
                color = AppColors.Text,
                fontSize = 44.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                stringResource(if (account.pin != null) R.string.account_locked else R.string.account_saved),
                color = AppColors.TextSub,
                fontSize = 31.sp,
            )
        }
        if (account.pin != null) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 16.dp, end = 21.dp)
                    .size(73.dp)
                    .clip(CircleShape)
                    .background(AppColors.Card)
                    .border(BorderWidth, AppColors.Line, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(R.drawable.account_ic_lock), contentDescription = null, Modifier.size(42.dp), tint = AppColors.Text)
            }
        }
    }
}

/** S-03, S-06, S-07: 왼쪽에 계정과 PIN 칸, 오른쪽에 숫자판. 좁은 창에서는 위아래로 놓는다. */
@Composable
internal fun PinContent(
    backLabel: String,
    onBack: () -> Unit,
    account: Account,
    prompt: String,
    input: PinInput,
    length: Int,
    errorText: String,
    keypadBlocked: Boolean,
    onDigit: (Char) -> Unit,
    onDelete: () -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= WideWidth
        Column(Modifier.fillMaxSize().then(if (wide) Modifier else Modifier.verticalScroll(rememberScrollState())).padding(35.dp)) {
            BackChip(backLabel, onBack)
            if (wide) {
                // 가로 창은 높이가 넉넉해서 스크롤 없이 남은 높이의 가운데에 둔다.
                Row(
                    Modifier.fillMaxWidth().weight(1f),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PinSide(account, prompt, input, length, errorText, showAvatar = true)
                    Keypad(onDigit, onDelete, keypadBlocked)
                }
            } else {
                // 세로 창에서는 숫자판이 한 화면에 들어오도록 그림을 뺀다.
                Column(
                    Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    PinSide(account, prompt, input, length, errorText, showAvatar = false)
                    Keypad(onDigit, onDelete, keypadBlocked)
                }
            }
        }
    }
}

/** S-04-a/b/c QR 로그인. 길게 누르면 휴대폰에서 로그인한 것으로 친다(시연용). */
@Composable
internal fun QrContent(
    phase: QrPhase,
    remainingSec: Int,
    ttlSec: Int,
    canLeave: Boolean,
    onLeave: () -> Unit,
    onRenew: () -> Unit,
    onLongPress: () -> Unit,
) {
    if (phase == QrPhase.CANCELLED) return QrCancelledContent(canLeave, onLeave)
    val expired = phase == QrPhase.EXPIRED
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= WideWidth
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(35.dp)) {
            if (canLeave) BackChip(stringResource(R.string.account_back_select), onLeave) else Spacer(Modifier.height(100.dp))
            Text(
                stringResource(R.string.account_qr_title),
                Modifier.padding(start = 43.dp, top = 48.dp),
                color = AppColors.Text,
                fontSize = 62.sp,
                fontWeight = FontWeight.Bold,
            )
            val card = @Composable { QrCard(expired, onLongPress) }
            val side = @Composable { QrSide(expired, remainingSec, ttlSec, canLeave, onLeave, onRenew) }
            if (wide) {
                Row(Modifier.padding(start = 43.dp, top = 120.dp), horizontalArrangement = Arrangement.spacedBy(78.dp)) {
                    card()
                    side()
                }
            } else {
                Column(
                    Modifier.fillMaxWidth().padding(top = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(48.dp),
                ) {
                    card()
                    side()
                }
            }
        }
    }
}

@Composable
private fun QrCard(expired: Boolean, onLongPress: () -> Unit) {
    val latest by rememberUpdatedState(onLongPress)
    val shape = RoundedCornerShape(21.dp)
    val qrLabel = stringResource(R.string.account_qr_desc)
    Box(
        Modifier.size(598.dp).clip(shape).background(AppColors.Card).border(BorderWidth, AppColors.Line, shape),
        contentAlignment = Alignment.Center,
    ) {
        QrPattern(
            Modifier
                .size(468.dp)
                .alpha(if (expired) 0.15f else 1f)
                .semantics { contentDescription = qrLabel }
                .pointerInput(Unit) { detectTapGestures(onLongPress = { latest() }) },
        )
        if (expired) {
            Text(stringResource(R.string.account_qr_expired), color = AppColors.Text, fontSize = 52.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun QrSide(expired: Boolean, remainingSec: Int, ttlSec: Int, canLeave: Boolean, onLeave: () -> Unit, onRenew: () -> Unit) {
    Column(Modifier.widthIn(max = 806.dp)) {
        listOf(R.string.account_qr_step1, R.string.account_qr_step2, R.string.account_qr_step3).forEach {
            Text(stringResource(it), color = AppColors.Text, fontSize = 42.sp, modifier = Modifier.padding(bottom = 23.dp))
        }
        if (expired) {
            Text(
                stringResource(R.string.account_qr_ttl, ttlSec / 60),
                Modifier.padding(top = 37.dp),
                color = AppColors.Error,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
            )
            Row(Modifier.padding(top = 42.dp), horizontalArrangement = Arrangement.spacedBy(35.dp)) {
                PrimaryButton(stringResource(R.string.account_qr_renew), onRenew, Modifier.widthIn(min = 390.dp))
                if (canLeave) SecondaryButton(stringResource(R.string.components_cancel), onLeave, Modifier.widthIn(min = 260.dp))
            }
        } else {
            Row(
                Modifier
                    .padding(top = 26.dp)
                    .fillMaxWidth()
                    .heightIn(min = 99.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(AppColors.Accent)
                    .padding(horizontal = 29.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(21.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(Modifier.size(47.dp), color = AppColors.Primary, strokeWidth = 6.5.dp, trackColor = Color.Transparent)
                Text(stringResource(R.string.account_qr_waiting), color = AppColors.Text, fontSize = 36.sp, fontWeight = FontWeight.Bold)
            }
            Text(
                stringResource(R.string.account_qr_remaining, remainingSec / 60, remainingSec % 60),
                Modifier.padding(top = 23.dp),
                color = AppColors.TextSub,
                fontSize = 34.sp,
            )
            if (canLeave) {
                SecondaryButton(stringResource(R.string.components_cancel), onLeave, Modifier.padding(top = 39.dp).widthIn(min = 260.dp))
            }
        }
    }
}

/**
 * S-04-c: P단을 벗어나 QR 로그인을 취소했을 때. 다시 P가 되면 AccountFlow가 이 화면을 닫는다.
 * 주행 중에도 계정 선택으로 돌아갈 수 있게 Figma에 없는 돌아가기 칩을 둔다.
 */
@Composable
internal fun QrCancelledContent(canLeave: Boolean, onLeave: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        if (canLeave) BackChip(stringResource(R.string.account_back_select), onLeave, Modifier.padding(35.dp)) else AppHeader()
        Column(
            Modifier.fillMaxWidth().padding(start = 40.dp, end = 40.dp, top = 220.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier.size(130.dp).clip(RoundedCornerShape(26.dp)).background(AppColors.Accent),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(R.drawable.account_ic_lock), contentDescription = null, Modifier.size(73.dp), tint = AppColors.Text)
            }
            Spacer(Modifier.height(52.dp))
            Text(
                stringResource(R.string.account_qr_cancelled_title),
                color = AppColors.Text,
                fontSize = 57.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))
            Text(stringResource(R.string.account_qr_cancelled_body), color = AppColors.TextSub, fontSize = 39.sp, textAlign = TextAlign.Center)
        }
    }
}

/** S-05 프로필 관리. 플랫폼이 설정 화면을 막으면 목록을 흐리게 덮는다. */
@Composable
internal fun ProfilesContent(
    accounts: List<Account>,
    currentId: String?,
    maxAccounts: Int,
    blocked: Boolean,
    onBack: () -> Unit,
    onPinSet: (Account) -> Unit,
    onPinOff: (Account) -> Unit,
    onDelete: (Account) -> Unit,
    onAdd: () -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= WideWidth
        Column(Modifier.fillMaxSize().padding(35.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(29.dp)) {
                BackChip(stringResource(R.string.account_back_select), onBack)
                AppChip(stringResource(R.string.account_profiles), selected = true, onClick = {})
            }
            Box(Modifier.weight(1f).fillMaxWidth().padding(top = 48.dp)) {
                Column(
                    Modifier.fillMaxSize().drivingBlur(blocked).verticalScroll(rememberScrollState()).padding(horizontal = 43.dp),
                    verticalArrangement = Arrangement.spacedBy(31.dp),
                ) {
                    accounts.forEach {
                        ProfileRow(it, current = !canDelete(it, currentId), wide, { onPinSet(it) }, { onPinOff(it) }, { onDelete(it) })
                    }
                    Row(
                        Modifier.padding(top = 21.dp, bottom = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(39.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlineButton(
                            stringResource(R.string.account_add),
                            onAdd,
                            Modifier.width(468.dp),
                            enabled = accounts.size < maxAccounts,
                        )
                        Text(stringResource(R.string.account_count, accounts.size, maxAccounts), color = AppColors.TextSub, fontSize = 36.sp)
                    }
                }
                if (blocked) DrivingCover(stringResource(R.string.account_park_hint_profiles))
            }
        }
    }
}

@Composable
private fun ProfileRow(account: Account, current: Boolean, wide: Boolean, onPinSet: () -> Unit, onPinOff: () -> Unit, onDelete: () -> Unit) {
    val sub = listOfNotNull(
        stringResource(R.string.account_current).takeIf { current },
        stringResource(R.string.account_pin_on).takeIf { account.pin != null },
    ).joinToString(" · ")
    val info = @Composable {
        Avatar(account.characterId, 99.dp, 21.dp)
        Column {
            Text(account.nickname, color = AppColors.Text, fontSize = 44.sp, fontWeight = FontWeight.Bold)
            if (sub.isNotEmpty()) Text(sub, color = AppColors.TextSub, fontSize = 31.sp)
        }
    }
    val buttons = @Composable {
        if (account.pin != null) {
            SecondaryButton(stringResource(R.string.account_pin_off), onPinOff, Modifier.widthIn(min = 299.dp))
        } else {
            SecondaryButton(stringResource(R.string.account_pin_set), onPinSet, Modifier.widthIn(min = 299.dp))
        }
        OutlineButton(
            stringResource(R.string.account_delete),
            onDelete,
            Modifier.widthIn(min = 212.dp),
            enabled = !current,
            danger = true,
        )
    }
    AppCard(Modifier.fillMaxWidth()) {
        if (wide) {
            Row(Modifier.padding(23.dp), horizontalArrangement = Arrangement.spacedBy(31.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(31.dp), verticalAlignment = Alignment.CenterVertically) {
                    info()
                }
                buttons()
            }
        } else {
            Column(Modifier.padding(23.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(31.dp), verticalAlignment = Alignment.CenterVertically) { info() }
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) { buttons() }
            }
        }
    }
}

/** D-01 계정 삭제 확인. 확인 버튼은 빨간 테두리라서 공용 AppDialog 대신 여기서 그린다. */
@Composable
internal fun DeletePanel(nickname: String, onConfirm: () -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(26.dp)
    Column(
        modifier
            .widthIn(max = 936.dp)
            .clip(shape)
            .background(AppColors.Card)
            .border(BorderWidth, AppColors.Line, shape)
            .padding(52.dp),
        verticalArrangement = Arrangement.spacedBy(31.dp),
    ) {
        Text(
            stringResource(R.string.account_delete_title, nickname),
            color = AppColors.Text,
            fontSize = 52.sp,
            lineHeight = 70.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(stringResource(R.string.account_delete_body), color = AppColors.TextSub, fontSize = 36.sp, lineHeight = 55.sp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(35.dp)) {
            SecondaryButton(stringResource(R.string.components_cancel), onDismiss, Modifier.weight(1f))
            OutlineButton(stringResource(R.string.account_delete_confirm), onConfirm, Modifier.weight(1f), danger = true)
        }
    }
}
