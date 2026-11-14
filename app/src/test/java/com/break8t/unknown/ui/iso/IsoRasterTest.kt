package com.break8t.unknown.ui.iso

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IsoRasterTest {
    private fun block(h: Int, color: Color) = IsoShape(1, 1, listOf(Cuboid(0, 0, 0, VPC, VPC, h, color)))

    /** Tall box behind, short box in front, both in the same screen column at every oblique step. */
    private fun twoBoxes() = IsoScene(
        width = 2, depth = 2, floorDepth = 0.25f,
        objects = listOf(IsoObject("tall", block(3 * VPC, Color.Blue), 0, 0), IsoObject("short", block(VPC, Color.Red), 1, 1)),
    )

    private fun idAt(f: IsoFrame, x: Float, y: Float, z: Float) =
        f.objectAt(f.projection.screenX(x, y, z).toInt(), f.projection.screenY(x, y, z).toInt())

    /**
     * Each probe is a point on the far box whose line of sight passes through the near box, so the
     * near box must win there; with the near box removed the far box shows, proving they overlap.
     */
    @Test
    fun depthBufferLetsTheNearerBoxWinWhereTheyOverlap() {
        val both = twoBoxes()
        val cases = listOf(
            Triple(0, floatArrayOf(1f, 0.5f, 0.3f), "short" to "tall"), // from (+x, +y): short is near
            Triple(2, floatArrayOf(1f, 1.5f, 0.3f), "tall" to "short"), // from (-x, -y): tall is near
        )
        for ((step, pt, ids) in cases) {
            val (near, far) = ids
            val cam = IsoCamera.of(ViewPreset.OBLIQUE, step)
            assertEquals("step $step", near, idAt(renderScene(both, cam, 300, 300), pt[0], pt[1], pt[2]))
            val alone = both.copy(objects = both.objects.filter { it.id == far })
            assertEquals("step $step control", far, idAt(renderScene(alone, cam, 300, 300), pt[0], pt[1], pt[2]))
        }
        for (step in 0..3) assertEquals("tall", idAt(renderScene(both, IsoCamera.of(ViewPreset.OBLIQUE, step), 300, 300), 0.5f, 0.5f, 3f - 0.01f))
    }

    @Test
    fun idBufferHitTestPicksObjectsThenFloorCellsInEditableViews() {
        val room = IsoSamples.room()
        for (preset in listOf(ViewPreset.OBLIQUE, ViewPreset.TOP)) for (step in 0..3) {
            val f = renderScene(room, IsoCamera.of(preset, step), 420, 300)
            val p = f.projection
            fun at(x: Float, y: Float, z: Float) = hitTest(room, f, p.screenX(x, y, z), p.screenY(x, y, z))
            val tag = "$preset step $step"
            assertEquals(tag, IsoHit.Item("plant"), at(4.5f, 4.5f, 10.5f / VPC))
            assertEquals(tag, IsoHit.Item("sofa"), at(1f, 0.5f, 3.5f / VPC))
            assertEquals(tag, IsoHit.Cell(0, 4), at(0.5f, 4.5f, 0f))
        }
    }

    @Test
    fun skyIsEmptyAndHitsNothing() {
        val f = renderScene(IsoSamples.island(), IsoCamera.of(ViewPreset.OBLIQUE, 0), 200, 150)
        assertEquals(0, f.pixels[0] ushr 24)
        assertNull(f.objectAt(0, 0))
        assertNull(f.objectAt(-1, 5))
    }

    @Test
    fun selectionFrameAndNightChangeThePixelsButNotTheIds() {
        val room = IsoSamples.room()
        val cam = IsoCamera.of(ViewPreset.OBLIQUE, 0)
        val plain = renderScene(room, cam, 200, 150)
        val picked = renderScene(room, cam, 200, 150, selectedId = "sofa")
        val night = renderScene(room, cam, 200, 150, night = true)
        assertTrue(plain.ids.contentEquals(picked.ids) && plain.ids.contentEquals(night.ids))
        assertTrue(picked.pixels.count { it == IsoPalette.SelectFrame } > plain.pixels.count { it == IsoPalette.SelectFrame })
        assertNotEquals(plain.pixels.toList(), night.pixels.toList())
    }

    @Test
    fun ghostTintsItsFootprintAndIsNotHitTestable() {
        val room = IsoSamples.room()
        val cam = IsoCamera.of(ViewPreset.OBLIQUE, 0)
        val g = Ghost(Placement("plant", 3, 3, 0), valid = false)
        val f = renderScene(room, cam, 300, 220, ghost = g)
        assertEquals(null, idAt(f, 3.5f, 3.5f, 10.5f / VPC)) // ghost pixels carry no object id
        assertEquals(IsoHit.Item("plant"), hitTest(room, f, f.projection.screenX(4.5f, 4.5f, 10.5f / VPC), f.projection.screenY(4.5f, 4.5f, 10.5f / VPC)))
    }
}
