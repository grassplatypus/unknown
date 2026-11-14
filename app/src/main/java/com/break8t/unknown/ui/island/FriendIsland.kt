package com.break8t.unknown.ui.island

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.break8t.unknown.R
import com.break8t.unknown.config.BalanceConfig
import com.break8t.unknown.ui.components.ScreenTitle
import com.break8t.unknown.ui.guestbook.FriendGuestbookPanel
import com.break8t.unknown.ui.house.ChipGap
import com.break8t.unknown.ui.house.DefaultLayout
import com.break8t.unknown.ui.house.Placed
import com.break8t.unknown.ui.house.RoomScene
import com.break8t.unknown.ui.house.RoomSceneState
import com.break8t.unknown.ui.house.ScenePlace
import com.break8t.unknown.ui.house.ScreenPad
import com.break8t.unknown.ui.house.TopChip
import com.break8t.unknown.ui.house.placeNewInRoom
import com.break8t.unknown.ui.iso.IsoHit
import com.break8t.unknown.ui.iso.IsoOverlayScope
import com.break8t.unknown.ui.iso.rememberIsoViewState
import com.break8t.unknown.ui.photo.FriendPhotoScreen
import com.break8t.unknown.ui.photo.HOUSE_SPAWN
import com.break8t.unknown.ui.photo.ISLAND_CHARACTER_SCALE
import com.break8t.unknown.ui.photo.ISLAND_SPAWN
import com.break8t.unknown.ui.photo.PhotoScreenImpl
import com.break8t.unknown.ui.photo.PhotoState
import com.break8t.unknown.ui.photo.PhotoVisit
import com.break8t.unknown.ui.scene.CharacterCard
import com.break8t.unknown.ui.scene.FriendVisitRule
import com.break8t.unknown.ui.scene.SceneStage
import com.break8t.unknown.ui.scene.StageBounds
import com.break8t.unknown.ui.scene.StashItem
import com.break8t.unknown.ui.scene.StashKind
import com.break8t.unknown.ui.scene.StashPanel
import com.break8t.unknown.ui.scene.allPoseOptions
import com.break8t.unknown.ui.scene.photoCharacter
import com.break8t.unknown.ui.scene.stageObjects
import kotlin.random.Random

/** 친구 섬의 모습. 친구 데이터는 견본이라 텃밭은 내 섬 견본을 쓰고, 밤낮은 실제 시각을 따른다. */
fun friendVisit(
    carId: String, islandLevel: Int, characterId: String, now: Long, hour: Int, board: Boolean = true, cfg: BalanceConfig = BalanceConfig.DEFAULT,
) = PhotoVisit(
    carId, islandSize(islandLevel, cfg), characterId, plotViews(demoPlots(now, cfg.farmGridSize), now),
    isNight(realTime = true, manualNight = false, hour = hour), board,
)

/** 친구 섬의 보관함: 섬 주인 캐릭터 하나뿐이다. */
fun friendStash(characterId: String) = listOf(StashItem(characterId, StashKind.CHARACTER))

/** 친구 섬에서 누르면 들어가는 곳. 라벨 칩이 그 위에 선다. */
enum class FriendSpot { HOUSE, BOARD }

/** 라벨을 세울 곳. 게시판은 친구가 섬에 둔 때만 있고, 캐릭터를 조종하는 동안에는 라벨을 모두 내린다. */
fun friendSpots(board: Boolean, steering: Boolean): List<FriendSpot> =
    if (steering) emptyList() else listOfNotNull(FriendSpot.HOUSE, FriendSpot.BOARD.takeIf { board })

/** 장면을 누른 곳이 친구 집이나 게시판이면 그것. */
fun friendSpotAt(hit: IsoHit, board: Boolean): FriendSpot? = when {
    hit !is IsoHit.Item -> null
    hit.id == HOUSE_ID -> FriendSpot.HOUSE
    board && hit.id == IslandMap.BOARD_ID -> FriendSpot.BOARD
    else -> null
}

