package com.break8t.unknown.ui.shop

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
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
import com.break8t.unknown.ui.components.ControlShape
import com.break8t.unknown.ui.components.PrimaryButton
import com.break8t.unknown.ui.components.SecondaryButton
import com.break8t.unknown.ui.theme.AppColors

/** 상점 진입점(S-19). 섬으로와 보관함은 다른 묶음 화면이라 콜백으로 알린다. */
@Composable
fun ShopScreen(onBack: () -> Unit, onOpenInventory: () -> Unit) {
    val state = LocalDemoState.current
    val profile by state.profile.collectAsState()
    val context = LocalContext.current
    val config = BalanceConfig.DEFAULT
    var tab by rememberSaveable { mutableStateOf(ShopTab.SEED) }
    var playingId by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingId by rememberSaveable { mutableStateOf<String?>(null) }
    var qty by rememberSaveable { mutableIntStateOf(1) }
    var askId by rememberSaveable { mutableStateOf<String?>(null) }
    var toast by rememberSaveable { mutableStateOf<String?>(null) }

    Box(Modifier.fillMaxSize()) {
        ShopContent(
            profile, tab, playingId, onBack, onOpenInventory,
            onTab = { tab = it },
            onTogglePlay = { id -> playingId = if (playingId == id) null else id },
            onPick = { pendingId = it.id; qty = 1 },
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
                    askAfterBuying(item) != null -> askId = item.id
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
    playingId: String?,
    onBack: () -> Unit,
    onOpenInventory: () -> Unit,
    onTab: (ShopTab) -> Unit,
    onTogglePlay: (String) -> Unit,
    onPick: (CatalogItem) -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize().padding(ScreenPadding)) {
        // Split screen (about 660dp of body) has no room for the stall picture beside the grid.
        val wide = maxWidth >= 1000.dp
        Column(verticalArrangement = Arrangement.spacedBy(Gap)) {
            HeaderBar(profile.coins) {
                HeaderChip(stringResource(R.string.shop_back), R.drawable.shop_ic_back, selected = false, onClick = onBack)
                HeaderChip(stringResource(R.string.shop_title), null, selected = true, onClick = null)
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
                        ShopTab.SEED, ShopTab.FURNITURE -> ProductGrid(shopItems(tab), profile, onPick)
                        ShopTab.VOICE -> VoiceList(profile, playingId, wide, onTogglePlay, onPick)
                        ShopTab.CHARACTER -> CharacterTab(profile, onPick)
                    }
                }
            }
        }
    }
}

private fun tabLabel(tab: ShopTab) = when (tab) {
    ShopTab.SEED -> R.string.shop_tab_seed
    ShopTab.FURNITURE -> R.string.shop_tab_furniture
    ShopTab.VOICE -> R.string.shop_tab_voice
    ShopTab.CHARACTER -> R.string.shop_tab_character
}

