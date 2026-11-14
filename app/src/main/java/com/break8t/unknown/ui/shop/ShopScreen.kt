package com.break8t.unknown.ui.shop

import com.break8t.unknown.ui.components.ScreenTitle
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.break8t.unknown.R
import com.break8t.unknown.config.BalanceConfig
import com.break8t.unknown.config.Catalog
import com.break8t.unknown.config.CatalogItem
import com.break8t.unknown.config.ItemKind
import com.break8t.unknown.data.LocalDemoState
import com.break8t.unknown.data.Profile
import com.break8t.unknown.ui.components.AppCard
import com.break8t.unknown.ui.components.AppChip
import com.break8t.unknown.ui.components.PrimaryButton
import com.break8t.unknown.ui.components.SecondaryButton
import com.break8t.unknown.ui.theme.AppColors

/**
 * 상점 진입점(S-19). 뒤로 가기와 보관함은 다른 묶음 화면이라 콜백으로 알린다.
 * @param fromVoicePacks 설정의 음성팩 고르기에서 열었으면 참이다. 뒤로 버튼이 "음성팩"이 되고, 목소리가 딸린 캐릭터 탭부터 보인다.
 */
@Composable
fun ShopScreen(onBack: () -> Unit, onOpenInventory: () -> Unit, fromVoicePacks: Boolean = false) {
    val state = LocalDemoState.current
    val profile by state.profile.collectAsState()
    val context = LocalContext.current
    val config = BalanceConfig.DEFAULT
    var tab by rememberSaveable { mutableStateOf(if (fromVoicePacks) ShopTab.CHARACTER else ShopTab.SEED) }
    var pendingId by rememberSaveable { mutableStateOf<String?>(null) }
    var qty by rememberSaveable { mutableIntStateOf(1) }
    var askId by rememberSaveable { mutableStateOf<String?>(null) }
    var toast by rememberSaveable { mutableStateOf<String?>(null) }

    Box(Modifier.fillMaxSize()) {
        ShopContent(
            profile, tab, onBack, onOpenInventory, onTab = { tab = it }, onPick = { pendingId = it.id; qty = 1 },
            backLabel = if (fromVoicePacks) R.string.shop_back_voice else R.string.shop_back,
        )
        ToastHost(toast) { toast = null }
    }

    pendingId?.let(Catalog::item)?.let { item ->
        val close = { pendingId = null }
        ShopDialog(close) {
            BuyDialogBody(item, profile.coins, qty, config, { qty = it }, close) {
                val bought = buy(state, item, qty, config) == BuyResult.BOUGHT
                close()
                when {
                    !bought -> Unit
                    asksToSwitch(item) -> askId = item.id
                    item.kind == ItemKind.POSE -> toast = context.getString(R.string.shop_toast_pose, item.name)
                    else -> toast = context.getString(R.string.shop_toast_bought, item.name, qty)
                }
            }
        }
    }
    askId?.let(Catalog::item)?.let { item ->
        val close = { askId = null }
        ShopDialog(close) { SwitchDialogBody(item, close) { switchTo(state, item); close() } }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ShopContent(
    profile: Profile,
    tab: ShopTab,
    onBack: () -> Unit,
    onOpenInventory: () -> Unit,
    onTab: (ShopTab) -> Unit,
    onPick: (CatalogItem) -> Unit,
    @StringRes backLabel: Int = R.string.shop_back,
) {
    BoxWithConstraints(Modifier.fillMaxSize().padding(ScreenPadding)) {
        // Split screen (about 660dp of body) has no room for the stall picture beside the grid.
        val wide = maxWidth >= 1000.dp
        Column(verticalArrangement = Arrangement.spacedBy(Gap)) {
            HeaderBar(profile.coins) {
                HeaderChip(stringResource(backLabel), R.drawable.shop_ic_back, selected = false, onClick = onBack)
                ScreenTitle(stringResource(R.string.shop_title))
                HeaderChip(stringResource(R.string.shop_inventory), R.drawable.shop_ic_box, selected = false, onClick = onOpenInventory)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Gap)) {
                if (wide) {
                    Image(
                        painterResource(R.drawable.shop_c3_shop),
                        contentDescription = null,
                        Modifier.size(width = 429.dp, height = 322.dp).clip(TileShape),
                        contentScale = ContentScale.Crop,
                    )
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Gap)) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(22.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        ShopTab.entries.forEach { t ->
                            AppChip(stringResource(tabLabel(t), shopItems(t).size), t == tab, { onTab(t) })
                        }
                    }
                    when (tab) {
                        ShopTab.SEED, ShopTab.FURNITURE -> ProductGrid(shopItems(tab), profile, 260.dp, ProductSize, onPick) { item ->
                            Text(stringResource(R.string.shop_count, profile.items[item.id] ?: 0), color = AppColors.TextSub, fontSize = 29.sp)
                        }
                        ShopTab.CHARACTER -> ProductGrid(shopItems(tab), profile, 260.dp, CharacterSize, onPick) { item ->
                            if (item.voiceId != null) {
                                Text(stringResource(R.string.shop_with_voice), color = AppColors.TextSub, fontSize = 29.sp)
                            }
                        }
                        ShopTab.POSE -> ProductGrid(
                            shopItems(tab), profile, 177.dp, PoseSize, onPick,
                            header = stringResource(R.string.shop_pose_any),
                        )
                    }
                }
            }
        }
    }
}

