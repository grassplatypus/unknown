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

    @Test fun aTapSkipsTheDriveToItsEnd() {
        assertEquals(TripPhase.PARKED, TripRules.skip(TripRules.enter(t0)).phase)
        assertEquals(TripPhase.PARKED, TripRules.skip(Trip(TripPhase.PARKED)).phase)
    }

    /** Leaving P mid-drive drops the island screen; it comes back with the car parked and no drive running. */
    @Test fun anInterruptedDriveComesBackParked() {
        val r = TripRules.interrupted(TripRules.enter(t0))
        assertEquals(TripPhase.PARKED, r.phase)
        assertFalse(TripRules.driving(r))
        assertEquals(1f, TripRules.progress(r, t0), 0f)
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
