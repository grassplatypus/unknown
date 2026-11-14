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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.break8t.unknown.R
import com.break8t.unknown.config.BalanceConfig
import com.break8t.unknown.ui.components.ScreenTitle
import com.break8t.unknown.ui.house.ChipGap
import com.break8t.unknown.ui.house.RoomScene
import com.break8t.unknown.ui.house.RoomSceneState
import com.break8t.unknown.ui.house.ScenePlace
import com.break8t.unknown.ui.house.ScreenPad
import com.break8t.unknown.ui.house.TopChip
import com.break8t.unknown.ui.iso.IsoHit
import com.break8t.unknown.ui.iso.rememberIsoViewState
import com.break8t.unknown.ui.photo.FriendPhotoScreen
import com.break8t.unknown.ui.photo.ISLAND_CHARACTER_SCALE
import com.break8t.unknown.ui.photo.ISLAND_SPAWN
import com.break8t.unknown.ui.photo.PhotoScreenImpl
import com.break8t.unknown.ui.photo.PhotoVisit
import com.break8t.unknown.ui.scene.photoStart
import com.break8t.unknown.ui.scene.CharacterCard
import com.break8t.unknown.ui.scene.FriendVisitRule
import com.break8t.unknown.ui.scene.SceneStage
import com.break8t.unknown.ui.scene.StageBounds
import com.break8t.unknown.ui.scene.StashItem
import com.break8t.unknown.ui.scene.StashKind
import com.break8t.unknown.ui.scene.StashPanel
import com.break8t.unknown.ui.scene.allPoseOptions
import com.break8t.unknown.ui.scene.stageObjects

/** 친구 섬의 모습. 친구 데이터는 견본이라 텃밭은 내 섬 견본을 쓰고, 밤낮은 실제 시각을 따른다. */
fun friendVisit(carId: String, islandLevel: Int, characterId: String, now: Long, hour: Int, cfg: BalanceConfig = BalanceConfig.DEFAULT) =
    PhotoVisit(
        carId, islandSize(islandLevel, cfg), characterId, plotViews(demoPlots(now, cfg.farmGridSize), now),
        isNight(realTime = true, manualNight = false, hour = hour),
    )

/** 친구 섬의 보관함: 섬 주인 캐릭터 하나뿐이다. */
fun friendStash(characterId: String) = listOf(StashItem(characterId, StashKind.CHARACTER))

/**
 * 친구 섬 구경(상점, 텃밭, 넓히기 없음). 집, 가구, 사물은 고르거나 옮길 수 없고, 보관함에서 친구 캐릭터만 꺼내 조종한다.
 * [stage]는 부르는 쪽이 들고 있어서 사진 찍기에 다녀와도 남는다. [header]는 돌아가기 칩과 제목이다.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FriendIslandScreen(
    visit: PhotoVisit,
    stage: SceneStage,
    onStage: (SceneStage) -> Unit,
    onPhoto: () -> Unit,
    header: @Composable () -> Unit,
) = Box(Modifier.fillMaxSize()) {
    val bounds = StageBounds(visit.islandSize, ISLAND_CHARACTER_SCALE)
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
        RoomSceneState(ScenePlace.ISLAND, night = visit.night, plots = visit.plots, carId = visit.carId, islandSize = visit.islandSize),
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
            }
        },
        lockScenery = true,
    )
    Column(Modifier.fillMaxSize().padding(ScreenPad), verticalArrangement = Arrangement.spacedBy(ChipGap)) {
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ChipGap), verticalArrangement = Arrangement.spacedBy(ChipGap)) {
            header()
            Spacer(Modifier.weight(1f))
            TopChip(stringResource(R.string.island_photo), onPhoto, icon = R.drawable.house_ic_cam)
            TopChip(stringResource(R.string.scene_stash), { onStage(stage.togglePanel()) }, icon = R.drawable.house_ic_box)
        }
        Spacer(Modifier.weight(1f))
        val piece = stage.piece(selected)
        when {
            stage.panelOpen -> StashPanel(
                stage.inStash(friendStash(visit.characterId)), FriendVisitRule, stringResource(R.string.scene_friend_note),
                onTakeOut = { onStage(stage.takeOut(it, FriendVisitRule, ISLAND_SPAWN, bounds)) },
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

// 상태마다 하나씩: 처음(캐릭터는 보관함에), 보관함 열림, 꺼내서 조종 중, 꺼낸 채 카드 닫음, 모두 꺼낸 보관함, 사진 찍기와 공유.
private val previewVisit = friendVisit("car_blue", 1, "ch_cat", 0L, hour = 12)
private val outStage = SceneStage().takeOut(friendStash("ch_cat").first(), FriendVisitRule, ISLAND_SPAWN, StageBounds(previewVisit.islandSize, ISLAND_CHARACTER_SCALE))

@Composable
private fun FriendIslandPreviewOf(start: SceneStage) = IslandPreviewFrame {
    var stage by remember { mutableStateOf(start) }
    FriendIslandScreen(previewVisit, stage, { stage = it }, {}) { ScreenTitle("도토리길의 섬") }
}

@IslandPreviews @Composable private fun FriendIslandPreview() = FriendIslandPreviewOf(SceneStage())
@IslandPreviews @Composable private fun FriendStashPreview() = FriendIslandPreviewOf(SceneStage(panelOpen = true))
@IslandPreviews @Composable private fun FriendSteerPreview() = FriendIslandPreviewOf(outStage)
@IslandPreviews @Composable private fun FriendPosedPreview() =
    FriendIslandPreviewOf(outStage.pose("ch_cat", "pose_wave").resize("ch_cat", 1, StageBounds(previewVisit.islandSize, ISLAND_CHARACTER_SCALE)).select(null))
@IslandPreviews @Composable private fun FriendStashEmptyPreview() = FriendIslandPreviewOf(outStage.togglePanel())
@IslandPreviews @Composable private fun FriendPhotoPreview() =
    IslandPreviewFrame { FriendPhotoScreen(previewVisit, outStage.photoStart("ch_cat"), {}) }
@IslandPreviews @Composable private fun FriendSharePreview() = IslandPreviewFrame {
    PhotoScreenImpl(false, {}, outStage.photoStart("ch_cat").copy(shot = true), previewVisit.night, previewVisit)
}
