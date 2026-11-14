package com.break8t.unknown.ui.account

import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.break8t.unknown.R
import com.break8t.unknown.config.BalanceConfig
import com.break8t.unknown.data.LocalDemoState
import com.break8t.unknown.drive.DriveMode
import com.break8t.unknown.drive.LocalUxLimits
import com.break8t.unknown.drive.UxLimits
import com.break8t.unknown.ui.components.AppToast
import com.break8t.unknown.ui.theme.AppColors
import kotlinx.coroutines.delay

private enum class Step { SPLASH, SELECT, PIN_SIGN_IN, QR, PROFILES, PIN_SET, PIN_OFF }

// 화면 갱신 주기와 토스트 시간. 밸런스 값이 아니라서 BalanceConfig에 없다.
private const val TickMs = 250L
private const val ToastMillis = 2500L

/** 시작과 계정 화면 묶음(Figma "1. 시작과 계정")의 진입점. 사이드 메뉴 없는 전체 화면이고, 로그인하면 onSignedIn을 한 번 부른다. */
@Composable
fun AccountFlow(mode: DriveMode, onSignedIn: () -> Unit) {
    val config = BalanceConfig.DEFAULT
    val demo = LocalDemoState.current
    val limits = LocalUxLimits.current
    val res = LocalContext.current.resources
    val store = AccountStore
    var step by rememberSaveable { mutableStateOf(Step.SPLASH) }
    var targetId by rememberSaveable { mutableStateOf<String?>(null) }
    var deletingId by rememberSaveable { mutableStateOf<String?>(null) }
    var toast by rememberSaveable { mutableStateOf<String?>(null) }
    var signedIn by rememberSaveable { mutableStateOf(false) }

    fun signIn(account: Account) {
        if (signedIn) return
        signedIn = true
        store.signIn(account.id)
        demo.update { it.copy(nickname = account.nickname) }
        onSignedIn()
    }

    fun open(account: Account) {
        if (account.pin == null) {
            signIn(account)
        } else {
            targetId = account.id
            step = Step.PIN_SIGN_IN
        }
    }

    fun toProfiles(message: String) {
        toast = message
        step = Step.PROFILES
    }

    Box(Modifier.fillMaxSize().background(AppColors.Background)) {
        val target = store.find(targetId)
        when (step) {
            Step.SPLASH -> SplashStep(config) { step = Step.SELECT }
            Step.SELECT ->
                if (store.accounts.isEmpty()) {
                    QrStep(mode, limits, config, canLeave = false, onLeave = {}, onLinked = { signIn(it) })
                } else {
                    SelectStep(config, onOpen = { open(it) }, onProfiles = { step = Step.PROFILES })
                }
            Step.QR -> QrStep(mode, limits, config, canLeave = true, onLeave = { step = Step.SELECT }, onLinked = { signIn(it) })
            Step.PROFILES -> ProfilesContent(
                accounts = store.accounts,
                currentId = store.lastUsedId,
                maxAccounts = config.maxAccounts,
                blocked = limits.noSetup,
                onBack = { step = Step.SELECT },
                onPinSet = {
                    targetId = it.id
                    step = Step.PIN_SET
                },
                onPinOff = {
                    targetId = it.id
                    step = Step.PIN_OFF
                },
                onDelete = { deletingId = it.id },
                onAdd = { step = Step.QR },
            )
            Step.PIN_SIGN_IN, Step.PIN_SET, Step.PIN_OFF ->
                if (target == null) {
                    LaunchedEffect(Unit) { step = Step.SELECT }
                } else if (step == Step.PIN_SIGN_IN) {
                    PinCheckStep(
                        target, config,
                        backLabel = stringResource(R.string.account_back_select),
                        prompt = stringResource(R.string.account_pin_prompt, config.pinLength),
                        blocked = limits.noDialpad,
                        onBack = { step = Step.SELECT },
                        onAccepted = { signIn(target) },
                    )
                } else if (step == Step.PIN_OFF) {
                    PinCheckStep(
                        target, config,
                        backLabel = stringResource(R.string.account_profiles),
                        prompt = stringResource(R.string.account_pin_current),
                        blocked = limits.noDialpad || limits.noSetup,
                        onBack = { step = Step.PROFILES },
                        onAccepted = {
                            store.setPin(target.id, null)
                            toProfiles(res.getString(R.string.account_toast_pin_off, target.nickname))
                        },
                    )
                } else {
                    PinSetStep(target, config, blocked = limits.noDialpad || limits.noSetup, onBack = { step = Step.PROFILES }) { pin ->
                        store.setPin(target.id, pin)
                        toProfiles(res.getString(R.string.account_toast_pin_set, target.nickname))
                    }
                }
        }

        toast?.let { message ->
            AppToast(message, Modifier.align(Alignment.BottomCenter).padding(bottom = 72.dp))
            LaunchedEffect(message) {
                delay(ToastMillis)
                toast = null
            }
        }
        val deleting = store.find(deletingId)
        if (deleting != null && step == Step.PROFILES && !limits.noSetup) {
            Dialog({ deletingId = null }, DialogProperties(usePlatformDefaultWidth = false)) {
                DeletePanel(
                    deleting.nickname,
                    onConfirm = {
                        store.delete(deleting.id)
                        deletingId = null
                    },
                    onDismiss = { deletingId = null },
                    modifier = Modifier.padding(24.dp),
                )
            }
        }
    }
}

/** 화면에 남은 초를 보이려고 일정 간격으로 지금 시각을 다시 읽는다. 시각은 부팅 뒤 흐른 시간이라 시계를 바꿔도 흔들리지 않는다. */
@Composable
private fun rememberNow(): Long {
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(TickMs)
            now = SystemClock.elapsedRealtime()
        }
    }
    return now
}

