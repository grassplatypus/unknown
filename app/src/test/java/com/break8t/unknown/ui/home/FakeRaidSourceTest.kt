package com.break8t.unknown.ui.home

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration

@OptIn(ExperimentalCoroutinesApi::class)
class FakeRaidSourceTest {
    private fun TestScope.after(d: Duration) {
        advanceTimeBy(d)
        runCurrent()
    }

    @Test
    fun drivingCyclesThroughEveryStateAndRepeats() = runTest {
        val src = FakeRaidSource(backgroundScope)
        src.setDriving(true)
        runCurrent()
        assertEquals(RaidState.None, src.state.value)
        after(HomeDemo.RAID_NONE)
        val first = (src.state.value as RaidState.Solo).jam
        assertEquals(HomeDemo.JAM.remainingM, first.remainingM, 0.0)
        assertEquals(HomeDemo.JAM.progress, first.progress, 1e-4f)
        after(HomeDemo.RAID_SOLO)
        assertEquals(HomeDemo.PARTICIPANTS, (src.state.value as RaidState.Group).participants)
        after(HomeDemo.RAID_GROUP)
        val passed = src.state.value as RaidState.Passed
        assertEquals(520L to 10, passed.tookSec to passed.coins)
        assertEquals(listOf(520L), src.jamSegmentsSec.value)
        after(HomeDemo.RAID_PASSED)
        assertEquals(RaidState.None, src.state.value)
        after(HomeDemo.RAID_NONE)
        assertTrue(src.state.value is RaidState.Solo)
    }

    @Test
    fun staysNoneWhileParked() = runTest {
        val src = FakeRaidSource(backgroundScope)
        src.setDriving(false)
        after(HomeDemo.RAID_NONE * 10)
        assertEquals(RaidState.None, src.state.value)
    }

    @Test
    fun parkingResetsAndStopsTheCycle() = runTest {
        val src = FakeRaidSource(backgroundScope)
        src.setDriving(true)
        after(HomeDemo.RAID_NONE)
        assertTrue(src.state.value is RaidState.Solo)
        src.setDriving(false)
        assertEquals(RaidState.None, src.state.value)
        after(HomeDemo.RAID_NONE * 10)
        assertEquals(RaidState.None, src.state.value)
    }

    @Test
    fun repeatedDrivingSignalDoesNotRestartTheTimer() = runTest {
        val src = FakeRaidSource(backgroundScope)
        src.setDriving(true)
        after(HomeDemo.RAID_NONE / 2)
        src.setDriving(true)
        after(HomeDemo.RAID_NONE / 2)
        assertTrue(src.state.value is RaidState.Solo)
    }

    @Test
    fun endReturnsToNoneAndRestartsFromTheBeginning() = runTest {
        val src = FakeRaidSource(backgroundScope)
        src.setDriving(true)
        after(HomeDemo.RAID_NONE + HomeDemo.RAID_SOLO)
        assertTrue(src.state.value is RaidState.Group)
        src.end()
        assertEquals(RaidState.None, src.state.value)
        after(HomeDemo.RAID_NONE)
        assertTrue(src.state.value is RaidState.Solo)
    }

    @Test
    fun jamLengthGrowsAndShrinksDuringTheRaid() = runTest {
        val src = FakeRaidSource(backgroundScope)
        src.setDriving(true)
        after(HomeDemo.RAID_NONE)
        val tick = (HomeDemo.RAID_SOLO + HomeDemo.RAID_GROUP) / HomeDemo.FAKE_REMAINING_M.size
        val seen = mutableListOf<Double>()
        repeat(HomeDemo.FAKE_REMAINING_M.size) {
            val s = src.state.value
            seen += (s as? RaidState.Solo)?.jam?.remainingM ?: (s as RaidState.Group).jam.remainingM
            after(tick)
        }
        assertEquals(HomeDemo.FAKE_REMAINING_M, seen)
        assertTrue(seen.zipWithNext().any { (a, b) -> b > a })
        assertTrue(seen.zipWithNext().any { (a, b) -> b < a })
    }
}
