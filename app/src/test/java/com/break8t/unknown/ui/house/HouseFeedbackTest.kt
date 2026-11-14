package com.break8t.unknown.ui.house

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.break8t.unknown.config.BalanceConfig
import com.break8t.unknown.data.DemoState
import com.break8t.unknown.data.Profile
import com.break8t.unknown.ui.iso.Insets
import com.break8t.unknown.ui.iso.IsoCamera
import com.break8t.unknown.ui.iso.Placement
import com.break8t.unknown.ui.iso.Projection
import com.break8t.unknown.ui.iso.ViewPreset
import com.break8t.unknown.ui.iso.WallSide
import com.break8t.unknown.ui.iso.layoutOf
import com.break8t.unknown.ui.iso.screenPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 11/15 팀 피드백: 방이 잘리지 않음, 벽 장식 옮기기와 높이, 끌기 한계, 집 넓히기. */
class HouseFeedbackTest {
    private val cfg = BalanceConfig.DEFAULT
    private val big = cfg.copy(houseGridSize = cfg.houseGridSteps.last())

    // --- 1. The room stays inside the area between the chips and the tray. ---

    /** View and insets in pixels as HouseScreen lays them out; the 841 window runs at density 0.75 (AppTheme). */
    private class Window(val view: Size, val ins: Insets, val base: Int)

    private fun windows(): List<Window> {
        // The emulator runs at 160 dpi. Side rail 182 dp, chips one row (100 dp) wide and two narrow, tray about 361 dp.
        fun w(density: Float, windowPx: Float, top: Float, bottom: Float): Window {
            fun px(dp: Float) = Math.round(dp * density)
            return Window(Size(windowPx - px(182f), 1210f), Insets(px(35f), px(top), px(35f), px(bottom)), Math.round(3f * density))
        }
        return listOf(
            w(1f, 1682f, 35f + 100f + 24f, 35f), w(1f, 1682f, 35f + 100f + 24f, 35f + 361f + 24f),
            w(0.75f, 841f, 35f + 224f + 24f, 35f), w(0.75f, 841f, 35f + 224f + 24f, 35f + 361f + 24f),
        )
    }

    @Test fun roomFitsBetweenChipsAndTrayInBothWindowsAndEverySize() {
        val cam = IsoCamera.of(ViewPreset.OBLIQUE, 0)
        for (config in listOf(cfg, big)) for (w in windows()) {
            val n = config.houseGridSize.toFloat()
            val scene = RoomMap.scene(DefaultLayout, config, cam)
            val bw = (w.view.width.toInt() - w.ins.left - w.ins.right) / w.base
            val bh = (w.view.height.toInt() - w.ins.top - w.ins.bottom) / w.base
            val proj = Projection.framing(scene, cam, bw.toFloat(), bh.toFloat(), 1f, Offset.Zero)
            val lay = layoutOf(bw, bh, w.view, 1f, Offset.Zero + w.ins.shift, w.base)
            // Walls, posts and the floor slab: the whole room box with the posts' overhang.
            for (x in floatArrayOf(-0.4f, n + 0.4f)) for (y in floatArrayOf(-0.4f, n + 0.4f)) for (z in floatArrayOf(-0.6f, RoomMap.WALL_CELLS)) {
                val s = screenPoint(proj, lay, x, y, z)
                assertTrue("x $s in ${w.view} ${w.ins}", s.x >= w.ins.left && s.x <= w.view.width - w.ins.right)
                assertTrue("y $s in ${w.view} ${w.ins}", s.y >= w.ins.top && s.y <= w.view.height - w.ins.bottom)
            }
        }
    }

    // --- 3. Floor pieces cannot be dragged out of the room. ---

    @Test fun floorDragStaysInsideTheRoom() {
        val sofa = Placed("it_sofa", Surface.FLOOR, 0, 0)
        for (config in listOf(cfg, big)) {
            val n = config.houseGridSize
            for (rot in 0 until 4) for ((x, y) in listOf(-3 to -3, n + 2 to 1, 2 to n + 5, n to n)) {
                val p = dragTarget(Placement("p0", x, y, rot), Offset.Zero, sofa, config)
                val (w, h) = footprint(sofa.copy(turns = RoomMap.rotationOf(rot)))
                assertTrue(p.x in 0..n - w && p.y in 0..n - h)
                assertEquals(rot, p.rotation)
            }
        }
    }

    // --- 2. Wall pieces move to the nearer wall and keep their row; rows change with the buttons. ---

    @Test fun wallPieceGoesToTheWallTheFingerIsNearer() {
        val pic = Placed("it_picture", Surface.RIGHT_WALL, 3, 2)
        val west = dragTarget(Placement("p0", 3, 0, WallSide.NORTH.ordinal, WallSide.NORTH), Offset(0.2f, 3.6f), pic, cfg)
        assertEquals(Placement("p0", 0, 3, WallSide.WEST.ordinal, WallSide.WEST), west)
        val north = dragTarget(Placement("p0", 0, 3, WallSide.WEST.ordinal, WallSide.WEST), Offset(4.7f, -0.3f), pic, cfg)
        assertEquals(Placement("p0", 4, 0, WallSide.NORTH.ordinal, WallSide.NORTH), north)
        // Past the end of the wall it stops at the last cell.
        assertEquals(cfg.houseGridSize - 1, dragTarget(north, Offset(9f, 0f), pic, cfg).x)
    }

