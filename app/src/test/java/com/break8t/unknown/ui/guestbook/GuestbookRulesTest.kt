package com.break8t.unknown.ui.guestbook

import com.break8t.unknown.config.BalanceConfig
import com.break8t.unknown.config.Catalog
import com.break8t.unknown.config.ItemKind
import com.break8t.unknown.data.DemoState
import com.break8t.unknown.data.Profile
import com.break8t.unknown.ui.inventory.InventoryTab
import com.break8t.unknown.ui.inventory.discard
import com.break8t.unknown.ui.inventory.inventoryEntries
import com.break8t.unknown.ui.island.IslandMap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GuestbookRulesTest {
    private val now = 1_700_000_000_000L
    private val mine = demoGuestbooks(now).getValue(GuestbookStore.MY_ISLAND)
    private val lastVisit = now - DAY_MS

    private fun entry(author: String, at: Long = now) = GuestbookEntry("e_$author$at", author, author, "ch_cat", at, 5)

    @Test fun sampleHasOneOrTwoNewEntriesSinceTheSampleVisit() {
        assertEquals(listOf("g_me_1", "g_me_2"), newSince(mine, lastVisit).map { it.id })
        assertTrue(mine.size in 3..4)
    }

    @Test fun friendIslandsHaveOneToThreeSamplesUnlessTheBoardIsStored() {
        val books = demoGuestbooks(now)
        for (id in listOf("f_acorn", "f_dawn", "f_camper", "f_dusk")) {
            assertTrue(id, books.getValue(id).size in 1..3)
            assertTrue(id, friendBoardShown(id))
        }
        assertFalse(friendBoardShown("f_turtle"))
        assertNull(books["f_turtle"])
    }

    @Test fun newCountsOnlyLaterEntriesFromOthers() {
        val list = listOf(entry("f_dawn", now), entry("f_acorn", lastVisit), entry(GuestbookStore.ME, now))
        assertEquals(listOf("f_dawn"), newSince(list, lastVisit).map { it.authorId })
        assertTrue(newSince(list, now).isEmpty())
    }

    @Test fun noticeUsesTheSavedVisitOrTheSampleVisit() {
        assertEquals(2, noticeCount(Profile(), mine, lastVisit))
        assertEquals(0, noticeCount(Profile(guestbookLastVisitMs = now), mine, lastVisit))
        assertEquals(4, noticeCount(Profile(guestbookLastVisitMs = 0L), mine, lastVisit))
    }

    @Test fun storingTheBoardTurnsTheGuestbookOffAndPlacingTurnsItOn() {
        val placed = Profile()
        assertTrue(guestbookOn(placed))
        val stored = withBoard(placed, placed = false)
        assertFalse(guestbookOn(stored))
        assertEquals(0, noticeCount(stored, mine, lastVisit))
        assertTrue(guestbookOn(withBoard(stored, placed = true)))
    }

    @Test fun theBoardIsAnOutdoorDecorationNotFurnitureAndNotSold() {
        val board = Catalog.item("deco_board")!!
        assertEquals(ItemKind.OUTDOOR, board.kind)
        assertNull(board.price)
        assertFalse(Catalog.ofKind(ItemKind.FURNITURE).any { it.id == "deco_board" })
        for (tab in com.break8t.unknown.ui.shop.ShopTab.entries) {
            assertTrue(tab.name, com.break8t.unknown.ui.shop.shopItems(tab).none { it.id == "deco_board" })
        }
    }

    @Test fun theBoardIsAlwaysOneInTheInventoryWhetherPlacedOrStored() {
        for (p in listOf(Profile(), Profile(guestbookBoardPlaced = false), Profile(items = emptyMap()))) {
            assertEquals(listOf("deco_board" to 1), inventoryEntries(p, InventoryTab.OUTDOOR).map { it.item.id to it.count })
            assertTrue(inventoryEntries(p, InventoryTab.FURNITURE).none { it.item.id == "deco_board" })
        }
    }

    @Test fun theBoardCannotBeDiscardedEvenIfACountSneaksIn() {
        val s = DemoState(Profile(items = mapOf("deco_board" to 1, "it_rug" to 1)))
        assertFalse(isDiscardable("deco_board"))
        assertFalse(discard(s, "deco_board", 1))
        assertEquals(1, s.profile.value.items["deco_board"])
        assertTrue(isDiscardable("it_rug"))
        assertTrue(discard(s, "it_rug", 1))
    }

    @Test fun ownerDeletesAnyEntryOnTheirIslandOthersOnlyTheirOwn() {
        val fromFriend = entry("f_dawn")
        val fromMe = entry(GuestbookStore.ME)
        assertTrue(canDelete(GuestbookStore.MY_ISLAND, fromFriend))
        assertTrue(canDelete("f_acorn", fromMe))
        assertFalse(canDelete("f_acorn", fromFriend))
        // Seen from the friend's side: the island owner may remove what I left.
        assertTrue(canDelete("f_acorn", fromMe, viewerId = "f_acorn"))
    }

    @Test fun recordingCountsDownToZeroAndNeverExceedsTheLimit() {
        val cfg = BalanceConfig(guestbookMaxSec = 15)
        assertEquals(15, recordSecondsLeft(0, cfg))
        assertEquals(15, recordSecondsLeft(999, cfg))
        assertEquals(14, recordSecondsLeft(1000, cfg))
        assertEquals(1, recordSecondsLeft(14_001, cfg))
        assertEquals(0, recordSecondsLeft(15_000, cfg))
        assertEquals(0, recordSecondsLeft(20_000, cfg))
        assertEquals(1, recordedSeconds(200, cfg))
        assertEquals(6, recordedSeconds(6_400, cfg))
        assertEquals(15, recordedSeconds(16_000, cfg))
        assertEquals(15, BalanceConfig.DEFAULT.guestbookMaxSec)
    }

    @Test fun recordStepFollowsSoundDraftAndSaving() {
        val draft = GuestbookDraft("/d.m4a", 4)
        assertEquals(RecordStep.IDLE, recordStep(GuestbookSound.Idle, null, false))
        assertEquals(RecordStep.RECORDING, recordStep(GuestbookSound.Recording(now), null, false))
        assertEquals(RecordStep.RECORDED, recordStep(GuestbookSound.Idle, draft, false))
        assertEquals(RecordStep.RECORDED, recordStep(GuestbookSound.Playing(DRAFT_KEY, now, 4000), draft, false))
        assertEquals(RecordStep.SAVING, recordStep(GuestbookSound.Idle, draft, true))
    }

    @Test fun agoPicksTheLargestWholeUnit() {
        assertEquals(Ago.JustNow, ago(now, now - 30_000))
        assertEquals(Ago.Minutes(5), ago(now, now - 5 * 60_000))
        assertEquals(Ago.Hours(2), ago(now, now - 2 * 3_600_000))
        assertEquals(Ago.Days(3), ago(now, now - 3 * DAY_MS))
        assertEquals(Ago.JustNow, ago(now, now + 60_000))
    }

    @Test fun theSceneHasTheBoardOnGrassOnlyWhenPlaced() {
        val with = IslandMap.scene(emptyList(), "car_red")
        val board = with.objects.single { it.id == IslandMap.BOARD_ID }
        assertTrue(board.pickable)
        val cell = with.cells[board.y * with.width + board.x]
        assertEquals(com.break8t.unknown.ui.iso.FloorType.GRASS, cell.floor)
        // Nothing else stands on the board's cell, so it covers no house, stall, farm or road object.
        assertTrue(with.objects.none { it.id != board.id && it.z == 0f && board.x in it.x until it.x + it.footW && board.y in it.y until it.y + it.footD })
        assertTrue(IslandMap.scene(emptyList(), "car_red", board = false).objects.none { it.id == IslandMap.BOARD_ID })
    }

    /** The guestbook chip stands just above the board's drawn pixels, so it points at the board. */
    @Test fun boardChipAnchorSitsOnTheBoard() {
        val scene = IslandMap.scene(emptyList(), "car_red")
        val f = com.break8t.unknown.ui.iso.renderScene(scene, com.break8t.unknown.ui.iso.IsoCamera.of(com.break8t.unknown.ui.iso.ViewPreset.OBLIQUE, 0), 500, 390)
        var x0 = Int.MAX_VALUE; var x1 = -1; var y0 = Int.MAX_VALUE
        for (y in 0 until f.h) for (x in 0 until f.w) if (f.objectAt(x, y) == IslandMap.BOARD_ID) {
            x0 = minOf(x0, x); x1 = maxOf(x1, x); y0 = minOf(y0, y)
        }
        assertTrue("board drawn", x1 >= 0)
        val a = IslandMap.BOARD_ANCHOR
        val sx = f.projection.screenX(a[0], a[1], a[2]); val sy = f.projection.screenY(a[0], a[1], a[2])
        assertTrue("x $sx in $x0..$x1", sx in x0.toFloat()..x1 + 1f)
        assertTrue("y $sy near top $y0", sy in y0 - 4f..y0 + 2f)
    }
}