private fun tabLabel(tab: ShopTab) = when (tab) {
    ShopTab.SEED -> R.string.shop_tab_seed
    ShopTab.FURNITURE -> R.string.shop_tab_furniture
    ShopTab.CHARACTER -> R.string.shop_tab_character
    ShopTab.POSE -> R.string.shop_tab_pose
}

@Composable
private fun ProductGrid(
    items: List<CatalogItem>,
    profile: Profile,
    minCell: Dp,
    size: TileSize,
    onPick: (CatalogItem) -> Unit,
    header: String? = null,
    footer: @Composable (CatalogItem) -> Unit = {},
) {
    LazyVerticalGrid(
        GridCells.Adaptive(minCell),
        horizontalArrangement = Arrangement.spacedBy(Gap),
        verticalArrangement = Arrangement.spacedBy(Gap),
    ) {
        if (header != null) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(header, color = AppColors.TextSub, fontSize = 31.sp, fontWeight = FontWeight.Bold)
            }
        }
        items(items, key = { it.id }) { item ->
            Tile(item, profile, Modifier.fillMaxWidth(), size, onPick) { footer(item) }
        }
    }
}

/** 칸 종류마다 다른 크기. Figma S-19-a(상품), S-19-e(캐릭터, 포즈) 값 x 1.3. */
private class TileSize(val padding: Dp, val box: Dp, val image: Dp, val name: TextUnit, val price: TextUnit)

private val ProductSize = TileSize(18.dp, 156.dp, 130.dp, 39.sp, 39.sp)
private val CharacterSize = TileSize(18.dp, 143.dp, 130.dp, 34.sp, 36.sp)
private val PoseSize = TileSize(13.dp, 120.dp, 109.dp, 31.sp, 36.sp)

/** 상품 칸. 하나만 가지는 것을 이미 가졌으면 "소유 중"을 보이고 누를 수 없다. */
@Composable
private fun Tile(
    item: CatalogItem,
    profile: Profile,
    modifier: Modifier,
    size: TileSize,
    onPick: (CatalogItem) -> Unit,
    footer: @Composable () -> Unit = {},
) {
    val owned = !item.isCounted() && item.id in profile.owned
    val price = item.price
    val canPick = !owned && price != null
    AppCard(modifier.clip(TileShape).clickable(enabled = canPick, role = Role.Button) { onPick(item) }) {
        Column(
            Modifier.padding(size.padding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(Modifier.fillMaxWidth().height(size.box).clip(BoxShape).background(AppColors.Accent), Alignment.Center) {
                ItemImage(item.id, item.name, size.image)
            }
            Text(item.name, color = AppColors.Text, fontSize = size.name, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 1)
            when {
                owned -> Text(stringResource(R.string.shop_owned), color = AppColors.Success, fontSize = 29.sp, fontWeight = FontWeight.Bold)
                price != null -> CoinAmount(price, size.price, if (price > profile.coins) AppColors.Error else AppColors.Text)
            }
            footer()
        }
    }
}

/** D-14, D-17, D-19. 씨앗과 가구만 개수를 고른다. 캐릭터는 함께 받는 목소리를, 포즈는 쓰는 곳을 밝힌다. */
@Composable
internal fun ColumnScope.BuyDialogBody(
    item: CatalogItem,
    coins: Int,
    qty: Int,
    config: BalanceConfig,
    onQty: (Int) -> Unit,
    onCancel: () -> Unit,
    onBuy: () -> Unit,
) {
    val count = if (item.isCounted()) qty else 1
    val total = totalPrice(item, count)
    if (item.isCounted()) {
        ItemHeaderRow(item.id, item.name) { CoinAmount(item.price ?: 0, 39.sp) }
        QtyStepper(count, 1, config.shopMaxQty) { onQty(clampQty(it, config)) }
    } else {
        BigImageBox(item.id, item.name)
        DialogTitle(buyTitle(item))
        buyNote(item)?.let { Text(it, color = AppColors.TextSub, fontSize = 36.sp) }
    }
    CoinSummary(coins, total)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(35.dp)) {
        SecondaryButton(stringResource(R.string.shop_cancel), onCancel, Modifier.weight(1f))
        if (total <= coins) {
            PrimaryButton(stringResource(R.string.shop_buy), onBuy, Modifier.weight(1f))
        } else {
            DisabledButton(stringResource(R.string.shop_buy), Modifier.weight(1f))
        }
    }
}

@Composable
private fun buyTitle(item: CatalogItem): String = when (item.kind) {
    ItemKind.POSE -> stringResource(R.string.shop_buy_pose_title, item.name)
    else -> stringResource(R.string.shop_buy_title, item.name, josa(item.name, "을", "를"))
}

@Composable
private fun buyNote(item: CatalogItem): String? = when (item.kind) {
    ItemKind.POSE -> stringResource(R.string.shop_pose_any)
    else -> item.voiceId?.let(Catalog::item)?.let { stringResource(R.string.shop_buy_with_voice, it.name) }
}

/** D-18. 바꾸기는 대표 캐릭터를 산 것으로 바꾸고, 함께 받은 목소리가 있으면 목소리도 바꾼다. */
@Composable
internal fun ColumnScope.SwitchDialogBody(item: CatalogItem, onLater: () -> Unit, onSwitch: () -> Unit) {
    BigImageBox(item.id, item.name)
    DialogTitle(stringResource(if (item.voiceId != null) R.string.shop_switch_character_voice else R.string.shop_switch_character))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(35.dp)) {
        SecondaryButton(stringResource(R.string.shop_later), onLater, Modifier.weight(1f))
        PrimaryButton(stringResource(R.string.shop_switch), onSwitch, Modifier.weight(1f))
    }
}
