package com.break8t.unknown.ui.iso

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RoadOutlineTest {
    @Test fun roadCellsDoNotOutlineEachOtherButStillOutlineAgainstOtherThings() {
        // Next road cell a little nearer, as along the road in the low front view: no seam.
        assertFalse(outlines(ROAD_ID - 3, 5.0f, ROAD_ID - 4, 4.9f, bothRoad = true))
        // A real step on the road (a kerb in front) still outlines.
        assertTrue(outlines(ROAD_ID - 3, 5.0f, ROAD_ID - 4, 4.5f, bothRoad = true))
        // Road against grass or an object nearer to the camera keeps its outline.
        assertTrue(outlines(ROAD_ID - 3, 5.0f, 12, 4.9f, bothRoad = false))
        // Two different objects (not road) still outline as before.
        assertTrue(outlines(7, 5.0f, 8, 4.9f))
    }
}