/** 친구 집의 견본 배치: 기본 배치에 친구마다 정해진 가구 둘을 더한다. 같은 친구는 늘 같은 방이다. */
fun friendHouseLayout(friendId: String, config: BalanceConfig = BalanceConfig.DEFAULT): List<Placed> =
    FriendHouseExtras.shuffled(Random(friendId.hashCode())).take(FRIEND_HOUSE_EXTRAS)
        .fold(DefaultLayout) { layout, id -> placeNewInRoom(layout, id, config) ?: layout }

private val FriendHouseExtras = listOf("it_rug", "it_lamp", "it_shelf", "it_plant", "it_picture")
private const val FRIEND_HOUSE_EXTRAS = 2
private const val HOUSE_ID = "house" // IslandMap.scene's id for the house

/** 친구 집의 캐릭터 이동 범위. 방은 기본 크기이고 캐릭터는 내 집 사진과 같은 크기다. */
fun friendHouseBounds(config: BalanceConfig = BalanceConfig.DEFAULT) = StageBounds(config.houseGridSize)

/**
 * 친구 섬 구경(상점, 텃밭, 넓히기 없음). 집, 가구, 사물은 옮길 수 없고, 보관함에서 친구 캐릭터만 꺼내 조종한다.
 * 집과 게시판에는 라벨이 서고, 누르면 친구 집([onHouse])과 친구 방명록으로 간다.
 * [stage]는 부르는 쪽이 들고 있어서 사진 찍기나 친구 집에 다녀와도 남는다. [header]는 돌아가기 칩과 제목이다.
 */
@Composable
fun FriendIslandScreen(
    visit: PhotoVisit,
    friendId: String,
    friendNickname: String,
    stage: SceneStage,
    onStage: (SceneStage) -> Unit,
    onPhoto: () -> Unit,
    onHouse: () -> Unit,
    header: @Composable () -> Unit,
) = Box(Modifier.fillMaxSize()) {
    var guestbook by rememberSaveable { mutableStateOf(false) }
    val open: (FriendSpot) -> Unit = { spot ->
        onStage(stage.select(null))
        when (spot) {
            FriendSpot.HOUSE -> onHouse()
            FriendSpot.BOARD -> guestbook = true
        }
    }
    val spots = friendSpots(visit.board, steering = stage.selected != null)
    FriendScene(
        RoomSceneState(ScenePlace.ISLAND, night = visit.night, plots = visit.plots, carId = visit.carId, islandSize = visit.islandSize, board = visit.board),
        StageBounds(visit.islandSize, ISLAND_CHARACTER_SCALE), ISLAND_SPAWN, visit.characterId, stage, onStage, header,
        chips = { TopChip(stringResource(R.string.island_photo), onPhoto, icon = R.drawable.house_ic_cam) },
        tappable = if (spots.isEmpty()) emptySet() else setOf(HOUSE_ID, IslandMap.BOARD_ID),
        onTapScenery = { hit -> friendSpotAt(hit, visit.board)?.let(open) },
    ) {
        for (spot in spots) {
            // Same chips and points as my island (S-12): above the roof and above the board.
            val (p, align) = if (spot == FriendSpot.HOUSE) IslandMap.anchor(IslandSpot.HOUSE) else IslandMap.BOARD_ANCHOR to Alignment.BottomCenter
            val text = if (spot == FriendSpot.HOUSE) R.string.island_house else R.string.guestbook_chip
            Box(Modifier.isoAnchor(p[0], p[1], p[2], align)) {
                IslandChip(stringResource(text), Modifier.padding(bottom = 16.dp), onClick = { open(spot) })
            }
        }
    }
    if (guestbook) {
        BackHandler { guestbook = false }
        FriendGuestbookPanel(friendId, friendNickname, onClose = { guestbook = false })
    }
}

