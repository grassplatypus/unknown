package com.break8t.unknown.ui.iso

import com.break8t.unknown.ui.island.IslandMap
import com.break8t.unknown.ui.island.PlotView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IsoFarmTest {
    /**
     * Crops read as planted: for every stage, the lowest sprite row sits under the ground, so the pixel where
     * it would project shows soil (brown) rather than crop colour, while a row above the ground shows the crop.
     */
    @Test fun buriedRowsAreHiddenUnderTheGround() {
        val seeds = listOf("seed_carrot", "seed_potato", "seed_wheat")
        for (seed in seeds) for (grown in listOf(false, true)) {
            val view = PlotView(seed, grown, if (grown) 1f else 0.5f)
            val scene = IslandMap.farmScene(List(9) { view })
            val f = renderScene(scene, IslandMap.FARM_CAMERA, 600, 600)
            val bed = scene.objects.first { it.id == "plot4" }
            val sink = -bed.z
            assertTrue("$seed $grown is sunk", sink > 0f)
            // Centre column of the bed, at the sprite's front face, half a voxel above its bottom (below ground).
            val x = bed.x + IsoFarm.FINE.toFloat(); val y = bed.y + (IsoFarm.BED_VOXELS * 5 / 8 + 1) / VPC.toFloat()
            fun rgb(z: Float): Int {
                val px = f.projection.screenX(x, y, z).toInt(); val py = f.projection.screenY(x, y, z).toInt()
                return f.pixels[py * f.w + px]
            }
            val under = rgb(-sink + 1.5f / VPC)
            val r = under shr 16 and 0xFF; val g = under shr 8 and 0xFF; val b = under and 0xFF
            // Soil brown, or the near-black outline where the plate meets the crop; never leaf, carrot or tuber colour.
            val soilOrEdge = (r > g && g > b && g < 120) || maxOf(r, g, b) < 70
            assertTrue("$seed $grown: below ground shows soil, got ${Integer.toHexString(under)}", soilOrEdge)
        }
    }

    @Test fun rootCropsAreBuriedDeeperThanWheatAndGrowingDeeperThanRipe() {
        assertEquals(0, IsoFarm.buriedRows(PlotStage.EMPTY, Crop.CARROT))
        assertEquals(0, IsoFarm.sink(PlotStage.EMPTY, Crop.WHEAT))
        assertTrue(IsoFarm.buriedRows(PlotStage.GROWN, Crop.CARROT) > IsoFarm.buriedRows(PlotStage.GROWN, Crop.WHEAT))
        assertTrue(IsoFarm.buriedRows(PlotStage.GROWING, Crop.CARROT) > IsoFarm.buriedRows(PlotStage.GROWN, Crop.CARROT))
        assertTrue(IsoFarm.buriedRows(PlotStage.GROWING, Crop.SWEET_POTATO) > IsoFarm.buriedRows(PlotStage.GROWN, Crop.SWEET_POTATO))
    }
}
