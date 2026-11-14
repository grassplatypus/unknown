package com.break8t.unknown.ui.collection

import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.break8t.unknown.ui.photo.ShareHandler
import com.break8t.unknown.R
import com.break8t.unknown.data.LocalDemoState
import com.break8t.unknown.data.Profile
import com.break8t.unknown.ui.Route
import com.break8t.unknown.ui.components.AppChip
import com.break8t.unknown.ui.components.AppToast
import com.break8t.unknown.ui.components.PrimaryButton
import com.break8t.unknown.ui.components.SecondaryButton
import com.break8t.unknown.ui.theme.AppColors
import kotlinx.coroutines.delay

private const val TOAST_MS = 2_500L

@StringRes
internal fun CollectionKind.label() = when (this) {
    CollectionKind.CARD -> R.string.collection_kind_card
    CollectionKind.POSTCARD -> R.string.collection_kind_postcard
    CollectionKind.CHARACTER -> R.string.collection_kind_character
    CollectionKind.CAR -> R.string.collection_kind_car
    CollectionKind.BADGE -> R.string.collection_kind_badge
}

/**
 * 내 정보 목록(S-21)과 도감(S-24)이 함께 쓰는 묶음 화면. 사용자 상태는 LocalDemoState로 바꾼다.
 * @param showLocked 도감처럼 아직 못 얻은 항목도 ???로 보일지
 * @param header 맨 위 제목 칩 자리
 */
@Composable
fun CollectionScreen(
    kind: CollectionKind,
    onKind: (CollectionKind) -> Unit,
    loaded: Boolean,
    showLocked: Boolean,
    onShare: ShareHandler,
    onOpenFieldDrop: (() -> Unit)?,
    header: @Composable () -> Unit,
) {
    val demo = LocalDemoState.current
    val profile by demo.profile.collectAsState()
    val all = collections(profile)
    var openId by rememberSaveable { mutableStateOf<String?>(null) }
    var toast by rememberSaveable { mutableStateOf<String?>(null) }
    val open = openId?.let { id -> all.values.flatten().firstOrNull { it.id == id } }
    val changedText = toast?.let { stringResource(R.string.collection_character_changed, Josa.euroRo(it)) }

    Box(Modifier.fillMaxSize()) {
        CollectionPane(all, kind, onKind, loaded, showLocked, profile, header, onOpenFieldDrop) { item ->
            when {
                !item.owned || item.kind == CollectionKind.CARD || item.kind == CollectionKind.POSTCARD || item.kind == CollectionKind.BADGE -> openId = item.id
                item.kind == CollectionKind.CHARACTER && item.id != profile.selectedCharacterId -> openId = item.id
                item.kind == CollectionKind.CAR -> demo.update { it.copy(selectedCarId = item.id) }
            }
        }
        if (changedText != null) {
            AppToast(changedText, Modifier.align(Alignment.BottomCenter).padding(bottom = 72.dp))
            LaunchedEffect(toast) { delay(TOAST_MS); toast = null }
        }
        if (open != null) {
            val close = { openId = null }
            Overlay(close) {
                ItemDialog(open, close, onShare) {
                    demo.update { it.copy(selectedCharacterId = open.id) }
                    openId = null
                    toast = open.name
                }
            }
        }
    }
}

/** 항목에 맞는 다이얼로그: 잠김 D-26, 정체 카드 D-24, 엽서 D-25, 뱃지, 캐릭터 바꾸기 D-20. */
@Composable
internal fun ItemDialog(
    item: Collectible,
    onClose: () -> Unit,
    onShare: ShareHandler,
    onChangeCharacter: () -> Unit,
) {
    when {
        !item.owned -> LockedHint(item, onClose)
        item.kind == CollectionKind.CARD -> CardDetail(item, onClose, onShare)
        item.kind == CollectionKind.POSTCARD -> PostcardDetail(item, onClose, onShare)
        item.kind == CollectionKind.BADGE -> BadgeDetail(item, onClose)
        else -> DialogFrame(832.dp) {
            DialogArt(item.art, item.name)
            DialogTitle(stringResource(R.string.collection_change_character_title, Josa.euroRo(item.name)))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(35.dp)) {
                SecondaryButton(stringResource(R.string.components_cancel), onClose, Modifier.weight(1f))
                PrimaryButton(stringResource(R.string.collection_change_character_confirm), onChangeCharacter, Modifier.weight(1f))
            }
        }
    }
}

