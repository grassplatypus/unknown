package com.break8t.unknown.navi

import ai.pleos.playground.navi.constants.NaviMode
import com.break8t.unknown.config.BalanceConfig
import com.break8t.unknown.reward.DriveSession
import com.break8t.unknown.reward.RewardCalculator
import com.break8t.unknown.ui.home.JamView
import com.break8t.unknown.ui.home.RaidState
import kotlin.math.max

/** 내비 혼잡 구간 하나. SDK 타입을 감싸 두어 판단 함수와 테스트가 SDK 객체 없이 돈다. */
data class JamItem(
    val offsetM: Double,
    val lengthM: Double,
    val jammed: Boolean,
    val avgKmh: Double,
    val road: String,
    val startCross: String,
    val endCross: String,
)

/** 같은 도로에서 이어지는 조각을 합친 정체 하나. offsetM은 지금 위치에서 시작점까지다(SDK KDoc, 에뮬레이터 미검증). */
data class Jam(val offsetM: Double, val lengthM: Double, val road: String, val startCross: String, val endCross: String)

sealed interface RaidFix {
    data object None : RaidFix

    /** 경로 안내 중이지만 응답을 믿을 수 없다(시작 직후, 이전 경로 목록). 정체를 끝내지 않고 그대로 둔다. */
    data object Unknown : RaidFix
    data class Approaching(val jam: Jam, val aheadM: Double) : RaidFix
    data class Inside(val jam: Jam, val remainingM: Double) : RaidFix
}

/**
 * 판단 기준값. 근거는 `D:/playground/hyundai/docs/emulator-capabilities.md` 경로 혼잡 정보 절.
 * 게임 밸런스가 아니라 내비 데이터의 성질에서 나온 값이라 BalanceConfig와 따로 둔다.
 */
data class RaidRules(
    /** 응답의 offset은 100 m 단위보다 거칠다. 정체 속도(20~30 km/h)로 5초면 40 m라 더 자주 물어도 숫자가 안 바뀐다. */
    val pollMs: Long = 5_000,
    /** 경로 시작 26 ms 뒤 응답에 취소한 경로의 목록이 왔고 새 목록은 1.7~3.3초 뒤에 왔다(관측). */
    val earlyWindowMs: Long = 5_000,
    /** 목록의 절반 이상이 400 m 미만 조각이다. 짧은 조각마다 레이드를 열면 화면이 깜빡인다. */
    val minJamM: Double = 500.0,
    val mergeGapM: Double = 200.0,
    val approachM: Double = 2_000.0,
    /** 앱이 얼어 조회가 70초 멈춘 적이 있다(관측). 그보다 긴 공백은 정체에 있었다고 단정하지 않는다. */
    val maxCountedGapMs: Long = 120_000,
)

/** 같은 도로에서 [RaidRules.mergeGapM] 안으로 이어지는 SLOW, HEAVY 조각을 하나로 합친다. */
fun mergeJams(items: List<JamItem>, rules: RaidRules = RaidRules()): List<Jam> {
    val out = mutableListOf<Jam>()
    for (i in items.filter { it.jammed }.sortedBy { it.offsetM }) {
        val last = out.lastOrNull()
        val end = i.offsetM + i.lengthM
        if (last != null && last.road == i.road && i.offsetM <= last.offsetM + last.lengthM + rules.mergeGapM) {
            out[out.lastIndex] = last.copy(lengthM = max(last.offsetM + last.lengthM, end) - last.offsetM, endCross = i.endCross)
        } else {
            out += Jam(i.offsetM, i.lengthM, i.road, i.startCross, i.endCross)
        }
    }
    return out
}

/**
 * 응답 하나로 지금 정체 안인지, 다가가는 중인지 정한다. 상태는 [JamTracker]가 들고 있다.
 * [staleItems]는 경로가 없을 때 마지막으로 본 목록이다. 경로를 잡은 뒤에도 같은 목록이면 이전 경로의 것이다.
 * [insideRoad]는 이미 들어가 있는 정체의 도로다. 남은 길이가 [RaidRules.minJamM]보다 짧아져도 놓지 않는다.
 */
