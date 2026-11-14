package com.break8t.unknown.ui.account

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.break8t.unknown.config.BalanceConfig
import com.break8t.unknown.ui.components.AppToast
import com.break8t.unknown.ui.theme.AppColors
import com.break8t.unknown.ui.theme.AppTheme

// 각 화면 상태를 기본 창과 화면 나눔 두 크기로 본다. 견본 계정과 문구는 미리보기에서만 쓴다.
@Preview(name = "기본 창", widthDp = 1682, heightDp = 1210)
@Preview(name = "화면 나눔", widthDp = 841, heightDp = 1210)
internal annotation class AccountPreviews

private val config = BalanceConfig.DEFAULT
private val seeded = DemoAccounts.take(DemoSeedCount)
private val farmer = seeded[0]
private val locked = seeded[1]

@Composable
private fun Frame(content: @Composable BoxScope.() -> Unit) = AppTheme {
    Box(Modifier.fillMaxSize().background(AppColors.Background)) { content() }
}

@AccountPreviews @Composable
private fun S01Splash() = Frame { SplashContent(0.25f) }

@AccountPreviews @Composable
private fun S02Select() = Frame { SelectContent(seeded, farmer.id, countdown = null, onPick = {}, onProfiles = {}) }

@AccountPreviews @Composable
private fun SC00AutoLogin() = Frame { SelectContent(seeded, farmer.id, Countdown(farmer.nickname, 7), onPick = {}, onProfiles = {}) }

@AccountPreviews @Composable
private fun S02SelectFull() = Frame {
    SelectContent(DemoAccounts.take(config.maxAccounts), farmer.id, countdown = null, onPick = {}, onProfiles = {})
}

@Composable
private fun Pin(backLabel: String, account: Account, prompt: String, input: PinInput, error: String, blocked: Boolean = false) = Frame {
    PinContent(backLabel, {}, account, prompt, input, config.pinLength, error, blocked, onDigit = {}, onDelete = {})
}

@AccountPreviews @Composable
private fun S03aPin() = Pin("계정 선택", locked, "PIN 4자리를 눌러 주세요", PinInput("1"), "PIN이 맞지 않아요.")

@AccountPreviews @Composable
private fun S03bPinWrong() = Pin("계정 선택", locked, "PIN 4자리를 눌러 주세요", PinInput(error = true, failures = 1), "PIN이 맞지 않아요.")

@AccountPreviews @Composable
private fun S03PinDialpadBlocked() =
    Pin("계정 선택", locked, "PIN 4자리를 눌러 주세요", PinInput(), "PIN이 맞지 않아요.", blocked = true)

@AccountPreviews @Composable
private fun S04aQr() = Frame {
    QrContent(QrPhase.WAITING, 223, config.loginQrTtlSec, canLeave = true, onLeave = {}, onRenew = {}, onLongPress = {})
}

@AccountPreviews @Composable
private fun S04aQrNoAccounts() = Frame {
    QrContent(QrPhase.WAITING, 223, config.loginQrTtlSec, canLeave = false, onLeave = {}, onRenew = {}, onLongPress = {})
}

@AccountPreviews @Composable
private fun S04bQrExpired() = Frame {
    QrContent(QrPhase.EXPIRED, 0, config.loginQrTtlSec, canLeave = true, onLeave = {}, onRenew = {}, onLongPress = {})
}

@AccountPreviews @Composable
private fun S04cQrCancelled() = Frame {
    QrContent(QrPhase.CANCELLED, 0, config.loginQrTtlSec, canLeave = true, onLeave = {}, onRenew = {}, onLongPress = {})
}

@Composable
private fun Profiles(blocked: Boolean = false, toast: String? = null) = Frame {
    ProfilesContent(seeded, farmer.id, config.maxAccounts, blocked, {}, {}, {}, {}, {})
    if (toast != null) AppToast(toast, Modifier.align(Alignment.BottomCenter).padding(bottom = 72.dp))
}

@AccountPreviews @Composable
private fun S05Profiles() = Profiles()

@AccountPreviews @Composable
private fun S05ProfilesBlocked() = Profiles(blocked = true)

@AccountPreviews @Composable
private fun D01Delete() = Frame {
    Profiles()
    Box(Modifier.fillMaxSize().background(AppColors.Text.copy(alpha = 0.4f)), Alignment.Center) {
        DeletePanel(seeded[2].nickname, onConfirm = {}, onDismiss = {}, modifier = Modifier.padding(24.dp))
    }
}

@AccountPreviews @Composable
private fun T01PinSet() = Profiles(toast = "${farmer.nickname} 계정에 PIN을 설정했어요")

@AccountPreviews @Composable
private fun T02PinOff() = Profiles(toast = "${locked.nickname} 계정의 PIN을 껐어요")

@AccountPreviews @Composable
private fun S06aPinNew() = Pin("프로필 관리", farmer, "새 PIN 4자리를 눌러 주세요", PinInput("12"), "두 번 누른 PIN이 달라요.")

@AccountPreviews @Composable
private fun S06bPinAgain() = Pin("프로필 관리", farmer, "한 번 더 눌러 주세요", PinInput(), "두 번 누른 PIN이 달라요.")

@AccountPreviews @Composable
private fun S06cPinMismatch() =
    Pin("프로필 관리", farmer, "새 PIN 4자리를 눌러 주세요", PinInput(error = true, failures = 1), "두 번 누른 PIN이 달라요.")

@AccountPreviews @Composable
private fun S07aPinOff() = Pin("프로필 관리", locked, "지금 쓰는 PIN을 눌러 주세요", PinInput("1"), "PIN이 맞지 않아요.")

@AccountPreviews @Composable
private fun S07bPinOffWrong() =
    Pin("프로필 관리", locked, "지금 쓰는 PIN을 눌러 주세요", PinInput(error = true, failures = 1), "PIN이 맞지 않아요.")
