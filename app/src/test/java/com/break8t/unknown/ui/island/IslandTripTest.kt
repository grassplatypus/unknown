package com.break8t.unknown.ui.island

import com.break8t.unknown.ui.iso.IsoCamera
import com.break8t.unknown.ui.iso.VPC
import com.break8t.unknown.ui.iso.ViewPreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IslandTripTest {
    private val t0 = 1_000L
    private val d = CAR_DRIVE_MS

    @Test fun arrivalRunsForItsDurationThenParks() {
        val a = TripRules.enter(t0)
        assertEquals(TripPhase.ARRIVING, TripRules.tick(a, t0 + d - 1).phase)
        assertEquals(TripPhase.PARKED, TripRules.tick(a, t0 + d).phase)
        assertEquals(0.5f, TripRules.progress(a, t0 + d / 2), 1e-3f)
    }

    @Test fun leavingWhileParkedDrivesOutThenIsGone() {
        val l = TripRules.leave(Trip(TripPhase.PARKED), t0, parked = true)
        assertEquals(TripPhase.LEAVING, l.phase)
        assertEquals(TripPhase.GONE, TripRules.tick(l, t0 + d).phase)
    }

    @Test fun aTapSkipsEitherDriveToItsEnd() {
        assertEquals(TripPhase.PARKED, TripRules.skip(TripRules.enter(t0)).phase)
        assertEquals(TripPhase.GONE, TripRules.skip(TripRules.leave(Trip(TripPhase.PARKED), t0, true)).phase)
        assertEquals(TripPhase.PARKED, TripRules.skip(Trip(TripPhase.PARKED)).phase)
    }

    /** Leaving P mid-drive never leaves a drive running: arrival parks, departure is gone (navigates now). */
    @Test fun gearLeavingParkEndsAnyDriveAtOnce() {
        val arriving = TripRules.enter(t0)
        val leaving = TripRules.leave(Trip(TripPhase.PARKED), t0, true)
        assertEquals(TripPhase.PARKED, TripRules.gear(arriving, parked = false).phase)
        assertEquals(TripPhase.GONE, TripRules.gear(leaving, parked = false).phase)
        assertEquals(arriving, TripRules.gear(arriving, parked = true))
        // A leave asked while not parked skips the drive entirely.
        assertEquals(TripPhase.GONE, TripRules.leave(Trip(TripPhase.PARKED), t0, parked = false).phase)
        assertFalse(TripRules.driving(TripRules.gear(leaving, false)))
    }

    @Test fun theCarMovesInWholeVoxelStepsAlongTheRoad() {
        val park = IslandMap.PARK_Y; val far = 30f
        assertEquals(far, carPose(TripPhase.ARRIVING, 0f, park, far).y, 1e-4f)
        assertEquals(park, carPose(TripPhase.ARRIVING, 1f, park, far).y, 1f / VPC)
        var last = Float.MAX_VALUE
        for (k in 0..100) {
            val y = carPose(TripPhase.ARRIVING, k / 100f, park, far).y
            assertEquals("whole voxels", 0f, (y * VPC) % 1f, 1e-3f)
            assertTrue("never backs up", y <= last)
            last = y
        }
        // Leaving turns round in the spot first (3, then 0), then drives down the road facing it (1).
        assertEquals(CarPose(park, 3), carPose(TripPhase.LEAVING, 0.05f, park, far))
        assertEquals(0, carPose(TripPhase.LEAVING, 0.15f, park, far).rotation)
        assertEquals(1, carPose(TripPhase.LEAVING, 0.6f, park, far).rotation)
        assertEquals(far, carPose(TripPhase.LEAVING, 1f, park, far).y, 1f / VPC)
    }

    /** The drive starts and ends past the visible end of the road, for each window and rotation. */
    @Test fun farEndIsOffScreenForEveryWindow() {
        for ((w, h) in listOf(1487f to 1171f, 701f to 1171f, 2400f to 700f, 480f to 320f)) for (step in 0..3) {
            val cam = IsoCamera.of(ViewPreset.OBLIQUE, step)
            val far = IslandMap.farY(cam, w, h)
            val p = com.break8t.unknown.ui.iso.Projection.framing(
                IslandMap.scene(emptyList(), "car_red"), cam, w, h, 1f, androidx.compose.ui.geometry.Offset.Zero,
            )
            // The whole car (1.4 cells long) is past the edge there.
            val x = IslandMap.ROAD.x + 0.5f
            val sx = p.screenX(x, far - 0.7f, 0f); val sy = p.screenY(x, far - 0.7f, 0f)
            assertTrue("$w x $h step $step: ($sx, $sy)", sx < 0f || sx > w || sy < 0f || sy > h)
        }
    }
}
