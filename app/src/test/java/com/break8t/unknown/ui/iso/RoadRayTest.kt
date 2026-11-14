package com.break8t.unknown.ui.iso

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.break8t.unknown.ui.island.IslandMap
import com.break8t.unknown.ui.island.demoPlots
import com.break8t.unknown.ui.island.plotViews
import org.junit.Assert.assertTrue
import org.junit.Test

/** The island road must run past the edge of the screen for any window, rotation, zoom and pan. */
class RoadRayTest {
    private val now = 1_700_000_000_000L
    private val scene = IslandMap.scene(plotViews(demoPlots(now, 3), now), "car_red")
    private val base = 3

    // Content areas in px at density 1: the two app windows, a wide and a tall car screen, a small one.
    private val views = listOf(Size(1487f, 1171f), Size(701f, 1171f), Size(2400f, 700f), Size(900f, 1800f), Size(480f, 320f))
    private val gestures = listOf(1f to Offset.Zero, IsoViewState.MIN_ZOOM to Offset(-300f, 200f), 2.5f to Offset(400f, -350f))

    @Test
    fun roadEndLiesOutsideTheViewport() {
        for (view in views) for (step in 0..3) for ((zoom, pan) in gestures) {
            val bw = (view.width / base).toInt(); val bh = (view.height / base).toInt()
            val cam = IsoCamera.of(ViewPreset.OBLIQUE, step)
            val win = visibleWindow(bw, bh, view, zoom, pan, base)
            val f = renderScene(scene, cam, bw, bh, window = win)
            val road = scene.roads.single()
            val n = roadLength(road, f.projection, f.w, f.h)
            val end = road.x + (n - 1) * road.dx + 0.5f to road.y + (n - 1) * road.dy + 0.5f
            val ex = f.projection.screenX(end.first, end.second, 0f)
            val ey = f.projection.screenY(end.first, end.second, 0f)
            // The viewport in this frame's pixels.
            val l = layoutOf(bw, bh, view, zoom, pan, base)
            val vx0 = -l.left.toFloat() / l.px - win.x0; val vy0 = -l.top.toFloat() / l.px - win.y0
            val vx1 = vx0 + view.width / l.px; val vy1 = vy0 + view.height / l.px
            val tag = "view $view step $step zoom $zoom"
            assertTrue("$tag end ($ex, $ey) inside", ex < vx0 || ex > vx1 || ey < vy0 || ey > vy1)
            // The window covers the viewport, so drawing to the window's edge reaches the screen's edge.
            assertTrue(tag, vx0 >= 0f && vy0 >= 0f && vx1 <= f.w && vy1 <= f.h)
            val drawn = f.ids.count { it <= ROAD_ID }
            if (zoom == 1f) assertTrue("$tag no road pixels", drawn > 0)
        }
    }
}
