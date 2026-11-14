package com.break8t.unknown.ui.house

import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import com.break8t.unknown.R
import com.break8t.unknown.config.BalanceConfig
import com.break8t.unknown.ui.island.IslandMap
import com.break8t.unknown.ui.island.PixelSky
import com.break8t.unknown.ui.island.PlotView
import com.break8t.unknown.ui.island.sceneSky
import com.break8t.unknown.ui.iso.Cell as IsoCell
import com.break8t.unknown.ui.iso.FloorType
import com.break8t.unknown.ui.iso.FrameBox
import com.break8t.unknown.ui.iso.IsoCamera
import com.break8t.unknown.ui.iso.IsoObject
import com.break8t.unknown.ui.iso.IsoOverlayScope
import com.break8t.unknown.ui.iso.IsoScene
import com.break8t.unknown.ui.iso.IsoSceneView
import com.break8t.unknown.ui.iso.IsoScenery
import com.break8t.unknown.ui.iso.IsoShapes
import com.break8t.unknown.ui.iso.IsoViewState
import com.break8t.unknown.ui.iso.Placement
import com.break8t.unknown.ui.iso.Projection
import com.break8t.unknown.ui.iso.ViewPreset
import com.break8t.unknown.ui.iso.WallSide
import com.break8t.unknown.ui.iso.VPC
import com.break8t.unknown.ui.iso.rememberIsoViewState
import com.break8t.unknown.ui.iso.wallVisible
import kotlin.math.max
import kotlin.math.roundToInt

enum class ScenePlace { HOUSE, ISLAND }

enum class CameraPreset { OBLIQUE, LOW_FRONT, SIDE, TOP }

/** 사진 찍기의 보는 각도. 돌리기는 90도씩 네 번이면 처음으로 돌아온다. */
data class SceneCamera(val preset: CameraPreset = CameraPreset.OBLIQUE, val rotation: Int = 0) {
    fun rotated() = copy(rotation = (rotation + 1) % 4)
    val iso: IsoCamera get() = IsoCamera.of(ViewPreset.valueOf(preset.name), rotation)
}

/** 장면 위에 세운 캐릭터. x, y는 장면 너비와 높이에 대한 비율, size는 높이에 대한 비율이다. */
data class SceneCharacter(val imageId: String, val x: Float, val y: Float, val size: Float, val flipped: Boolean)

/** 장면을 그리는 데 필요한 것 전부. 섬 사진이면 [plots]와 [carId]로 섬을 그린다. */
data class RoomSceneState(
    val place: ScenePlace,
    val layout: List<Placed> = emptyList(),
    val camera: SceneCamera = SceneCamera(),
    val night: Boolean = false,
    val selected: Int = -1,
    val character: SceneCharacter? = null,
    val plots: List<PlotView> = emptyList(),
    val carId: String = "car_red",
)

/**
 * 방의 배치를 엔진 장면으로 옮기는 규칙. 바닥 칸은 그대로, 오른쪽 벽은 북쪽 벽(y = 0),
 * 왼쪽 벽은 서쪽 벽(x = 0)이고, 벽의 줄은 높이로 바뀐다. 창문과 등은 고정 장식이라 그 칸에는 걸 수 없다.
 */
object RoomMap {
    const val WALL_CELLS = 3f
    private const val FLOOR_DEPTH = 0.6f

    fun rowHeight(config: BalanceConfig) = WALL_CELLS / config.houseWallHeight

    fun idOf(index: Int) = "p$index"
    fun indexOf(id: String?): Int = id?.takeIf { it.startsWith("p") }?.drop(1)?.toIntOrNull() ?: -1

    /** Furniture front faces +y in the models; a quarter turn in the house logic turns it the other way round. */
    fun rotationOf(turns: Int) = (-turns).mod(4)

    fun toObject(p: Placed, index: Int, config: BalanceConfig): IsoObject {
        val shape = IsoShapes.of(p.itemId) ?: IsoShapes.plant
        return when (p.surface) {
            Surface.FLOOR -> IsoObject(idOf(index), shape, p.x, p.y, rotationOf(p.turns))
            Surface.RIGHT_WALL -> IsoObject(idOf(index), shape, p.x, 0, wall = WallSide.NORTH, z = p.y * rowHeight(config))
            Surface.LEFT_WALL -> IsoObject(idOf(index), shape, 0, p.x, wall = WallSide.WEST, z = p.y * rowHeight(config))
        }
    }