@Composable
private fun ProductGrid(items: List<CatalogItem>, profile: Profile, onPick: (CatalogItem) -> Unit) {
    LazyVerticalGrid(
        GridCells.Adaptive(260.dp),
        horizontalArrangement = Arrangement.spacedBy(Gap),
        verticalArrangement = Arrangement.spacedBy(Gap),
    ) {
        items(items, key = { it.id }) { item ->
            Tile(item, profile, Modifier.fillMaxWidth(), ProductSize, onPick) {
                Text(
                    stringResource(R.string.shop_count, profile.items[item.id] ?: 0),
                    color = AppColors.TextSub,
                    fontSize = 29.sp,
                )
            }
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

@Composable
private fun VoiceList(
    profile: Profile,
    playingId: String?,
    wide: Boolean,
    onTogglePlay: (String) -> Unit,
    onPick: (CatalogItem) -> Unit,
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(26.dp)) {
        items(shopItems(ShopTab.VOICE), key = { it.id }) { item ->
            VoiceRow(item, profile, playing = item.id == playingId, wide, { onTogglePlay(item.id) }, { onPick(item) })
        }
    }
}

/** S-19-c, S-19-d. 미리 듣기는 아직 녹음 파일이 없어서 재생 표시만 바꾼다. */
@Composable
private fun VoiceRow(item: CatalogItem, profile: Profile, playing: Boolean, wide: Boolean, onTogglePlay: () -> Unit, onBuy: () -> Unit) {
    AppCard(Modifier.fillMaxWidth()) {
        Row(
            Modifier.heightIn(min = 161.dp).padding(horizontal = 23.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(26.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (wide) {
                Box(Modifier.size(109.dp).clip(TileShape).background(AppColors.Accent), Alignment.Center) {
                    ItemImage(item.id, item.name, 109.dp)
                }
            }
            Box(
                Modifier
                    .size(109.dp)
                    .clip(ControlShape)
                    .background(AppColors.Card)
                    .border(3.dp, AppColors.Line, ControlShape)
                    .clickable(role = Role.Button, onClick = onTogglePlay),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(if (playing) R.drawable.shop_ic_pause else R.drawable.shop_ic_play),
                    contentDescription = stringResource(if (playing) R.string.shop_preview_stop else R.string.shop_preview),
                    Modifier.size(52.dp),
                    tint = AppColors.Text,
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(item.name, color = AppColors.Text, fontSize = 42.sp, fontWeight = FontWeight.Bold)
                if (playing) {
                    Text(stringResource(R.string.shop_previewing), color = AppColors.Success, fontSize = 31.sp, fontWeight = FontWeight.Bold)
                }
            }
            VoiceStatus(item, profile, onBuy)
        }
    }
}

@Composable
private fun VoiceStatus(item: CatalogItem, profile: Profile, onBuy: () -> Unit) {
    val price = item.price
    when {
        item.id == profile.voicePackId -> StatusText(stringResource(R.string.shop_in_use))
        item.id in profile.owned -> StatusText(stringResource(R.string.shop_owned))
        price != null -> Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val short = price > profile.coins
            CoinAmount(price, 39.sp, if (short) AppColors.Error else AppColors.Text)
            SmallBuyButton(enabled = !short, onBuy)
        }
    }
}

@Composable
private fun StatusText(text: String) = Text(text, color = AppColors.Success, fontSize = 36.sp, fontWeight = FontWeight.Bold)

@Composable
private fun SmallBuyButton(enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(width = 199.dp, height = 68.dp)
            .clip(ControlShape)
            .background(if (enabled) AppColors.Primary else AppColors.Disabled)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            stringResource(R.string.shop_buy),
            color = if (enabled) AppColors.Card else AppColors.DisabledText,
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

/** S-19-e. 캐릭터와 사진 포즈를 줄마다 옆으로 넘겨 본다. */
@Composable
private fun CharacterTab(profile: Profile, onPick: (CatalogItem) -> Unit) {
    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel(stringResource(R.string.shop_section_character))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(Gap)) {
            items(Catalog.ofKind(ItemKind.CHARACTER), key = { it.id }) { Tile(it, profile, Modifier.width(294.dp), CharacterSize, onPick) }
        }
        SectionLabel(stringResource(R.string.shop_section_pose), Modifier.padding(top = 23.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(17.dp)) {
            items(Catalog.ofKind(ItemKind.POSE), key = { it.id }) { Tile(it, profile, Modifier.width(177.dp), PoseSize, onPick) }
        }
    }
}

@Composable
private fun SectionLabel(text: String, modifier: Modifier = Modifier) = Text(text, modifier, color = AppColors.TextSub, fontSize = 31.sp, fontWeight = FontWeight.Bold)

/** D-14, D-15, D-17, D-19. 씨앗과 가구만 개수를 고른다. */
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

/** D-16(음성팩), D-18(캐릭터). 바꾸기는 지금 쓰는 것을 산 것으로 바꾼다. */
@Composable
internal fun ColumnScope.SwitchDialogBody(item: CatalogItem, onLater: () -> Unit, onSwitch: () -> Unit) {
    val voice = item.kind == ItemKind.VOICE_PACK
    if (!voice) BigImageBox(item.id, item.name)
    DialogTitle(stringResource(if (voice) R.string.shop_switch_voice else R.string.shop_switch_character))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(35.dp)) {
        SecondaryButton(stringResource(R.string.shop_later), onLater, Modifier.weight(1f))
        PrimaryButton(stringResource(R.string.shop_switch), onSwitch, Modifier.weight(1f))
    }
}
