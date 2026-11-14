package com.break8t.unknown.ui.myinfo

import com.break8t.unknown.config.BalanceConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FriendRulesTest {
    // 기본값이 바뀌어도 테스트가 깨지지 않도록 값을 직접 고정한다.
    private val config = BalanceConfig(maxFriends = 3, friendCodeTtlSec = 300)
    private val two = demoFriends.take(2)

    @Test
    fun demoCodeAddsFriend() {
        assertEquals(AddCheck.OK, checkFriendCode(FriendCode.DEMO, two, config))
    }

    @Test
    fun otherCodeIsWrong() {
        assertEquals(AddCheck.WRONG, checkFriendCode("654321", two, config))
    }

    @Test
    fun shortCodeIsIncomplete() {
        assertEquals(AddCheck.INCOMPLETE, checkFriendCode("12345", two, config))
        assertEquals(AddCheck.INCOMPLETE, checkFriendCode("", two, config))
    }

    @Test
    fun demoFriendCannotBeAddedTwice() {
        assertEquals(AddCheck.WRONG, checkFriendCode(FriendCode.DEMO, two.take(1) + demoNewFriend(), config))
    }

    @Test
    fun limitWinsOverAnyCode() {
        val full = demoFriends.take(3)
        assertEquals(AddCheck.FULL, checkFriendCode(FriendCode.DEMO, full, config))
        assertEquals(AddCheck.FULL, checkFriendCode("", full, config))
        assertEquals(AddCheck.FULL, checkFriendCode(FriendCode.DEMO, demoFriends, config))
    }

    @Test
    fun myCodeIsStableWithinTtlAndChangesAfter() {
        val start = 1_000L * 300
        val a = FriendCode.mine(start, config)
        assertEquals(a, FriendCode.mine(start + 299, config))
        assertEquals(FriendCode.LENGTH, a.length)
        assertTrue(a.all { it.isDigit() })
        // 구간이 바뀌면 새 번호다. 우연히 같을 수 있어서 여러 구간을 본다.
        val next = (1..5).map { FriendCode.mine(start + it * 300L, config) }
        assertTrue(next.any { it != a })
    }

    @Test
    fun myCodeNeverEqualsDemoCode() {
        (0L until 2_000L).forEach { assertNotEquals(FriendCode.DEMO, FriendCode.mine(it * 300, config)) }
    }

    @Test
    fun countdownRunsFromTtlToOne() {
        assertEquals(300, FriendCode.secondsLeft(600, config))
        assertEquals(1, FriendCode.secondsLeft(899, config))
        assertEquals(48, FriendCode.secondsLeft(852, config))
    }

    @Test
    fun codeIsShownInTwoGroups() {
        assertEquals("482 913", FriendCode.spaced("482913"))
    }
}