    /** The house placement for an engine placement of [current]; walls keep their row, the engine only slides along them. */
    fun toPlaced(pl: Placement, current: Placed): Placed = when (pl.wall) {
        null -> current.copy(surface = Surface.FLOOR, x = pl.x, y = pl.y, turns = rotationOf(pl.rotation))
        WallSide.NORTH -> current.copy(surface = Surface.RIGHT_WALL, x = pl.x)
        WallSide.WEST -> current.copy(surface = Surface.LEFT_WALL, x = pl.y)
        else -> current
    }

    /** Wall cells the window and the lantern take. Passed to the placement rules as if something hung there. */
    val Fixed: List<Placed> = listOf(
        Placed(FIXED_ID, Surface.LEFT_WALL, 1, 1), Placed(FIXED_ID, Surface.LEFT_WALL, 1, 2),
        Placed(FIXED_ID, Surface.RIGHT_WALL, 1, 2), Placed(FIXED_ID, Surface.RIGHT_WALL, 1, 3),
    )

    /** Window, lantern and the posts on the corners whose walls this camera shows. */
    fun decor(camera: IsoCamera, config: BalanceConfig): List<IsoObject> {
        val n = config.houseGridSize
        val row = rowHeight(config)
        val proj = Projection(camera, 1f, 0f, 0f, 0f, 0f)
        val post = IsoScenery.post((WALL_CELLS * VPC).toInt())
        val posts = listOf(0 to 0, n to 0, 0 to n, n to n).filter { (cx, cy) ->
            wallVisible(if (cx == 0) WallSide.WEST else WallSide.EAST, proj) ||
                wallVisible(if (cy == 0) WallSide.NORTH else WallSide.SOUTH, proj)
        }.map { (cx, cy) ->
            IsoObject(
                "post$cx$cy", post, if (cx == 0) 0 else n - 1, if (cy == 0) 0 else n - 1,
                ox = if (cx == 0) -POST_OUT else 1f - POST_IN, oy = if (cy == 0) -POST_OUT else 1f - POST_IN, pickable = false,
            )
        }
        return posts + listOf(
            IsoObject("window", IsoScenery.window, 0, 1, wall = WallSide.WEST, z = row, pickable = false),
            IsoObject("lantern", IsoScenery.lantern, 1, 0, wall = WallSide.NORTH, z = 2 * row, pickable = false),
        )
    }

    /** Where the rotate and put-away buttons hang: the front corner of a floor piece, the bottom of a wall piece. */
    fun buttonAnchor(p: Placed, config: BalanceConfig): Triple<Float, Float, Float> {
        val (w, h) = footprint(p)
        return when (p.surface) {
            Surface.FLOOR -> Triple(p.x + w.toFloat(), p.y + h.toFloat(), 0f)
            Surface.RIGHT_WALL -> Triple(p.x + 0.5f, 0f, p.y * rowHeight(config))
            Surface.LEFT_WALL -> Triple(0f, p.x + 0.5f, p.y * rowHeight(config))
        }
    }

    fun scene(layout: List<Placed>, config: BalanceConfig, camera: IsoCamera, extra: List<IsoObject> = emptyList()): IsoScene {
        val n = config.houseGridSize
        val furniture = layout.mapIndexed { i, p -> toObject(p, i, config) }
        return IsoScene(
            n, n, List(n * n) { IsoCell(FloorType.WOOD) }, decor(camera, config) + furniture + extra,
            wallHeight = WALL_CELLS, floorDepth = FLOOR_DEPTH,
            frame = FrameBox(-ROOM_MARGIN, -ROOM_MARGIN, n + ROOM_MARGIN, n + ROOM_MARGIN, -FLOOR_DEPTH, WALL_CELLS),
        )
    }

    private const val FIXED_ID = "fixed"
    private const val ROOM_MARGIN = 2f // S-14 shows the room at about 40 % of the width, floating in the backdrop
    private const val POST_OUT = 0.3f
    private const val POST_IN = 0.075f
}

/** The rotate and put-away buttons show for a selected piece, but not while a finger is moving it. */
fun showPieceButtons(selected: Int, dragging: Boolean): Boolean = selected >= 0 && !dragging

/** [fits] with the window and lantern cells taken. */
fun fitsRoom(layout: List<Placed>, candidate: Placed, config: BalanceConfig, ignore: Int = -1): Boolean =
    fits(layout + RoomMap.Fixed, candidate, config, ignore)

/** [placeNew] that leaves the window and lantern cells free. */
fun placeNewInRoom(layout: List<Placed>, itemId: String, config: BalanceConfig): List<Placed>? =
    placeNew(layout + RoomMap.Fixed, itemId, config)?.let { it.take(layout.size) + it.last() }

