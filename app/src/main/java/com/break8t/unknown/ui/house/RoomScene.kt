package com.break8t.unknown.ui.house

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.break8t.unknown.R
import com.break8t.unknown.config.BalanceConfig
import com.break8t.unknown.config.Catalog
import com.break8t.unknown.ui.theme.AppColors

enum class ScenePlace { HOUSE, ISLAND }

enum class CameraPreset { OBLIQUE, LOW_FRONT, SIDE, TOP }

/** 사진 찍기의 보는 각도. 돌리기는 90도씩 네 번이면 처음으로 돌아온다. */
data class SceneCamera(val preset: CameraPreset = CameraPreset.OBLIQUE, val rotation: Int = 0) {
    fun rotated() = copy(rotation = (rotation + 1) % 4)
}

/** 장면 위에 세운 캐릭터. x, y는 장면 너비와 높이에 대한 비율, size는 높이에 대한 비율이다. */
data class SceneCharacter(val imageId: String, val x: Float, val y: Float, val size: Float, val flipped: Boolean)

/** 장면을 그리는 데 필요한 것 전부. 렌더러가 바뀌어도 이 상태와 RoomScene 시그니처는 그대로 둔다. */
data class RoomSceneState(
    val place: ScenePlace,
    val layout: List<Placed> = emptyList(),
    val camera: SceneCamera = SceneCamera(),
    val night: Boolean = false,
    val selected: Int = -1,
    val character: SceneCharacter? = null,
)

/** 펼친 격자의 칸 묶음. 단위는 칸이다. */
data class GridRect(val col: Int, val row: Int, val w: Int, val h: Int)

/**
 * 자리 표시용 렌더러의 좌표. 방을 위에서 펼쳐 본 모양이라 오른쪽 벽은 바닥 위에,
 * 왼쪽 벽은 바닥 왼쪽에 눕는다. 한 변은 (바닥 칸 수 + 벽 높이)칸이다.
 */
object UnfoldedGrid {
    fun side(config: BalanceConfig) = config.houseGridSize + config.houseWallHeight

    fun rectOf(p: Placed, config: BalanceConfig): GridRect {
        val rows = config.houseWallHeight
        val (w, h) = footprint(p)
        return when (p.surface) {
            Surface.FLOOR -> GridRect(rows + p.x, rows + p.y, w, h)
            Surface.RIGHT_WALL -> GridRect(rows + p.x, rows - (p.y + h), w, h)
            Surface.LEFT_WALL -> GridRect(rows - (p.y + h), rows + p.x, h, w)
        }
    }

    fun cellAt(col: Int, row: Int, config: BalanceConfig): Cell? {
        val rows = config.houseWallHeight
        val side = side(config)
        if (col !in 0 until side || row !in 0 until side) return null
        return when {
            col >= rows && row >= rows -> Cell(Surface.FLOOR, col - rows, row - rows)
            col >= rows -> Cell(Surface.RIGHT_WALL, col - rows, rows - 1 - row)
            row >= rows -> Cell(Surface.LEFT_WALL, row - rows, rows - 1 - col)
            else -> null
        }
    }
}

@DrawableRes
internal fun furnitureImage(itemId: String): Int = when (itemId) {
    "it_sofa" -> R.drawable.house_it_sofa
    "it_picture" -> R.drawable.house_it_picture
    "it_rug" -> R.drawable.house_it_rug
    "it_lamp" -> R.drawable.house_it_lamp
    "it_shelf" -> R.drawable.house_it_shelf
    else -> R.drawable.house_it_plant
}

@DrawableRes
internal fun characterImage(id: String): Int = when (id) {
    "ch_cat" -> R.drawable.photo_ch_cat
    "ch_mush" -> R.drawable.photo_ch_mush
    "ch_cloud" -> R.drawable.photo_ch_cloud
    "ch_firefly" -> R.drawable.photo_ch_firefly
    "pose_wave" -> R.drawable.photo_pose_wave
    "pose_jump" -> R.drawable.photo_pose_jump
    "pose_sit" -> R.drawable.photo_pose_sit
    "pose_side" -> R.drawable.photo_pose_side
    "pose_back" -> R.drawable.photo_pose_back
    else -> R.drawable.photo_ch_sprout
}

/**
 * 집과 섬 장면을 그리는 한 자리. 지금 본문은 자리 표시용이다(위에서 본 격자와 이름표 가구).
 * 실제 렌더러가 정해지면 이 함수 본문만 바꾼다. 콜백이 있으면 눌린 칸과 끌린 칸을 알린다.
 */