/** 탭, 목록, 불러오는 중, 비어 있음. 상태를 갖지 않아서 미리보기에서 그대로 쓴다. */
@Composable
internal fun CollectionPane(
    all: Map<CollectionKind, List<Collectible>>,
    kind: CollectionKind,
    onKind: (CollectionKind) -> Unit,
    loaded: Boolean,
    showLocked: Boolean,
    profile: Profile,
    header: @Composable () -> Unit,
    onOpenFieldDrop: (() -> Unit)?,
    onItem: (Collectible) -> Unit,
) {
    val visible = all[kind].orEmpty().filter { showLocked || it.owned }
    Column(Modifier.fillMaxSize().padding(start = ScreenPadding, top = ScreenPadding, end = ScreenPadding)) {
        header()
        FlowRow(
            Modifier.padding(vertical = 35.dp),
            horizontalArrangement = Arrangement.spacedBy(35.dp),
            verticalArrangement = Arrangement.spacedBy(21.dp),
        ) {
            CollectionKind.entries.forEach { k ->
                val name = stringResource(k.label())
                val text = if (loaded) stringResource(R.string.collection_tab, name, all[k].orEmpty().count { it.owned }) else name
                AppChip(text, selected = k == kind, onClick = { onKind(k) })
            }
        }
        when (listPhase(loaded, visible.size)) {
            ListPhase.LOADING -> Loading()
            ListPhase.EMPTY -> Empty(kind, onOpenFieldDrop)
            ListPhase.CONTENT -> Grid(kind, visible, profile, onItem)
        }
    }
}

@Composable
private fun Grid(kind: CollectionKind, items: List<Collectible>, profile: Profile, onItem: (Collectible) -> Unit) {
    val cells = when (kind) {
        CollectionKind.CARD -> GridCells.Adaptive(260.dp)
        CollectionKind.BADGE -> GridCells.Fixed(1)
        else -> GridCells.Adaptive(380.dp)
    }
    LazyVerticalGrid(
        cells,
        Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(35.dp),
        verticalArrangement = Arrangement.spacedBy(if (kind == CollectionKind.BADGE) 31.dp else 35.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = ScreenPadding),
    ) {
        items(items, key = { it.id }) { item ->
            val select = when (kind) {
                CollectionKind.CHARACTER -> item.id == profile.selectedCharacterId
                CollectionKind.CAR -> item.id == profile.selectedCarId
                else -> false
            }
            when (kind) {
                CollectionKind.CARD -> CardTile(item) { onItem(item) }
                CollectionKind.POSTCARD -> PostcardTile(item) { onItem(item) }
                CollectionKind.CHARACTER, CollectionKind.CAR -> PickTile(item, select) { onItem(item) }
                CollectionKind.BADGE -> BadgeRow(item) { onItem(item) }
            }
        }
    }
}

@Composable
private fun Name(item: Collectible, size: Int) = Text(
    if (item.owned) item.name else stringResource(R.string.collection_locked_name),
    color = if (item.owned) AppColors.Text else AppColors.TextSub,
    fontSize = size.sp,
    lineHeight = (size * 1.4f).sp, // "???"와 한글 이름의 줄 높이를 같게 둔다
    fontWeight = FontWeight.Bold,
    maxLines = 1,
    overflow = TextOverflow.Ellipsis,
)

// 빈 문자열은 한글 줄보다 낮게 잡혀 잠긴 카드만 키가 작아진다. 빈 줄도 한글 한 글자를 투명하게 그려 높이를 맞춘다.
@Composable
private fun Sub(text: String, color: androidx.compose.ui.graphics.Color = AppColors.TextSub, bold: Boolean = false) = Text(
    text.ifEmpty { "가" },
    Modifier.alpha(if (text.isEmpty()) 0f else 1f),
    color = color,
    fontSize = 29.sp,
    lineHeight = 40.sp,
    fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
    maxLines = 1,
)

