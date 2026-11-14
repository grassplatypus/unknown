package com.break8t.unknown.ui.iso

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IsoModelTest {
    private val part = Cuboid(0.2f, 0.1f, 0f, 0.5f, 0.3f, 1f, Color.Red)

    @Test
    fun quarterTurnsComposeAndStayInsideTheTurnedFootprint() {
        val w = 2; val d = 1
        var c = part
        for (r in 1..4) {
            val turnedW = if (r % 2 == 0) w else d
            val turnedD = if (r % 2 == 0) d else w
            val prevW = if (r % 2 == 1) w else d
            val prevD = if (r % 2 == 1) d else w
            c = c.rotated(prevW, prevD, 1)
            assertNear(part.rotated(w, d, r), c)
            assertTrue(c.x >= 0f && c.y >= 0f && c.x + c.w <= turnedW + 1e-5f && c.y + c.d <= turnedD + 1e-5f)
        }
        assertNear(part, c)
        assertNear(Cuboid(0.6f, 0.2f, 0f, 0.3f, 0.5f, 1f, Color.Red), part.rotated(w, d, 1))
    }

    private fun assertNear(a: Cuboid, b: Cuboid) {
        val av = floatArrayOf(a.x, a.y, a.z, a.w, a.d, a.h); val bv = floatArrayOf(b.x, b.y, b.z, b.w, b.d, b.h)
        for (i in av.indices) assertEquals("$a vs $b", av[i], bv[i], 1e-5f)
    }

    @Test
    fun footprintSwapsOnOddRotationsAndWallFixesRotation() {
        val sofa = IsoObject("s", IsoShapes.sofa, 0, 0)
        assertEquals(2 to 1, sofa.footW to sofa.footD)
        assertEquals(1 to 2, sofa.copy(rotation = 1).let { it.footW to it.footD })
        assertEquals(2 to 1, sofa.copy(rotation = 6).let { it.footW to it.footD })
        assertEquals(3, IsoObject("p", IsoShapes.picture, 0, 1, rotation = 0, wall = WallSide.WEST).rot)
    }

    @Test
    fun rotatedObjectBoxesStayInsideTheFootprint() {
        for (r in 0..3) {
            val o = IsoObject("sh", IsoShapes.shelf, 1, 2, rotation = r)
            for (b in o.worldBoxes(0f)) {
                assertTrue("rot $r", b.x0 >= 1f - 1e-5f && b.x1 <= 1f + o.footW + 1e-5f)
                assertTrue("rot $r", b.y0 >= 2f - 1e-5f && b.y1 <= 2f + o.footD + 1e-5f)
            }
        }
    }

    @Test
    fun snappingKeepsTheGrabOffsetAndWallObjectsStayOnTheirWall() {
        val room = IsoSamples.room()
        val rug = room.find("rug")!!
        assertEquals(Placement("rug", 2, 3, 0), room.snap(rug, cx = 3, cy = 4, grabX = 1, grabY = 1))
        val west = room.find("picture")!!
        assertEquals(Placement("picture", 0, 4, 3, WallSide.WEST), room.snap(west, cx = 3, cy = 4, grabX = 0, grabY = 0))
        val north = room.find("picture2")!!
        assertEquals(Placement("picture2", 3, 0, 0, WallSide.NORTH), room.snap(north, cx = 3, cy = 2, grabX = 0, grabY = 0))
    }

    @Test
    fun pointerSnapsToTheCellUnderItThroughTheProjection() {
        val room = IsoSamples.room()
        val p = Projection.framing(room, IsoCamera.of(ViewPreset.OBLIQUE, 1), 800f, 600f, 1f, Offset.Zero)
        val plant = room.find("plant")!!
        val sx = p.screenX(1.9f, 3.1f, 0f); val sy = p.screenY(1.9f, 3.1f, 0f)
        val (cx, cy) = p.floorCell(sx, sy)!!
        assertEquals(Placement("plant", 1, 3, 0), room.snap(plant, cx, cy, 0, 0))
    }

    @Test
    fun validatorRejectsOverlapAndOutOfBoundsButAllowsFurnitureOnARug() {
        val room = IsoSamples.room()
        assertFalse(room.isFree(Placement("plant", 0, 0, 0))) // on the sofa
        assertFalse(room.isFree(Placement("plant", 5, 4, 0))) // off the grid
        assertTrue(room.isFree(Placement("plant", 2, 3, 0))) // on the rug
        assertFalse(room.isFree(Placement("rug", 4, 3, 0))) // 2x2 rug sticks out
        assertFalse(room.isFree(Placement("sofa", 4, 3, 1))) // turned sofa is 1x2 and hits the plant
        assertTrue(room.isFree(Placement("sofa", 4, 1, 1)))
        assertFalse(room.isFree(Placement("picture", 1, 2, 3, WallSide.WEST))) // off its wall
        assertNull(room.turned("picture"))
        assertEquals(Placement("sofa", 0, 0, 1), room.turned("sofa"))
    }

    @Test
    fun islandValidatorRejectsWater() {
        val island = IsoSamples.island()
        assertFalse(island.isFree(Placement("tree1", 0, 0, 0)))
        assertTrue(island.isFree(Placement("tree1", 12, 6, 0)))
    }

    @Test
    fun everyKnownIdHasAShapeWithPartsInsideItsFootprint() {
        val ids = listOf(
            "it_sofa", "it_plant", "it_picture", "it_rug", "it_lamp", "it_shelf", "house",
            "farm_empty", "farm_sprout", "farm_growing", "farm_grown", "tree", "fence", "path", "water", "car",
        )
        for (id in ids) {
            val s = IsoShapes.of(id) ?: error("no shape for $id")
            for (c in s.parts) assertTrue(id, c.x >= 0f && c.y >= 0f && c.x + c.w <= s.w + 1e-5f && c.y + c.d <= s.d + 1e-5f)
        }
        assertTrue(IsoShapes.rug.flat && IsoShapes.path.flat && !IsoShapes.sofa.flat)
        assertTrue(IsoShapes.picture.wallMounted)
    }
}
