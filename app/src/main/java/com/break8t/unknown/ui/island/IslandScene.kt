package com.break8t.unknown.ui.island

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import com.break8t.unknown.ui.iso.IsoFarm
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
import com.break8t.unknown.ui.iso.VPC
import com.break8t.unknown.ui.iso.RoadRay
import com.break8t.unknown.ui.iso.roadCellsInView
import com.break8t.unknown.ui.iso.rememberIsoViewState
import com.break8t.unknown.ui.theme.AppColors
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

/** 장면이 어느 곳을 비추는지. */
enum class SceneFocus { MAIN, FARM }

/** 장면이 그려야 하는 한 칸. progress는 0에서 1. */
data class PlotView(val seedId: String?, val grown: Boolean, val progress: Float)

/** 장면에 넘기는 것 전부. 장면은 이것만 보고 그린다. */
data class IslandSceneState(
    val night: Boolean,
    val focus: SceneFocus,
    val plots: List<PlotView>,
    val carId: String,
    val drive: CarDrive? = CarDrive(TripPhase.PARKED, 1f),
    val islandSize: Int = IslandMap.SIZE,
)

/** Where the car is in its trip; null means no car on the island (the entrance, S-11-a). */
data class CarDrive(val phase: TripPhase, val progress: Float)

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

    /** Where the parked car stands on the road, in cells. */
    const val PARK_Y = 1.3f
    private const val FARM_X = 5
    private const val FARM_Y = 4

    private fun cells(size: Int): List<Cell> = List(size * size) { i ->
        val x = i % size
        val y = i / size
        when {
            x == ROAD_X && y >= 1 -> Cell(FloorType.ROAD, depth = underside(x, y, size))
            else -> Cell(FloorType.GRASS, depth = underside(x, y, size))
        }
    }

    /** Deeper toward the middle, so the rock under the island steps inward like the art. */
    internal fun underside(x: Int, y: Int, size: Int = SIZE): Float {
        val edge = min(min(x, y), min(size - 1 - x, size - 1 - y))
        val jitter = ((x * 7 + y * 13) % 5) * 0.12f
        return if (edge == 0) EDGE_DEPTH else EDGE_DEPTH + min(edge, 4) * RING_DEPTH + jitter
    }

    private fun o(id: String, shape: IsoShape, x: Int, y: Int, rot: Int = 0, z: Float = 0f, pick: Boolean = false) =
        IsoObject(id, shape, x, y, rot, z = z, pickable = pick)

    /** The island for the island screens and the island photo. */
    /** The car parked or on the road; headlights are on while it drives. */
    fun carObject(carId: String, pose: CarPose, lit: Boolean): IsoObject? {
        val shape = (if (lit) IsoShapes.litCar(carId) else IsoShapes.of(carId)) ?: return null
        val y = floor(pose.y).toInt()
        // The art parks a small car in a one-cell box, so the 2-cell model is scaled down and centred.
        return IsoObject("car", shape, ROAD_X, y, pose.rotation, ox = 0.15f, oy = pose.y - y, scale = 0.7f, pickable = false)
    }

    /** First road position past the edge of a [w] x [h] px view, where a drive in starts and a drive out ends. */
    fun farY(camera: IsoCamera, w: Float, h: Float, size: Int = SIZE): Float =
        size + roadCellsInView(road(size), IsoScene(size, size, frame = frame(size)), camera, w, h).toFloat()

    fun scene(
        plots: List<PlotView>, carId: String, extra: List<IsoObject> = emptyList(),
        car: CarPose? = CarPose(PARK_Y, 3), carLit: Boolean = false, size: Int = SIZE,
    ): IsoScene {
        val n = sqrt(plots.size.toDouble()).roundToInt().coerceAtLeast(1)
        val farm = plots.mapIndexed { k, v ->
            o("plot$k", IsoShapes.plot(plotStage(v), cropOf(v.seedId)), FARM_X + k % n, FARM_Y + k / n, pick = true)
        }
        val fence = (0 until n).map { o("fence$it", IsoScenery.lowFence, FARM_X + it, FARM_Y - 1, pick = true) } +
            o("fence_end", IsoScenery.lowPost, FARM_X + n, FARM_Y - 1, pick = true)
        val lanes = (3 until size).map { o("lane$it", IsoScenery.laneY, ROAD_X, it) }
        val scenery = listOf(
            o("house", IsoShapes.house, 1, 0, pick = true),
            o("stall", IsoScenery.stall, 7, 0, pick = true),
            o("parking", IsoScenery.parking, ROAD_X, 1),
        ) + listOfNotNull(
            car?.let { carObject(carId, it, carLit) },
        ) + listOf(
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
            o("cloud1", IsoScenery.cloud, -2, 3, z = 2.5f),
            o("cloud2", IsoScenery.cloud, 9, -2, z = 3f),
            o("islet1", IsoScenery.islet, -1, 6, z = 1.5f),
            o("islet2", IsoScenery.islet, size + 1, 1, z = 2f),
        ) + grownScenery.filter { it.x + it.footW <= size && it.y + it.footD <= size }
        return IsoScene(
            size, size, cells(size), lanes + scenery + fence + farm + extra, floorDepth = EDGE_DEPTH, frame = frame(size),
            roads = listOf(road(size)),
        )
    }

    // Land added by expanding grows toward +x and +y, so every cell above keeps its place; these fill it.
    private val grownScenery = listOf(
        o("grow1", IsoScenery.tree, 10, 2), o("grow2", IsoScenery.bush, 11, 5), o("grow3", IsoScenery.redFlowers, 10, 7),
        o("grow4", IsoScenery.appleTree, 11, 9), o("grow5", IsoScenery.yellowFlowers, 8, 10), o("grow6", IsoScenery.tree, 6, 11),
        o("grow7", IsoScenery.bush, 1, 11), o("grow8", IsoScenery.rocks, 10, 11), o("grow9", IsoScenery.redFlowers, 3, 10),
        o("grow10", IsoScenery.bigTree, 12, 3), o("grow11", IsoScenery.bush, 13, 7), o("grow12", IsoScenery.yellowFlowers, 12, 10),
        o("grow13", IsoScenery.tree, 13, 12), o("grow14", IsoScenery.rocks, 9, 13), o("grow15", IsoScenery.appleTree, 6, 13),
        o("grow16", IsoScenery.redFlowers, 2, 12), o("grow17", IsoScenery.bush, 0, 13), o("grow18", IsoScenery.tree, 12, 0),
    )

    /** The farm screen (S-13): plots ringed by grass, a fence of lights at the back, seen from the front. */
    // Each farm-screen bed is 2 x 2 cells inside a one-cell grass ring, so the beds dominate like S-13.
    private const val BED = 2
    private const val FARM_SLAB = 1.1f

    fun farmScene(plots: List<PlotView>): IsoScene {
        val n = sqrt(plots.size.toDouble()).roundToInt().coerceAtLeast(1)
        val side = n * BED + 2
        val f = IsoFarm.FINE
        val e = side * f
        val farm = plots.mapIndexed { k, v ->
            val (st, cr) = plotStage(v) to cropOf(v.seedId)
            // Lowered so the plate is at ground level and the crop's base is in the soil (S-13).
            IsoObject(
                "plot$k", IsoFarm.bed(st, cr), (1 + BED * (k % n)) * f, (1 + BED * (k / n)) * f,
                z = -IsoFarm.sink(st, cr).toFloat() / VPC,
            )
        }
        // The art's fence spans about three quarters of the farm, centred on the back row.
        val fence = o("fence", IsoFarm.fence, (e - IsoFarm.fence.w) / 2, 1)
        val bed = { x: Int, y: Int -> x in 1..n * BED && y in 1..n * BED }
        val tufts = (0 until side * side).filter { !bed(it % side, it / side) }.map { i ->
            o("tuft$i", IsoFarm.tuft, (i % side) * f + 1, (i / side) * f + 1)
        }
        // Fit the beds and the fence, not the default headroom, so the farm fills its panel like S-13.
        // A slab about one dirt band deep, so its front shows the art's brown edge rather than stone.
        val frame = FrameBox(0f, 0f, e.toFloat(), e.toFloat(), -FARM_SLAB, 1.5f * f)
        return IsoScene(e, e, objects = listOf(fence) + farm + tufts, floorDepth = FARM_SLAB, frame = frame, tile = f)
    }

    val FARM_CAMERA = IsoCamera(0f, 55f)

    /** Index of the plot a tap landed on, from the plot model or the floor cell under it. */
    fun plotAt(hit: IsoHit, plots: Int, farmView: Boolean): Int? {
        val n = sqrt(plots.toDouble()).roundToInt().coerceAtLeast(1)
        val (x0, y0) = if (farmView) IsoFarm.FINE to IsoFarm.FINE else FARM_X to FARM_Y
        val size = if (farmView) BED * IsoFarm.FINE else 1
        return when (hit) {
            is IsoHit.Item -> hit.id.removePrefix("plot").takeIf { hit.id.startsWith("plot") }?.toIntOrNull()
            is IsoHit.Cell -> {
                val cx = Math.floorDiv(hit.x - x0, size); val cy = Math.floorDiv(hit.y - y0, size)
                if (cx in 0 until n && cy in 0 until n) cy * n + cx else null
            }
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
        val f = IsoFarm.FINE.toFloat()
        val x = (1f + BED * (k % n)) * f
        val y = (1f + BED * (k / n)) * f
        return floatArrayOf(x, y, x + BED * f, y + BED * f)
    }

    private const val EDGE_DEPTH = 1f
    private const val RING_DEPTH = 1.1f // steep enough that each ring shows below the one outside it at 30 degrees
    private const val BRIDGE_DEPTH = 0.25f

    /** The road leaves the island as a thin bridge and runs on to the edge of whatever the screen shows. */
    val ROAD = road(SIZE)
    fun road(size: Int) = RoadRay(ROAD_X, size, 0, 1, BRIDGE_DEPTH, IsoScenery.laneY)
    private fun frame(size: Int) = FrameBox(0f, 0f, size.toFloat(), size.toFloat(), -4.5f, 6.5f) // headroom for the chips above the roof
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
    val size = state.islandSize
    val base = remember(farm, stages, state.carId, size) {
        if (farm) IslandMap.farmScene(state.plots) else IslandMap.scene(state.plots, state.carId, car = null, size = size)
    }
    val view = rememberIsoViewState(night = state.night)
    SideEffect { view.night = state.night }
    val count = state.plots.size
    BoxWithConstraints(modifier.background(sceneSky(state.night))) {
        val camera = if (farm) IslandMap.FARM_CAMERA else view.camera
        val w = constraints.maxWidth.toFloat(); val h = constraints.maxHeight.toFloat()
        val farY = remember(camera, w, h, size) { IslandMap.farY(camera, w, h, size) }
        // The car is added per frame, so a drive only rebuilds the object list, not the island.
        val car = if (farm) null else state.drive?.let { d ->
            IslandMap.carObject(state.carId, carPose(d.phase, d.progress, IslandMap.PARK_Y, farY), lit = d.phase != TripPhase.PARKED)
        }
        val scene = if (car == null) base else base.copy(objects = base.objects + car)
        if (!farm) PixelSky(state.night, Modifier.fillMaxSize())
        IsoSceneView(
            scene, view, Modifier.fillMaxSize(),
            camera = camera,
            matchVoxels = farm,
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
 * 하늘의 픽셀 장식. 밤에는 별과 그날 위상의 달([phase], [moonPhase]), 낮에는 그리지 않는다(구름은 장면의 복셀이다).
 * 별 자리는 고정된 표에서 뽑아서 다시 그려도 깜빡이지 않는다. 달의 어두운 쪽도 흐리게 칠해서 신월에도 자리가 보인다.
 */
@Composable
internal fun PixelSky(
    night: Boolean,
    modifier: Modifier = Modifier,
    phase: Double = moonPhase(LocalDate.now(), ZoneId.systemDefault()),
) {
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
        val cx = (cols * 0.86f).toInt(); val cy = (rows * 0.16f).toInt(); val r = MOON_R
        for (j in -r..r) for (i in -r..r) {
            if (i * i + j * j > r * r) continue
            drawRect(if (moonLit(i, j, r, phase)) Moon else MoonDark, Offset((cx + i) * px, (cy + j) * px), Size(px, px))
        }
    }
}

private const val STAR_COUNT = 70
private const val MOON_R = 6
private val Star = Color(0xFFF7F4EC)
private val StarDim = Color(0xFF8C9AC8)
private val Moon = Color(0xFFFFE7A8)
private val MoonDark = Color(0xFF1C2A5E) // a step above the night sky (IsoPalette.Night)
