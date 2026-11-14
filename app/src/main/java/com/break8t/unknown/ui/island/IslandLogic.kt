package com.break8t.unknown.ui.island

import com.break8t.unknown.config.BalanceConfig
import com.break8t.unknown.config.CropSpec
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.math.ceil

private const val MINUTE_MS = 60_000L
private const val HOUR_MS = 60 * MINUTE_MS

/** 실제 시간을 따르면 19:00부터 06:00 전까지가 밤이다. 따르지 않으면 설정에서 고른 값을 쓴다. */
fun isNight(realTime: Boolean, manualNight: Boolean, hour: Int): Boolean =
    if (realTime) hour >= 19 || hour < 6 else manualNight

/** 텃밭 한 칸. seedId가 null이면 빈 칸이다. 시간은 epoch 밀리초. */
data class Plot(val seedId: String? = null, val plantedAt: Long = 0L, val readyAt: Long = 0L) {
    val isEmpty: Boolean get() = seedId == null
    fun isGrown(now: Long): Boolean = seedId != null && now >= readyAt
    fun isGrowing(now: Long): Boolean = seedId != null && now < readyAt

    /** 0에서 1 사이. 심은 시각과 다 자라는 시각 사이에서 지난 비율이다. */
    fun progress(now: Long): Float =
        if (readyAt <= plantedAt) 1f else ((now - plantedAt).toFloat() / (readyAt - plantedAt)).coerceIn(0f, 1f)
}

/** 빈 칸에만 심는다. 씨앗을 빼는 일은 부르는 쪽이 DemoState.addItem으로 한다. */
fun plant(plots: List<Plot>, index: Int, crop: CropSpec, now: Long): List<Plot> {
    if (plots.getOrNull(index)?.isEmpty != true) return plots
    val plot = Plot(crop.seedId, now, now + crop.growMinutes * MINUTE_MS)
    return plots.toMutableList().also { it[index] = plot }
}

/** 자라는 중인 작물을 뽑는다. 코인도 씨앗도 돌려주지 않는다. */
fun pull(plots: List<Plot>, index: Int): List<Plot> =
    if (index in plots.indices) plots.toMutableList().also { it[index] = Plot() } else plots

/** counts는 씨앗 id별 수확한 칸 수이며 밭 순서를 따른다. */
data class Harvest(val counts: Map<String, Int>, val coins: Int)

/** 다 자란 칸을 모두 거둔다(Figma T-05: "밀 2칸을 수확했어요"). */
fun harvestAll(plots: List<Plot>, now: Long, crops: List<CropSpec>): Pair<List<Plot>, Harvest> {
    val counts = LinkedHashMap<String, Int>()
    var coins = 0
    val next = plots.map { p ->
        if (!p.isGrown(now)) return@map p
        val id = p.seedId!!
        counts[id] = (counts[id] ?: 0) + 1
        coins += crops.firstOrNull { it.seedId == id }?.harvestCoins ?: 0
        Plot()
    }
    return next to Harvest(counts, coins)
}

data class Duration(val days: Int, val hours: Int, val minutes: Int)

/** 1시간 미만은 분, 그 이상은 시간 단위로 올림한다. 그래야 "2시간 남음"이 한 시간 동안 그대로 보인다. */
fun remainingTime(ms: Long): Duration {
    val minutes = ceil(ms.coerceAtLeast(0L) / MINUTE_MS.toDouble()).toInt()
    if (minutes < 60) return Duration(0, 0, minutes)
    val hours = ceil(ms / HOUR_MS.toDouble()).toInt()
    return Duration(hours / 24, hours % 24, 0)
}

/** 숫자를 한국어로 읽었을 때 받침이 없으면(2, 4, 5, 9로 끝남) "를"을 붙인다. */
fun takesReul(n: Int): Boolean = Math.floorMod(n, 10) in setOf(2, 4, 5, 9)

/** Figma S-13 견본: 남은 시간과 진행 막대(자란 만큼/Track)를 그대로 재현한다. 줄은 위에서부터. */
private class Sample(val seedId: String?, val remainingMin: Int, val progress: Float)

private val figmaSamples = listOf(
    Sample("seed_carrot", 2 * 60, 0.6f),
    Sample("seed_carrot", 2 * 60, 0.6f),
    Sample("seed_carrot", 5 * 60, 0.2f), // Catalog의 당근은 2시간이라 이 칸만 견본이 Catalog와 다르다.
    Sample("seed_potato", 27 * 60, 0.3f),
    Sample("seed_potato", 27 * 60, 0.3f),
    Sample("seed_potato", 48 * 60, 0f),
    Sample("seed_wheat", 0, 1f),
    Sample("seed_wheat", 0, 1f),
    Sample(null, 0, 0f),
)

/** 시연을 시작할 때의 밭. 칸 수가 견본보다 많으면 나머지는 빈 칸이다. */
fun demoPlots(now: Long, gridSize: Int): List<Plot> = List(gridSize * gridSize) { i ->
    val s = figmaSamples.getOrNull(i) ?: return@List Plot()
    when {
        s.seedId == null -> Plot()
        s.remainingMin == 0 -> Plot(s.seedId, now - 7 * HOUR_MS, now - HOUR_MS)
        else -> {
            val readyAt = now + s.remainingMin * MINUTE_MS
            val total = (s.remainingMin * MINUTE_MS / (1f - s.progress)).toLong()
            Plot(s.seedId, readyAt - total, readyAt)
        }
    }
}

/** 화면을 나누면 Activity가 다시 만들어지므로 밭은 프로세스에 하나만 둔다(시연용). */
object FarmStore {
    val plots = MutableStateFlow(demoPlots(System.currentTimeMillis(), BalanceConfig.DEFAULT.farmGridSize))
}