@Composable
private fun CardTile(item: Collectible, onClick: () -> Unit) = Tile(onClick) {
    // Figma: 잠긴 카드는 안개 그림을 그대로 보이고 왼쪽 위에 자물쇠를 얹는다.
    Box(Modifier.fillMaxWidth().aspectRatio(220f / 147f).clip(ArtShape)) {
        Art(item.art, item.name, Modifier.fillMaxSize(), ContentScale.Crop)
        if (!item.owned) LockIcon()
    }
    Spacer(Modifier.height(17.dp))
    Name(item, 36)
    Spacer(Modifier.height(13.dp))
    Sub(item.meetCount?.let { stringResource(R.string.collection_meet_count, it) } ?: "")
}

@Composable
private fun PostcardTile(item: Collectible, onClick: () -> Unit) = Tile(onClick) {
    Box(Modifier.fillMaxWidth().aspectRatio(313f / 209f).clip(ArtShape), Alignment.Center) {
        Art(item.art, item.name, Modifier.fillMaxSize(), ContentScale.Crop, if (item.owned) 1f else 0.25f)
        if (!item.owned) LockIcon()
    }
    Spacer(Modifier.height(20.dp))
    Name(item, 39)
    Spacer(Modifier.height(13.dp))
    Sub(if (item.owned) item.note.orEmpty() else "")
}

@Composable
private fun PickTile(item: Collectible, selected: Boolean, onClick: () -> Unit) = Tile(onClick, selected = selected) {
    Row(horizontalArrangement = Arrangement.spacedBy(26.dp)) {
        Box(Modifier.size(208.dp).clip(ArtShape).background(AppColors.Accent), Alignment.Center) {
            Art(item.art, item.name, Modifier.size(182.dp), alpha = if (item.owned) 1f else 0.25f)
            if (!item.owned) LockIcon()
        }
        Column(Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Name(item, 39)
            if (selected) Sub(stringResource(R.string.collection_selected), AppColors.Success, bold = true)
        }
    }
}

@Composable
private fun BadgeRow(item: Collectible, onClick: () -> Unit) = Tile(onClick, Modifier.heightIn(min = 164.dp)) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(31.dp)) {
        Box(Modifier.size(125.dp), Alignment.Center) {
            Art(item.art, item.name, Modifier.fillMaxSize(), alpha = if (item.owned) 1f else 0.25f)
            if (!item.owned) LockIcon()
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Name(item, 42)
            if (item.owned) Text(item.note.orEmpty(), color = AppColors.TextSub, fontSize = 31.sp)
        }
        if (item.owned) Text(item.date.orEmpty(), color = AppColors.TextSub, fontSize = 36.sp, fontWeight = FontWeight.Medium)
    }
}

// Figma S-24-b. 주행 중 화면이 정적이어야 하므로 반짝이는 효과 없이 상자만 둔다.
@Composable
private fun Loading() = Column(Modifier.fillMaxSize()) {
    FlowRow(
        Modifier.weight(1f),
        horizontalArrangement = Arrangement.spacedBy(35.dp),
        verticalArrangement = Arrangement.spacedBy(35.dp),
    ) {
        repeat(8) {
            Column(
                Modifier.width(328.dp).clip(TileShape).background(AppColors.Muted).padding(21.dp),
                verticalArrangement = Arrangement.spacedBy(21.dp),
            ) {
                SkeletonBox(Modifier.fillMaxWidth().aspectRatio(220f / 147f))
                SkeletonBox(Modifier.size(208.dp, 34.dp))
                SkeletonBox(Modifier.size(130.dp, 26.dp))
            }
        }
    }
    Text(stringResource(R.string.encyclopedia_loading), Modifier.padding(vertical = 20.dp), color = AppColors.TextSub, fontSize = 31.sp)
}

