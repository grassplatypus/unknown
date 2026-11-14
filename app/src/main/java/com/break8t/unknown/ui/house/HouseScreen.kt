package com.break8t.unknown.ui.house

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
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
    var grab by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    val noSpace = stringResource(R.string.house_no_space)
    val noTurn = stringResource(R.string.house_no_turn)
    val resetDone = stringResource(R.string.house_reset_done)

    fun setDraft(l: List<Placed>?) { draftCodes = l?.let(::encode) }
    fun finish(save: Boolean) {
        if (draft != null) HouseStore.layout.value = resolveExit(saved, draft, save)
        setDraft(null)
        selected = -1
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

    Box(Modifier.fillMaxSize().background(AppColors.Text)) {
        Column(Modifier.fillMaxSize().padding(ScreenPad), verticalArrangement = Arrangement.spacedBy(ChipGap)) {
            FlowRow(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ChipGap),
                verticalArrangement = Arrangement.spacedBy(ChipGap),
            ) {
                if (draft == null) {
                    TopChip(stringResource(R.string.house_to_island), onBack, icon = R.drawable.house_ic_back)
                    TopChip(stringResource(R.string.house_title), null)
                    Spacer(Modifier.weight(1f))
                    TopChip(stringResource(R.string.house_storage), onOpenInventory, icon = R.drawable.house_ic_box)
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
                RoomScene(RoomSceneState(ScenePlace.HOUSE, saved), Modifier.weight(1f).fillMaxWidth(), config)
            } else {
                RoomScene(
                    RoomSceneState(ScenePlace.HOUSE, draft, selected = selected),
                    Modifier.weight(1f).fillMaxWidth(),
                    config,
                    onTapCell = { c ->
                        val hit = c?.let { itemAt(draft, it) } ?: -1
                        when {
                            hit >= 0 -> selected = if (hit == selected) -1 else hit
                            selected >= 0 && c != null -> setDraft(moveTo(draft, selected, c, config))
                            else -> selected = -1
                        }
                    },
                    onDragStartCell = { c ->
                        val hit = c?.let { itemAt(draft, it) } ?: -1
                        grab = if (hit >= 0 && c != null) {
                            selected = hit
                            (c.x - draft[hit].x) to (c.y - draft[hit].y)
                        } else null
                    },
                    onDragCell = { c ->
                        val g = grab
                        val current = draftCodes?.let(::decode)
                        if (g != null && c != null && current != null && selected >= 0) {
                            val sameSurface = current[selected].surface == c.surface
                            val target = if (sameSurface) c.copy(x = c.x - g.first, y = c.y - g.second) else c
                            setDraft(moveTo(current, selected, target, config))
                        }
                    },
                    selectionActions = {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (draft.getOrNull(selected)?.let { specOf(it.itemId).onWall } == false) {
                                SquareIconButton(R.drawable.house_ic_turn, stringResource(R.string.house_turn), {
                                    rotate(draft, selected, config)?.let(::setDraft) ?: run { toast = noTurn }
                                })
                            }
                            SquareIconButton(R.drawable.house_ic_box, stringResource(R.string.house_put_away), {
                                setDraft(storeBack(draft, selected))
                                selected = -1
                            })
                        }
                    },
                )
                StorageCard(profile.items, draft, tab, { tab = it }) { id ->
                    val placed = placeNew(draft, id, config)
                    if (placed == null) toast = noSpace else {
                        setDraft(placed)
                        selected = placed.lastIndex
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
                selected = -1
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
            if (entries.isEmpty()) {
                Box(Modifier.fillMaxWidth().heightIn(min = 208.dp), contentAlignment = Alignment.Center) {
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
private fun StorageTile(id: String, name: String, count: Int, onClick: () -> Unit) {
    Column(
        Modifier
            .width(166.dp)
            .clip(ControlShape)
            .background(AppColors.Card)
            .border(BorderWidth, AppColors.Line, ControlShape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(painterResource(furnitureImage(id)), null, Modifier.size(99.dp))
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
