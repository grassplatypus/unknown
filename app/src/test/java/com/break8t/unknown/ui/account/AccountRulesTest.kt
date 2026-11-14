package com.break8t.unknown.ui.account

import com.break8t.unknown.config.BalanceConfig
import com.break8t.unknown.drive.DriveMode
import com.break8t.unknown.drive.UxLimits
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountRulesTest {
    private val config = BalanceConfig(pinLength = 4, pinFailAlertCount = 3, autoLoginSec = 10, loginQrTtlSec = 300, maxAccounts = 5)

    private fun type(input: PinInput, pin: String, expected: String): PinResult {
        var r = PinResult(input, false)
        pin.forEach { r = enterPin(r.input, it, expected, config.pinLength) }
        return r
    }

    // PIN

    @Test fun correctPinIsAccepted() {
        val r = type(PinInput(), "1234", "1234")
        assertTrue(r.accepted)
        assertEquals(0, r.input.failures)
    }

    @Test fun wrongPinClearsBoxesShowsErrorAndCounts() {
        val r = type(PinInput(), "1111", "1234")
        assertFalse(r.accepted)
        assertTrue(r.input.error)
        assertEquals("", r.input.digits)
        assertEquals(1, r.input.failures)
    }

    @Test fun partialPinIsNotCheckedYet() {
        val r = type(PinInput(), "123", "1234")
        assertFalse(r.accepted)
        assertFalse(r.input.error)
        assertEquals(0, r.input.failures)
    }

    @Test fun pastAlertCountItKeepsShowingErrorAndNeverLocks() {
        var input = PinInput()
        repeat(config.pinFailAlertCount + 2) { input = type(input, "0000", "1234").input }
        assertEquals(config.pinFailAlertCount + 2, input.failures)
        assertTrue(input.error)
        assertTrue(type(input, "1234", "1234").accepted)
    }

    @Test fun typingAfterErrorClearsErrorButKeepsCount() {
        val wrong = type(PinInput(), "0000", "1234").input
        val next = wrong.press('5', config.pinLength)
        assertFalse(next.error)
        assertEquals("5", next.digits)
        assertEquals(1, next.failures)
    }

    @Test fun cannotTypeMoreThanPinLength() {
        assertEquals("1234", PinInput("1234").press('5', config.pinLength).digits)
    }

    @Test fun pinSetupNeedsTheSamePinTwice() {
        var s = PinSetup()
        "4321".forEach { s = s.press(it, config.pinLength) }
        assertEquals("4321", s.first)
        assertNull(s.result)
        "4321".forEach { s = s.press(it, config.pinLength) }
        assertEquals("4321", s.result)
    }

    @Test fun pinSetupMismatchStartsOverWithError() {
        var s = PinSetup()
        "4321".forEach { s = s.press(it, config.pinLength) }
        "9999".forEach { s = s.press(it, config.pinLength) }
        assertNull(s.first)
        assertNull(s.result)
        assertTrue(s.input.error)
    }

    // SC-00 자동 로그인

    private val accounts = DemoAccounts.take(DemoSeedCount)

    @Test fun autoLoginTargetsLastUsedAccountOnly() {
        assertEquals(accounts[0].id, AutoLogin.offer(accounts, accounts[0].id, 0)?.targetId)
        assertNull(AutoLogin.offer(accounts, null, 0))
        assertNull(AutoLogin.offer(accounts, "deleted", 0))
    }

    @Test fun autoLoginCountsDownAndFiresWithoutInput() {
        val a = AutoLogin("x", startedAtMs = 1_000)
        assertEquals(10, a.remainingSec(1_000, config))
        assertEquals(1, a.remainingSec(10_500, config))
        assertFalse(a.due(10_999, config))
        assertTrue(a.due(11_000, config))
    }

    @Test fun touchCancelsAutoLoginForGood() {
        val a = AutoLogin("x", startedAtMs = 0).touched()
        assertFalse(a.due(10_000, config))
        assertFalse(a.due(1_000_000, config))
    }

    // S-04 QR

    private val parked = UxLimits.NONE

    @Test fun qrExpiresAfterTtl() {
        val s = QrSession(issuedAtMs = 0)
        assertEquals(QrPhase.WAITING, s.phase(299_999, config))
        assertEquals(1, s.remainingSec(299_999, config))
        assertEquals(QrPhase.EXPIRED, s.phase(300_000, config))
        assertEquals(0, s.remainingSec(400_000, config))
        assertFalse(s.canComplete(300_000, config, blocked = false))
        assertTrue(QrSession(issuedAtMs = 300_000).canComplete(300_000, config, blocked = false))
    }

    @Test fun qrIsBlockedUnlessParked() {
        assertFalse(qrBlocked(DriveMode.PARKED, parked))
        assertTrue(qrBlocked(DriveMode.DRIVING, parked))
        assertTrue(qrBlocked(DriveMode.UNKNOWN, parked))
        assertTrue(qrBlocked(DriveMode.PARKED, UxLimits(true, UxLimits.NO_SETUP)))
        assertTrue(qrBlocked(DriveMode.PARKED, UxLimits.UNKNOWN))
    }

    @Test fun leavingParkCancelsQrAndParkingAgainDoesNotResumeIt() {
        var s = QrSession(issuedAtMs = 0)
        for (mode in listOf(DriveMode.PARKED, DriveMode.DRIVING, DriveMode.PARKED)) s = s.onBlocked(qrBlocked(mode, parked))
        assertEquals(QrPhase.CANCELLED, s.phase(1_000, config))
        assertFalse(s.canComplete(1_000, config, blocked = false))
    }

    @Test fun unknownGearCancelsQr() {
        val s = QrSession(issuedAtMs = 0).onBlocked(qrBlocked(DriveMode.UNKNOWN, parked))
        assertEquals(QrPhase.CANCELLED, s.phase(0, config))
    }

    @Test fun cannotCompleteQrWhileBlocked() {
        assertFalse(QrSession(issuedAtMs = 0).canComplete(0, config, blocked = true))
    }

    // 계정 목록

    @Test fun addedAccountsAreUniqueWithoutPinAndStopAtMax() {
        var list = accounts
        while (true) list = list + (nextDemoAccount(list, config) ?: break)
        assertEquals(config.maxAccounts, list.size)
        assertEquals(list.size, list.map { it.nickname }.toSet().size)
        assertNull(list.drop(DemoSeedCount).firstOrNull { it.pin != null })
    }

    @Test fun reAddAfterDeleteHasNoPinAndStaysUnique() {
        val withoutCat = accounts - accounts[1]
        val added = nextDemoAccount(withoutCat, config)
        assertNotNull(added)
        assertNull(added!!.pin)
        assertTrue(withoutCat.none { it.id == added.id })
    }

    @Test fun demoNicknamesFitNicknameMax() {
        DemoAccounts.forEach { assertTrue(it.nickname, it.nickname.length <= config.nicknameMax) }
    }

    @Test fun currentAccountCannotBeDeleted() {
        assertFalse(canDelete(accounts[0], accounts[0].id))
        assertTrue(canDelete(accounts[1], accounts[0].id))
    }

    @Test fun splashProgressStepsUpOverHold() {
        val c = BalanceConfig(splashHoldSec = 1)
        assertEquals(0.25f, splashProgress(0, c))
        assertEquals(0.5f, splashProgress(250, c))
        assertEquals(1f, splashProgress(999, c))
        assertEquals(1f, splashProgress(5_000, c))
    }
}
