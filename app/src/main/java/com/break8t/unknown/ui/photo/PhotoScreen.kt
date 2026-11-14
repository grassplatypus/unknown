package com.break8t.unknown.ui.photo

import com.break8t.unknown.ui.components.ScreenTitle
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.break8t.unknown.R
import com.break8t.unknown.ui.island.islandSize
import com.break8t.unknown.data.DemoState
import com.break8t.unknown.data.LocalDemoState
import com.break8t.unknown.data.Profile
import com.break8t.unknown.ui.components.AppCard
import com.break8t.unknown.ui.components.BorderWidth
import com.break8t.unknown.ui.components.ControlShape
import com.break8t.unknown.ui.house.CameraPreset
import com.break8t.unknown.ui.house.ChipGap
import com.break8t.unknown.ui.house.HouseStore
import com.break8t.unknown.ui.house.PreviewFrame
import com.break8t.unknown.ui.house.PrimaryIconButton
import com.break8t.unknown.ui.house.RoomScene
import com.break8t.unknown.ui.house.RoomSceneState
import com.break8t.unknown.ui.house.ScenePlace
import com.break8t.unknown.ui.house.ScreenPad
import com.break8t.unknown.ui.house.SquareIconButton
import com.break8t.unknown.ui.house.TopChip
import com.break8t.unknown.ui.house.WindowPreviews
import com.break8t.unknown.ui.house.presetLabel
import com.break8t.unknown.ui.house.trimToOwned
import com.break8t.unknown.config.BalanceConfig
import com.break8t.unknown.ui.island.FarmStore
import com.break8t.unknown.ui.island.IslandMap
import com.break8t.unknown.ui.island.plotViews
import com.break8t.unknown.ui.island.PlotView
import com.break8t.unknown.ui.scene.CharacterCard
import com.break8t.unknown.ui.house.SceneCharacter
import com.break8t.unknown.ui.iso.IsoHit
import com.break8t.unknown.ui.iso.rememberIsoViewState
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.LaunchedEffect
import com.break8t.unknown.ui.theme.AppColors
import java.time.LocalTime

private val PanelWidth = 243.dp
private val PanelGap = 26.dp
private const val CHARACTER_ID = "character"
internal const val ISLAND_CHARACTER_SCALE = 1.6f // the island is framed smaller than the room; S-16-f shows a larger figure

// Where a new character first stands: open ground in front of the house (S-16-f) or the room (S-16-g).
internal val ISLAND_SPAWN = 0.27f to 0.36f
internal val HOUSE_SPAWN = 0.5f to 0.7f

private val PhotoStateSaver = listSaver<PhotoState, Any>({ it.toList() }, { PhotoState.fromList(it) })

/** 사진 찍기(S-16). 촬영하면 같은 장면으로 공유 화면(S-17-a)을 보이고, 다시 찍기로 돌아온다. */
@Composable
fun PhotoScreen(inHouse: Boolean, onClose: () -> Unit) = PhotoScreenImpl(inHouse, onClose)

/**
 * 친구 섬의 모습. 섬은 친구의 차, 크기, 게시판으로 그린다. [characterId]는 친구 캐릭터이고,
 * [friendCharacter]는 친구 섬에 꺼내 둔 그 캐릭터의 자리, 크기, 포즈다(없으면 사진에 나오지 않는다).
 */
data class PhotoVisit(
    val carId: String, val islandSize: Int, val characterId: String, val plots: List<PlotView>, val night: Boolean,
    val board: Boolean = true, val friendCharacter: SceneCharacter? = null,
)

/** 친구 섬 사진 찍기. 친구 캐릭터는 둔 그대로 서 있고, "캐릭터"로는 내 캐릭터를 세운다. 열 때마다 찍기 화면부터 보인다. */
@Composable
fun FriendPhotoScreen(visit: PhotoVisit, onClose: () -> Unit) = PhotoScreenImpl(false, onClose, PhotoState(), visit.night, visit)

