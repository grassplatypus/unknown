package com.playground.jeongche.reward

import com.playground.jeongche.config.BalanceConfig

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

/** 계산 규칙은 Notion "개발 지침" 3장. reward 담당이 구현한다. */
object RewardCalculator {
    fun calculate(session: DriveSession, config: BalanceConfig): RewardResult = TODO("reward 담당")
}