    /** Whichever height of the wall the finger is on, the piece lands on the cell under it, through the real projection. */
    @Test fun wallDragFollowsTheFingerAtAnyHeight() {
        val cam = IsoCamera.of(ViewPreset.OBLIQUE, 0)
        val proj = Projection.framing(RoomMap.scene(DefaultLayout, cfg, cam), cam, 400f, 300f, 1f, Offset.Zero)
        val pic = Placed("it_picture", Surface.RIGHT_WALL, 0, 2)
        val mid = 2 * RoomMap.rowHeight(cfg) + 0.5f
        val start = Placement("p0", 0, 0, WallSide.NORTH.ordinal, WallSide.NORTH)
        for (cell in 0 until cfg.houseGridSize) for (z in listOf(0.2f, 1f, 1.9f, 2.8f)) for (side in WallSide.entries.filter { it == WallSide.NORTH || it == WallSide.WEST }) {
            val (x, y) = if (side == WallSide.NORTH) cell + 0.5f to 0f else 0f to cell + 0.5f
            val at = proj.floorPoint(proj.screenX(x, y, z), proj.screenY(x, y, z), mid)!!
            val p = dragTarget(start, at, pic, cfg)
            assertEquals("$side cell $cell at z $z", side, p.wall)
            assertEquals("$side cell $cell at z $z", cell, if (side == WallSide.NORTH) p.x else p.y)
        }
    }

    @Test fun wallSwitchKeepsTheRowAndPassesTheRoomRules() {
        val layout = DefaultLayout // picture on the right wall, row 2
        val i = layout.indexOfFirst { it.itemId == "it_picture" }
        val target = dragTarget(Placement(RoomMap.idOf(i), 3, 0, 0, WallSide.NORTH), Offset(0.1f, 3.5f), layout[i], cfg)
        val moved = placeAt(layout, i, target, cfg)
        assertNotNull(moved)
        assertEquals(Placed("it_picture", Surface.LEFT_WALL, 3, 2), moved!![i])
        // The window takes rows 1..3 of the left wall at x = 1, so the picture cannot go there.
        val onWindow = dragTarget(target, Offset(0.1f, 1.5f), layout[i], cfg)
        assertNull(placeAt(layout, i, onWindow, cfg))
    }

    @Test fun rowButtonsMoveWallPiecesAndSkipTakenRows() {
        val layout = listOf(Placed("it_picture", Surface.RIGHT_WALL, 1, 0)) // under the lantern (rows 2, 3 at x = 1)
        val up = shiftRowInRoom(layout, 0, 1, cfg)
        assertEquals(1, up!![0].y)
        assertNull(shiftRowInRoom(up, 0, 1, cfg)) // rows 2 and 3 are the lantern's
        assertNull(shiftRowInRoom(layout, 0, -1, cfg)) // already on the bottom row
        val free = listOf(Placed("it_picture", Surface.RIGHT_WALL, 3, 0), Placed("it_picture", Surface.RIGHT_WALL, 3, 1))
        assertEquals(2, shiftRowInRoom(free, 0, 1, cfg)!![0].y) // jumps over the other picture
        assertEquals(cfg.houseWallHeight - 1, shiftRowInRoom(listOf(Placed("it_picture", Surface.LEFT_WALL, 4, 2)), 0, 1, cfg)!![0].y)
        assertNull(shiftRowInRoom(DefaultLayout, 0, 1, cfg)) // the sofa stands on the floor
    }

    // --- 4. Buying a bigger house. ---

    @Test fun expansionStepsFollowTheConfig() {
        assertEquals(Expansion(cfg.houseGridSteps[0], cfg.houseGridStepPrices[0]), nextExpansion(cfg.houseGridSize, cfg))
        assertEquals(Expansion(cfg.houseGridSteps[1], cfg.houseGridStepPrices[1]), nextExpansion(cfg.houseGridSteps[0], cfg))
        assertNull(nextExpansion(cfg.houseGridSteps.last(), cfg))
        assertEquals(cfg.houseGridSteps.size, cfg.houseGridStepPrices.size)
    }

    @Test fun expandingSpendsCoinsAndGrowsOneStep() {
        val price = cfg.houseGridStepPrices[0]
        val s = DemoState(Profile(coins = price + 5))
        assertEquals(ExpandResult.DONE, expandHouse(s, cfg))
        assertEquals(5, s.profile.value.coins)
        assertEquals(cfg.houseGridSteps[0], s.profile.value.houseGridSize)
        // Not enough for the next step: nothing changes.
        val before = s.profile.value
        assertEquals(ExpandResult.NOT_ENOUGH, expandHouse(s, cfg))
        assertEquals(before, s.profile.value)
    }

    @Test fun fullyExpandedHouseStaysAsItIs() {
        val s = DemoState(Profile(coins = 10_000, houseGridSize = cfg.houseGridSteps.last()))
        val before = s.profile.value
        assertEquals(ExpandResult.MAX, expandHouse(s, cfg))
        assertEquals(before, s.profile.value)
    }

    @Test fun placementsStillFitAfterGrowing() {
        val layout = DefaultLayout + Placed("it_rug", Surface.FLOOR, 3, 3) + Placed("it_picture", Surface.RIGHT_WALL, 4, 0)
        for (config in listOf(cfg) + cfg.houseGridSteps.map { cfg.copy(houseGridSize = it) }) {
            layout.forEachIndexed { i, p -> assertTrue("$p at ${config.houseGridSize}", fitsRoom(layout, p, config, i)) }
        }
        // And the new cells take furniture.
        assertNotNull(placeAt(layout, 3, Placement(RoomMap.idOf(3), 5, 5, 0), big))
    }
}