fun decideRaid(
    mode: NaviMode,
    items: List<JamItem>,
    sinceRouteStartMs: Long,
    staleItems: List<JamItem>? = null,
    insideRoad: String? = null,
    rules: RaidRules = RaidRules(),
): RaidFix {
    if (mode != NaviMode.ROUTE_GUIDING) return RaidFix.None
    if (sinceRouteStartMs < rules.earlyWindowMs) return RaidFix.Unknown
    if (staleItems != null && items == staleItems) return RaidFix.Unknown
    val jams = mergeJams(items, rules)
    val inside = jams.firstOrNull {
        it.offsetM <= 0 && it.offsetM + it.lengthM > 0 && (it.lengthM >= rules.minJamM || it.road == insideRoad)
    }
    if (inside != null) return RaidFix.Inside(inside, inside.offsetM + inside.lengthM)
    val next = jams.filter { it.offsetM > 0 && it.lengthM >= rules.minJamM }.minByOrNull { it.offsetM }
    return if (next != null && next.offsetM <= rules.approachM) RaidFix.Approaching(next, next.offsetM) else RaidFix.None
}

/** 남은 길이가 줄면 차고, 정체가 앞으로 늘어나면 다시 준다. [longestM]은 이 정체에서 본 가장 긴 남은 길이다. */
fun jamProgress(remainingM: Double, longestM: Double): Float =
    if (longestM <= 0) 0f else (1 - remainingM / longestM).toFloat().coerceIn(0f, 1f)

fun Jam.view(remainingM: Double, progress: Float) = JamView(road, startCross, endCross, remainingM, progress)

/**
 * 판단 결과를 차례로 받아 화면 상태와 정체 구간별 머문 시간을 만든다. 시각은 단조 시계(ms)를 넣는다.
 * 머문 시간은 정체 안이라고 본 두 조회 사이만 센다. 들어갈 때와 나올 때 조회 간격만큼 덜 셀 수 있다.
 */
class JamTracker(private val config: BalanceConfig = BalanceConfig.DEFAULT, private val rules: RaidRules = RaidRules()) {
    private val closedMs = mutableListOf<Long>()
    private var open: Jam? = null
    private var openMs = 0L
    private var lastMs = 0L
    private var longestM = 0.0
    private var lastView: JamView? = null

    val insideRoad: String? get() = open?.road

    /** 끝난 구간과 지금 머무는 구간의 초. RewardCalculator의 `jamSegmentsSec`에 그대로 넣는다. */
    fun segmentsSec(): List<Long> = (closedMs + listOfNotNull(openMs.takeIf { open != null && it >= rules.pollMs })).map { it / 1000 }

    fun update(fix: RaidFix, nowMs: Long): RaidState = when (fix) {
        is RaidFix.Inside -> {
            if (open?.road != fix.jam.road) {
                close()
                open = fix.jam
                openMs = 0
                longestM = fix.remainingM
            } else {
                val gap = nowMs - lastMs
                if (gap in 0..rules.maxCountedGapMs) openMs += gap
                longestM = max(longestM, fix.remainingM)
            }
            lastMs = nowMs
            RaidState.Solo(fix.jam.view(fix.remainingM, jamProgress(fix.remainingM, longestM)).also { lastView = it })
        }
        // 믿을 수 없는 응답은 지나간 것으로 치지 않는다. 다음 Inside가 공백까지 이어서 센다.
        RaidFix.Unknown -> lastView?.takeIf { open != null }?.let { RaidState.Solo(it) } ?: RaidState.None
        else -> {
            val passed = close()
            passed ?: if (fix is RaidFix.Approaching) RaidState.Approaching(fix.jam.view(fix.jam.lengthM, 0f), fix.aheadM) else RaidState.None
        }
    }

    /** 조회 한 번 사이보다 짧게 머문 정체는 구간으로 남기지 않는다. 0초 결과 화면과 0코인 구간이 생긴다. */
    private fun close(): RaidState.Passed? {
        if (open == null) return null
        val ms = openMs
        open = null
        if (ms < rules.pollMs) return null
        closedMs += ms
        val sec = ms / 1000
        val coins = RewardCalculator.calculate(DriveSession(sec, listOf(sec), safetyScore = 0), config).raidCoin
        return RaidState.Passed(sec, coins, emptyList(), lastView!!.copy(remainingM = 0.0, progress = 1f))
    }
}
