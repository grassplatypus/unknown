package com.break8t.unknown.ui.island

import com.break8t.unknown.config.BalanceConfig
import com.break8t.unknown.ui.house.DefaultLayout
import com.break8t.unknown.ui.house.SceneCharacter
import com.break8t.unknown.ui.house.fitsRoom
import com.break8t.unknown.ui.iso.IsoHit
import com.break8t.unknown.ui.myinfo.Page
import com.break8t.unknown.ui.myinfo.backFrom
import com.break8t.unknown.ui.myinfo.demoFriends
import com.break8t.unknown.ui.photo.CharacterSizes
import com.break8t.unknown.ui.photo.FRIEND_CHARACTER_ID
import com.break8t.unknown.ui.photo.ISLAND_CHARACTER_SCALE
import com.break8t.unknown.ui.photo.ISLAND_SPAWN
import com.break8t.unknown.ui.photo.friendPhotoSpawn
import com.break8t.unknown.ui.photo.photoObjects
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class FriendVisitTest {
    private val cfg = BalanceConfig.DEFAULT

    @Test fun boardLabelOnlyWhenTheFriendKeepsTheBoardOut() {
        assertEquals(listOf(FriendSpot.HOUSE, FriendSpot.BOARD), friendSpots(board = true, steering = false))
        assertEquals(listOf(FriendSpot.HOUSE), friendSpots(board = false, steering = false))
        assertTrue(friendSpots(board = true, steering = true).isEmpty())
    }

    @Test fun tapsOnTheHouseAndTheBoardOpenThem() {
        assertEquals(FriendSpot.HOUSE, friendSpotAt(IsoHit.Item("house"), board = true))
        assertEquals(FriendSpot.BOARD, friendSpotAt(IsoHit.Item(IslandMap.BOARD_ID), board = true))
        assertNull(friendSpotAt(IsoHit.Item(IslandMap.BOARD_ID), board = false))
        assertNull(friendSpotAt(IsoHit.Item("stall"), board = true))
        assertNull(friendSpotAt(IsoHit.Cell(2, 2), board = true))
    }

    @Test fun friendHouseIsTheSameEveryVisitAndFitsTheRoom() {
        for (f in demoFriends) {
            val layout = friendHouseLayout(f.id, cfg)
            assertEquals(layout, friendHouseLayout(f.id, cfg))
            assertEquals(DefaultLayout, layout.take(DefaultLayout.size))
            assertTrue(layout.size > DefaultLayout.size)
            layout.forEachIndexed { i, p -> assertTrue("${f.id} $p", fitsRoom(layout, p, cfg, ignore = i)) }
        }
        assertTrue(demoFriends.map { friendHouseLayout(it.id, cfg) }.toSet().size > 1)
    }

    @Test fun friendPhotoHoldsBothCharactersUnderDifferentIds() {
        val friend = SceneCharacter("pose_wave", ISLAND_SPAWN.first, ISLAND_SPAWN.second, CharacterSizes[1], 0)
        val mine = SceneCharacter("ch_sprout", 0.4f, 0.4f, CharacterSizes[1], 0)
        val objs = photoObjects(mine, "ch_sprout", 10, ISLAND_CHARACTER_SCALE, editing = true, friend, "ch_cat")
        assertEquals(listOf(FRIEND_CHARACTER_ID, "character"), objs.map { it.id })
        assertFalse(objs[0].pickable)
        assertTrue(objs[1].pickable)
        assertEquals(listOf("character"), photoObjects(mine, "ch_sprout", 10, 1f, editing = false).map { it.id })
    }

    @Test fun myCharacterDoesNotStartOnTopOfTheFriendsOne() {
        for (grid in cfg.islandSizes) {
            val (x, y) = friendPhotoSpawn(grid)
            val apart = maxOf(abs(x - ISLAND_SPAWN.first), abs(y - ISLAND_SPAWN.second)) * grid
            assertTrue("grid $grid", apart >= ISLAND_CHARACTER_SCALE)
        }
    }

    @Test fun friendHouseGoesBackToTheFriendIsland() {
        assertEquals(Page.FRIEND_ISLAND, backFrom(Page.FRIEND_HOUSE))
    }
}
