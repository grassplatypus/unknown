package com.break8t.unknown.ui.iso

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.sqrt

class CharacterModelsTest {
    private val ids = listOf("ch_sprout", "ch_cat", "ch_mush", "ch_cloud", "ch_firefly")
    private val poses = listOf(null, "pose_wave", "pose_jump", "pose_sit", "pose_side", "pose_back")
    private fun m(id: String, pose: String?) = IsoShapes.character(id, pose)!!.model
    private val eye = CharacterPalette.Eye.toArgb()

    /** Exposed faces facing [dir] (VoxelModel.DX order) whose color is [argb]. */
    private fun VoxelModel.faces(dir: Int, argb: Int) = (0 until faceCount).count { faces[it * 5 + 3] == dir && day[faces[it * 5 + 4]] == argb }

    @Test fun everyCharacterHasItsOwnModelForEveryPose() {
        val all = ids.flatMap { id -> poses.map { (id to it) to m(id, it) } }
        assertEquals(30, all.size)
        for ((key, model) in all) {
            assertTrue("$key is empty", model.faceCount > 0)
            assertEquals("$key footprint", 16 to 16, model.sx to model.sy)
        }
        for (i in all.indices) for (j in i + 1 until all.size) {
            val a = all[i].second; val b = all[j].second
            val same = a.faces.contentEquals(b.faces) && a.day.contentEquals(b.day)
            assertFalse("${all[i].first} and ${all[j].first} are the same model", same)
        }
    }

    @Test fun lookupsAreCachedAndFallBack() {
        assertSame(IsoShapes.character("ch_cat", "pose_sit"), IsoShapes.character("ch_cat", "pose_sit"))
        assertSame(IsoShapes.cat, IsoShapes.character("ch_cat", "pose_unknown"))
        assertSame(IsoShapes.cat, IsoShapes.of("ch_cat"))
        assertNull(IsoShapes.character("ch_unknown", null))
    }

    // The 8-voxel models these replaced: height in voxels at 8 per cell and the most common face color.
    private val legacy = mapOf(
        "ch_sprout" to (9 to 0xFFB5D96A), "ch_cat" to (9 to 0xFFF2A93B), "ch_mush" to (8 to 0xFFD2473B),
        "ch_cloud" to (6 to 0xFFF7F4EC), "ch_firefly" to (9 to 0xFFF2B33D),
    )

    @Test fun theStandingModelStaysCloseToTheOldLook() {
        for ((id, old) in legacy) {
            val shape = IsoShapes.character(id, null)!!
            val model = shape.model
            // characterObject draws a 2 x 2 cell model at half scale, so 16 voxels make one cell.
            val cells = model.sz / (VPC * shape.w).toFloat()
            val oldCells = old.first / VPC.toFloat()
            assertTrue("$id height $cells against $oldCells", abs(cells - oldCells) <= oldCells * 0.3f)
            val counts = IntArray(model.day.size)
            for (q in 0 until model.faceCount) counts[model.faces[q * 5 + 4]]++
            val top = model.day[counts.indices.maxBy { counts[it] }]
            val o = old.second.toInt()
            val dist = sqrt(listOf(16, 8, 0).sumOf { s -> ((top shr s and 0xFF) - (o shr s and 0xFF)).let { it * it }.toDouble() })
            assertTrue("$id main color %08X against %08X".format(top, o), dist < 70.0)
        }
    }

    /** Whether any eye pixel reaches the frame: render once as is and once with the eyes recolored. */
    private fun eyesShow(shape: IsoShape, step: Int): Boolean {
        val eyeColors = setOf(CharacterPalette.Eye, CharacterPalette.Spark)
        val marked = shape.copy(parts = shape.parts.map { if (it.color in eyeColors) it.copy(color = Color.Magenta) else it })
        fun draw(s: IsoShape) = renderScene(thumbnailScene(s), IsoCamera.of(ViewPreset.OBLIQUE, step), 120, 150).pixels
        return !draw(shape).contentEquals(draw(marked))
    }

    @Test fun theFaceIsWhereThePoseLooks() {
        for (id in ids) {
            // In the default view +x is dir 0 and +y (the front) is dir 2.
            assertTrue("$id stand shows its eyes", m(id, null).faces(2, eye) > 0)
            assertTrue("$id side looks toward +x", m(id, "pose_side").faces(0, eye) > 0)
            assertTrue("$id stand eyes are drawn", eyesShow(IsoShapes.character(id, null)!!, 0))
            for (step in intArrayOf(0, 3)) assertFalse("$id back shows its face at camera step $step", eyesShow(IsoShapes.character(id, "pose_back")!!, step))
        }
    }

    @Test fun jumpLeavesTheGroundAndSitIsLower() {
        for (id in ids) {
            fun minZ(model: VoxelModel) = (0 until model.faceCount).minOf { model.faces[it * 5 + 2] }
            assertTrue("$id jump", minZ(m(id, "pose_jump")) > 0)
            for (p in poses - "pose_jump") assertEquals("$id $p stands on the ground", 0, minZ(m(id, p)))
            assertTrue("$id sit", m(id, "pose_sit").sz < m(id, null).sz)
        }
    }

    @Test fun theFireflyGlowsInEveryPose() {
        for (p in poses) assertNotNull(p, m("ch_firefly", p).glow.firstOrNull { it != 0 })
    }
}
