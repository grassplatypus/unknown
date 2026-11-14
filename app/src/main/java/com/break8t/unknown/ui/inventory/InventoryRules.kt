package com.break8t.unknown.ui.inventory

import com.break8t.unknown.config.Catalog
import com.break8t.unknown.config.CatalogItem
import com.break8t.unknown.config.ItemKind
import com.break8t.unknown.data.DemoState
import com.break8t.unknown.data.Profile
import com.break8t.unknown.ui.house.Placed
import com.break8t.unknown.ui.house.available
import com.break8t.unknown.ui.house.trimToOwned
import com.break8t.unknown.ui.guestbook.isDiscardable

/** 보관함 탭. Figma S-18 순서(가구, 씨앗) 뒤에 섬에 세우는 야외 장식을 둔다. */
enum class InventoryTab(val kind: ItemKind) {
    FURNITURE(ItemKind.FURNITURE), SEED(ItemKind.SEED), OUTDOOR(ItemKind.OUTDOOR),
}

data class InventoryEntry(val item: CatalogItem, val count: Int)

/**
 * 집에 놓지 않은 개수가 1 이상인 것만 보인다. 0개인 키가 남아 있을 수 있어서 키가 있는지로 보지 않는다.
 * [placed]는 집의 저장된 배치다. 가진 개수보다 많이 놓인 것은 집 화면과 똑같이 셈에서 뺀다.
 * 야외 장식(게시판)은 늘 하나 가지고 있어서 개수 대신 Profile의 놓임 깃발만 본다.
 */
fun inventoryEntries(profile: Profile, tab: InventoryTab, placed: List<Placed> = emptyList()): List<InventoryEntry> {
    if (tab == InventoryTab.OUTDOOR) return Catalog.ofKind(ItemKind.OUTDOOR).map { InventoryEntry(it, 1) }
    val inHouse = trimToOwned(placed, profile.items)
    return Catalog.ofKind(tab.kind).mapNotNull { item ->
        available(profile.items, inHouse, item.id).takeIf { it > 0 }?.let { InventoryEntry(item, it) }
    }
}

/** 버린다. 1개부터 보관함에 있는 개수까지만 받고, 그 밖이거나 버릴 수 없는 것(야외 장식)이면 아무것도 바꾸지 않고 false다. 집에 놓인 것은 버리지 않는다. */
fun discard(state: DemoState, id: String, qty: Int, placed: List<Placed> = emptyList()): Boolean {
    if (!isDiscardable(id)) return false
    val p = state.profile.value
    val held = available(p.items, trimToOwned(placed, p.items), id)
    if (qty !in 1..held) return false
    return state.addItem(id, -qty)
}
