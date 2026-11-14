package com.break8t.unknown.ui.house

import com.break8t.unknown.config.BalanceConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class HouseLayoutTest {
    private val cfg = BalanceConfig(houseGridSize = 5, houseWallHeight = 4)

    @Test fun placeNewPutsFloorItemInFirstFreeSpot() {
        val l = placeNew(emptyList(), "it_sofa", cfg)!!
        assertEquals(Placed("it_sofa", Surface.FLOOR, 0, 0), l.single())
        val l2 = placeNew(l, "it_plant", cfg)!!
        assertEquals(Placed("it_plant", Surface.FLOOR, 2, 0), l2[1])
    }

    @Test fun wallItemGoesOnWallAtEyeLevel() {
        val p = placeNew(emptyList(), "it_picture", cfg)!!.single()
        assertEquals(Surface.LEFT_WALL, p.surface)
        assertEquals(cfg.houseWallHeight / 2, p.y)
    }

    @Test fun placeNewReturnsNullWhenFloorIsFull() {
        var l: List<Placed> = emptyList()
        repeat(25) { l = placeNew(l, "it_plant", cfg)!! }
        assertNull(placeNew(l, "it_plant", cfg))
    }

    @Test fun furnitureNeverOverlaps() {
        val l = listOf(Placed("it_rug", Surface.FLOOR, 1, 1))
        assertFalse(fits(l, Placed("it_plant", Surface.FLOOR, 2, 2), cfg))
        assertTrue(fits(l, Placed("it_plant", Surface.FLOOR, 3, 1), cfg))
        assertFalse(fits(l, Placed("it_sofa", Surface.FLOOR, 0, 1), cfg))
    }

    @Test fun outOfBoundsAndWrongSurfaceDoNotFit() {
        assertFalse(fits(emptyList(), Placed("it_sofa", Surface.FLOOR, 4, 0), cfg))
        assertFalse(fits(emptyList(), Placed("it_picture", Surface.FLOOR, 0, 0), cfg))
        assertFalse(fits(emptyList(), Placed("it_plant", Surface.LEFT_WALL, 0, 0), cfg))
        assertFalse(fits(emptyList(), Placed("it_picture", Surface.RIGHT_WALL, 0, cfg.houseWallHeight), cfg))
    }

    @Test fun moveIgnoresItselfButRefusesOverlap() {
        val l = listOf(Placed("it_sofa", Surface.FLOOR, 0, 0), Placed("it_plant", Surface.FLOOR, 0, 2))
        assertEquals(Placed("it_sofa", Surface.FLOOR, 1, 0), moveTo(l, 0, Cell(Surface.FLOOR, 1, 0), cfg)[0])
        assertSame(l, moveTo(l, 0, Cell(Surface.FLOOR, 0, 2), cfg))
    }

    @Test fun moveClampsToGridEdge() {
        val l = listOf(Placed("it_sofa", Surface.FLOOR, 0, 0))
        assertEquals(3, moveTo(l, 0, Cell(Surface.FLOOR, 4, 0), cfg)[0].x)
    }

    @Test fun moveKeepsWallItemOffTheFloor() {
        val l = listOf(Placed("it_picture", Surface.LEFT_WALL, 0, 1))
        assertSame(l, moveTo(l, 0, Cell(Surface.FLOOR, 2, 2), cfg))
        assertEquals(Surface.RIGHT_WALL, moveTo(l, 0, Cell(Surface.RIGHT_WALL, 2, 2), cfg)[0].surface)
    }

    @Test fun rotateSwapsFootprintAndCycles() {
        val l = listOf(Placed("it_sofa", Surface.FLOOR, 0, 0))
        val once = rotate(l, 0, cfg)!!
        assertEquals(1 to 2, footprint(once[0]))
        var p = once
        repeat(3) { p = rotate(p, 0, cfg)!! }
        assertEquals(0, p[0].turns)
    }

    @Test fun rotateRefusedWhenBlockedOrOnWall() {
        val l = listOf(Placed("it_sofa", Surface.FLOOR, 0, 0), Placed("it_plant", Surface.FLOOR, 0, 1))
        assertNull(rotate(l, 0, cfg))
        assertNull(rotate(listOf(Placed("it_picture", Surface.LEFT_WALL, 0, 0)), 0, cfg))
    }

    @Test fun storeBackAndResetReturnItemsToStorage() {
        val owned = mapOf("it_plant" to 2, "it_sofa" to 1)
        val l = listOf(Placed("it_plant", Surface.FLOOR, 0, 0), Placed("it_sofa", Surface.FLOOR, 2, 2))
        assertEquals(1, available(owned, l, "it_plant"))
        assertEquals(0, available(owned, l, "it_sofa"))
        assertEquals(2, available(owned, storeBack(l, 0), "it_plant"))
        assertEquals(1, available(owned, reset(), "it_sofa"))
    }

    @Test fun trimDropsPlacementsBeyondOwnedCount() {
        val l = listOf(Placed("it_plant", Surface.FLOOR, 0, 0), Placed("it_plant", Surface.FLOOR, 1, 0))
        assertEquals(listOf(l[0]), trimToOwned(l, mapOf("it_plant" to 1)))
    }

    @Test fun exitAsksOnlyWhenChanged() {
        val saved = DefaultLayout
        assertEquals(ExitAction.LEAVE, exitAction(saved, saved.toList()))
        assertEquals(ExitAction.ASK_SAVE, exitAction(saved, storeBack(saved, 0)))
    }

    @Test fun saveKeepsDraftDiscardKeepsSaved() {
        val draft = reset()
        assertEquals(draft, resolveExit(DefaultLayout, draft, save = true))
        assertEquals(DefaultLayout, resolveExit(DefaultLayout, draft, save = false))
    }

    @Test fun defaultLayoutIsValid() {
        DefaultLayout.forEachIndexed { i, p -> assertTrue(fits(DefaultLayout, p, BalanceConfig.DEFAULT, ignore = i)) }
    }

    @Test fun encodeRoundTrips() {
        assertEquals(DefaultLayout, decode(encode(DefaultLayout)))
    }

    @Test fun unfoldedGridHitsTheCellsItDraws() {
        for (p in DefaultLayout + Placed("it_picture", Surface.LEFT_WALL, 2, 3)) {
            val r = UnfoldedGrid.rectOf(p, cfg)
            for (c in r.col until r.col + r.w) for (row in r.row until r.row + r.h) {
                val cell = UnfoldedGrid.cellAt(c, row, cfg)
                assertNotNull(cell)
                assertTrue("$p at $cell", cell in cellsOf(p))
            }
        }
        assertNull(UnfoldedGrid.cellAt(0, 0, cfg))
    }
}
