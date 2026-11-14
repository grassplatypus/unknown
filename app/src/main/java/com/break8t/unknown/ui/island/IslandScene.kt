package com.break8t.unknown.ui.island

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import com.break8t.unknown.ui.iso.Cell
import com.break8t.unknown.ui.iso.Crop
import com.break8t.unknown.ui.iso.FloorType
import com.break8t.unknown.ui.iso.FrameBox
import com.break8t.unknown.ui.iso.IsoCamera
import com.break8t.unknown.ui.iso.IsoHit
import com.break8t.unknown.ui.iso.IsoObject
import com.break8t.unknown.ui.iso.IsoOverlayScope
import com.break8t.unknown.ui.iso.IsoPalette
import com.break8t.unknown.ui.iso.IsoScene
import com.break8t.unknown.ui.iso.IsoSceneView
import com.break8t.unknown.ui.iso.IsoScenery
import com.break8t.unknown.ui.iso.IsoShape
import com.break8t.unknown.ui.iso.IsoShapes
import com.break8t.unknown.ui.iso.PIXEL_DP
import com.break8t.unknown.ui.iso.PlotStage
import com.break8t.unknown.ui.iso.RoadRay
import com.break8t.unknown.ui.iso.rememberIsoViewState
import com.break8t.unknown.ui.theme.AppColors
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

/** 장면이 어느 곳을 비추는지. */
enum class SceneFocus { MAIN, FARM }

/** 장면이 그려야 하는 한 칸. progress는 0에서 1. */
data class PlotView(val seedId: String?, val grown: Boolean, val progress: Float)

/** 장면에 넘기는 것 전부. 장면은 이것만 보고 그린다. */
data class IslandSceneState(val night: Boolean, val focus: SceneFocus, val plots: List<PlotView>, val carId: String)

fun plotViews(plots: List<Plot>, now: Long): List<PlotView> =
    plots.map { PlotView(it.seedId, it.isGrown(now), it.progress(now)) }

/** 섬에서 누르면 열리는 곳. 칩과 물체가 같은 곳을 가리킨다. */
enum class IslandSpot { HOUSE, SHOP, FARM }

/** Figma S-13 처럼 모든 심은 칸은 다 큰 잎으로 보이고, 다 자란 칸만 익은 모양이 된다. */
fun plotStage(v: PlotView): PlotStage = when {
    v.seedId == null -> PlotStage.EMPTY
    v.grown -> PlotStage.GROWN
    else -> PlotStage.GROWING
}

fun cropOf(seedId: String?): Crop = when (seedId) {
    "seed_potato" -> Crop.SWEET_POTATO
    "seed_wheat" -> Crop.WHEAT
    else -> Crop.CARROT
}

internal fun sceneSky(night: Boolean) = if (night) IsoPalette.Night else AppColors.Sky

/**
 * 섬의 배치. 좌표는 칸이고 x는 화면 오른쪽 아래, y는 왼쪽 아래로 간다(비스듬히 보기).
 * Figma S-12-a를 따라 집은 뒤, 가게는 오른쪽, 텃밭은 가운데, 길은 집 앞에서 섬 밖으로 나간다.
 */
object IslandMap {
    const val SIZE = 10
    private const val ROAD_X = 4
    private const val FARM_X = 5
    private const val FARM_Y = 4

    private fun cells(): List<Cell> = List(SIZE * SIZE) { i ->
        val x = i % SIZE
        val y = i / SIZE
        when {
            x == ROAD_X && y >= 1 -> Cell(FloorType.ROAD, depth = underside(x, y))
            else -> Cell(FloorType.GRASS, depth = underside(x, y))
        }
    }

    /** Deeper toward the middle, so the rock under the island steps inward like the art. */
    internal fun underside(x: Int, y: Int): Float {
        val edge = min(min(x, y), min(SIZE - 1 - x, SIZE - 1 - y))
        val jitter = ((x * 7 + y * 13) % 5) * 0.12f
        return if (edge == 0) EDGE_DEPTH else EDGE_DEPTH + min(edge, 4) * RING_DEPTH + jitter
    }

