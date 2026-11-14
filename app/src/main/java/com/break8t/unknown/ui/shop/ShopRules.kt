package com.break8t.unknown.ui.shop

import com.break8t.unknown.config.BalanceConfig
import com.break8t.unknown.config.Catalog
import com.break8t.unknown.config.CatalogItem
import com.break8t.unknown.config.ItemKind
import com.break8t.unknown.data.DemoState

enum class ShopTab(val kinds: List<ItemKind>) {
    SEED(listOf(ItemKind.SEED)),
    FURNITURE(listOf(ItemKind.FURNITURE)),
    VOICE(listOf(ItemKind.VOICE_PACK)),
    CHARACTER(listOf(ItemKind.CHARACTER, ItemKind.POSE)),
}

enum class BuyResult { BOUGHT, NOT_ENOUGH_COINS, ALREADY_OWNED, NOT_FOR_SALE, BAD_QUANTITY }

/** 씨앗과 가구는 개수로 사고, 나머지는 하나만 가진다. */
fun CatalogItem.isCounted(): Boolean = kind == ItemKind.SEED || kind == ItemKind.FURNITURE

fun shopItems(tab: ShopTab): List<CatalogItem> = Catalog.items.filter { it.kind in tab.kinds }

fun totalPrice(item: CatalogItem, qty: Int): Int = (item.price ?: 0) * qty

/** 개수 고르기의 범위. 상한은 BalanceConfig.shopMaxQty다. */
fun clampQty(qty: Int, config: BalanceConfig): Int = qty.coerceIn(1, config.shopMaxQty)

/**
 * 상품을 산다. 실패하면 코인과 보유 목록을 바꾸지 않는다.
 * @param qty 씨앗과 가구만 쓴다. 1부터 shopMaxQty까지가 아니면 BAD_QUANTITY다.
 */
fun buy(state: DemoState, item: CatalogItem, qty: Int, config: BalanceConfig): BuyResult {
    val profile = state.profile.value
    if (item.price == null) return BuyResult.NOT_FOR_SALE
    if (!item.isCounted() && item.id in profile.owned) return BuyResult.ALREADY_OWNED
    val count = if (item.isCounted()) qty else 1
    if (count !in 1..config.shopMaxQty) return BuyResult.BAD_QUANTITY
    if (!state.spend(totalPrice(item, count))) return BuyResult.NOT_ENOUGH_COINS
    if (item.isCounted()) state.addItem(item.id, count) else state.update { it.copy(owned = it.owned + item.id) }
    return BuyResult.BOUGHT
}

/** 산 뒤에 묻는 것. 음성팩은 D-16, 캐릭터는 D-18을 띄운다. */
fun askAfterBuying(item: CatalogItem): ItemKind? =
    item.kind.takeIf { it == ItemKind.VOICE_PACK || it == ItemKind.CHARACTER }

/** D-16, D-18의 바꾸기. */
fun switchTo(state: DemoState, item: CatalogItem) = state.update {
    when (item.kind) {
        ItemKind.VOICE_PACK -> it.copy(voicePackId = item.id)
        ItemKind.CHARACTER -> it.copy(selectedCharacterId = item.id)
        else -> it
    }
}

/** 받침이 있으면 [withFinal], 없으면 [without]. 한글이 아닌 끝 글자는 받침 없음으로 본다. */
fun josa(word: String, withFinal: String, without: String): String {
    val c = word.lastOrNull() ?: return without
    val hasFinal = c in '가'..'힣' && (c - '가') % 28 != 0
    return if (hasFinal) withFinal else without
}
