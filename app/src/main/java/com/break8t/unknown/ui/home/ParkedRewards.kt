package com.break8t.unknown.ui.home

import com.break8t.unknown.config.BalanceConfig
import com.break8t.unknown.data.DemoState
import com.break8t.unknown.data.Profile
import com.break8t.unknown.drive.DriveMode
import com.break8t.unknown.reward.RewardCalculator
import com.break8t.unknown.reward.RewardResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/** P로 바꾼 뒤 차례로 띄우는 알림. 순서가 곧 표시 순서다. */
enum class HomePopup { REWARD, LEVEL_UP, POSTCARD, JAM_CARD }

fun popupsFor(levelBefore: Int, levelAfter: Int): List<HomePopup> = buildList {
    add(HomePopup.REWARD)
    if (levelAfter > levelBefore) add(HomePopup.LEVEL_UP)
    add(HomePopup.POSTCARD)
    add(HomePopup.JAM_CARD)
}

/** 코인과 경험치를 더한다. 레벨마다 필요한 경험치 곡선이 아직 없어서 xpToNext는 그대로 둔다. */
fun applyReward(p: Profile, r: RewardResult): Profile {
    require(p.xpToNext > 0) { "xpToNext must be positive: ${p.xpToNext}" }
    var level = p.level
    var xp = p.xp + r.xp
    while (xp >= p.xpToNext) {
        xp -= p.xpToNext
        level++
    }
    return p.copy(coins = p.coins + r.totalCoin, level = level, xp = xp)
}

/**
 * 홈 상태. 화면을 나누면 Activity가 다시 만들어지고 기어는 UNKNOWN부터 다시 읽으므로,
 * 실제로 DRIVING을 본 뒤에 P가 됐을 때만 지급한다. UNKNOWN은 화면만 주행 중처럼 막는다.
 */
class HomeController(scope: CoroutineScope, config: BalanceConfig = BalanceConfig.DEFAULT) {
    val raid = FakeRaidSource(scope, config)
    val reward: RewardResult = RewardCalculator.calculate(HomeDemo.SESSION, config)
    private val queue = MutableStateFlow<List<HomePopup>>(emptyList())
    val popups: StateFlow<List<HomePopup>> = queue
    private var drove = false

    fun onMode(mode: DriveMode, demo: DemoState) {
        raid.setDriving(!mode.isParked)
        when (mode) {
            DriveMode.DRIVING -> { drove = true; queue.value = emptyList() }
            DriveMode.UNKNOWN -> queue.value = emptyList()
            DriveMode.PARKED -> if (drove) { drove = false; grant(demo) }
        }
    }

    fun dismiss() = queue.update { it.drop(1) }

    private fun grant(demo: DemoState) {
        val before = demo.profile.value.level
        demo.update { applyReward(it, reward) }
        queue.value = popupsFor(before, demo.profile.value.level)
    }
}

/** 프로세스에 하나만 둔다. Activity를 다시 만들거나 다른 메뉴로 가도 두 번 지급하지 않는다. */
object HomeStore {
    val controller by lazy { HomeController(MainScope()) }
}
