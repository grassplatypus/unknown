package com.break8t.unknown.ui.house

import com.break8t.unknown.ui.components.ScreenTitle
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.break8t.unknown.R
import com.break8t.unknown.config.BalanceConfig
import com.break8t.unknown.config.Catalog
import com.break8t.unknown.config.ItemKind
import com.break8t.unknown.data.LocalDemoState
import com.break8t.unknown.ui.components.AppCard
import com.break8t.unknown.ui.components.AppChip
import com.break8t.unknown.ui.components.AppDialog
import com.break8t.unknown.ui.components.AppToast
import com.break8t.unknown.ui.components.BorderWidth
import com.break8t.unknown.ui.components.ControlShape
import com.break8t.unknown.ui.components.DialogPanel
import com.break8t.unknown.ui.theme.AppColors
import com.break8t.unknown.ui.iso.IsoShapes
import com.break8t.unknown.ui.iso.IsoThumbnail
import com.break8t.unknown.ui.iso.rememberIsoViewState
import kotlinx.coroutines.flow.MutableStateFlow

/** 저장한 배치. Profile에 자리가 없어서 DemoStore처럼 프로세스에 하나만 둔다(시연용). */
object HouseStore {
    val layout = MutableStateFlow(DefaultLayout)
}

enum class StorageTab { ALL, FLOOR, WALL }

/** 집 안(S-14)과 집 꾸미기(S-15). 꾸미는 중인 배치는 저장하기 전까지 HouseStore에 쓰지 않는다. */
@Composable
fun HouseScreen(onBack: () -> Unit, onOpenInventory: () -> Unit, onTakePhoto: () -> Unit) =
    HouseScreenImpl(onBack, onOpenInventory, onTakePhoto)

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun HouseScreenImpl(
    onBack: () -> Unit,
    onOpenInventory: () -> Unit,
    onTakePhoto: () -> Unit,
    startDraft: List<Placed>? = null,
    startSelected: Int = -1,
    startToast: String? = null,
    config: BalanceConfig = BalanceConfig.DEFAULT,
) {
    val profile by LocalDemoState.current.profile.collectAsState()
    val storedLayout by HouseStore.layout.collectAsState()
    val saved = trimToOwned(storedLayout, profile.items)

    // 화면을 나누면 Activity가 다시 만들어지므로 꾸미는 중인 배치도 저장 가능한 형태로 둔다.
    var draftCodes by rememberSaveable { mutableStateOf(startDraft?.let(::encode)) }
    val draft = draftCodes?.let(::decode)
    var selected by rememberSaveable { mutableIntStateOf(startSelected) }
    var tab by rememberSaveable { mutableStateOf(StorageTab.ALL) }
    var askExit by rememberSaveable { mutableStateOf(false) }
    var askReset by rememberSaveable { mutableStateOf(false) }
    var toast by remember { mutableStateOf(startToast) }
    val view = rememberIsoViewState()
    remember(view) { if (startSelected >= 0) view.selectedId = RoomMap.idOf(startSelected) }

    val noSpace = stringResource(R.string.house_no_space)
    val noTurn = stringResource(R.string.house_no_turn)
    val resetDone = stringResource(R.string.house_reset_done)

    fun setDraft(l: List<Placed>?) { draftCodes = l?.let(::encode) }
    fun select(i: Int) {
        selected = i
        view.selectedId = if (i >= 0) RoomMap.idOf(i) else null
    }
    fun finish(save: Boolean) {
        if (draft != null) HouseStore.layout.value = resolveExit(saved, draft, save)
        setDraft(null)
        select(-1)
        askExit = false
    }
    fun tryExit() {
        if (draft == null) return
        when (exitAction(saved, draft)) {
            ExitAction.LEAVE -> finish(save = false)
            ExitAction.ASK_SAVE -> askExit = true
        }
    }
    BackHandler(enabled = draft != null) { tryExit() }

    // S-14: the pixel backdrop fills the whole screen, behind the chips and the tray too.
    Box(Modifier.fillMaxSize().background(HouseBg)) {
        PixelNoise(Modifier.fillMaxSize())
        Column(Modifier.fillMaxSize().padding(ScreenPad), verticalArrangement = Arrangement.spacedBy(ChipGap)) {
            FlowRow(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ChipGap),
                verticalArrangement = Arrangement.spacedBy(ChipGap),
            ) {
                if (draft == null) {
                    TopChip(stringResource(R.string.house_to_island), onBack, icon = R.drawable.house_ic_back)
                    ScreenTitle(stringResource(R.string.house_title))
                    Spacer(Modifier.weight(1f))
                    TopChip(stringResource(R.string.house_take_photo), onTakePhoto, icon = R.drawable.house_ic_cam)
                    TopChip(stringResource(R.string.house_decorate), { setDraft(saved) })
                } else {
                    TopChip(stringResource(R.string.house_exit), ::tryExit, icon = R.drawable.house_ic_back)
                    TopChip(stringResource(R.string.house_decorate), null)
                    Spacer(Modifier.weight(1f))
                    TopChip(stringResource(R.string.house_reset), { askReset = true })
                    TopChip(stringResource(R.string.house_save), { finish(save = true) })
                }
            }
            if (draft == null) {
                RoomScene(RoomSceneState(ScenePlace.HOUSE, saved), Modifier.weight(1f).fillMaxWidth(), config, view, backdrop = false)
            } else {
                RoomScene(
                    RoomSceneState(ScenePlace.HOUSE, draft, selected = selected),
                    Modifier.weight(1f).fillMaxWidth(),
                    config,
                    view,
                    backdrop = false,
                    editMode = true,
                    onSelect = { selected = it },
                    canPlace = { pl -> placeAt(draft, RoomMap.indexOf(pl.id), pl, config) != null },
                    onPlace = { pl ->
                        val current = draftCodes?.let(::decode)
                        val i = RoomMap.indexOf(pl.id)
                        current?.let { placeAt(it, i, pl, config) }?.let { setDraft(it); select(i) }
                    },
                ) {
                    // Under the selected piece, as in S-15-a; the engine projects its front corner.
                    draft.getOrNull(selected)?.takeIf { showPieceButtons(selected, view.dragging) }?.let { p ->
                        val (x, y, z) = RoomMap.buttonAnchor(p, config)
                        Row(
                            Modifier.isoAnchor(x, y, z, Alignment.TopCenter).padding(top = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            if (!specOf(p.itemId).onWall) {
                                SquareIconButton(R.drawable.house_ic_turn, stringResource(R.string.house_turn), {
                                    rotateInRoom(draft, selected, config)?.let(::setDraft) ?: run { toast = noTurn }
                                })
                            }
                            SquareIconButton(R.drawable.house_ic_box, stringResource(R.string.house_put_away), {
                                setDraft(storeBack(draft, selected))
                                select(-1)
                            })
                        }
                    }
                }
                StorageCard(profile.items, draft, tab, { tab = it }) { id ->
                    val placed = placeNewInRoom(draft, id, config)
                    if (placed == null) toast = noSpace else {
                        setDraft(placed)
                        select(placed.lastIndex)
                    }
                }
            }
        }
        TimedToast(toast, { toast = null }, Modifier.align(Alignment.BottomCenter).padding(bottom = 140.dp))
    }

    if (askExit) {
        ChoiceDialog(
            stringResource(R.string.house_exit_title),
            stringResource(R.string.house_exit_body),
            stringResource(R.string.house_exit_discard),
            stringResource(R.string.house_save),
            onDismissButton = { finish(save = false) },
            onConfirm = { finish(save = true) },
            onOutside = { askExit = false },
        )
    }
    if (askReset) {
        AppDialog(
            stringResource(R.string.house_reset_title),
            stringResource(R.string.house_reset_body),
            stringResource(R.string.house_reset),
            onConfirm = {
                setDraft(reset())
                select(-1)
                askReset = false
                toast = resetDone
            },
            onDismiss = { askReset = false },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StorageCard(
    owned: Map<String, Int>,
    draft: List<Placed>,
    tab: StorageTab,
    onTab: (StorageTab) -> Unit,
    onPick: (String) -> Unit,
) {
    val entries = Catalog.ofKind(ItemKind.FURNITURE)
        .filter {
            val wall = specOf(it.id).onWall
            tab == StorageTab.ALL || (tab == StorageTab.WALL) == wall
        }
        .map { it to available(owned, draft, it.id) }
        .filter { it.second > 0 }
    AppCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(23.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AppChip(stringResource(R.string.house_tab_all), tab == StorageTab.ALL, { onTab(StorageTab.ALL) })
                AppChip(stringResource(R.string.house_tab_floor), tab == StorageTab.FLOOR, { onTab(StorageTab.FLOOR) })
                AppChip(stringResource(R.string.house_tab_wall), tab == StorageTab.WALL, { onTab(StorageTab.WALL) })
            }
            // DemoState는 메모리에 바로 있어서 불러오는 단계가 없다. 그래서 0개면 곧 빈 상태다.
            // 보이지 않는 칸 하나를 깔아서, 마지막 가구를 놓아도 카드 높이가 그대로다.
            if (entries.isEmpty()) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Box(Modifier.alpha(0f).clearAndSetSemantics {}) { StorageTile("it_plant", " ", 0, null) }
                    Text(stringResource(R.string.house_storage_empty), color = AppColors.TextSub, fontSize = 31.sp)
                }
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(21.dp)) {
                    items(entries, key = { it.first.id }) { (item, count) ->
                        StorageTile(item.id, item.name, count) { onPick(item.id) }
                    }
                }
            }
        }
    }
}

