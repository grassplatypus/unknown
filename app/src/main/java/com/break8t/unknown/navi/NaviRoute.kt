package com.break8t.unknown.navi

import ai.pleos.playground.navi.constants.WaypointIndex
import ai.pleos.playground.navi.data.DestinationInfo
import ai.pleos.playground.navi.data.RequestWaypointInfo
import ai.pleos.playground.navi.data.RouteInfo
import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener
import android.content.Context
import android.util.Log
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/** 안내를 건 방식. 취소할 때 경유지를 뺄지 경로를 끝낼지가 이것으로 갈린다. */
enum class RouteKind { WAYPOINT, DESTINATION }

/**
 * 안내 중인 목적지가 있으면(남은 거리 > 0) 경유지로, 없거나 모르면 목적지로 안내한다.
 * 주차 중에는 정체 조회를 하지 않아 경로 상태를 모르므로, 그때마다 내비에 목적지를 묻는다(emulator-capabilities.md 1번).
 */
internal fun routeKind(remainDistanceM: Int?): RouteKind = if ((remainDistanceM ?: 0) > 0) RouteKind.WAYPOINT else RouteKind.DESTINATION

/** 필드 드랍 길 안내를 실제 내비로 보낸다. 모든 NaviHelper 호출은 IO 스레드에서 한다. */
class NaviRoute(context: Context) {
    private val helper = NaviHelper(context.applicationContext)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val ready = CompletableDeferred<Boolean>()
    @Volatile private var asked: CompletableDeferred<DestinationInfo>? = null

    // 응답은 NaviHelper를 쓰는 모든 앱에 간다(측정 7번). 다른 앱이 물은 목적지여도 같은 내비의 값이라 그대로 쓴다.
    private val listener = object : NaviHelperEventListener {
        override fun onDestinationInfo(destinationInfo: DestinationInfo) {
            asked?.complete(destinationInfo)
        }
    }

    init {
        scope.launch {
            helper.addListener(listener)
            helper.initialize({ ready.complete(true) }, {
                Log.w(TAG, "navi service unavailable", it)
                ready.complete(false)
            })
        }
    }

    /** 안내를 건다. 내비에 연결하지 못하면 null. */
    suspend fun guide(lat: Double, lng: Double, name: String, id: String): RouteKind? = withContext(Dispatchers.IO) {
        if (withTimeoutOrNull(CONNECT_MS) { ready.await() } != true) return@withContext null
        val answer = CompletableDeferred<DestinationInfo>().also { asked = it }
        runCatching { helper.getDestinationInfo() }
        val kind = routeKind(withTimeoutOrNull(ASK_MS) { answer.await() }?.remainDistance)
        runCatching {
            when (kind) {
                RouteKind.WAYPOINT -> helper.addWaypoint(RequestWaypointInfo(lat, lng, name, WaypointIndex.FIRST, id, name, "0"))
                RouteKind.DESTINATION -> helper.requestRoute(RouteInfo(lat, lng, poiName = name, poiId = id, address = name, poiSubId = "0"))
            }
        }.onFailure { Log.w(TAG, "guide failed", it); return@withContext null }
        kind
    }

    /** [guide]로 건 안내를 거둔다. 경유지는 첫 경유지를 빼고, 목적지면 경로를 끝낸다. */
    fun cancel(kind: RouteKind) {
        scope.launch {
            runCatching {
                when (kind) {
                    RouteKind.WAYPOINT -> helper.removeWaypoint(WaypointIndex.FIRST)
                    RouteKind.DESTINATION -> helper.cancelRoute()
                }
            }.onFailure { Log.w(TAG, "cancel failed", it) }
        }
    }

    private companion object {
        const val TAG = "NaviRoute"
        const val CONNECT_MS = 3_000L
        const val ASK_MS = 3_000L
    }
}