    private fun o(id: String, shape: IsoShape, x: Int, y: Int, rot: Int = 0, z: Float = 0f, pick: Boolean = false) =
        IsoObject(id, shape, x, y, rot, z = z, pickable = pick)

    /** The island for the island screens and the island photo. */
    fun scene(plots: List<PlotView>, carId: String, extra: List<IsoObject> = emptyList()): IsoScene {
        val n = sqrt(plots.size.toDouble()).roundToInt().coerceAtLeast(1)
        val farm = plots.mapIndexed { k, v ->
            o("plot$k", IsoShapes.plot(plotStage(v), cropOf(v.seedId)), FARM_X + k % n, FARM_Y + k / n, pick = true)
        }
        val fence = (0 until n).map { o("fence$it", IsoScenery.lightFence, FARM_X + it, FARM_Y - 1, pick = true) } +
            o("fence_end", IsoScenery.fencePost, FARM_X + n, FARM_Y - 1, pick = true)
        val lanes = (3 until SIZE).map { o("lane$it", IsoScenery.laneY, ROAD_X, it) }
        val scenery = listOf(
            o("house", IsoShapes.house, 1, 0, pick = true),
            o("stall", IsoScenery.stall, 7, 0, pick = true),
            o("parking", IsoScenery.parking, ROAD_X, 1),
            // The art parks a small car in a one-cell box, so the 2-cell model is scaled down and centred.
            IsoObject("car", IsoShapes.of(carId) ?: IsoShapes.car(Color.Red), ROAD_X, 1, 1, ox = 0.15f, oy = 0.3f, scale = 0.7f, pickable = false),
            o("mailbox", IsoScenery.mailbox, 3, 3),
            o("bigtree", IsoScenery.bigTree, 0, 3),
            o("tree1", IsoScenery.tree, 0, 0),
            o("tree2", IsoScenery.tree, 1, 6),
            o("tree3", IsoScenery.tree, 9, 3),
            o("apple1", IsoScenery.appleTree, 9, 5),
            o("apple2", IsoScenery.appleTree, 3, 9),
            o("pond", IsoShapes.water, 6, 8),
            o("sign", IsoScenery.signpost, 3, 7),
            o("bush1", IsoScenery.bush, 2, 5),
            o("bush2", IsoScenery.bush, 9, 1),
            o("bush3", IsoScenery.bush, 8, 7),
            o("bush4", IsoScenery.bush, 0, 9),
            o("bush5", IsoScenery.bush, 5, 9),
            o("flower1", IsoScenery.redFlowers, 1, 8),
            o("flower2", IsoScenery.redFlowers, 9, 8),
            o("flower3", IsoScenery.yellowFlowers, 2, 3),
            o("flower4", IsoScenery.yellowFlowers, 8, 2),
            o("flower5", IsoScenery.yellowFlowers, 5, 7),
            o("flower6", IsoScenery.redFlowers, 0, 5),
            o("rocks1", IsoScenery.rocks, 8, 5),
            o("rocks2", IsoScenery.rocks, 2, 9),
            o("cloud1", IsoScenery.cloud, -3, 1, z = 3.5f),
            o("cloud2", IsoScenery.cloud, 10, -3, z = 2.5f),
            o("islet1", IsoScenery.islet, -2, 6, z = 2f),
            o("islet2", IsoScenery.islet, 12, 2, z = 1.5f),
        )
        return IsoScene(
            SIZE, SIZE, cells(), lanes + scenery + fence + farm + extra, floorDepth = EDGE_DEPTH, frame = FRAME,
            roads = listOf(ROAD),
        )
    }

