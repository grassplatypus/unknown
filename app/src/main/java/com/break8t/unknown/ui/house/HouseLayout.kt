package com.break8t.unknown.ui.house

import com.break8t.unknown.config.BalanceConfig
import com.break8t.unknown.data.DemoState
import kotlin.math.abs

/** 가구가 놓이는 면. 벽은 바닥 격자의 두 변 위에 선다. */
enum class Surface { FLOOR, LEFT_WALL, RIGHT_WALL }

/**
 * 놓인 가구 하나. 바닥에서 x는 오른쪽 벽을 따라, y는 왼쪽 벽을 따라 센다.
 * 벽에서 x는 벽을 따라, y는 바닥부터 센 줄이다. turns는 90도 돌린 횟수다.
 */
data class Placed(val itemId: String, val surface: Surface, val x: Int, val y: Int, val turns: Int = 0)

/** 가구의 칸 크기. 이 기능에서만 쓰는 값이라 여기 둔다(Catalog에는 이름과 가격만 둔다). */
data class FurnitureSpec(val w: Int, val h: Int, val onWall: Boolean = false)

val FurnitureSpecs: Map<String, FurnitureSpec> = mapOf(
    "it_sofa" to FurnitureSpec(2, 1),
    "it_plant" to FurnitureSpec(1, 1),
    "it_picture" to FurnitureSpec(1, 1, onWall = true),
    "it_rug" to FurnitureSpec(2, 2),
    "it_lamp" to FurnitureSpec(1, 1),
    "it_shelf" to FurnitureSpec(2, 1),
)

fun specOf(itemId: String): FurnitureSpec = FurnitureSpecs[itemId] ?: FurnitureSpec(1, 1)

/** Figma S-14 견본 배치: 왼쪽 벽 소파, 안쪽 화분, 오른쪽 벽 액자. */
val DefaultLayout: List<Placed> = listOf(
    Placed("it_sofa", Surface.FLOOR, x = 0, y = 1, turns = 1),
    Placed("it_plant", Surface.FLOOR, x = 0, y = 0),
    Placed("it_picture", Surface.RIGHT_WALL, x = 3, y = 2),
)

data class Cell(val surface: Surface, val x: Int, val y: Int)

/** 돌린 상태를 반영한 (가로, 세로) 칸 수. 벽 장식은 돌리지 않는다. */
fun footprint(p: Placed): Pair<Int, Int> {
    val s = specOf(p.itemId)
    return if (!s.onWall && p.turns % 2 == 1) s.h to s.w else s.w to s.h
}

fun cellsOf(p: Placed): Set<Cell> {
    val (w, h) = footprint(p)
    return buildSet { for (dx in 0 until w) for (dy in 0 until h) add(Cell(p.surface, p.x + dx, p.y + dy)) }
}

private fun inBounds(p: Placed, config: BalanceConfig): Boolean {
    val (w, h) = footprint(p)
    val n = config.houseGridSize
    val rows = if (p.surface == Surface.FLOOR) n else config.houseWallHeight
    val surfaceOk = specOf(p.itemId).onWall == (p.surface != Surface.FLOOR)
    return surfaceOk && p.x >= 0 && p.y >= 0 && p.x + w <= n && p.y + h <= rows
}

/** 칸 밖으로 나가거나 다른 가구와 겹치면 false. ignore 번째 가구(움직이는 자신)는 빼고 본다. */
fun fits(layout: List<Placed>, candidate: Placed, config: BalanceConfig, ignore: Int = -1): Boolean {
    if (!inBounds(candidate, config)) return false
    val mine = cellsOf(candidate)
    return layout.withIndex().none { (i, other) -> i != ignore && cellsOf(other).any { it in mine } }
}

/** 빈 자리를 찾아 놓는다. 자리가 없으면 null. 벽 장식은 눈높이 줄부터 찾는다. */
fun placeNew(layout: List<Placed>, itemId: String, config: BalanceConfig): List<Placed>? {
    val n = config.houseGridSize
    val candidates = if (specOf(itemId).onWall) {
        val rows = (0 until config.houseWallHeight).sortedBy { abs(it - config.houseWallHeight / 2) }
        listOf(Surface.LEFT_WALL, Surface.RIGHT_WALL).flatMap { s -> rows.flatMap { y -> (0 until n).map { x -> Placed(itemId, s, x, y) } } }
    } else {
        (0 until n).flatMap { y -> (0 until n).map { x -> Placed(itemId, Surface.FLOOR, x, y) } }
    }
    val spot = candidates.firstOrNull { fits(layout, it, config) } ?: return null
    return layout + spot
}

