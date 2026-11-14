package com.break8t.unknown.ui.island

import com.break8t.unknown.config.BalanceConfig
import com.break8t.unknown.config.CropSpec
import com.break8t.unknown.data.Profile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class IslandLogicTest {
    private val min = 60_000L
    private val hour = 60 * min
    private val carrot = CropSpec("seed_carrot", "당근", growMinutes = 120, harvestCoins = 15)
    private val wheat = CropSpec("seed_wheat", "밀", growMinutes = 360, harvestCoins = 20)
    private val crops = listOf(carrot, wheat)
    private val t0 = 1_000_000_000L

    @Test fun realTimeNightIsFrom19To06() {
        assertFalse(isNight(realTime = true, manualNight = true, hour = 18))
        assertTrue(isNight(realTime = true, manualNight = false, hour = 19))
        assertTrue(isNight(realTime = true, manualNight = false, hour = 0))
        assertTrue(isNight(realTime = true, manualNight = false, hour = 5))
        assertFalse(isNight(realTime = true, manualNight = true, hour = 6))
    }

    @Test fun manualSettingIgnoresClock() {
        assertTrue(isNight(realTime = false, manualNight = true, hour = 12))
        assertFalse(isNight(realTime = false, manualNight = false, hour = 23))
    }

    @Test fun plantingFillsEmptyPlotWithCatalogTime() {
        val plots = plant(List(4) { Plot() }, 2, carrot, t0)
        assertEquals(Plot("seed_carrot", t0, t0 + 120 * min), plots[2])
        assertTrue(plots[2].isGrowing(t0 + 119 * min))
        assertTrue(plots[2].isGrown(t0 + 120 * min))
        assertEquals(0.5f, plots[2].progress(t0 + 60 * min), 1e-6f)
    }

    @Test fun plantingOnOccupiedPlotChangesNothing() {
        val plots = plant(List(4) { Plot() }, 0, carrot, t0)
        assertSame(plots, plant(plots, 0, wheat, t0 + min))
        assertSame(plots, plant(plots, 9, wheat, t0))
    }

    @Test fun harvestTakesEveryGrownPlotAndPaysCatalogCoins() {
        var plots = List(4) { Plot() }
        plots = plant(plots, 0, wheat, t0)
        plots = plant(plots, 1, wheat, t0)
        plots = plant(plots, 2, carrot, t0 + 5 * hour) // still growing at t0 + 6h
        val (next, h) = harvestAll(plots, t0 + 6 * hour, crops)
        assertEquals(mapOf("seed_wheat" to 2), h.counts)
        assertEquals(40, h.coins) // Figma T-05
        assertTrue(next[0].isEmpty && next[1].isEmpty)
        assertEquals(plots[2], next[2])
    }

    @Test fun harvestWithNothingGrownIsEmpty() {
        val plots = plant(List(4) { Plot() }, 0, wheat, t0)
        val (next, h) = harvestAll(plots, t0 + hour, crops)
        assertEquals(plots, next)
        assertEquals(Harvest(emptyMap(), 0), h)
    }

    @Test fun pullEmptiesWithoutReward() {
        val plots = plant(List(4) { Plot() }, 1, carrot, t0)
        assertTrue(pull(plots, 1)[1].isEmpty)
        assertSame(plots, pull(plots, -1))
    }

    @Test fun remainingTimeRoundsUpToShownUnit() {
        assertEquals(Duration(0, 2, 0), remainingTime(2 * hour))
        assertEquals(Duration(0, 2, 0), remainingTime(2 * hour - 1000))
        assertEquals(Duration(0, 1, 0), remainingTime(59 * min + 30_000))
        assertEquals(Duration(0, 0, 59), remainingTime(59 * min))
        assertEquals(Duration(0, 0, 1), remainingTime(1000))
        assertEquals(Duration(1, 3, 0), remainingTime(27 * hour))
        assertEquals(Duration(2, 0, 0), remainingTime(48 * hour - min))
        assertEquals(Duration(0, 6, 0), remainingTime(360 * min))
        assertEquals(Duration(0, 0, 0), remainingTime(-5))
    }

    @Test fun demoPlotsMatchFigmaSamples() {
        val plots = demoPlots(t0, 3)
        val now = t0 + 30_000 // half a minute after start the labels must not have moved
        val labels = plots.filter { it.isGrowing(now) }.map { remainingTime(it.readyAt - now) }
        assertEquals(
            listOf(
                Duration(0, 2, 0), Duration(0, 2, 0), Duration(0, 5, 0),
                Duration(1, 3, 0), Duration(1, 3, 0), Duration(2, 0, 0),
            ),
            labels,
        )
        assertEquals(0.6f, plots[0].progress(t0), 1e-3f)
        assertEquals(2, plots.count { it.isGrown(now) })
        assertEquals(1, plots.count { it.isEmpty })
        assertEquals(16, demoPlots(t0, 4).size)
    }

    @Test fun objectParticleFollowsLastDigitReading() {
        assertFalse(takesReul(40)) // 사십을
        assertTrue(takesReul(55)) // 오십오를
        assertTrue(takesReul(12))
        assertFalse(takesReul(0))
        assertTrue(takesReul(15)) // 십오를
        assertFalse(takesReul(20)) // 이십을
    }

    private val islandCfg = BalanceConfig(islandSizes = listOf(10, 12, 14), islandExpandCoins = listOf(200, 400))

    @Test fun islandSizeFollowsLevelAndClamps() {
        assertEquals(10, islandSize(0, islandCfg))
        assertEquals(14, islandSize(2, islandCfg))
        assertEquals(14, islandSize(9, islandCfg))
        assertEquals(10, islandSize(-1, islandCfg))
    }

    @Test fun expandingSpendsCoinsAndRaisesLevel() {
        assertEquals(Profile(coins = 40, islandLevel = 1), expandIsland(Profile(coins = 240), islandCfg))
        assertEquals(Profile(coins = 0, islandLevel = 2), expandIsland(Profile(coins = 400, islandLevel = 1), islandCfg))
    }

    @Test fun expandingRefusesWhenShortOrLargest() {
        assertNull(expandIsland(Profile(coins = 399, islandLevel = 1), islandCfg))
        assertNull(expandIsland(Profile(coins = 9_999, islandLevel = 2), islandCfg))
        assertNull(expandCost(2, islandCfg))
        assertEquals(200, expandCost(0, islandCfg))
    }

    @Test fun defaultDemoCanAffordFirstStepButNotSecond() {
        val first = expandIsland(Profile(), BalanceConfig.DEFAULT)!!
        assertNull(expandIsland(first, BalanceConfig.DEFAULT))
    }
}