    /** The farm screen (S-13): plots ringed by grass, a fence of lights at the back, seen from the front. */
    fun farmScene(plots: List<PlotView>): IsoScene {
        val n = sqrt(plots.size.toDouble()).roundToInt().coerceAtLeast(1)
        val side = n + 2
        val farm = plots.mapIndexed { k, v -> IsoObject("plot$k", IsoShapes.farmPlot(plotStage(v), cropOf(v.seedId)), 1 + k % n, 1 + k / n, scale = 0.5f) }
        val fence = (0 until side - 1).map { o("fence$it", IsoScenery.lightFence, it, 0) } + o("fence_end", IsoScenery.fencePost, side - 1, 0)
        val grass = (0 until side * side).filter { i -> (i % side) in 1..n && (i / side) in 1..n }.toSet()
        val bushes = (0 until side * side).filter { it !in grass && it / side > 0 }.map { i ->
            o("tuft$i", IsoScenery.tuft, i % side, i / side)
        }
        return IsoScene(side, side, objects = fence + farm + bushes, floorDepth = 0.6f)
    }

    val FARM_CAMERA = IsoCamera(0f, 55f)

    /** Index of the plot a tap landed on, from the plot model or the floor cell under it. */
    fun plotAt(hit: IsoHit, plots: Int, farmView: Boolean): Int? {
        val n = sqrt(plots.toDouble()).roundToInt().coerceAtLeast(1)
        val (x0, y0) = if (farmView) 1 to 1 else FARM_X to FARM_Y
        return when (hit) {
            is IsoHit.Item -> hit.id.removePrefix("plot").takeIf { hit.id.startsWith("plot") }?.toIntOrNull()
            is IsoHit.Cell -> if (hit.x - x0 in 0 until n && hit.y - y0 in 0 until n) (hit.y - y0) * n + (hit.x - x0) else null
        }?.takeIf { it in 0 until plots }
    }

    /** Which place a tap on the island opens. */
    fun spotAt(hit: IsoHit, plots: Int): IslandSpot? = when {
        hit is IsoHit.Item && hit.id == "house" -> IslandSpot.HOUSE
        hit is IsoHit.Item && hit.id == "stall" -> IslandSpot.SHOP
        hit is IsoHit.Item && hit.id.startsWith("fence") -> IslandSpot.FARM
        plotAt(hit, plots, farmView = false) != null -> IslandSpot.FARM
        else -> null
    }

    /** The world point a spot's chip stands on, and which point of the chip touches it. */
    fun anchor(spot: IslandSpot): Pair<FloatArray, Alignment> = when (spot) {
        IslandSpot.HOUSE -> floatArrayOf(2.5f, 1.5f, 3.6f) to Alignment.BottomCenter
        IslandSpot.SHOP -> floatArrayOf(8.6f, 0.6f, 3.0f) to Alignment.BottomStart
        IslandSpot.FARM -> floatArrayOf(FARM_X + 2.6f, FARM_Y + 3.6f, 0f) to Alignment.TopCenter
    }

    /** Cell rectangle of plot [k] on the farm screen, for the labels that sit on it. */
    fun farmPlotRect(k: Int, plots: Int): FloatArray {
        val n = sqrt(plots.toDouble()).roundToInt().coerceAtLeast(1)
        val x = 1f + k % n
        val y = 1f + k / n
        return floatArrayOf(x, y, x + 1f, y + 1f)
    }

    private const val EDGE_DEPTH = 1f
    private const val RING_DEPTH = 1.1f // steep enough that each ring shows below the one outside it at 30 degrees
    private const val BRIDGE_DEPTH = 0.25f

    /** The road leaves the island as a thin bridge and runs on to the edge of whatever the screen shows. */
    val ROAD = RoadRay(ROAD_X, SIZE, 0, 1, BRIDGE_DEPTH, IsoScenery.laneY)
    private val FRAME = FrameBox(-0.5f, -0.5f, SIZE + 0.5f, SIZE + 0.5f, -5f, 3f)
}

/**
 * 섬과 텃밭 장면. 섬에서는 [spotLabel]을 그 물체 위에 세우고, 물체나 칩을 누르면 [onSpot]을 부른다.
 * 텃밭에서는 칸을 누르면 [onPlot]을 부르고, [plotOverlay]를 칸 위에 얹는다.
 */