/** 가구의 왼쪽 위 칸을 target으로 옮긴다. 면이 맞지 않거나 겹치면 그대로 돌려준다. */
fun moveTo(layout: List<Placed>, index: Int, target: Cell, config: BalanceConfig): List<Placed> {
    val p = layout.getOrNull(index) ?: return layout
    val (w, h) = footprint(p)
    val rows = if (target.surface == Surface.FLOOR) config.houseGridSize else config.houseWallHeight
    // 끝 칸을 잡고 끌어도 판 밖으로 밀려나지 않게 안쪽으로 당긴다.
    val moved = p.copy(
        surface = target.surface,
        x = target.x.coerceIn(0, maxOf(0, config.houseGridSize - w)),
        y = target.y.coerceIn(0, maxOf(0, rows - h)),
    )
    return if (fits(layout, moved, config, ignore = index)) layout.toMutableList().also { it[index] = moved } else layout
}

/** 90도 돌린다. 돌린 모양이 들어가지 않으면 null. */
fun rotate(layout: List<Placed>, index: Int, config: BalanceConfig): List<Placed>? {
    val p = layout.getOrNull(index) ?: return null
    if (specOf(p.itemId).onWall) return null
    val turned = p.copy(turns = (p.turns + 1) % 4)
    return if (fits(layout, turned, config, ignore = index)) layout.toMutableList().also { it[index] = turned } else null
}

fun storeBack(layout: List<Placed>, index: Int): List<Placed> = layout.filterIndexed { i, _ -> i != index }

fun reset(): List<Placed> = emptyList()

fun itemAt(layout: List<Placed>, cell: Cell): Int = layout.indexOfFirst { cell in cellsOf(it) }

/** 보관함에 남은 개수. profile.items는 가진 전체 개수이고, 방에 놓은 것을 뺀다. */
fun available(owned: Map<String, Int>, layout: List<Placed>, itemId: String): Int =
    (owned[itemId] ?: 0) - layout.count { it.itemId == itemId }

/** 가진 개수보다 많이 놓인 가구는 뒤에서부터 뺀다(다른 화면에서 개수가 줄었을 때). */
fun trimToOwned(layout: List<Placed>, owned: Map<String, Int>): List<Placed> {
    val left = owned.toMutableMap()
    return layout.filter { p ->
        val n = left[p.itemId] ?: 0
        (n > 0).also { if (it) left[p.itemId] = n - 1 }
    }
}

enum class ExitAction { LEAVE, ASK_SAVE }

/** 꾸미기를 나갈 때: 바꾼 것이 없으면 바로 나가고, 있으면 D-11로 묻는다. */
fun exitAction(saved: List<Placed>, draft: List<Placed>): ExitAction =
    if (saved == draft) ExitAction.LEAVE else ExitAction.ASK_SAVE

/** D-11의 답. 저장하면 draft가, 저장하지 않으면 saved가 남는다. */
fun resolveExit(saved: List<Placed>, draft: List<Placed>, save: Boolean): List<Placed> = if (save) draft else saved

/** rememberSaveable용 문자열 표현. */
fun encode(layout: List<Placed>): ArrayList<String> =
    layout.mapTo(ArrayList()) { "${it.itemId},${it.surface.name},${it.x},${it.y},${it.turns}" }

fun decode(saved: List<String>): List<Placed> = saved.map { line ->
    val (id, s, x, y, t) = line.split(',')
    Placed(id, Surface.valueOf(s), x.toInt(), y.toInt(), t.toInt())
}

/** 다음 넓히기 단계: 넓힌 뒤 한 변의 칸 수와 드는 코인. */
data class Expansion(val size: Int, val price: Int)

/** 지금 크기 [current]보다 큰 첫 단계. 다 넓혔으면 null. */
fun nextExpansion(current: Int, config: BalanceConfig): Expansion? {
    val i = config.houseGridSteps.indexOfFirst { it > current }
    return if (i < 0) null else Expansion(config.houseGridSteps[i], config.houseGridStepPrices[i])
}

enum class ExpandResult { DONE, NOT_ENOUGH, MAX }

/** 코인을 내고 한 단계 넓힌다. 모자라거나 끝이면 바꾸지 않는다. 칸 번호가 그대로라 배치는 손대지 않는다. */
fun expandHouse(state: DemoState, config: BalanceConfig): ExpandResult {
    var result = ExpandResult.MAX
    state.update { p ->
        val next = nextExpansion(p.houseGridSize, config)
        result = when {
            next == null -> ExpandResult.MAX
            p.coins < next.price -> ExpandResult.NOT_ENOUGH
            else -> ExpandResult.DONE
        }
        if (next != null && result == ExpandResult.DONE) p.copy(coins = p.coins - next.price, houseGridSize = next.size) else p
    }
    return result
}