/** 친구 집 구경. 가구는 친구의 견본 배치 그대로 두고, 친구 섬처럼 보관함에서 친구 캐릭터를 꺼내 조종하고 사진을 찍는다. */
@Composable
fun FriendHouseScreen(
    friendId: String,
    characterId: String,
    stage: SceneStage,
    onStage: (SceneStage) -> Unit,
    onPhoto: () -> Unit,
    header: @Composable () -> Unit,
) {
    val layout = remember(friendId) { friendHouseLayout(friendId) }
    FriendScene(
        RoomSceneState(ScenePlace.HOUSE, layout), friendHouseBounds(), HOUSE_SPAWN, characterId, stage, onStage, header,
        chips = { TopChip(stringResource(R.string.island_photo), { onStage(stage.select(null)); onPhoto() }, icon = R.drawable.house_ic_cam) },
    )
}

/** 친구 섬과 친구 집이 같이 쓰는 장면: 잠긴 장면 위에 친구 캐릭터를 꺼내 조종한다. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FriendScene(
    state: RoomSceneState,
    bounds: StageBounds,
    spawn: Pair<Float, Float>,
    characterId: String,
    stage: SceneStage,
    onStage: (SceneStage) -> Unit,
    header: @Composable () -> Unit,
    chips: @Composable () -> Unit = {},
    tappable: Set<String> = emptySet(),
    onTapScenery: (IsoHit) -> Unit = {},
    overlay: @Composable IsoOverlayScope.() -> Unit = {},
) = Box(Modifier.fillMaxSize()) {
    val selected = stage.selected
    val extra = remember(stage.pieces, bounds) { stageObjects(stage, bounds) }
    val view = rememberIsoViewState()
    BackHandler(enabled = stage.back() != null) { stage.back()?.let(onStage) }
    // The piece being steered keeps the selection frame (as in the photo); a tap on the floor would clear it.
    LaunchedEffect(view, selected) {
        if (selected == null) view.selectedId = null
        else snapshotFlow { view.selectedId }.collect { if (it != selected) view.selectedId = selected }
    }
    RoomScene(
        state,
        Modifier.fillMaxSize(),
        view = view,
        editMode = selected != null,
        extra = extra,
        canPlace = { it.id == selected },
        // A character keeps cell (0, 0) with its spot in ox/oy, so a dropped placement is the move in cells.
        onPlace = { p -> if (p.id == selected) onStage(stage.moveBy(p.id, p.x / bounds.grid.toFloat(), p.y / bounds.grid.toFloat(), bounds)) },
        onTapCell = { hit ->
            when {
                hit is IsoHit.Item && stage.isOut(hit.id) -> onStage(stage.select(hit.id))
                hit is IsoHit.Cell && selected != null -> onStage(stage.placeAtCell(selected, hit.x, hit.y, bounds))
                selected == null -> onTapScenery(hit)
            }
        },
        lockScenery = true,
        tappable = tappable,
        overlay = overlay,
    )
    Column(Modifier.fillMaxSize().padding(ScreenPad), verticalArrangement = Arrangement.spacedBy(ChipGap)) {
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ChipGap), verticalArrangement = Arrangement.spacedBy(ChipGap)) {
            header()
            Spacer(Modifier.weight(1f))
            chips()
            TopChip(stringResource(R.string.scene_stash), { onStage(stage.togglePanel()) }, icon = R.drawable.house_ic_box, on = stage.panelOpen)
        }
        Spacer(Modifier.weight(1f))
        val piece = stage.piece(selected)
        when {
            stage.panelOpen -> StashPanel(
                stage.inStash(friendStash(characterId)), FriendVisitRule, stringResource(R.string.scene_friend_note),
                onTakeOut = { onStage(stage.takeOut(it, FriendVisitRule, spawn, bounds)) },
            )
            piece != null -> CharacterCard(
                piece.itemId, piece.look, allPoseOptions(piece.itemId),
                onPose = { onStage(stage.pose(piece.itemId, it)) },
                onResize = { onStage(stage.resize(piece.itemId, it, bounds)) },
                onTurn = { onStage(stage.turn(piece.itemId)) },
                onDone = { onStage(stage.select(null)) },
                onPutAway = { onStage(stage.putAway(piece.itemId)) },
            )
        }
    }
}

// 상태마다 하나씩: 처음(라벨, 캐릭터는 보관함에), 게시판 없는 친구, 보관함 열림, 조종 중(라벨 없음), 꺼낸 채 카드 닫음,
// 모두 꺼낸 보관함, 친구 집(처음, 조종 중), 사진 찍기(친구 캐릭터와 내 캐릭터)와 공유. 방명록 패널은 GuestbookPreviews에 있다.
private const val PREVIEW_FRIEND = "f_acorn"
private val previewVisit = friendVisit("car_blue", 1, "ch_cat", 0L, hour = 12)
private val outStage = SceneStage().takeOut(friendStash("ch_cat").first(), FriendVisitRule, ISLAND_SPAWN, StageBounds(previewVisit.islandSize, ISLAND_CHARACTER_SCALE))
private val houseOut = SceneStage().takeOut(friendStash("ch_cat").first(), FriendVisitRule, HOUSE_SPAWN, friendHouseBounds())
private val photoVisit = previewVisit.copy(friendCharacter = outStage.photoCharacter("ch_cat"))

@Composable
private fun FriendIslandPreviewOf(start: SceneStage, visit: PhotoVisit = previewVisit) = IslandPreviewFrame {
    var stage by remember { mutableStateOf(start) }
    FriendIslandScreen(visit, PREVIEW_FRIEND, "도토리길", stage, { stage = it }, {}, {}) { ScreenTitle("도토리길의 섬") }
}

@Composable
private fun FriendHousePreviewOf(start: SceneStage) = IslandPreviewFrame {
    var stage by remember { mutableStateOf(start) }
    FriendHouseScreen(PREVIEW_FRIEND, "ch_cat", stage, { stage = it }, {}) { ScreenTitle("도토리길의 집") }
}

@IslandPreviews @Composable private fun FriendIslandPreview() = FriendIslandPreviewOf(SceneStage())
@IslandPreviews @Composable private fun FriendNoBoardPreview() = FriendIslandPreviewOf(SceneStage(), previewVisit.copy(board = false))
@IslandPreviews @Composable private fun FriendStashPreview() = FriendIslandPreviewOf(SceneStage(panelOpen = true))
@IslandPreviews @Composable private fun FriendSteerPreview() = FriendIslandPreviewOf(outStage)
@IslandPreviews @Composable private fun FriendPosedPreview() =
    FriendIslandPreviewOf(outStage.pose("ch_cat", "pose_wave").resize("ch_cat", 1, StageBounds(previewVisit.islandSize, ISLAND_CHARACTER_SCALE)).select(null))
@IslandPreviews @Composable private fun FriendStashEmptyPreview() = FriendIslandPreviewOf(outStage.togglePanel())
@IslandPreviews @Composable private fun FriendHousePreview() = FriendHousePreviewOf(SceneStage())
@IslandPreviews @Composable private fun FriendHouseSteerPreview() = FriendHousePreviewOf(houseOut)
@IslandPreviews @Composable private fun FriendHousePhotoPreview() = IslandPreviewFrame {
    FriendPhotoScreen(previewVisit.copy(houseLayout = friendHouseLayout(PREVIEW_FRIEND), friendCharacter = houseOut.photoCharacter("ch_cat")), {}, inHouse = true)
}
@IslandPreviews @Composable private fun FriendPhotoPreview() = IslandPreviewFrame { FriendPhotoScreen(photoVisit, {}) }
@IslandPreviews @Composable private fun FriendPhotoBothPreview() = IslandPreviewFrame {
    PhotoScreenImpl(false, {}, PhotoState(poseId = "ch_sprout", editing = true).copy(x = 0.375f, y = 0.375f), photoVisit.night, photoVisit)
}
@IslandPreviews @Composable private fun FriendSharePreview() = IslandPreviewFrame {
    PhotoScreenImpl(false, {}, PhotoState(poseId = "ch_sprout", shot = true, x = 0.375f, y = 0.375f), photoVisit.night, photoVisit)
}