@Composable
private fun SkeletonBox(modifier: Modifier) = Box(modifier.clip(RoundedCornerShape(8.dp)).background(AppColors.Divider))

@Composable
private fun Empty(kind: CollectionKind, onOpenFieldDrop: (() -> Unit)?) {
    val (title, body) = when (kind) {
        CollectionKind.CARD -> R.string.collection_empty_card to R.string.collection_empty_card_body
        CollectionKind.POSTCARD -> R.string.collection_empty_postcard to R.string.collection_empty_postcard_body
        CollectionKind.CHARACTER -> R.string.collection_empty_character to R.string.collection_empty_pick_body
        CollectionKind.CAR -> R.string.collection_empty_car to R.string.collection_empty_pick_body
        CollectionKind.BADGE -> R.string.collection_empty_badge to R.string.collection_empty_badge_body
    }
    Column(
        Modifier.fillMaxSize().padding(bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(31.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(title), color = AppColors.Text, fontSize = 52.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text(stringResource(body), color = AppColors.TextSub, fontSize = 36.sp, textAlign = TextAlign.Center)
        if (onOpenFieldDrop != null) SecondaryButton(stringResource(R.string.collection_open_field_drop), onOpenFieldDrop)
    }
}

@Composable
private fun CloseShareRow(onClose: () -> Unit, onShare: () -> Unit) =
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(35.dp)) {
        PrimaryButton(stringResource(R.string.collection_close), onClose, Modifier.weight(1f))
        SecondaryButton(stringResource(R.string.collection_share), onShare, Modifier.weight(1f))
    }

/** D-24. 왼쪽에 그림, 오른쪽에 기록. 대화상자가 좁은 세로 창에서는 그림 아래에 기록을 둔다. */
@Composable
private fun CardDetail(item: Collectible, onClose: () -> Unit, onShare: ShareHandler) {
    val meets = stringResource(R.string.collection_meet_count, item.meetCount ?: 0)
    DialogFrame(1196.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            DialogTitle(item.name)
            item.route?.let { DialogBody(it) }
        }
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val art = @Composable { m: Modifier -> Art(item.art, item.name, m.aspectRatio(390f / 260f).clip(ArtShape), ContentScale.Crop) }
            val stats = @Composable { m: Modifier ->
                Column(m, verticalArrangement = Arrangement.spacedBy(26.dp)) {
                    Stat(R.string.collection_meets, meets)
                    item.fastest?.let { Stat(R.string.collection_fastest, it) }
                    item.slowest?.let { Stat(R.string.collection_slowest, it) }
                    item.usualTime?.let { Stat(R.string.collection_usual_time, it) }
                }
            }
            if (maxWidth >= 900.dp) {
                Row(horizontalArrangement = Arrangement.spacedBy(39.dp), verticalAlignment = Alignment.CenterVertically) {
                    art(Modifier.weight(1f))
                    stats(Modifier.weight(1f))
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(31.dp)) {
                    art(Modifier.fillMaxWidth())
                    stats(Modifier.fillMaxWidth())
                }
            }
        }
        CloseShareRow(onClose) { onShare(false, item.name, item.route ?: meets, item.art) }
    }
}

/** 받은 뱃지를 크게 보인다. 받은 조건과 날짜를 함께 쓴다. */
@Composable
private fun BadgeDetail(item: Collectible, onClose: () -> Unit) = DialogFrame(832.dp) {
    Box(Modifier.fillMaxWidth().clip(ArtShape).background(AppColors.Accent).padding(26.dp), Alignment.Center) {
        Art(item.art, item.name, Modifier.size(312.dp))
    }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        DialogTitle(item.name)
        item.note?.let { DialogBody(it) }
    }
    item.date?.let { Stat(R.string.collection_badge_date, it) }
    PrimaryButton(stringResource(R.string.collection_close), onClose, Modifier.fillMaxWidth())
}

@Composable
private fun Stat(@StringRes label: Int, value: String) =
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(label), color = AppColors.TextSub, fontSize = 34.sp)
        Text(value, color = AppColors.Text, fontSize = 42.sp, fontWeight = FontWeight.Bold)
    }

