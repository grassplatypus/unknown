package com.break8t.unknown.navi

import ai.pleos.playground.navi.constants.NaviMode
import com.break8t.unknown.config.BalanceConfig
import com.break8t.unknown.ui.home.HomeDemo
import com.break8t.unknown.ui.home.RaidSource
import com.break8t.unknown.ui.home.RaidState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * 내비 경로 위의 정체로 레이드를 만든다. 판단은 [decideRaid], 시간과 진행률은 [JamTracker]가 한다.
 * 상태는 한 줄로 줄 세운 디스패처에서만 바꾼다. 응답 콜백과 종료 버튼이 서로 다른 스레드에서 온다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NaviRaidSource(
    private val traffic: NaviTraffic,
    private val scope: CoroutineScope,
    config: BalanceConfig = BalanceConfig.DEFAULT,
    private val rules: RaidRules = RaidRules(),
) : RaidSource {
    private val lane = Dispatchers.Default.limitedParallelism(1)
    private val tracker = JamTracker(config, rules)
    private val current = MutableStateFlow<RaidState>(RaidState.None)
    override val state: StateFlow<RaidState> = current
    private val segments = MutableStateFlow<List<Long>>(emptyList())
    override val jamSegmentsSec: StateFlow<List<Long>> = segments
    private val routeGuiding = MutableStateFlow(false)

    /** 마지막 응답이 ROUTE_GUIDING이었나. 홈은 이 값으로 견본과 이 소스 중에 고른다. */
    val guiding: StateFlow<Boolean> = routeGuiding

    private var staleItems: List<JamItem>? = null
    private var routeStartMs: Long? = null
    private var passedUntil = 0L
    private var dismissedRoad: String? = null

    init {
        scope.launch(lane) { traffic.events.collect(::handle) }
        traffic.start()
    }

    fun setDriving(driving: Boolean) {
        traffic.setPolling(driving)
        if (!driving) scope.launch(lane) { passedUntil = 0; current.value = RaidState.None }
    }

    /** 종료하면 지금 정체가 끝날 때까지 레이드와 결과를 숨긴다. 머문 시간은 계속 센다. */
    override fun end() {
        scope.launch(lane) {
            dismissedRoad = tracker.insideRoad
            passedUntil = 0
            current.value = RaidState.None
        }
    }

    private fun handle(e: NaviTraffic.Event) {
        when (e) {
            is NaviTraffic.Event.RouteStarted -> routeStartMs = e.atMs
            is NaviTraffic.Event.State -> onState(e)
        }
    }

    private fun onState(e: NaviTraffic.Event.State) {
        val guiding = e.mode == NaviMode.ROUTE_GUIDING
        routeGuiding.value = guiding
        if (!guiding) {
            staleItems = e.items
            routeStartMs = null
        }
        // 앱을 켰을 때 이미 경로 안내 중이면 시작 이벤트가 없다. 처음 본 순간을 시작으로 친다.
        val since = e.atMs - (routeStartMs ?: e.atMs.also { if (guiding) routeStartMs = it })
        val fix = decideRaid(e.mode, e.items, if (guiding) since else 0, staleItems, tracker.insideRoad, rules)
        if (guiding && since >= rules.earlyWindowMs && e.items != staleItems) staleItems = null

        val s = tracker.update(fix, e.atMs)
        segments.value = tracker.segmentsSec()
        val shown = when {
            (s is RaidState.Solo && s.jam.road == dismissedRoad) || (s is RaidState.Passed && s.jam.road == dismissedRoad) -> RaidState.None
            s is RaidState.Passed -> s.also { passedUntil = e.atMs + HomeDemo.RAID_PASSED.inWholeMilliseconds }
            s !is RaidState.Solo && e.atMs < passedUntil -> current.value
            else -> s
        }
        if (tracker.insideRoad == null) dismissedRoad = null
        current.value = shown
    }
}
