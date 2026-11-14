package com.break8t.unknown.ui.iso

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IsoModelTest {
    private val part = Cuboid(2, 1, 0, 5, 3, 4, Color.Red)

    @Test
    fun quarterTurnsComposeAndStayInsideTheTurnedFootprint() {
        val w = 16; val d = 8
        var c = part
        for (r in 1..4) {
            val prevW = if (r % 2 == 1) w else d
            val prevD = if (r % 2 == 1) d else w
            c = c.rotated(prevW, prevD, 1)
            assertEquals(part.rotated(w, d, r), c)
            val tw = if (r % 2 == 0) w else d
            val td = if (r % 2 == 0) d else w
            assertTrue(c.x >= 0 && c.y >= 0 && c.x + c.w <= tw && c.y + c.d <= td)
        }
        assertEquals(part, c)
        assertEquals(Cuboid(4, 2, 0, 3, 5, 4, Color.Red), part.rotated(w, d, 1))
    }

    @Test
    fun footprintSwapsOnOddRotationsAndWallFixesRotation() {
        val sofa = IsoObject("s", IsoShapes.sofa, 0, 0)
        assertEquals(2 to 1, sofa.footW to sofa.footD)
        assertEquals(1 to 2, sofa.copy(rotation = 1).let { it.footW to it.footD })
        assertEquals(2 to 1, sofa.copy(rotation = 6).let { it.footW to it.footD })
        assertEquals(3, IsoObject("p", IsoShapes.picture, 0, 1, rotation = 0, wall = WallSide.WEST).rot)
    }

    /** Every scenery model stays inside its footprint, so hit tests and placement match what is drawn. */
    @Test
    fun sceneryModelsStayInsideTheirFootprints() {
        val all = listOf(
            IsoScenery.bigTree, IsoScenery.tree, IsoScenery.appleTree, IsoScenery.bush, IsoScenery.redFlowers,
            IsoScenery.stall, IsoScenery.lightFence, IsoScenery.fencePost, IsoScenery.laneY, IsoScenery.parking,
            IsoScenery.cloud, IsoScenery.islet, IsoScenery.window, IsoScenery.lantern, IsoScenery.tuft, IsoScenery.post(24),
        ) + PlotStage.entries.flatMap { s -> Crop.entries.map { IsoShapes.plot(s, it) } }
        for (s in all) for (c in s.parts) {
            assertTrue("$c", c.x >= 0 && c.y >= 0 && c.z >= 0 && c.x + c.w <= s.w * VPC && c.y + c.d <= s.d * VPC)
        }
        assertTrue(IsoShapes.plot(PlotStage.GROWN, Crop.WHEAT) === IsoShapes.plot(PlotStage.GROWN, Crop.WHEAT))
    }

    /** Voxel sizes the report quotes; a shape that outgrows its footprint fails here. */
    @Test
    fun voxelModelsHaveTheirFootprintAndExpectedHeight() {
        val expected = mapOf(
            "it_sofa" to Triple(16, 8, 7), "it_plant" to Triple(8, 8, 18), "it_picture" to Triple(8, 8, 6),
            "it_rug" to Triple(16, 16, 1), "it_lamp" to Triple(8, 8, 14), "it_shelf" to Triple(16, 8, 18),
            "house" to Triple(24, 24, 24), "farm_empty" to Triple(8, 8, 1), "farm_sprout" to Triple(8, 8, 4),
            "farm_growing" to Triple(8, 8, 8), "farm_grown" to Triple(8, 8, 10), "tree" to Triple(8, 8, 12),
            "fence" to Triple(8, 8, 6), "path" to Triple(8, 8, 1), "water" to Triple(16, 16, 1),
            "car" to Triple(16, 8, 8), "ch_sprout" to Triple(8, 8, 9), "ch_cat" to Triple(8, 8, 9),
            "ch_mush" to Triple(8, 8, 8), "ch_cloud" to Triple(8, 8, 6), "ch_firefly" to Triple(8, 8, 9),
        )
        for ((id, size) in expected) {
            val s = IsoShapes.of(id) ?: error("no shape for $id")
            val m = s.model
            assertEquals(id, size, Triple(m.sx, m.sy, m.sz))
            assertEquals(id, s.w * VPC to s.d * VPC, m.sx to m.sy)
            for (c in s.parts) assertTrue(id, c.x >= 0 && c.y >= 0 && c.x + c.w <= m.sx && c.y + c.d <= m.sy)
            assertTrue("$id has visible faces", m.faceCount > 0)
        }
        assertTrue(IsoShapes.rug.flat && IsoShapes.path.flat && !IsoShapes.sofa.flat)
        assertTrue(IsoShapes.picture.wallMounted)
    }

    @Test
    fun ripeCropsDifferInFormNotOnlyColor() {
        val grids = Crop.entries.map { crop ->
            val m = IsoShapes.plot(PlotStage.GROWN, crop).model
            (0 until m.faceCount).map { listOf(m.faces[it * 5], m.faces[it * 5 + 1], m.faces[it * 5 + 2], m.faces[it * 5 + 3]) }.toSet()
        }
        assertEquals(3, grids.toSet().size)
    }

    @Test
    fun hiddenVoxelFacesAreCulledAndCarvingRemovesVoxels() {
        val cube = IsoShape(1, 1, listOf(Cuboid(0, 0, 0, 2, 2, 2, Color.Red)))
        assertEquals(4 * 4 + 4, cube.model.faceCount) // 2 x 2 per side, 4 on top, no bottom, none inside
        val carved = IsoShape(1, 1, listOf(Cuboid(0, 0, 0, 2, 2, 2, Color.Red), Cuboid(1, 1, 1, 1, 1, 1, Color.Transparent)))
        val f = carved.model.faces
        val faces = (0 until carved.model.faceCount).map { listOf(f[it * 5], f[it * 5 + 1], f[it * 5 + 2], f[it * 5 + 3]) }
        assertTrue(listOf(1, 1, 0, 4) in faces) // the notch floor is now exposed
        assertFalse(faces.any { it[0] == 1 && it[1] == 1 && it[2] == 1 }) // the carved voxel is gone
        assertTrue(IsoShapes.lamp.model.glow.any { it != 0 })
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
        val (cx, cy) = p.floorCell(p.screenX(1.9f, 3.1f, 0f), p.screenY(1.9f, 3.1f, 0f))!!
        assertEquals(Placement("plant", 1, 3, 0), room.snap(plant, cx, cy, 0, 0))
    }

    @Test
    fun validatorRejectsOverlapAndOutOfBoundsButAllowsFurnitureOnARug() {
        val room = IsoSamples.room()
        assertFalse(room.isFree(Placement("plant", 0, 0, 0))) // on the sofa
        assertFalse(room.isFree(Placement("plant", 5, 4, 0))) // off the grid
        assertTrue(room.isFree(Placement("plant", 1, 2, 0))) // on the rug
        assertFalse(room.isFree(Placement("plant", 2, 3, 0))) // on the cat
        assertFalse(room.isFree(Placement("rug", 4, 3, 0))) // 2x2 rug sticks out
        assertFalse(room.isFree(Placement("sofa", 4, 3, 1))) // turned sofa is 1x2 and hits the plant
        assertTrue(room.isFree(Placement("sofa", 4, 1, 1)))
        assertFalse(room.isFree(Placement("picture", 1, 2, 3, WallSide.WEST))) // off its wall
        assertNull(room.turned("picture"))
        assertEquals(Placement("sofa", 0, 0, 1), room.turned("sofa"))
    }

    @Test
    fun islandValidatorRejectsOccupiedCells() {
        val island = IsoSamples.island()
        assertFalse(island.isFree(Placement("tree1", 5, 1, 0))) // the house
        assertTrue(island.isFree(Placement("tree1", 9, 8, 0)))
    }
}
