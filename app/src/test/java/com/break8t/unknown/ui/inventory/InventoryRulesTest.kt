package com.break8t.unknown.ui.inventory

import com.break8t.unknown.data.DemoState
import com.break8t.unknown.data.Profile
import com.break8t.unknown.ui.house.Placed
import com.break8t.unknown.ui.house.Surface
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InventoryRulesTest {
    private fun state() = DemoState(Profile(items = mapOf("it_rug" to 3, "it_sofa" to 0, "seed_carrot" to 2)))

    @Test fun discardRemovesChosenCount() {
        val s = state()
        assertTrue(discard(s, "it_rug", 2))
        assertEquals(1, s.profile.value.items["it_rug"])
    }

    @Test fun discardAllHidesTheEntry() {
        val s = state()
        assertTrue(discard(s, "it_rug", 3))
        assertTrue(inventoryEntries(s.profile.value, InventoryTab.FURNITURE).isEmpty())
    }

    @Test fun discardMoreThanHeldOrNothingChangesNothing() {
        val s = state()
        val before = s.profile.value
        assertFalse(discard(s, "it_rug", 4))
        assertFalse(discard(s, "it_rug", 0))
        assertFalse(discard(s, "it_rug", -1))
        assertFalse(discard(s, "it_lamp", 1))
        assertEquals(before, s.profile.value)
    }

    @Test fun entriesSkipZeroCountsAndSplitByTab() {
        val p = state().profile.value
        assertEquals(listOf("it_rug"), inventoryEntries(p, InventoryTab.FURNITURE).map { it.item.id })
        assertEquals(listOf("seed_carrot" to 2), inventoryEntries(p, InventoryTab.SEED).map { it.item.id to it.count })
    }

    private val room = listOf(
        Placed("it_rug", Surface.FLOOR, 0, 0), Placed("it_rug", Surface.FLOOR, 2, 2),
        Placed("it_sofa", Surface.FLOOR, 0, 3), // owned 0, so the house drops it and it counts for nothing
    )

    @Test fun furnitureInTheHouseIsNotListed() {
        val p = state().profile.value
        assertEquals(listOf("it_rug" to 1), inventoryEntries(p, InventoryTab.FURNITURE, room).map { it.item.id to it.count })
        val all = room + Placed("it_rug", Surface.FLOOR, 4, 0)
        assertTrue(inventoryEntries(p, InventoryTab.FURNITURE, all).isEmpty())
    }

    @Test fun onlyUnplacedFurnitureCanBeDiscarded() {
        val s = state()
        assertFalse(discard(s, "it_rug", 2, room))
        assertEquals(3, s.profile.value.items["it_rug"])
        assertTrue(discard(s, "it_rug", 1, room))
        assertEquals(2, s.profile.value.items["it_rug"])
    }
}
