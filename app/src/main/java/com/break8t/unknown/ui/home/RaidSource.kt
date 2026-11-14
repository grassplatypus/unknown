package com.break8t.unknown.ui.home

import com.break8t.unknown.config.BalanceConfig
import com.break8t.unknown.navi.jamProgress
import com.break8t.unknown.reward.DriveSession
import com.break8t.unknown.reward.RewardCalculator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.max

data class Participant(val name: String, val friend: Boolean)

/**
 * 화면에 보일 정체 하나. 교차로 이름은 내비가 비워 보내는 일이 많다(27개 중 시작 15개, 끝 21개).
 * [meetCount]와 [usualMin]은 지난 기록이 있어야 아는 값이라 내비에서 온 정체는 null이다.
 */
data class JamView(
    val road: String,
    val startCross: String,
    val endCross: String,
    val remainingM: Double,
    val progress: Float,
    val meetCount: Int? = null,
    val usualMin: Int? = null,
)

sealed interface RaidState {
    data object None : RaidState
    data class Approaching(val jam: JamView, val aheadM: Double) : RaidState
    data class Solo(val jam: JamView) : RaidState
    data class Group(val jam: JamView, val participants: List<Participant>) : RaidState
    data class Passed(val tookSec: Long, val coins: Int, val participants: List<Participant>, val jam: JamView) : RaidState
}

/** 지금 지나는 정체 레이드. 내비 경로가 있으면 navi.NaviRaidSource, 없으면 FakeRaidSource다. */
interface RaidSource {
    val state: StateFlow<RaidState>

    /** 정체 구간마다 머문 초. RewardCalculator의 `jamSegmentsSec`에 넣는다. */
    val jamSegmentsSec: StateFlow<List<Long>>

    /** 종료 버튼. 레이드 화면을 닫고 주행 중 홈으로 돌아간다. */
    fun end()
}

/** 주행 중에만 None, Solo, Group, Passed를 HomeDemo의 시간대로 돌린다. 메인 스레드에서만 부른다. */
class FakeRaidSource(
    private val scope: CoroutineScope,
    private val config: BalanceConfig = BalanceConfig.DEFAULT,
) : RaidSource {
    private val current = MutableStateFlow<RaidState>(RaidState.None)
    override val state: StateFlow<RaidState> = current
    private val segments = MutableStateFlow<List<Long>>(emptyList())
    override val jamSegmentsSec: StateFlow<List<Long>> = segments
    private var cycle: Job? = null

    fun setDriving(driving: Boolean) {
        if (driving == (cycle != null)) return
        if (driving) restart() else stopCycle()
    }

    override fun end() {
        if (cycle != null) restart()
    }

    private fun stopCycle() {
        cycle?.cancel()
        cycle = null
        current.value = RaidState.None
    }

    private fun restart() {
        stopCycle()
        cycle = scope.launch {
            while (true) {
                delay(HomeDemo.RAID_NONE)
                val tick = (HomeDemo.RAID_SOLO + HomeDemo.RAID_GROUP) / HomeDemo.FAKE_REMAINING_M.size
                var longest = HomeDemo.JAM_LONGEST_M
                HomeDemo.FAKE_REMAINING_M.forEachIndexed { i, m ->
                    longest = max(longest, m)
                    val jam = HomeDemo.JAM.copy(remainingM = m, progress = jamProgress(m, longest))
                    current.value = if (tick * i < HomeDemo.RAID_SOLO) RaidState.Solo(jam) else RaidState.Group(jam, HomeDemo.PARTICIPANTS)
                    delay(tick)
                }
                current.value = passed()
                segments.update { it + HomeDemo.RAID_JAM_SEC }
                delay(HomeDemo.RAID_PASSED)
                current.value = RaidState.None
            }
        }
    }

    private fun passed(): RaidState.Passed {
        val sec = HomeDemo.RAID_JAM_SEC
        val coins = RewardCalculator.calculate(DriveSession(sec, listOf(sec), safetyScore = 0), config).raidCoin
        return RaidState.Passed(sec, coins, HomeDemo.PARTICIPANTS, HomeDemo.JAM.copy(remainingM = 0.0, progress = 1f))
    }
}
