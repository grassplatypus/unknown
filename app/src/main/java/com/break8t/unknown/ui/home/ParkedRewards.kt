package com.break8t.unknown.ui.home

import android.content.Context
import com.break8t.unknown.config.BalanceConfig
import com.break8t.unknown.data.DemoState
import com.break8t.unknown.data.Profile
import com.break8t.unknown.drive.DriveMode
import com.break8t.unknown.navi.NaviRaidSource
import com.break8t.unknown.navi.NaviTraffic
import com.break8t.unknown.reward.RewardCalculator
import com.break8t.unknown.reward.RewardResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
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
@OptIn(ExperimentalCoroutinesApi::class)
class HomeController(private val scope: CoroutineScope, private val config: BalanceConfig = BalanceConfig.DEFAULT) {
    private val fake = FakeRaidSource(scope, config)
    private val navi = MutableStateFlow<NaviRaidSource?>(null)
    private val useNavi = navi.flatMapLatest { it?.guiding ?: flowOf(false) }.stateIn(scope, SharingStarted.Eagerly, false)
    private val active get() = if (useNavi.value) navi.value ?: fake else fake
    private var driving = false

    /** 내비 경로를 안내 중이면 내비의 정체를, 아니면 견본을 보인다. 경로 없이도 시연이 돌아야 한다. */
    val raid: RaidSource = object : RaidSource {
        override val state = pick { it.state }
        override val jamSegmentsSec = pick { it.jamSegmentsSec }
        override fun end() = active.end()
    }

    val reward: RewardResult = RewardCalculator.calculate(HomeDemo.SESSION, config)
    private val queue = MutableStateFlow<List<HomePopup>>(emptyList())
    val popups: StateFlow<List<HomePopup>> = queue
    private var drove = false

    /** 한 번만 연결한다. 연결과 조회는 NaviTraffic이 메인 스레드 밖에서 한다. */
    fun attachNavi(context: Context) {
        if (navi.value != null) return
        navi.value = NaviRaidSource(NaviTraffic(context.applicationContext), scope, config).also { it.setDriving(driving) }
    }

    fun onMode(mode: DriveMode, demo: DemoState) {
        driving = !mode.isParked
        fake.setDriving(driving)
        navi.value?.setDriving(driving)
        when (mode) {
            DriveMode.DRIVING -> { drove = true; queue.value = emptyList() }
            DriveMode.UNKNOWN -> queue.value = emptyList()
            DriveMode.PARKED -> if (drove) { drove = false; grant(demo) }
        }
    }

    private fun <T> pick(of: (RaidSource) -> StateFlow<T>): StateFlow<T> =
        combine(useNavi, navi.flatMapLatest { n -> n?.let(of) ?: of(fake) }, of(fake)) { use, n, f -> if (use) n else f }
            .stateIn(scope, SharingStarted.Eagerly, of(fake).value)

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
