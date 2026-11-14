package com.break8t.unknown.ui.iso

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IsoSortTest {
    private fun box(x0: Float, y0: Float, z0: Float, x1: Float, y1: Float, z1: Float, id: String) =
        WorldBox(x0, y0, z0, x1, y1, z1, Color.Gray, ownerId = id)

    private fun List<WorldBox>.ids() = map { it.ownerId }
    private fun oblique(step: Int) = IsoCamera.of(ViewPreset.OBLIQUE, step)

    @Test
    fun longBoxBehindAShortNeighbourWhereCentreDepthGetsItWrong() {
        val long = box(0f, 0f, 0f, 1f, 4f, 1f, "long")
        val short = box(1f, 0f, 0f, 2f, 1f, 1f, "short")
        // Centre depth puts the long box in front at step 0; the separating axis x says otherwise.
        val p = Projection(oblique(0), 1f, 0f, 0f, 0f, 0f)
        assertTrue(p.depth(0.5f, 2f, 0.5f) > p.depth(1.5f, 0.5f, 0.5f))
        assertEquals(listOf("long", "short"), paintOrder(listOf(short, long), oblique(0)).ids())
        assertEquals(listOf("short", "long"), paintOrder(listOf(long, short), oblique(2)).ids())
    }

    @Test
    fun tallMultiCellObjectSwapsWithItsNeighbourAsTheViewTurns() {
        val big = box(0f, 0f, 0f, 2f, 2f, 3f, "big")
        val small = box(2f, 0f, 0f, 3f, 1f, 1f, "small") // east of big
        // Camera east of the scene at steps 0 and 1 (yaw 45, 135), west at 2 and 3.
        val expected = listOf("small", "small", "big", "big")
        for (step in 0..3) {
            assertEquals("step $step", expected[step], paintOrder(listOf(small, big), oblique(step)).last().ownerId)
            assertEquals("step $step", expected[step], paintOrder(listOf(big, small), oblique(step)).last().ownerId)
        }
    }

    @Test
    fun overlappingFootprintsStackedInZPaintBottomFirst() {
        val bottom = box(0f, 0f, 0f, 2f, 2f, 1f, "bottom")
        val top = box(0.5f, 0.5f, 1f, 1.5f, 1.5f, 2f, "top")
        for (preset in listOf(ViewPreset.OBLIQUE, ViewPreset.LOW_FRONT, ViewPreset.TOP)) for (step in 0..3) {
            assertEquals(listOf("bottom", "top"), paintOrder(listOf(top, bottom), IsoCamera.of(preset, step)).ids())
        }
    }

    @Test
    fun lowFrontPutsTheSouthernBoxInFrontAndSideTheEastern() {
        val north = box(0f, 0f, 0f, 1f, 1f, 2f, "north")
        val south = box(0f, 1f, 0f, 1f, 2f, 1f, "south")
        assertEquals("south", paintOrder(listOf(south, north), IsoCamera.of(ViewPreset.LOW_FRONT, 0)).last().ownerId)
        val west = box(0f, 0f, 0f, 1f, 1f, 2f, "west")
        val east = box(1f, 0f, 0f, 2f, 1f, 1f, "east")
        assertEquals("east", paintOrder(listOf(east, west), IsoCamera.of(ViewPreset.SIDE, 0)).last().ownerId)
    }

    @Test
    fun resultIsAPermutationOfTheInput() {
        val boxes = (0 until 30).map { i -> box((i % 6).toFloat(), (i / 6).toFloat(), 0f, i % 6 + 1f, i / 6 + 1f, 1f + i % 3, "b$i") }
        for (step in 0..3) {
            val out = paintOrder(boxes, oblique(step))
            assertEquals(boxes.toSet(), out.toSet())
            assertEquals(boxes.size, out.size)
        }
    }

    @Test
    fun drawListHidesNearWallsAndTheirPictures() {
        val room = IsoSamples.room()
        for (step in 0..3) {
            val list = buildDrawList(room, oblique(step))
            val pictures = list.boxes.mapNotNull { it.ownerId }.filter { it.startsWith("picture") }.toSet()
            // The camera is east of the room at yaw 45 and 135 (west wall shows), south at 315 and 45.
            val expected = buildSet {
                if (step == 0 || step == 1) add("picture")
                if (step == 3 || step == 0) add("picture2")
            }
            assertEquals("step $step", expected, pictures)
        }
        val top = buildDrawList(room, IsoCamera.of(ViewPreset.TOP, 0))
        assertTrue(top.boxes.any { it.ownerId == "picture" } && top.boxes.any { it.ownerId == "picture2" })
    }
}