@Composable
internal fun PhotoScreenImpl(
    inHouse: Boolean,
    onClose: () -> Unit,
    start: PhotoState = PhotoState(),
    night: Boolean? = null,
    visit: PhotoVisit? = null,
) {
    val profile by LocalDemoState.current.profile.collectAsState()
    val layout by HouseStore.layout.collectAsState()
    val plots by FarmStore.plots.collectAsState()
    var s by rememberSaveable(stateSaver = PhotoStateSaver) { mutableStateOf(start) }
    val isNight = night ?: remember { isNight(profile.islandRealTime, profile.islandNight, LocalTime.now().hour) }
    val islandGrid = visit?.islandSize ?: islandSize(profile.islandLevel, BalanceConfig.DEFAULT)
    val characterId = profile.selectedCharacterId
    val scene = RoomSceneState(
        place = if (inHouse) ScenePlace.HOUSE else ScenePlace.ISLAND,
        layout = if (inHouse) trimToOwned(layout, profile.items) else emptyList(),
        camera = s.camera,
        night = isNight,
        character = s.character,
        plots = visit?.plots ?: plotViews(plots, remember { System.currentTimeMillis() }),
        carId = visit?.carId ?: profile.selectedCarId,
        islandSize = islandGrid,
        board = visit?.board,
    )
    // The house photo shows the room at the size the player bought; the island photo at the size the island was grown to.
    val config = BalanceConfig.DEFAULT.copy(houseGridSize = profile.houseGridSize)
    val grid = if (inHouse) config.houseGridSize else islandGrid
    val extra = photoObjects(
        s.character, characterId, grid, if (inHouse) 1f else ISLAND_CHARACTER_SCALE, s.editing, visit?.friendCharacter, visit?.characterId.orEmpty(),
    )
    val view = rememberIsoViewState()
    // Inside the house the view stays where it is framed: dragging the screen does nothing (11/15 feedback).
    LaunchedEffect(view, inHouse) { if (inHouse) view.resetView() }
    // While the character is being set up it carries the scene's selection frame, as S-16-f shows.
    // Edit mode lets a tap select furniture or nothing, so the frame is put back on the character.
    LaunchedEffect(s.editing) {
        if (!s.editing) view.selectedId = null
        else snapshotFlow { view.selectedId }.collect { if (it != CHARACTER_ID) view.selectedId = CHARACTER_ID }
    }
    BackHandler {
        when {
            s.shot -> s = s.copy(shot = false)
            s.editing -> s = s.copy(editing = false)
            else -> onClose()
        }
    }
    if (s.shot) {
        ShareQrLayout(ShareKind.PHOTO, onClose, onRetake = { s = s.copy(shot = false) }) {
            // 카메라 화면과 같은 비율(1144x901)이라 찍을 때 본 구도가 그대로 보인다.
            RoomScene(
                scene, Modifier.aspectRatio(1144f / 901f, matchHeightConstraintsFirst = true).clip(RoundedCornerShape(10.dp)),
                config = config,
                extra = extra,
                fixedView = inHouse,
                lockScenery = visit != null,
            )
        }
        return
    }
    val options = poseOptions(profile.selectedCharacterId, profile.owned)
    val spawn = when {
        inHouse -> HOUSE_SPAWN
        visit != null -> friendPhotoSpawn(grid)
        else -> ISLAND_SPAWN
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        // While editing, a tap on the ground moves the character there and dragging it carries it along.
        // The character's cell stays (0, 0) with its spot in ox/oy, so a dropped placement is the move in cells.
        RoomScene(
            scene,
            Modifier.fillMaxSize(),
            config = config,
            view = view,
            fixedView = inHouse,
            editMode = s.editing,
            extra = extra,
            canPlace = { it.id == CHARACTER_ID },
            onPlace = { p -> if (p.id == CHARACTER_ID) s = s.moved(p.x / grid.toFloat(), p.y / grid.toFloat()) },
            onTapCell = { hit -> if (s.editing && hit is IsoHit.Cell) s = s.placedAt(hit.x, hit.y, grid) },
            lockScenery = visit != null,
        )
        // 찍히는 범위(Figma S-16의 흰 테두리). 오른쪽 각도 카드와 겹치지 않게 그 왼쪽에 둔다.
        val panelLeft = maxWidth - ScreenPad - PanelWidth
        val f = captureFrame(maxWidth.value, maxHeight.value, panelLeft.value, PanelGap.value)
        Box(Modifier.offset(f.left.dp, f.top.dp).size(f.width.dp, f.height.dp).border(5.dp, Color.White))
        Row(Modifier.padding(ScreenPad), horizontalArrangement = Arrangement.spacedBy(ChipGap)) {
            TopChip(stringResource(R.string.photo_cancel), onClose, icon = R.drawable.house_ic_back)
            ScreenTitle(stringResource(R.string.photo_title))
        }
        if (!s.editing) PresetCard(s, { s = it }, Modifier.align(Alignment.TopEnd).padding(top = 195.dp, end = ScreenPad))
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(ScreenPad)) {
            if (s.editing) {
                CharacterCard(
                    characterId, s.poseId ?: characterId, options,
                    onPose = { s = s.copy(poseId = it) }, onResize = { s = s.resized(it) }, onTurn = { s = s.turned() },
                    onDone = { s = s.copy(editing = false) },
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TopChip(stringResource(R.string.photo_character), { s = s.startCharacter(options, spawn) })
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        PrimaryIconButton(
                            stringResource(R.string.photo_shoot),
                            R.drawable.house_ic_cam,
                            { s = s.copy(shot = true, editing = false) },
                            Modifier.widthIn(min = 364.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PresetCard(s: PhotoState, onChange: (PhotoState) -> Unit, modifier: Modifier = Modifier) {
    AppCard(modifier.width(PanelWidth)) {
        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
            for (p in CameraPreset.entries) {
                val on = s.preset == p
                Box(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 99.dp)
                        .clip(ControlShape)
                        .background(if (on) AppColors.Selected else AppColors.Card)
                        .border(BorderWidth, AppColors.Line, ControlShape)
                        .clickable(role = Role.RadioButton) { onChange(s.copy(preset = p)) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(stringResource(presetLabel(p)), color = AppColors.Text, fontSize = 34.sp, fontWeight = FontWeight.Bold)
                }
            }
            SquareIconButton(
                R.drawable.photo_ic_rotate,
                stringResource(R.string.photo_rotate),
                { onChange(s.rotated()) },
                Modifier.fillMaxWidth(),
                size = 99.dp,
                iconSize = 57.dp,
            )
        }
    }
}

// S-16 상태마다 하나씩. 그림은 복셀 엔진이 그린다.
@WindowPreviews @Composable private fun ObliquePreview() = PreviewFrame { PhotoScreenImpl(false, {}, night = false) }
@WindowPreviews @Composable private fun RotatedPreview() = PreviewFrame { PhotoScreenImpl(false, {}, PhotoState(rotation = 1), night = false) }
@WindowPreviews @Composable private fun TopPreview() = PreviewFrame { PhotoScreenImpl(false, {}, PhotoState(preset = CameraPreset.TOP), night = false) }
@WindowPreviews @Composable private fun LowPreview() = PreviewFrame { PhotoScreenImpl(false, {}, PhotoState(preset = CameraPreset.LOW_FRONT), night = false) }
@WindowPreviews @Composable private fun SidePreview() = PreviewFrame { PhotoScreenImpl(false, {}, PhotoState(preset = CameraPreset.SIDE), night = false) }
@WindowPreviews @Composable private fun NightPreview() = PreviewFrame { PhotoScreenImpl(false, {}, night = true) }
@WindowPreviews @Composable private fun CharacterPreview() =
    PreviewFrame { PhotoScreenImpl(false, {}, PhotoState(poseId = "pose_wave", editing = true), night = false) }
@WindowPreviews @Composable private fun CatPosePreview() = PreviewFrame {
    CompositionLocalProvider(LocalDemoState provides DemoState(Profile(selectedCharacterId = "ch_cat"))) {
        PhotoScreenImpl(false, {}, PhotoState(poseId = "pose_wave", editing = true), night = false)
    }
}
@WindowPreviews @Composable private fun InHousePreview() = PreviewFrame { PhotoScreenImpl(true, {}, PhotoState(poseId = "pose_wave"), night = false) }
@WindowPreviews @Composable private fun InHouseRotatedPreview() =
    PreviewFrame { PhotoScreenImpl(true, {}, PhotoState(rotation = 1, poseId = "pose_wave"), night = false) }
@WindowPreviews @Composable private fun SharePhotoPreview() =
    PreviewFrame { PhotoScreenImpl(true, {}, PhotoState(poseId = "pose_wave", shot = true), night = false) }
