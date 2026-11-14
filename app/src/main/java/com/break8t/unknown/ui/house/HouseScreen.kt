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
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.break8t.unknown.R
import com.break8t.unknown.config.BalanceConfig
import com.break8t.unknown.config.Catalog
import com.break8t.unknown.config.ItemKind
import com.break8t.unknown.data.DemoState
import com.break8t.unknown.data.LocalDemoState
import com.break8t.unknown.data.Profile
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
import androidx.compose.runtime.CompositionLocalProvider

/** 저장한 배치. Profile에 자리가 없어서 DemoStore처럼 프로세스에 하나만 둔다(시연용). */
object HouseStore {
    val layout = MutableStateFlow(DefaultLayout)
}

enum class StorageTab { ALL, FLOOR, WALL }

/**
 * 집 안(S-14)과 집 꾸미기(S-15). 꾸미는 중인 배치는 저장하기 전까지 HouseStore에 쓰지 않는다.
 * @param backToInventory 보관함의 방에 놓기로 들어왔으면 true. 뒤로 칩이 "보관함으로"가 된다.
 */
@Composable
fun HouseScreen(onBack: () -> Unit, onOpenInventory: () -> Unit, onTakePhoto: () -> Unit, backToInventory: Boolean = false) =
    HouseScreenImpl(onBack, onOpenInventory, onTakePhoto, backToInventory)

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun HouseScreenImpl(
    onBack: () -> Unit,
    onOpenInventory: () -> Unit,
    onTakePhoto: () -> Unit,
    backToInventory: Boolean = false,
    startDraft: List<Placed>? = null,
    startSelected: Int = -1,
    startToast: String? = null,
    startAskExpand: Boolean = false,
    config: BalanceConfig = BalanceConfig.DEFAULT,
) {
    val demo = LocalDemoState.current
    val profile by demo.profile.collectAsState()
    // Every placement rule reads the grid from the config, so the bought size goes in here once.
    val cfg = config.copy(houseGridSize = profile.houseGridSize)
    val storedLayout by HouseStore.layout.collectAsState()
    val saved = trimToOwned(storedLayout, profile.items)

    // 화면을 나누면 Activity가 다시 만들어지므로 꾸미는 중인 배치도 저장 가능한 형태로 둔다.
    var draftCodes by rememberSaveable { mutableStateOf(startDraft?.let(::encode)) }
    val draft = draftCodes?.let(::decode)
    var selected by rememberSaveable { mutableIntStateOf(startSelected) }
    var tab by rememberSaveable { mutableStateOf(StorageTab.ALL) }
    var askExit by rememberSaveable { mutableStateOf(false) }
    var askReset by rememberSaveable { mutableStateOf(false) }
    var askExpand by rememberSaveable { mutableStateOf(startAskExpand) }
    var toast by remember { mutableStateOf(startToast) }
    val view = rememberIsoViewState()
    remember(view) { if (startSelected >= 0) view.selectedId = RoomMap.idOf(startSelected) }
    // A zoom or pan kept from another window size could leave the room half off screen, so start framed.
    LaunchedEffect(view) { view.resetView() }

    val noSpace = stringResource(R.string.house_no_space)
    val noTurn = stringResource(R.string.house_no_turn)
    val noRow = stringResource(R.string.house_no_row)
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

    // The room is drawn under the chips and the tray and framed in the space between them (item 1 of the 11/15 feedback).
    val density = LocalDensity.current
    var chipsPx by remember { mutableIntStateOf(0) }
    var trayPx by remember { mutableIntStateOf(0) }
    val insets = with(density) {
        PaddingValues(
            start = ScreenPad, end = ScreenPad, top = ScreenPad + chipsPx.toDp() + ChipGap,
            bottom = ScreenPad + if (draft != null) trayPx.toDp() + ChipGap else 0.dp,
        )
    }
    val next = nextExpansion(profile.houseGridSize, config)
    Box(Modifier.fillMaxSize().background(HouseBg)) {
        RoomScene(
            RoomSceneState(ScenePlace.HOUSE, draft ?: saved, selected = if (draft != null) selected else -1),
            Modifier.fillMaxSize(),
            cfg,
            view,
            backdrop = false,
            editMode = draft != null,
            onSelect = { if (draft != null) selected = it },
            canPlace = { pl -> draft != null && placeAt(draft, RoomMap.indexOf(pl.id), pl, cfg) != null },
            onPlace = { pl ->
                val current = draftCodes?.let(::decode)
                val i = RoomMap.indexOf(pl.id)
                current?.let { placeAt(it, i, pl, cfg) }?.let { setDraft(it); select(i) }
            },
            frameInsets = insets,
        ) {
            // Under the selected piece, as in S-15-a; the engine projects its front corner.
            draft?.getOrNull(selected)?.takeIf { showPieceButtons(selected, view.dragging) }?.let { p ->
                val (x, y, z) = RoomMap.buttonAnchor(p, cfg)
                Row(
                    Modifier.isoAnchor(x, y, z, Alignment.TopCenter).padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (specOf(p.itemId).onWall) {
                        // house_ic_back points left; a quarter turn either way makes the up and down arrows.
                        SquareIconButton(R.drawable.house_ic_back, stringResource(R.string.house_raise), {
                            shiftRowInRoom(draft, selected, 1, cfg)?.let(::setDraft) ?: run { toast = noRow }
                        }, iconRotation = 90f)
                        SquareIconButton(R.drawable.house_ic_back, stringResource(R.string.house_lower), {
                            shiftRowInRoom(draft, selected, -1, cfg)?.let(::setDraft) ?: run { toast = noRow }
                        }, iconRotation = -90f)
                    } else {
                        SquareIconButton(R.drawable.house_ic_turn, stringResource(R.string.house_turn), {
                            rotateInRoom(draft, selected, cfg)?.let(::setDraft) ?: run { toast = noTurn }
                        })
                    }
                    SquareIconButton(R.drawable.house_ic_box, stringResource(R.string.house_put_away), {
                        setDraft(storeBack(draft, selected))
                        select(-1)
                    })
                }
            }
        }
        Column(Modifier.fillMaxSize().padding(ScreenPad), verticalArrangement = Arrangement.spacedBy(ChipGap)) {
            FlowRow(
                Modifier.fillMaxWidth().onSizeChanged { chipsPx = it.height },
                horizontalArrangement = Arrangement.spacedBy(ChipGap),
                verticalArrangement = Arrangement.spacedBy(ChipGap),
            ) {
                if (draft == null) {
                    val back = if (backToInventory) R.string.house_to_inventory else R.string.house_to_island
                    TopChip(stringResource(back), onBack, icon = R.drawable.house_ic_back)
                    ScreenTitle(stringResource(R.string.house_title))
                    Spacer(Modifier.weight(1f))
                    TopChip(stringResource(R.string.house_take_photo), onTakePhoto, icon = R.drawable.house_ic_cam)
                    if (next != null) TopChip(stringResource(R.string.house_expand), { askExpand = true })
                    TopChip(stringResource(R.string.house_decorate), { setDraft(saved) })
                } else {
                    TopChip(stringResource(R.string.house_exit), ::tryExit, icon = R.drawable.house_ic_back)
                    TopChip(stringResource(R.string.house_decorate), null)
                    Spacer(Modifier.weight(1f))
                    TopChip(stringResource(R.string.house_reset), { askReset = true })
                    TopChip(stringResource(R.string.house_save), { finish(save = true) })
                }
            }
            Spacer(Modifier.weight(1f))
            if (draft != null) {
                // The empty pointerInput keeps touches on the tray from reaching the room underneath.
                Box(Modifier.onSizeChanged { trayPx = it.height }.pointerInput(Unit) {}) {
                    StorageCard(profile.items, draft, tab, { tab = it }) { id ->
                        val placed = placeNewInRoom(draft, id, cfg)
                        if (placed == null) toast = noSpace else {
                            setDraft(placed)
                            select(placed.lastIndex)
                        }
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
    if (askExpand && next != null) {
        val text = expandDialogText(profile, next)
        val done = stringResource(R.string.house_expand_done, next.size)
        AppDialog(text.title, text.body, text.confirm, onConfirm = {
            askExpand = false
            if (expandHouse(demo, config) == ExpandResult.DONE) toast = done
        }, onDismiss = { askExpand = false })
    }
}

private class DialogText(val title: String, val body: String, val confirm: String)

/** 확인 창 문구. 코인이 모자라면 얼마나 필요한지 알리고 확인만 받는다. */
@Composable
private fun expandDialogText(profile: Profile, next: Expansion): DialogText =
    if (profile.coins >= next.price) {
        DialogText(
            stringResource(R.string.house_expand_title),
            stringResource(R.string.house_expand_body, next.price, profile.houseGridSize, next.size),
            stringResource(R.string.house_expand),
        )
    } else {
        DialogText(
            stringResource(R.string.house_expand_short_title),
            stringResource(R.string.house_expand_short_body, next.price, profile.coins),
            stringResource(R.string.house_expand_ok),
        )
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

/** 미리보기용 사용자 상태. 넓힌 집과 코인이 모자란 경우를 보인다. */
@Composable
private fun WithProfile(profile: Profile, content: @Composable () -> Unit) =
    CompositionLocalProvider(LocalDemoState provides remember { DemoState(profile) }, content = content)

@WindowPreviews
@Composable
private fun HouseViewPreview() = PreviewFrame { HouseScreenImpl({}, {}, {}) }

@WindowPreviews
@Composable
private fun HouseFromInventoryPreview() = PreviewFrame { HouseScreenImpl({}, {}, {}, backToInventory = true) }

@WindowPreviews
@Composable
private fun HouseExpandedPreview() = PreviewFrame {
    val done = stringResource(R.string.house_expand_done, 7)
    WithProfile(Profile(houseGridSize = 7)) { HouseScreenImpl({}, {}, {}, startToast = done) }
}

@WindowPreviews
@Composable
private fun DecorateSelectedPreview() = PreviewFrame { HouseScreenImpl({}, {}, {}, startDraft = DefaultLayout, startSelected = 0) }

@WindowPreviews
@Composable
private fun DecorateWallSelectedPreview() = PreviewFrame { HouseScreenImpl({}, {}, {}, startDraft = DefaultLayout, startSelected = 2) }

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
private fun ExpandDialogPreview() = PreviewFrame {
    val p = Profile()
    val text = expandDialogText(p, nextExpansion(p.houseGridSize, BalanceConfig.DEFAULT)!!)
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { DialogPanel(text.title, text.body, text.confirm, {}, {}) }
}

@WindowPreviews
@Composable
private fun ExpandShortDialogPreview() = PreviewFrame {
    val p = Profile(coins = 20)
    val text = expandDialogText(p, nextExpansion(p.houseGridSize, BalanceConfig.DEFAULT)!!)
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { DialogPanel(text.title, text.body, text.confirm, {}, {}) }
}

@WindowPreviews
@Composable
private fun ResetToastPreview() = PreviewFrame {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { AppToast(stringResource(R.string.house_reset_done)) }
}
