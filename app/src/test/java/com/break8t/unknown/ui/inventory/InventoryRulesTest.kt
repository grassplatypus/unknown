package com.break8t.unknown.ui.inventory

import com.break8t.unknown.data.DemoState
import com.break8t.unknown.data.Profile
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
}
