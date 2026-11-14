package com.break8t.unknown.ui.house

import com.break8t.unknown.config.BalanceConfig
import com.break8t.unknown.ui.iso.IsoCamera
import com.break8t.unknown.ui.iso.ViewPreset
import com.break8t.unknown.ui.iso.WallSide
import com.break8t.unknown.ui.iso.renderScene
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomMapTest {
    private val cfg = BalanceConfig.DEFAULT
    private val samples = DefaultLayout + listOf(
        Placed("it_picture", Surface.LEFT_WALL, 3, 3),
        Placed("it_shelf", Surface.FLOOR, 2, 3, turns = 2),
        Placed("it_rug", Surface.FLOOR, 3, 0, turns = 3),
    )

    @Test fun placementsSurviveTheTripThroughTheEngine() {
        samples.forEachIndexed { i, p ->
            val o = RoomMap.toObject(p, i, cfg)
            assertEquals(i, RoomMap.indexOf(o.id))
            assertEquals(p, RoomMap.toPlaced(o.placement, p))
        }
    }

    @Test fun wallsMapToTheWallsTheObliqueViewShows() {
        assertEquals(WallSide.NORTH, RoomMap.toObject(Placed("it_picture", Surface.RIGHT_WALL, 3, 2), 0, cfg).wall)
        assertEquals(WallSide.WEST, RoomMap.toObject(Placed("it_picture", Surface.LEFT_WALL, 3, 2), 0, cfg).wall)
        // Row 2 of 4 on a 3-cell wall hangs 1.5 cells up.
        assertEquals(1.5f, RoomMap.toObject(Placed("it_picture", Surface.RIGHT_WALL, 3, 2), 0, cfg).z, 1e-4f)
    }

    /** S-14: the default picture is drawn where the right-wall point projects, so it is on the visible wall. */
    @Test fun defaultLayoutDrawsEveryPieceWhereItsMappedPositionProjects() {
        val cam = IsoCamera.of(ViewPreset.OBLIQUE, 0)
        val f = renderScene(RoomMap.scene(DefaultLayout, cfg, cam), cam, 400, 300)
        val p = f.projection
        fun at(x: Float, y: Float, z: Float) = f.objectAt(p.screenX(x, y, z).toInt(), p.screenY(x, y, z).toInt())
        assertEquals("p2", at(3.5f, 0.05f, 1.5f + 3f / 8))
        assertEquals("p1", at(0.5f, 0.5f, 10f / 8))
        assertEquals("p0", at(0.5f, 2f, 3.5f / 8))
    }

    @Test fun fixedDecorCellsAreNeverOffered() {
        assertFalse(fitsRoom(emptyList(), Placed("it_picture", Surface.LEFT_WALL, 1, 1), cfg))
        assertFalse(fitsRoom(emptyList(), Placed("it_picture", Surface.RIGHT_WALL, 1, 3), cfg))
        assertTrue(fitsRoom(emptyList(), Placed("it_picture", Surface.RIGHT_WALL, 2, 3), cfg))
        var layout = emptyList<Placed>()
        while (true) layout = placeNewInRoom(layout, "it_picture", cfg) ?: break
        assertTrue(layout.none { p -> RoomMap.Fixed.any { it.surface == p.surface && it.x == p.x && it.y == p.y } })
        assertEquals(2 * cfg.houseGridSize * cfg.houseWallHeight - RoomMap.Fixed.size, layout.size)
    }

    /** The engine's own validator lets furniture stand on a rug; the room rules do not. */
    @Test fun dragPlacementGoesThroughTheRoomRules() {
        val layout = listOf(Placed("it_rug", Surface.FLOOR, 2, 2), Placed("it_plant", Surface.FLOOR, 0, 4))
        val onRug = RoomMap.toObject(layout[1], 1, cfg).placement.copy(x = 3, y = 3)
        assertNull(placeAt(layout, 1, onRug, cfg))
        val free = onRug.copy(x = 4, y = 4)
        assertEquals(Placed("it_plant", Surface.FLOOR, 4, 4), placeAt(layout, 1, free, cfg)!![1])
    }

    @Test fun wallPiecesSlideAlongTheirWallAndKeepTheirRow() {
        val p = Placed("it_picture", Surface.LEFT_WALL, 3, 3)
        val o = RoomMap.toObject(p, 0, cfg)
        val slid = o.placement.copy(y = 4)
        assertEquals(p.copy(x = 4), placeAt(listOf(p), 0, slid, cfg)!![0])
    }

    @Test fun decorPostsFollowTheVisibleWalls() {
        val oblique = RoomMap.decor(IsoCamera.of(ViewPreset.OBLIQUE, 0), cfg).filter { it.id.startsWith("post") }
        assertEquals(3, oblique.size) // back corner and the two front ends
        val top = RoomMap.decor(IsoCamera.of(ViewPreset.TOP, 0), cfg).filter { it.id.startsWith("post") }
        assertEquals(4, top.size)
        assertTrue(RoomMap.decor(IsoCamera.of(ViewPreset.OBLIQUE, 0), cfg).none { it.pickable })
    }

    @Test fun buttonsHangUnderTheFrontCornerOfAFloorPiece() {
        assertEquals(Triple(1f, 3f, 0f), RoomMap.buttonAnchor(DefaultLayout[0], cfg))
        assertNotNull(RoomMap.buttonAnchor(DefaultLayout[2], cfg))
    }
}
