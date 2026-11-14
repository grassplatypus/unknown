package com.break8t.unknown.ui.inventory

import com.break8t.unknown.ui.components.ScreenTitle
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.break8t.unknown.R
import com.break8t.unknown.config.Catalog
import com.break8t.unknown.config.ItemKind
import com.break8t.unknown.data.LocalDemoState
import com.break8t.unknown.data.Profile
import com.break8t.unknown.ui.components.AppChip
import com.break8t.unknown.ui.components.SecondaryButton
import com.break8t.unknown.ui.shop.BoxShape
import com.break8t.unknown.ui.shop.DangerButton
import com.break8t.unknown.ui.shop.Gap
import com.break8t.unknown.ui.shop.HeaderBar
import com.break8t.unknown.ui.shop.HeaderChip
import com.break8t.unknown.ui.shop.ItemHeaderRow
import com.break8t.unknown.ui.shop.ItemImage
import com.break8t.unknown.ui.shop.QtyStepper
import com.break8t.unknown.ui.shop.ScreenPadding
import com.break8t.unknown.ui.shop.ShopDialog
import com.break8t.unknown.ui.shop.TileShape
import com.break8t.unknown.ui.shop.ToastHost
import com.break8t.unknown.ui.theme.AppColors

/** 보관함 진입점(S-18). 섬, 상점, 방 꾸미기는 다른 묶음 화면이라 콜백으로 알린다. */
@Composable
fun InventoryScreen(onBack: () -> Unit, onOpenShop: () -> Unit, onPlaceFurniture: () -> Unit) {
    val state = LocalDemoState.current
    val profile by state.profile.collectAsState()
    val context = LocalContext.current
    var tab by rememberSaveable { mutableStateOf(InventoryTab.FURNITURE) }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var discardId by rememberSaveable { mutableStateOf<String?>(null) }
    var qty by rememberSaveable { mutableIntStateOf(1) }
    var toast by rememberSaveable { mutableStateOf<String?>(null) }

    Box(Modifier.fillMaxSize()) {
        InventoryContent(
            profile, tab, selectedId, onBack, onOpenShop, onPlaceFurniture,
            onTab = { tab = it },
            onSelect = { selectedId = it },
            onDiscard = { discardId = it; qty = 1 },
        )
        ToastHost(toast) { toast = null }
    }

    discardId?.let(Catalog::item)?.let { item ->
        val held = profile.items[item.id] ?: 0
        val close = { discardId = null }
        ShopDialog(close) {
            DiscardDialogBody(item.id, item.name, held, qty.coerceAtMost(held), { qty = it }, close) {
                val n = qty.coerceAtMost(held)
                if (discard(state, item.id, n)) toast = context.getString(R.string.inventory_toast_discarded, item.name, n)
                close()
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun InventoryContent(
    profile: Profile,
    tab: InventoryTab,
    selectedId: String?,
    onBack: () -> Unit,
    onOpenShop: () -> Unit,
    onPlaceFurniture: () -> Unit,
    onTab: (InventoryTab) -> Unit,
    onSelect: (String) -> Unit,
    onDiscard: (String) -> Unit,
) {
    val entries = inventoryEntries(profile, tab)
    // After the last one is thrown away the selection falls back to the first tile.
    val selected = entries.firstOrNull { it.item.id == selectedId } ?: entries.firstOrNull()
    BoxWithConstraints(Modifier.fillMaxSize().padding(ScreenPadding)) {
        val wide = maxWidth >= 1000.dp
        Column(verticalArrangement = Arrangement.spacedBy(Gap)) {
            HeaderBar(profile.coins) {
                HeaderChip(stringResource(R.string.inventory_back), R.drawable.shop_ic_back, selected = false, onClick = onBack)
                HeaderChip(stringResource(R.string.inventory_shop), null, selected = false, onClick = onOpenShop)
                ScreenTitle(stringResource(R.string.inventory_title))
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(22.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                InventoryTab.entries.forEach { t ->
                    AppChip(stringResource(tabLabel(t), inventoryEntries(profile, t).size), t == tab, { onTab(t) })
                }
            }
            // Profile lives in memory and is always loaded, so an empty list really is empty.
            if (entries.isEmpty()) {
                Text(stringResource(emptyLabel(tab)), color = AppColors.TextSub, fontSize = 39.sp)
                return@Column
            }
            val grid = @Composable { m: Modifier -> EntryGrid(entries, selected?.item?.id, onSelect, m) }
            val card = @Composable { m: Modifier ->
                selected?.let { DetailCard(it, wide, { onDiscard(it.item.id) }, onPlaceFurniture, m) }
            }
            if (wide) {
                Row(horizontalArrangement = Arrangement.spacedBy(Gap)) {
                    grid(Modifier.weight(1f))
                    card(Modifier.width(472.dp))
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(Gap)) {
                    grid(Modifier.weight(1f))
                    card(Modifier.fillMaxWidth())
                }
            }
        }
    }
}

private fun tabLabel(tab: InventoryTab) = when (tab) {
    InventoryTab.FURNITURE -> R.string.inventory_tab_furniture
    InventoryTab.SEED -> R.string.inventory_tab_seed
}

private fun emptyLabel(tab: InventoryTab) = when (tab) {
    InventoryTab.FURNITURE -> R.string.inventory_empty_furniture
    InventoryTab.SEED -> R.string.inventory_empty_seed
}

@Composable
private fun EntryGrid(entries: List<InventoryEntry>, selectedId: String?, onSelect: (String) -> Unit, modifier: Modifier) {
    LazyVerticalGrid(
        GridCells.Adaptive(200.dp),
        modifier,
        horizontalArrangement = Arrangement.spacedBy(22.dp),
        verticalArrangement = Arrangement.spacedBy(26.dp),
    ) {
        items(entries, key = { it.item.id }) { entry ->
            val selected = entry.item.id == selectedId
            Column(
                Modifier
                    .clip(TileShape)
                    .background(AppColors.Card)
                    .border(if (selected) 6.5.dp else 3.dp, if (selected) AppColors.Selected else AppColors.Line, TileShape)
                    .clickable(role = Role.Tab) { onSelect(entry.item.id) }
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(Modifier.fillMaxWidth().height(135.dp).clip(BoxShape).background(AppColors.Accent), Alignment.Center) {
                    ItemImage(entry.item.id, entry.item.name, 120.dp)
                }
                Text(entry.item.name, color = AppColors.Text, fontSize = 34.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(stringResource(R.string.inventory_count, entry.count), color = AppColors.TextSub, fontSize = 29.sp)
            }
        }
    }
}

/** 고른 아이템 카드. 방에 놓기는 가구에만 있다(씨앗은 텃밭에서 심는다). */
@Composable
private fun DetailCard(entry: InventoryEntry, wide: Boolean, onDiscard: () -> Unit, onPlace: () -> Unit, modifier: Modifier) {
    val item = entry.item
    val furniture = item.kind == ItemKind.FURNITURE
    Column(
        modifier
            .clip(TileShape)
            .background(AppColors.Card)
            .border(3.dp, AppColors.Line, TileShape)
            .padding(29.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        val name = @Composable { Text(item.name, color = AppColors.Text, fontSize = 49.sp, fontWeight = FontWeight.Bold) }
        val count = @Composable { Text(stringResource(R.string.inventory_count, entry.count), color = AppColors.TextSub, fontSize = 31.sp) }
        if (wide) {
            Box(Modifier.fillMaxWidth().height(260.dp).clip(BoxShape).background(AppColors.Accent), Alignment.Center) {
                ItemImage(item.id, item.name, 208.dp)
            }
            name()
            count()
            if (furniture) SecondaryButton(stringResource(R.string.inventory_place), onPlace, Modifier.fillMaxWidth().padding(top = 26.dp))
            DangerButton(stringResource(R.string.inventory_discard), onDiscard, Modifier.fillMaxWidth())
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(26.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(130.dp).clip(BoxShape).background(AppColors.Accent), Alignment.Center) {
                    ItemImage(item.id, item.name, 110.dp)
                }
                Column { name(); count() }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(26.dp)) {
                if (furniture) SecondaryButton(stringResource(R.string.inventory_place), onPlace, Modifier.weight(1f))
                DangerButton(stringResource(R.string.inventory_discard), onDiscard, Modifier.weight(1f))
            }
        }
    }
}

/** D-13. 가진 개수까지만 고를 수 있다. */
@Composable
internal fun ColumnScope.DiscardDialogBody(
    id: String,
    name: String,
    held: Int,
    qty: Int,
    onQty: (Int) -> Unit,
    onCancel: () -> Unit,
    onDiscard: () -> Unit,
) {
    ItemHeaderRow(id, name) { Text(stringResource(R.string.inventory_held, held), color = AppColors.TextSub, fontSize = 36.sp) }
    QtyStepper(qty, 1, held) { onQty(it.coerceIn(1, held)) }
    Text(stringResource(R.string.inventory_discard_note), color = AppColors.TextSub, fontSize = 34.sp, textAlign = TextAlign.Start)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(35.dp)) {
        SecondaryButton(stringResource(R.string.inventory_cancel), onCancel, Modifier.weight(1f))
        DangerButton(stringResource(R.string.inventory_discard), onDiscard, Modifier.weight(1f))
    }
}
