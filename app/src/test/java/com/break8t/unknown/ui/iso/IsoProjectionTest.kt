package com.break8t.unknown.ui.iso

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin

class IsoProjectionTest {
    private val scene = IsoScene(width = 6, depth = 4)
    private fun frame(preset: ViewPreset, step: Int = 0) =
        Projection.framing(scene, IsoCamera.of(preset, step), 1000f, 700f, 1.4f, Offset(23f, -17f))

    private fun rad(d: Float) = Math.toRadians(d.toDouble()).toFloat()

    /** Screen delta of moving one unit along a world axis. */
    private fun delta(p: Projection, dx: Float, dy: Float, dz: Float): Pair<Float, Float> =
        (p.screenX(1f + dx, 1f + dy, dz) - p.screenX(1f, 1f, 0f)) to (p.screenY(1f + dx, 1f + dy, dz) - p.screenY(1f, 1f, 0f))

    @Test
    fun hitTestRoundTripsEveryCellInAllRotationsOfEditableViews() {
        for (preset in listOf(ViewPreset.OBLIQUE, ViewPreset.TOP)) for (step in 0..3) {
            val p = frame(preset, step)
            for (y in 0 until scene.depth) for (x in 0 until scene.width) {
                for ((fx, fy) in listOf(0.5f to 0.5f, 0.05f to 0.05f, 0.95f to 0.95f, 0.05f to 0.95f)) {
                    val sx = p.screenX(x + fx, y + fy, 0f)
                    val sy = p.screenY(x + fx, y + fy, 0f)
                    assertEquals("$preset step $step at ($x,$y)", x to y, p.cellAt(sx, sy, scene))
                }
            }
        }
    }

    @Test
    fun hitTestOutsideTheGridIsNull() {
        val p = frame(ViewPreset.OBLIQUE)
        assertNull(p.cellAt(p.screenX(-0.5f, 1f, 0f), p.screenY(-0.5f, 1f, 0f), scene))
        assertEquals(-1 to 1, p.floorCell(p.screenX(-0.5f, 1.5f, 0f), p.screenY(-0.5f, 1.5f, 0f)))
    }

    @Test
    fun obliqueIsTwoToOnePixelArtAndZRisesStraightUp() {
        val p = frame(ViewPreset.OBLIQUE)
        val (xdx, xdy) = delta(p, 1f, 0f, 0f)
        val (ydx, ydy) = delta(p, 0f, 1f, 0f)
        assertEquals(2f, xdx / xdy, 1e-4f) // +x runs down-right
        assertEquals(-2f, ydx / ydy, 1e-4f) // +y runs down-left
        val (zdx, zdy) = delta(p, 0f, 0f, 1f)
        assertEquals(0f, zdx, 1e-3f)
        assertEquals(-p.unit * cos(rad(30f)), zdy, 1e-3f)
    }

    @Test
    fun topViewIgnoresHeightAndKeepsTheGridSquare() {
        val p = frame(ViewPreset.TOP)
        val (xdx, xdy) = delta(p, 1f, 0f, 0f)
        assertEquals(p.unit, xdx, 1e-3f); assertEquals(0f, xdy, 1e-3f)
        val (ydx, ydy) = delta(p, 0f, 1f, 0f)
        assertEquals(0f, ydx, 1e-3f); assertEquals(p.unit, ydy, 1e-3f)
        val (zdx, zdy) = delta(p, 0f, 0f, 1f)
        assertEquals(0f, zdx, 1e-3f); assertEquals(0f, zdy, 1e-3f)
    }

    @Test
    fun lowFrontLooksAlongYFromTheSouth() {
        val p = frame(ViewPreset.LOW_FRONT)
        val (xdx, xdy) = delta(p, 1f, 0f, 0f)
        assertEquals(p.unit, xdx, 1e-3f); assertEquals(0f, xdy, 1e-3f)
        val (ydx, ydy) = delta(p, 0f, 1f, 0f)
        assertEquals(0f, ydx, 1e-3f); assertEquals(p.unit * sin(rad(12f)), ydy, 1e-3f)
        assertEquals(-p.unit * cos(rad(12f)), delta(p, 0f, 0f, 1f).second, 1e-3f)
        assertTrue(p.toCamY > 0.9f)
    }

    @Test
    fun sideLooksAlongXFromTheEast() {
        val p = frame(ViewPreset.SIDE)
        val (ydx, ydy) = delta(p, 0f, 1f, 0f)
        assertEquals(-p.unit, ydx, 1e-3f); assertEquals(0f, ydy, 1e-3f)
        val (xdx, xdy) = delta(p, 1f, 0f, 0f)
        assertEquals(0f, xdx, 1e-3f); assertEquals(p.unit * sin(rad(12f)), xdy, 1e-3f)
        assertTrue(p.toCamX > 0.9f)
    }

    @Test
    fun eachRotationStepPutsADifferentCornerAtTheBack() {
        val w = scene.width.toFloat(); val d = scene.depth.toFloat()
        val corners = listOf(0f to 0f, 0f to d, w to d, w to 0f)
        for (step in 0..3) {
            val p = frame(ViewPreset.OBLIQUE, step)
            val top = corners.minBy { (x, y) -> p.screenY(x, y, 0f) }
            assertEquals("step $step", corners[step], top)
        }
        assertEquals(IsoCamera.of(ViewPreset.OBLIQUE, 0), IsoCamera.of(ViewPreset.OBLIQUE, 4))
        assertEquals(IsoCamera.of(ViewPreset.OBLIQUE, 3), IsoCamera.of(ViewPreset.OBLIQUE, -1))
    }

    @Test
    fun framingCentresTheSceneAndZoomScalesAboutIt() {
        val a = Projection.framing(scene, IsoCamera.of(ViewPreset.TOP, 0), 1000f, 700f, 1f, Offset.Zero)
        val b = Projection.framing(scene, IsoCamera.of(ViewPreset.TOP, 0), 1000f, 700f, 2f, Offset.Zero)
        assertEquals(500f, a.screenX(3f, 2f, 0f), 1e-2f)
        assertEquals(350f, a.screenY(3f, 2f, 0f), 1e-2f)
        assertEquals(2f * a.unit, b.unit, 1e-3f)
        assertTrue(a.screenX(6f, 2f, 0f) <= 1000f && a.screenX(0f, 2f, 0f) >= 0f)
    }
}