@Composable
private fun StorageTile(id: String, name: String, count: Int, onClick: (() -> Unit)?) {
    Column(
        Modifier
            .width(166.dp)
            .clip(ControlShape)
            .background(AppColors.Card)
            .border(BorderWidth, AppColors.Line, ControlShape)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IsoThumbnail(IsoShapes.of(id) ?: IsoShapes.plant, Modifier.size(99.dp))
        Text(name, color = AppColors.Text, fontSize = 31.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        Text(stringResource(R.string.house_count, count), color = AppColors.TextSub, fontSize = 29.sp)
    }
}

@WindowPreviews
@Composable
private fun HouseViewPreview() = PreviewFrame { HouseScreenImpl({}, {}, {}) }

@WindowPreviews
@Composable
private fun DecorateSelectedPreview() = PreviewFrame { HouseScreenImpl({}, {}, {}, startDraft = DefaultLayout, startSelected = 0) }

@WindowPreviews
@Composable
private fun DecorateResetPreview() =
    PreviewFrame { HouseScreenImpl({}, {}, {}, startDraft = reset(), startToast = stringResource(R.string.house_reset_done)) }

@WindowPreviews
@Composable
private fun ExitDialogPreview() = PreviewFrame {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        ChoicePanel(
            stringResource(R.string.house_exit_title),
            stringResource(R.string.house_exit_body),
            stringResource(R.string.house_exit_discard),
            stringResource(R.string.house_save),
            {},
            {},
        )
    }
}

@WindowPreviews
@Composable
private fun ResetDialogPreview() = PreviewFrame {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        DialogPanel(stringResource(R.string.house_reset_title), stringResource(R.string.house_reset_body), stringResource(R.string.house_reset), {}, {})
    }
}

@WindowPreviews
@Composable
private fun ResetToastPreview() = PreviewFrame {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { AppToast(stringResource(R.string.house_reset_done)) }
}
