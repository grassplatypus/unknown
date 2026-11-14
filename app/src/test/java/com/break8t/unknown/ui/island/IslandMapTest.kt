package com.break8t.unknown.ui.island

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.break8t.unknown.config.BalanceConfig
import com.break8t.unknown.ui.iso.Crop
import com.break8t.unknown.ui.iso.IsoCamera
import com.break8t.unknown.ui.iso.IsoHit
import com.break8t.unknown.ui.iso.PlotStage
import com.break8t.unknown.ui.iso.ViewPreset
import com.break8t.unknown.ui.iso.layoutOf
import com.break8t.unknown.ui.iso.renderScene
import com.break8t.unknown.ui.iso.screenPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IslandMapTest {
    private val now = 1_700_000_000_000L
    private val plots = plotViews(demoPlots(now, 3), now)

    @Test fun plotsMapToStagesAndCrops() {
        assertEquals(PlotStage.EMPTY, plotStage(PlotView(null, false, 0f)))
        assertEquals(PlotStage.GROWING, plotStage(PlotView("seed_potato", false, 0f)))
        assertEquals(PlotStage.GROWN, plotStage(PlotView("seed_wheat", true, 1f)))
        assertEquals(Crop.SWEET_POTATO, cropOf("seed_potato"))
        assertEquals(Crop.WHEAT, cropOf("seed_wheat"))
        assertEquals(Crop.CARROT, cropOf("seed_carrot"))
    }

    @Test fun tapsFindThePlotFromTheModelOrTheGround() {
        assertEquals(4, IslandMap.plotAt(IsoHit.Item("plot4"), 9, farmView = true))
        // Beds are 2 x 2 farm cells from (1, 1); the farm view is built FINE engine cells per farm cell.
        val f = com.break8t.unknown.ui.iso.IsoFarm.FINE
        assertEquals(5, IslandMap.plotAt(IsoHit.Cell(5 * f, 3 * f), 9, farmView = true))
        assertEquals(5, IslandMap.plotAt(IsoHit.Cell(7 * f - 1, 5 * f - 1), 9, farmView = true))
        assertNull(IslandMap.plotAt(IsoHit.Cell(0, 2 * f), 9, farmView = true))
        assertNull(IslandMap.plotAt(IsoHit.Cell(7 * f, 2 * f), 9, farmView = true))
        assertEquals(5, IslandMap.plotAt(IsoHit.Cell(7, 5), 9, farmView = false))
        assertNull(IslandMap.plotAt(IsoHit.Item("fence1"), 9, farmView = true))
        assertEquals(IslandSpot.HOUSE, IslandMap.spotAt(IsoHit.Item("house"), 9))
        assertEquals(IslandSpot.SHOP, IslandMap.spotAt(IsoHit.Item("stall"), 9))
        assertEquals(IslandSpot.FARM, IslandMap.spotAt(IsoHit.Item("plot0"), 9))
        assertEquals(IslandSpot.FARM, IslandMap.spotAt(IsoHit.Item("fence2"), 9))
        assertNull(IslandMap.spotAt(IsoHit.Cell(0, 9), 9))
    }

    /** In both window sizes a farm voxel covers at least one buffer pixel, so outlines do not swallow the art. */
    @Test fun farmPixelSizeNeverDropsBelowAVoxel() {
        val scene = IslandMap.farmScene(plots)
        // Farm panels measured on the emulator at density 1: landscape 903 x 1005 px, portrait 588 x 588 px.
        for ((w, h) in listOf(903f to 1005f, 588f to 588f, 300f to 300f)) {
            val base = com.break8t.unknown.ui.iso.pixelBase(scene, IslandMap.FARM_CAMERA, w, h, 3)
            val voxel = com.break8t.unknown.ui.iso.Projection.framing(scene, IslandMap.FARM_CAMERA, w, h, 1f, Offset.Zero).unit / 8
            assertTrue("$w x $h: base $base voxel $voxel", base <= voxel || base == 1)
        }
        assertEquals(3, com.break8t.unknown.ui.iso.pixelBase(scene, IslandMap.FARM_CAMERA, 903f, 1005f, 3))
    }

    /** The label box of each farm plot (which also takes its taps) covers that plot's cell, and the plot model is drawn there. */
    @Test fun farmLabelBoxesCoverTheirPlots() {
        val f = renderScene(IslandMap.farmScene(plots), IslandMap.FARM_CAMERA, 300, 300)
        for (k in plots.indices) {
            val r = IslandMap.farmPlotRect(k, plots.size)
            assertEquals(k, IslandMap.plotAt(IsoHit.Cell(r[0].toInt(), r[1].toInt()), plots.size, farmView = true))
            val p = f.projection
            val top = f.objectAt(p.screenX(r[0] + 0.5f, r[1] + 0.2f, 0.13f).toInt(), p.screenY(r[0] + 0.5f, r[1] + 0.2f, 0.13f).toInt())
            assertTrue("plot $k shows $top", top == "plot$k" || plots[k].seedId == null || top?.startsWith("plot") == true)
        }
    }

    /**
     * Chip anchors land on their objects on screen for both window sizes, also zoomed and panned:
     * the house chip above the house's drawn pixels, the shop chip at the stall, the farm chip below the plots.
     */
    @Test fun chipAnchorsSitOnTheirObjects() = BalanceConfig.DEFAULT.islandSizes.forEach { size ->
        val scene = IslandMap.scene(plots, "car_red", size = size)
        val cam = IsoCamera.of(ViewPreset.OBLIQUE, 0)
        for ((vw, vh) in listOf(1487f to 1171f, 841f * 1.3f to 1100f)) for ((zoom, pan) in listOf(1f to Offset.Zero, 2f to Offset(80f, -40f))) {
            val base = 3
            val bw = (vw / base).toInt(); val bh = (vh / base).toInt()
            val f = renderScene(scene, cam, bw, bh)
            val l = layoutOf(bw, bh, Size(vw, vh), zoom, pan, base)
            fun box(prefix: String): FloatArray {
                var x0 = Float.MAX_VALUE; var y0 = Float.MAX_VALUE; var x1 = -1f; var y1 = -1f
                for (y in 0 until f.h) for (x in 0 until f.w) if (f.objectAt(x, y)?.startsWith(prefix) == true) {
                    x0 = minOf(x0, x.toFloat()); x1 = maxOf(x1, x + 1f); y0 = minOf(y0, y.toFloat()); y1 = maxOf(y1, y + 1f)
                }
                return floatArrayOf(l.left + x0 * l.px, l.top + y0 * l.px, l.left + x1 * l.px, l.top + y1 * l.px)
            }
            fun at(spot: IslandSpot): Offset {
                val a = IslandMap.anchor(spot).first
                return screenPoint(f.projection, l, a[0], a[1], a[2])
            }
            val tag = "size $size $vw x $vh zoom $zoom"
            val house = box("house"); val h = at(IslandSpot.HOUSE)
            assertTrue(tag, h.x in house[0]..house[2] && h.y <= house[1] + 2 * l.px)
            val stall = box("stall"); val s = at(IslandSpot.SHOP)
            assertTrue(tag, s.x in stall[0]..stall[2] + 2 * l.px && s.y <= stall[1] + (stall[3] - stall[1]) / 2)
            val farm = box("plot"); val fm = at(IslandSpot.FARM)
            assertTrue(tag, fm.x in farm[0]..farm[2] && fm.y >= farm[3])
        }
    }

    /** Expanding grows the floor but keeps every place where it was, and new scenery stays off the road. */
    @Test fun expandedIslandKeepsPlaces() {
        val base = IslandMap.scene(plots, "car_red")
        for (size in BalanceConfig.DEFAULT.islandSizes) {
            val big = IslandMap.scene(plots, "car_red", size = size)
            assertEquals(size, big.width)
            assertEquals(size, big.depth)
            for (id in listOf("house", "stall", "parking", "mailbox", "fence0") + plots.indices.map { "plot$it" }) {
                val a = base.find(id)!!; val b = big.find(id)!!
                assertEquals("$id at $size", a.x to a.y, b.x to b.y)
            }
            assertEquals(size, big.roads.single().y)
            for (o in big.objects.filter { it.id.startsWith("grow") }) {
                assertTrue("${o.id} inside $size", o.x + o.footW <= size && o.y + o.footD <= size)
                assertTrue("${o.id} off the road", big.cell(o.x, o.y).floor != com.break8t.unknown.ui.iso.FloorType.ROAD)
            }
            val islet = big.find("islet2")!!
            assertTrue("islet floats outside", !big.inside(islet.x, islet.y))
        }
    }
}