/** [rotate] that does not turn furniture into the fixed cells. Floor items never reach them, so this is [rotate]. */
fun rotateInRoom(layout: List<Placed>, index: Int, config: BalanceConfig): List<Placed>? =
    rotate(layout + RoomMap.Fixed, index, config)?.take(layout.size)

/** Moves item [index] to an engine placement if the room rules allow it. */
fun placeAt(layout: List<Placed>, index: Int, pl: Placement, config: BalanceConfig): List<Placed>? {
    val current = layout.getOrNull(index) ?: return null
    val moved = RoomMap.toPlaced(pl, current)
    return if (fitsRoom(layout, moved, config, index)) layout.toMutableList().also { it[index] = moved } else null
}

/**
 * 집과 섬 장면을 그리는 한 자리. 방은 [editMode]일 때 엔진의 편집(고르기, 끌기, 놓을 수 있는지 색)을 쓴다.
 * [canPlace]와 [onPlace]는 집의 배치 규칙을 거친다. [overlay]는 장면 위의 버튼 자리다.
 */
@Composable
fun RoomScene(
    state: RoomSceneState,
    modifier: Modifier = Modifier,
    config: BalanceConfig = BalanceConfig.DEFAULT,
    view: IsoViewState = rememberIsoViewState(),
    backdrop: Boolean = true,
    editMode: Boolean = false,
    extra: List<IsoObject> = emptyList(),
    onSelect: (Int) -> Unit = {},
    onPlace: (Placement) -> Unit = {},
    canPlace: (Placement) -> Boolean = { false },
    onTapCell: (com.break8t.unknown.ui.iso.IsoHit) -> Unit = {},
    overlay: @Composable IsoOverlayScope.() -> Unit = {},
) {
    val camera = state.camera.iso
    val house = state.place == ScenePlace.HOUSE
    val stages = state.plots.map { it.seedId to it.grown }
    val scene = remember(house, state.layout, camera, extra, stages, state.carId) {
        if (house) RoomMap.scene(state.layout, config, camera, extra) else IslandMap.scene(state.plots, state.carId, extra)
    }
    SideEffect { view.night = state.night && !house }
    // The engine picks on tap and long press; the screen keeps an index, so follow the engine's choice.
    LaunchedEffect(view, editMode) {
        if (editMode) snapshotFlow { view.selectedId }.collect { onSelect(RoomMap.indexOf(it)) }
    }
    Box(if (!backdrop) modifier else modifier.background(if (house) HouseBg else sceneSky(state.night))) {
        if (backdrop) { if (house) PixelNoise(Modifier.fillMaxSize()) else PixelSky(state.night, Modifier.fillMaxSize()) }
        IsoSceneView(
            scene, view, Modifier.fillMaxSize(),
            editMode = editMode, onTap = onTapCell, canPlace = canPlace, onPlace = onPlace, camera = camera, overlay = overlay,
        )
    }
}

@StringRes
fun presetLabel(p: CameraPreset): Int = when (p) {
    CameraPreset.OBLIQUE -> R.string.photo_preset_oblique
    CameraPreset.LOW_FRONT -> R.string.photo_preset_low
    CameraPreset.SIDE -> R.string.photo_preset_side
    CameraPreset.TOP -> R.string.photo_preset_top
}

/**
 * Figma S-14 바탕: 검은 바탕에 파란 픽셀이 흩어져 있고 위쪽일수록 촘촘하다.
 * 표에서 뽑은 고정 무늬라 다시 그려도 바뀌지 않는다.
 */
@Composable
internal fun PixelNoise(modifier: Modifier = Modifier) {
    val cell = max(1f, (NOISE_DP * LocalDensity.current.density).roundToInt().toFloat())
    Canvas(modifier) {
        val cols = (size.width / cell).toInt() + 1
        val rows = (size.height / cell).toInt() + 1
        for (j in 0 until rows) for (i in 0 until cols) {
            var h = i * 73856093 xor j * 19349663
            h *= 0x27d4eb2d
            h = (h xor (h ushr 15)) and 0x7FFFFFFF
            val density = 0.75f - 0.55f * j / rows
            if ((h % 1000) / 1000f > density) continue
            drawRect(NoiseColors[(h / 1000) % NoiseColors.size], Offset(i * cell, j * cell), Size(cell, cell))
        }
    }
}

private const val NOISE_DP = 18f
internal val HouseBg = Color(0xFF020A12)
private val NoiseColors = listOf(
    Color(0xFF01233F), Color(0xFF01233F), Color(0xFF01233F), Color(0xFF004674), Color(0xFF004674),
    Color(0xFF088DB3), Color(0xFF00D3F3), Color(0xFF02142A),
)