@Composable
fun RoomScene(
    state: RoomSceneState,
    modifier: Modifier = Modifier,
    config: BalanceConfig = BalanceConfig.DEFAULT,
    onTapCell: ((Cell?) -> Unit)? = null,
    onDragStartCell: ((Cell?) -> Unit)? = null,
    onDragCell: ((Cell?) -> Unit)? = null,
) {
    val background = when {
        state.place == ScenePlace.HOUSE -> AppColors.Text
        state.night -> NightSky
        else -> AppColors.Sky
    }
    BoxWithConstraints(modifier.background(background)) {
        val sceneH = maxHeight
        val square = minOf(maxWidth, maxHeight) * 0.8f
        Box(Modifier.align(Alignment.Center).size(square).graphicsLayer { rotationZ = 90f * state.camera.rotation }) {
            if (state.place == ScenePlace.HOUSE) {
                HouseGrid(state, square, config, onTapCell, onDragStartCell, onDragCell)
            } else {
                Box(Modifier.fillMaxSize().padding(square * 0.1f).clip(RoundedCornerShape(24.dp)).background(IslandGrass))
            }
        }
        Text(
            stringResource(presetLabel(state.camera.preset)) + " ${state.camera.rotation * 90}°",
            Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp),
            color = AppColors.Card,
            fontSize = 26.sp,
        )
        state.character?.let { c ->
            val s = sceneH * c.size
            Image(
                painterResource(characterImage(c.imageId)),
                Catalog.item(c.imageId)?.name,
                Modifier
                    .offset(maxWidth * c.x - s / 2, sceneH * c.y - s / 2)
                    .size(s)
                    .graphicsLayer { if (c.flipped) scaleX = -1f },
            )
        }
    }
}

@StringRes
fun presetLabel(p: CameraPreset): Int = when (p) {
    CameraPreset.OBLIQUE -> R.string.photo_preset_oblique
    CameraPreset.LOW_FRONT -> R.string.photo_preset_low
    CameraPreset.SIDE -> R.string.photo_preset_side
    CameraPreset.TOP -> R.string.photo_preset_top
}

private val NightSky = Color(0xFF1B2A4A)
private val IslandGrass = Color(0xFF7DBA4A)

@Composable
private fun HouseGrid(
    state: RoomSceneState,
    square: Dp,
    config: BalanceConfig,
    onTapCell: ((Cell?) -> Unit)?,
    onDragStartCell: ((Cell?) -> Unit)?,
    onDragCell: ((Cell?) -> Unit)?,
) {
    val tap by rememberUpdatedState(onTapCell)
    val dragStart by rememberUpdatedState(onDragStartCell)
    val drag by rememberUpdatedState(onDragCell)
    val side = UnfoldedGrid.side(config)
    val unit = square / side
    fun cell(o: Offset, px: Int) = UnfoldedGrid.cellAt((o.x * side / px).toInt(), (o.y * side / px).toInt(), config)
    Box(
        Modifier.fillMaxSize().then(
            if (onTapCell == null) Modifier else Modifier
                .pointerInput(Unit) { detectTapGestures { tap?.invoke(cell(it, size.width)) } }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { dragStart?.invoke(cell(it, size.width)) },
                        onDrag = { change, _ -> drag?.invoke(cell(change.position, size.width)) },
                    )
                },
        ),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val u = size.width / side
            for (col in 0 until side) for (row in 0 until side) {
                val c = UnfoldedGrid.cellAt(col, row, config) ?: continue
                drawRect(if (c.surface == Surface.FLOOR) Floor else Wall, Offset(col * u, row * u), Size(u, u))
                drawRect(AppColors.Line, Offset(col * u, row * u), Size(u, u), style = Stroke(2f))
            }
        }
        state.layout.forEachIndexed { i, p ->
            val r = UnfoldedGrid.rectOf(p, config)
            val sel = i == state.selected
            Box(
                Modifier
                    .offset(unit * r.col, unit * r.row)
                    .size(unit * r.w, unit * r.h)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(AppColors.Card)
                    .border(if (sel) 6.dp else 2.dp, if (sel) AppColors.Selected else AppColors.Line, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) {
                if (p.surface == Surface.FLOOR) FrontMark(p.turns)
                Text(
                    Catalog.item(p.itemId)?.name ?: p.itemId,
                    color = AppColors.Text,
                    fontSize = (unit.value * 0.22f).sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/** 가구 앞쪽을 굵은 띠로 보인다. 돌린 횟수마다 아래, 왼쪽, 위, 오른쪽으로 옮긴다. */
@Composable
private fun FrontMark(turns: Int) {
    val align = when (turns % 4) {
        0 -> Alignment.BottomCenter
        1 -> Alignment.CenterStart
        2 -> Alignment.TopCenter
        else -> Alignment.CenterEnd
    }
    Box(Modifier.fillMaxSize()) {
        val bar = if (turns % 2 == 0) Modifier.size(width = 1000.dp, height = 8.dp) else Modifier.size(width = 8.dp, height = 1000.dp)
        Box(Modifier.align(align).then(bar).background(AppColors.Selected))
    }
}

private val Floor = Color(0xFFC98A4B)
private val Wall = Color(0xFF8A5A33)
