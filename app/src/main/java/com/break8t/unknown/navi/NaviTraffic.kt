package com.break8t.unknown.navi

import ai.pleos.playground.navi.constants.NaviMode
import ai.pleos.playground.navi.constants.TrafficCongestionType
import ai.pleos.playground.navi.data.RouteStartInfo
import ai.pleos.playground.navi.data.RouteStateInfo
import ai.pleos.playground.navi.data.TrafficCongestionInfo
import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener
import android.content.Context
import android.os.SystemClock
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * NaviHelper 연결과 주기 조회. 모든 호출은 IO 스레드에서 하고, 응답은 [events]로 흘린다.
 * 응답은 다른 앱이 요청한 것도 온다(측정 7번). 그래서 요청과 응답을 짝짓지 않고 오는 대로 쓴다.
 */
class NaviTraffic(context: Context, private val rules: RaidRules = RaidRules()) {
    sealed interface Event {
        /** [atMs]는 받은 순간의 `SystemClock.elapsedRealtime()`이다. */
        data class State(val mode: NaviMode, val items: List<JamItem>, val atMs: Long) : Event
        data class RouteStarted(val atMs: Long) : Event
    }

    private val helper = NaviHelper(context.applicationContext)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val flow = MutableSharedFlow<Event>(extraBufferCapacity = 16, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val events: SharedFlow<Event> = flow
    private val ready = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = ready
    private var polling: Job? = null

    private val listener = object : NaviHelperEventListener {
        override fun onRouteStateInfo(routeStateInfo: RouteStateInfo) {
            val items = routeStateInfo.trafficStatus?.congestionItemList.orEmpty().map(::toItem)
            flow.tryEmit(Event.State(routeStateInfo.naviMode, items, SystemClock.elapsedRealtime()))
        }

        override fun onRouteStarted(routeStartInfo: RouteStartInfo) {
            flow.tryEmit(Event.RouteStarted(SystemClock.elapsedRealtime()))
        }
    }

    fun start() {
        scope.launch {
            helper.addListener(listener)
            helper.initialize({ ready.value = true }, { Log.w(TAG, "navi service unavailable", it) })
        }
    }

    /** 주행 중에만 조회한다. 응답이 NaviHelper를 쓰는 모든 앱에 가서 주차 중에는 아낀다. */
    @Synchronized
    fun setPolling(on: Boolean) {
        if (on == (polling != null)) return
        polling?.cancel()
        polling = if (!on) null else scope.launch {
            ready.first { it }
            while (isActive) {
                runCatching { helper.getRouteStateInfo() }.onFailure { Log.w(TAG, "getRouteStateInfo failed", it) }
                delay(rules.pollMs)
            }
        }
    }

    fun release() {
        setPolling(false)
        helper.removeListener(listener)
        helper.release()
        ready.value = false
    }

    private fun toItem(c: TrafficCongestionInfo) = JamItem(
        offsetM = c.offset,
        lengthM = c.distance,
        jammed = c.trafficCongestionType == TrafficCongestionType.SLOW || c.trafficCongestionType == TrafficCongestionType.HEAVY,
        avgKmh = c.avrSpeed,
        road = c.roadName,
        startCross = c.startCrossName,
        endCross = c.endCrossName,
    )

    private companion object { const val TAG = "NaviTraffic" }
}
