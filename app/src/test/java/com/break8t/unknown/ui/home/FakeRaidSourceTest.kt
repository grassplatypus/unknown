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
        assertEquals(RaidState.Solo, src.state.value)
        after(HomeDemo.RAID_SOLO)
        assertEquals(RaidState.Group(HomeDemo.PARTICIPANTS), src.state.value)
        after(HomeDemo.RAID_GROUP)
        assertEquals(RaidState.Passed(520, coins = 10, HomeDemo.PARTICIPANTS), src.state.value)
        after(HomeDemo.RAID_PASSED)
        assertEquals(RaidState.None, src.state.value)
        after(HomeDemo.RAID_NONE)
        assertEquals(RaidState.Solo, src.state.value)
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
        assertEquals(RaidState.Solo, src.state.value)
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
        assertEquals(RaidState.Solo, src.state.value)
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
        assertEquals(RaidState.Solo, src.state.value)
    }
}