@Composable
private fun SplashStep(config: BalanceConfig, onDone: () -> Unit) {
    val start = rememberSaveable { SystemClock.elapsedRealtime() }
    var elapsed by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        val holdMs = config.splashHoldSec * 1000L
        while (true) {
            elapsed = SystemClock.elapsedRealtime() - start
            if (elapsed >= holdMs) break
            delay(holdMs / SplashSteps)
        }
        onDone()
    }
    SplashContent(splashProgress(elapsed, config))
}

@Composable
private fun SelectStep(config: BalanceConfig, onOpen: (Account) -> Unit, onProfiles: () -> Unit) {
    val store = AccountStore
    LaunchedEffect(Unit) { store.offerAutoLogin(SystemClock.elapsedRealtime()) }
    val now = rememberNow()
    val auto = store.autoLogin?.takeIf { !it.cancelled }
    val target = store.find(auto?.targetId)
    LaunchedEffect(auto, now) {
        if (auto != null && target != null && auto.due(now, config)) {
            store.stopAutoLogin()
            onOpen(target)
        }
    }
    SelectContent(
        accounts = store.accounts,
        lastUsedId = store.lastUsedId,
        countdown = if (auto != null && target != null) Countdown(target.nickname, auto.remainingSec(now, config)) else null,
        onPick = onOpen,
        onProfiles = onProfiles,
        // 아무 곳이나 누르면 카운트다운을 멈춘다. Initial 단계에서 보기만 하고 소비하지 않아서 누른 카드도 그대로 눌린다.
        modifier = Modifier
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        if (awaitPointerEvent(PointerEventPass.Initial).type == PointerEventType.Press) store.stopAutoLogin()
                    }
                }
            }
            .onPreviewKeyEvent {
                store.stopAutoLogin()
                false
            },
    )
}

@Composable
private fun QrStep(
    mode: DriveMode,
    limits: UxLimits,
    config: BalanceConfig,
    canLeave: Boolean,
    onLeave: () -> Unit,
    onLinked: (Account) -> Unit,
) {
    var session by rememberSaveable(stateSaver = QrSessionSaver) { mutableStateOf(QrSession(SystemClock.elapsedRealtime())) }
    val blocked = qrBlocked(mode, limits)
    // 막힌 뒤 다시 P가 되면: 돌아갈 곳이 있으면 계정 선택으로, 저장된 계정이 없으면 새 QR을 받는다.
    LaunchedEffect(blocked) {
        session = session.onBlocked(blocked)
        if (!blocked && session.cancelled) {
            if (canLeave) onLeave() else session = QrSession(SystemClock.elapsedRealtime())
        }
    }
    val now = rememberNow()
    val shown = session.onBlocked(blocked)
    QrContent(
        phase = shown.phase(now, config),
        remainingSec = shown.remainingSec(now, config),
        ttlSec = config.loginQrTtlSec,
        canLeave = canLeave,
        onLeave = onLeave,
        onRenew = { session = QrSession(SystemClock.elapsedRealtime()) },
        onLongPress = {
            if (shown.canComplete(SystemClock.elapsedRealtime(), config, blocked)) AccountStore.add(config)?.let(onLinked)
        },
    )
}

/** S-03 로그인과 S-07 PIN 끄기: 지금 PIN을 맞히면 onAccepted. */
@Composable
private fun PinCheckStep(
    account: Account,
    config: BalanceConfig,
    backLabel: String,
    prompt: String,
    blocked: Boolean,
    onBack: () -> Unit,
    onAccepted: () -> Unit,
) {
    var input by rememberSaveable(stateSaver = PinInputSaver) { mutableStateOf(PinInput()) }
    PinContent(
        backLabel, onBack, account, prompt, input, config.pinLength,
        errorText = stringResource(R.string.account_pin_wrong),
        keypadBlocked = blocked,
        onDigit = { d ->
            val r = enterPin(input, d, account.pin.orEmpty(), config.pinLength)
            input = r.input
            if (r.accepted) onAccepted()
        },
        onDelete = { input = input.backspace() },
    )
}

@Composable
private fun PinSetStep(account: Account, config: BalanceConfig, blocked: Boolean, onBack: () -> Unit, onDone: (String) -> Unit) {
    var setup by rememberSaveable(stateSaver = PinSetupSaver) { mutableStateOf(PinSetup()) }
    PinContent(
        stringResource(R.string.account_profiles), onBack, account,
        prompt = if (setup.first == null) stringResource(R.string.account_pin_new, config.pinLength) else stringResource(R.string.account_pin_again),
        input = setup.input,
        length = config.pinLength,
        errorText = stringResource(R.string.account_pin_mismatch),
        keypadBlocked = blocked,
        onDigit = { d ->
            val next = setup.press(d, config.pinLength)
            val pin = next.result
            if (pin != null) onDone(pin) else setup = next
        },
        onDelete = { setup = setup.backspace() },
    )
}

private val PinInputSaver = listSaver<PinInput, Any>(
    save = { listOf(it.digits, it.error, it.failures) },
    restore = { PinInput(it[0] as String, it[1] as Boolean, it[2] as Int) },
)

private val PinSetupSaver = listSaver<PinSetup, Any>(
    save = { listOf(it.first.orEmpty(), it.input.digits, it.input.error, it.input.failures) },
    restore = { PinSetup((it[0] as String).ifEmpty { null }, PinInput(it[1] as String, it[2] as Boolean, it[3] as Int)) },
)

private val QrSessionSaver = listSaver<QrSession, Any>(
    save = { listOf(it.issuedAtMs, it.cancelled) },
    restore = { QrSession(it[0] as Long, it[1] as Boolean) },
)
