package com.playground.jeongche.reward

import com.playground.jeongche.config.BalanceConfig
import java.math.BigDecimal
import java.math.RoundingMode

/** 한 번의 주행. 정체 구간마다 머문 시간을 따로 둔다(정체 보상은 구간마다 내림한다). */
data class DriveSession(
    val totalDriveSec: Long,
    val jamSegmentsSec: List<Long>,
    val safetyScore: Int,
)

data class RewardResult(
    val driveCoin: Int,
    val raidCoin: Int,
    val safeCoin: Int,
    val xp: Int,
) {
    val totalCoin: Int get() = driveCoin + raidCoin + safeCoin
}

/** 계산 규칙은 Notion "개발 지침" 3장, 요약은 `.claude/rules/30-domain.md` "보상". */
object RewardCalculator {
    private val SECONDS_PER_MINUTE = BigDecimal.valueOf(60)

    fun calculate(session: DriveSession, config: BalanceConfig): RewardResult {
        // 잘못된 기록을 조용히 고쳐 지급하면 틀린 보상이 남는다. 기록하는 쪽의 버그로 보고 멈춘다.
        require(session.totalDriveSec >= 0) { "totalDriveSec < 0: ${session.totalDriveSec}" }
        require(session.jamSegmentsSec.all { it >= 0 }) { "negative jam segment: ${session.jamSegmentsSec}" }
        val jamSec = session.jamSegmentsSec.sum()
        require(jamSec <= session.totalDriveSec) { "jam $jamSec s > total ${session.totalDriveSec} s" }
        val nonJamSec = session.totalDriveSec - jamSec

        return RewardResult(
            driveCoin = floorMinutes(dec(nonJamSec) * dec(config.driveCoinPerMin)),
            raidCoin = session.jamSegmentsSec.sumOf { floorMinutes(dec(it) * dec(config.raidCoinPerMin)) },
            safeCoin = if (session.safetyScore >= config.safeScoreThreshold) config.safeBonusCoin else 0,
            xp = floorMinutes((dec(nonJamSec) + dec(jamSec) * dec(config.xpJamWeight)) * dec(config.xpPerMin)),
        )
    }

    // valueOf는 Double을 가장 짧은 십진 표기("1.2")로 옮긴다. 그래서 50분 × 1.2가 59.999...가 아니라 정확히 60이 된다.
    private fun dec(v: Double): BigDecimal = BigDecimal.valueOf(v)
    private fun dec(v: Long): BigDecimal = BigDecimal.valueOf(v)

    /** 초 단위 값을 분으로 나누고 마지막에 한 번만 내림한다. */
    private fun floorMinutes(secTimesRate: BigDecimal): Int =
        secTimesRate.divide(SECONDS_PER_MINUTE, 0, RoundingMode.FLOOR).intValueExact()
}
