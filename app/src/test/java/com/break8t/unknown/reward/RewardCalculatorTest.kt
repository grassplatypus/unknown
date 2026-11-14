package com.break8t.unknown.reward

import com.break8t.unknown.config.BalanceConfig
import org.junit.Assert.assertEquals
import org.junit.Test

class RewardCalculatorTest {
    // 기본값이 바뀌어도 테스트가 깨지지 않도록 값을 직접 고정한다.
    private val config = BalanceConfig(
        driveCoinPerMin = 0.5,
        raidCoinPerMin = 1.2,
        safeScoreThreshold = 80,
        safeBonusCoin = 10,
        xpPerMin = 1.0,
        xpJamWeight = 1.5,
    )

    private fun calc(total: Long, jams: List<Long> = emptyList(), safety: Int = 0, cfg: BalanceConfig = config) =
        RewardCalculator.calculate(DriveSession(total, jams, safety), cfg)

    @Test
    fun figmaD02Sample() {
        val r = calc(total = 4200, jams = listOf(520, 320), safety = 92)
        assertEquals(RewardResult(driveCoin = 28, raidCoin = 16, safeCoin = 10, xp = 77), r)
        assertEquals(54, r.totalCoin)
    }

    @Test
    fun raidFloorsEachSegmentBeforeAdding() {
        assertEquals(10, calc(total = 520, jams = listOf(520)).raidCoin)
        assertEquals(6, calc(total = 320, jams = listOf(320)).raidCoin)
        // 30초는 0.6코인이라 구간마다 0이 된다. 합친 뒤 내리면 1이 나온다.
        assertEquals(0, calc(total = 60, jams = listOf(30, 30)).raidCoin)
    }

    @Test
    fun safeBonusThresholdIsInclusive() {
        assertEquals(0, calc(total = 0, safety = 79).safeCoin)
        assertEquals(10, calc(total = 0, safety = 80).safeCoin)
    }

    @Test
    fun zeroJams() {
        assertEquals(RewardResult(driveCoin = 35, raidCoin = 0, safeCoin = 0, xp = 70), calc(total = 4200))
    }

    @Test
    fun zeroDrive() {
        assertEquals(RewardResult(0, 0, 0, 0), calc(total = 0))
    }

    @Test
    fun jamEqualToTotalIsValid() {
        val r = calc(total = 3000, jams = listOf(3000))
        assertEquals(0, r.driveCoin)
        assertEquals(60, r.raidCoin)
        assertEquals(75, r.xp)
    }

    @Test
    fun wholeNumberProductsDoNotLoseAUnit() {
        assertEquals(60, calc(total = 3000, jams = listOf(3000)).raidCoin)
        assertEquals(1, calc(total = 50, jams = listOf(50)).raidCoin)
        assertEquals(1, calc(total = 120).driveCoin)
        assertEquals(1, calc(total = 40, jams = listOf(40)).xp)
        val tenth = config.copy(driveCoinPerMin = 0.1, raidCoinPerMin = 0.29, xpPerMin = 0.7)
        assertEquals(3, calc(total = 1800, cfg = tenth).driveCoin)
        assertEquals(29, calc(total = 6000, jams = listOf(6000), cfg = tenth).raidCoin)
        assertEquals(7, calc(total = 600, cfg = tenth).xp)
    }

    @Test
    fun valuesComeFromConfig() {
        val r = calc(
            total = 600, jams = listOf(120), safety = 50,
            cfg = config.copy(driveCoinPerMin = 2.0, raidCoinPerMin = 3.0, safeScoreThreshold = 50, safeBonusCoin = 7),
        )
        assertEquals(RewardResult(driveCoin = 16, raidCoin = 6, safeCoin = 7, xp = 11), r)
    }

    @Test(expected = IllegalArgumentException::class)
    fun negativeTotalIsRejected() {
        calc(total = -1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun negativeSegmentIsRejected() {
        calc(total = 600, jams = listOf(300, -60))
    }

    @Test(expected = IllegalArgumentException::class)
    fun jamLongerThanTotalIsRejected() {
        calc(total = 600, jams = listOf(400, 201))
    }
}
