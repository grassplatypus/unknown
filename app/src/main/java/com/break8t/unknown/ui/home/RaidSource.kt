package com.break8t.unknown.ui.home

import com.break8t.unknown.config.BalanceConfig
import com.break8t.unknown.reward.DriveSession
import com.break8t.unknown.reward.RewardCalculator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class Participant(val name: String, val friend: Boolean)

sealed interface RaidState {
    data object None : RaidState
    data object Solo : RaidState
    data class Group(val participants: List<Participant>) : RaidState
    data class Passed(val tookSec: Long, val coins: Int, val participants: List<Participant>) : RaidState
}

/** 지금 지나는 정체 레이드. 서버가 없어서 지금은 FakeRaidSource만 있다. */
interface RaidSource {
    val state: StateFlow<RaidState>

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
                current.value = RaidState.Solo
                delay(HomeDemo.RAID_SOLO)
                current.value = RaidState.Group(HomeDemo.PARTICIPANTS)
                delay(HomeDemo.RAID_GROUP)
                current.value = passed()
                delay(HomeDemo.RAID_PASSED)
                current.value = RaidState.None
            }
        }
    }

    private fun passed(): RaidState.Passed {
        val sec = HomeDemo.RAID_JAM_SEC
        val coins = RewardCalculator.calculate(DriveSession(sec, listOf(sec), safetyScore = 0), config).raidCoin
        return RaidState.Passed(sec, coins, HomeDemo.PARTICIPANTS)
    }
}
