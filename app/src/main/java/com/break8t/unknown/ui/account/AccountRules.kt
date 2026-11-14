package com.break8t.unknown.ui.account

import com.break8t.unknown.config.BalanceConfig
import com.break8t.unknown.drive.DriveMode
import com.break8t.unknown.drive.UxLimits

/** 이 차에 저장된 계정. 백엔드가 없으므로 PIN도 앱 메모리에만 있다(시연용). */
data class Account(val id: String, val nickname: String, val characterId: String, val pin: String? = null)

/** 시연 계정. 앞의 넷이 처음부터 저장돼 있고, 나머지는 QR 로그인으로 추가된다. 이름은 실존 인물과 겹치지 않는 별명이다. */
internal val DemoAccounts = listOf(
    Account("acc_farmer", "구름농부", "ch_sprout"),
    Account("acc_cat", "새벽고양이", "ch_cat", pin = "1234"),
    Account("acc_mush", "느긋한버섯", "ch_mush"),
    Account("acc_firefly", "반짝반딧불", "ch_firefly", pin = "1234"),
    Account("acc_cloud", "뭉게구름", "ch_cloud"),
    Account("acc_sun", "아침햇살", "ch_sprout"),
    Account("acc_sunset", "노을드라이버", "ch_cat"),
    Account("acc_trip", "주말나들이", "ch_mush"),
)
internal const val DemoSeedCount = 4

fun canAddAccount(accounts: List<Account>, config: BalanceConfig): Boolean = accounts.size < config.maxAccounts

/** QR 로그인으로 새로 들어온 계정. 지금 목록에 없는 별명을 고르고, PIN은 없이 시작한다. */
fun nextDemoAccount(accounts: List<Account>, config: BalanceConfig): Account? =
    if (!canAddAccount(accounts, config)) null
    else DemoAccounts.firstOrNull { d -> accounts.none { it.id == d.id } }?.copy(pin = null)

/** 지금 쓰는 계정은 삭제하지 않는다(Figma S-05). */
fun canDelete(account: Account, currentId: String?): Boolean = account.id != currentId

/** 눌러 둔 PIN 숫자와 틀린 횟수. 틀린 횟수가 쌓여도 잠그지 않는다(시연에는 알릴 휴대폰이 없다). */
data class PinInput(val digits: String = "", val error: Boolean = false, val failures: Int = 0) {
    fun press(digit: Char, length: Int): PinInput =
        if (digits.length >= length) this else copy(digits = digits + digit, error = false)

    fun backspace(): PinInput = copy(digits = digits.dropLast(1), error = false)

    fun isFull(length: Int): Boolean = digits.length >= length

    /** 틀렸을 때: 칸을 비우고 오류를 보인다. */
    fun rejected(): PinInput = PinInput(error = true, failures = failures + 1)
}

data class PinResult(val input: PinInput, val accepted: Boolean)

/** 숫자 하나를 넣고, 칸이 다 차면 expected와 견준다. */
fun enterPin(input: PinInput, digit: Char, expected: String, length: Int): PinResult {
    val next = input.press(digit, length)
    return when {
        !next.isFull(length) -> PinResult(next, accepted = false)
        next.digits == expected -> PinResult(next, accepted = true)
        else -> PinResult(next.rejected(), accepted = false)
    }
}

/** S-06: 새 PIN을 두 번 받는다. 두 번이 다르면 처음부터 다시 받는다. */
data class PinSetup(val first: String? = null, val input: PinInput = PinInput(), val result: String? = null) {
    fun press(digit: Char, length: Int): PinSetup {
        val next = input.press(digit, length)
        return when {
            !next.isFull(length) -> copy(input = next)
            first == null -> PinSetup(first = next.digits)
            first == next.digits -> copy(input = next, result = first)
            else -> PinSetup(input = input.rejected())
        }
    }

    fun backspace(): PinSetup = copy(input = input.backspace())
}

/**
 * SC-00: 마지막으로 쓴 계정으로 저절로 들어가기까지의 카운트다운. 아무 곳이나 누르면 멈춘다.
 * 시작 시각을 들고 있어서 화면을 나눠 Activity가 다시 만들어져도 이어진다.
 */
data class AutoLogin(val targetId: String, val startedAtMs: Long, val cancelled: Boolean = false) {
    fun remainingSec(nowMs: Long, config: BalanceConfig): Int {
        // 시작 시각이 화면이 읽은 지금 시각보다 늦게 찍힐 수 있어서 흐른 시간을 0 밑으로 내리지 않는다.
        val leftMs = config.autoLoginSec * 1000L - (nowMs - startedAtMs).coerceAtLeast(0)
        return ((leftMs + 999) / 1000).coerceAtLeast(0).toInt()
    }

    fun due(nowMs: Long, config: BalanceConfig): Boolean = !cancelled && remainingSec(nowMs, config) == 0

    fun touched(): AutoLogin = copy(cancelled = true)

    companion object {
        /** 마지막으로 쓴 계정이 아직 목록에 있을 때만 카운트다운한다. */
        fun offer(accounts: List<Account>, lastUsedId: String?, nowMs: Long): AutoLogin? =
            accounts.firstOrNull { it.id == lastUsedId }?.let { AutoLogin(it.id, nowMs) }
    }
}

internal const val SplashSteps = 4

/** S-01 입장 준비 막대. splashHoldSec 동안 네 단계로만 오른다. 앱을 켤 때 주행 중일 수 있어서 매끄럽게 움직이지 않는다. */
fun splashProgress(elapsedMs: Long, config: BalanceConfig): Float {
    val holdMs = (config.splashHoldSec * 1000L).coerceAtLeast(1)
    val step = (elapsedMs * SplashSteps / holdMs + 1).coerceIn(1, SplashSteps.toLong())
    return step / SplashSteps.toFloat()
}

enum class QrPhase { WAITING, EXPIRED, CANCELLED }

/** P가 아니거나 모르면, 또는 플랫폼이 설정 화면을 막으면 QR 로그인을 할 수 없다. */
fun qrBlocked(mode: DriveMode, limits: UxLimits): Boolean = !mode.isParked || limits.noSetup

/** S-04. 한 번 막히면 다시 P가 돼도 취소 상태로 남는다. 새 QR은 새 세션으로만 받는다. */
data class QrSession(val issuedAtMs: Long, val cancelled: Boolean = false) {
    fun onBlocked(blocked: Boolean): QrSession = if (blocked) copy(cancelled = true) else this

    fun phase(nowMs: Long, config: BalanceConfig): QrPhase = when {
        cancelled -> QrPhase.CANCELLED
        nowMs - issuedAtMs >= config.loginQrTtlSec * 1000L -> QrPhase.EXPIRED
        else -> QrPhase.WAITING
    }

    fun remainingSec(nowMs: Long, config: BalanceConfig): Int =
        ((config.loginQrTtlSec * 1000L - (nowMs - issuedAtMs).coerceAtLeast(0) + 999) / 1000).coerceAtLeast(0).toInt()

    /** 휴대폰 로그인을 흉내 내는 길게 누르기는 기다리는 중이고 막히지 않았을 때만 받는다. */
    fun canComplete(nowMs: Long, config: BalanceConfig, blocked: Boolean): Boolean =
        !blocked && phase(nowMs, config) == QrPhase.WAITING
}