@Composable
fun IslandScene(
    state: IslandSceneState,
    modifier: Modifier = Modifier,
    onPlot: (Int) -> Unit = {},
    plotOverlay: @Composable BoxScope.(Int) -> Unit = {},
    onSpot: (IslandSpot) -> Unit = {},
    spotLabel: (@Composable (IslandSpot) -> Unit)? = null,
) {
    val farm = state.focus == SceneFocus.FARM
    // Keyed on what is drawn, not on progress, so the 10 s clock does not re-render the island.
    val stages = state.plots.map { it.seedId to plotStage(it) }
    val scene = remember(farm, stages, state.carId) {
        if (farm) IslandMap.farmScene(state.plots) else IslandMap.scene(state.plots, state.carId)
    }
    val view = rememberIsoViewState(night = state.night)
    SideEffect { view.night = state.night }
    val count = state.plots.size
    Box(modifier.background(sceneSky(state.night))) {
        if (!farm) PixelSky(state.night, Modifier.fillMaxSize())
        IsoSceneView(
            scene, view, Modifier.fillMaxSize(),
            camera = if (farm) IslandMap.FARM_CAMERA else view.camera,
            onTap = { hit ->
                if (farm) IslandMap.plotAt(hit, count, farmView = true)?.let(onPlot)
                else IslandMap.spotAt(hit, count)?.let(onSpot)
            },
        ) {
            if (farm) {
                for (k in 0 until count) {
                    val r = IslandMap.farmPlotRect(k, count)
                    // The box takes the plot's taps: from this steep view a tall plant in front covers the bottom of the cell behind it.
                    Box(Modifier.isoRect(r[0], r[1], r[2], r[3]).clickable(role = Role.Button) { onPlot(k) }) { plotOverlay(k) }
                }
            } else if (spotLabel != null) {
                for (spot in IslandSpot.entries) SpotAnchor(spot) { spotLabel(spot) }
            }
        }
    }
}

@Composable
private fun IsoOverlayScope.SpotAnchor(spot: IslandSpot, content: @Composable () -> Unit) {
    val (p, align) = IslandMap.anchor(spot)
    Box(Modifier.isoAnchor(p[0], p[1], p[2], align)) { content() }
}

/**
 * 하늘의 픽셀 장식. 밤에는 별과 초승달(Figma S-12-b), 낮에는 그리지 않는다(구름은 장면의 복셀이다).
 * 별 자리는 고정된 표에서 뽑아서 다시 그려도 깜빡이지 않는다.
 */
@Composable
internal fun PixelSky(night: Boolean, modifier: Modifier = Modifier) {
    if (!night) return
    val px = max(1f, (PIXEL_DP * LocalDensity.current.density).roundToInt().toFloat())
    Canvas(modifier) {
        val cols = (size.width / px).toInt()
        val rows = (size.height / px).toInt()
        for (k in 0 until STAR_COUNT) {
            val h = (k * 2654435761L).toInt() ushr 1
            val x = (h % 1000) / 1000f * cols
            val y = ((h / 1000) % 1000) / 1000f * rows * 0.7f
            val at = Offset(x.toInt() * px, y.toInt() * px)
            drawRect(Star, at, Size(px, px))
            if (k % 5 == 0) {
                drawRect(StarDim, at + Offset(-px, 0f), Size(px, px)); drawRect(StarDim, at + Offset(px, 0f), Size(px, px))
                drawRect(StarDim, at + Offset(0f, -px), Size(px, px)); drawRect(StarDim, at + Offset(0f, px), Size(px, px))
            }
        }
        // Crescent: a lit disc with a sky-coloured disc cut out of it, both drawn in whole pixels.
        val cx = (cols * 0.86f).toInt(); val cy = (rows * 0.16f).toInt(); val r = 6
        for (j in -r..r) for (i in -r..r) {
            val lit = i * i + j * j <= r * r
            val cut = (i - 3) * (i - 3) + (j + 2) * (j + 2) <= r * r
            if (lit && !cut) drawRect(Moon, Offset((cx + i) * px, (cy + j) * px), Size(px, px))
        }
    }
}

private const val STAR_COUNT = 70
private val Star = Color(0xFFF7F4EC)
private val StarDim = Color(0xFF8C9AC8)
private val Moon = Color(0xFFFFE7A8)