/** D-25 */
@Composable
private fun PostcardDetail(item: Collectible, onClose: () -> Unit, onShare: ShareHandler) =
    DialogFrame(1040.dp) {
        Art(item.art, item.name, Modifier.fillMaxWidth().aspectRatio(720f / 400f).clip(ArtShape), ContentScale.Crop)
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            DialogTitle(item.name)
            DialogBody(item.note.orEmpty())
        }
        CloseShareRow(onClose) { onShare(true, item.name, item.note.orEmpty(), item.art) }
    }

/** D-26. Figma 힌트는 상점 문구 하나뿐이라, 값을 매긴 항목만 그 문구를 쓴다. */
@Composable
private fun LockedHint(item: Collectible, onClose: () -> Unit) {
    val hint = when (item.kind) {
        CollectionKind.CARD -> R.string.collection_hint_card
        CollectionKind.POSTCARD -> R.string.collection_hint_postcard
        CollectionKind.BADGE -> R.string.collection_hint_badge
        CollectionKind.CHARACTER, CollectionKind.CAR ->
            if (item.price != null) R.string.collection_hint_shop else R.string.collection_hint_unknown
    }
    DialogFrame(832.dp) {
        DialogTitle(stringResource(R.string.collection_locked_name))
        DialogBody(stringResource(hint))
        PrimaryButton(stringResource(R.string.collection_close), onClose, Modifier.fillMaxWidth())
    }
}

// Previews: Figma S-24-a..g and the dialogs D-20, D-24, D-25, D-26, T-10.

internal class EncyclopediaStates : PreviewParameterProvider<Pair<CollectionKind, Boolean>> {
    override val values = sequenceOf(
        CollectionKind.CARD to true, CollectionKind.CARD to false, CollectionKind.POSTCARD to true,
        CollectionKind.CHARACTER to true, CollectionKind.CAR to true, CollectionKind.BADGE to true,
    )
}

@ScreenPreviews
@Composable
private fun EncyclopediaPreview(@PreviewParameter(EncyclopediaStates::class) state: Pair<CollectionKind, Boolean>) =
    PreviewShell(Route.ENCYCLOPEDIA) {
        CollectionPane(collections(Profile()), state.first, {}, state.second, true, Profile(), { HeaderChip("도감") }, {}) {}
    }

@ScreenPreviews
@Composable
private fun EncyclopediaEmptyPreview() = PreviewShell(Route.ENCYCLOPEDIA) {
    val all = collections(Profile()) + (CollectionKind.POSTCARD to emptyList())
    CollectionPane(all, CollectionKind.POSTCARD, {}, true, true, Profile(), { HeaderChip("도감") }, {}) {}
}

internal class DialogStates : PreviewParameterProvider<String> {
    override val values = sequenceOf("card_seongsan", "post_cheomseongdae", "badge_safe", "ch_cat", "ch_firefly", "car_yellow", "card_gayang")
}

@ScreenPreviews
@Composable
private fun ItemDialogPreview(@PreviewParameter(DialogStates::class) id: String) = PreviewShell(Route.ENCYCLOPEDIA) {
    val all = collections(Profile())
    val kind = all.entries.first { e -> e.value.any { it.id == id } }.key
    CollectionPane(all, kind, {}, true, true, Profile(), { HeaderChip("도감") }, {}) {}
    Overlay({}) { ItemDialog(all.getValue(kind).first { it.id == id }, {}, { _, _, _, _ -> }, {}) }
}

@ScreenPreviews
@Composable
private fun CharacterChangedPreview() = PreviewShell(Route.ENCYCLOPEDIA) {
    CollectionPane(collections(Profile()), CollectionKind.CHARACTER, {}, true, true, Profile(selectedCharacterId = "ch_cat"), { HeaderChip("도감") }, {}) {}
    AppToast(stringResource(R.string.collection_character_changed, Josa.euroRo("냥이")), Modifier.align(Alignment.BottomCenter).padding(bottom = 72.dp))
}
