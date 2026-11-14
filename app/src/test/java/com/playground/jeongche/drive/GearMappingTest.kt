package com.playground.jeongche.drive

import org.junit.Assert.assertEquals
import org.junit.Test

class GearMappingTest {
    @Test fun parkIsParked() = assertEquals(DriveMode.PARKED, gearMode(4, available = true))

    @Test fun knownNonParkGearsAreDriving() {
        listOf(1, 2, 8, 0x10, 0x20, 0x40, 0x80, 0x100, 0x200, 0x400, 0x800, 0x1000).forEach {
            assertEquals("gear $it", DriveMode.DRIVING, gearMode(it, available = true))
        }
    }

    @Test fun missingOrUnknownValuesAreUnknown() {
        listOf(null, 0, 3, 5, 0x2000, -1).forEach {
            assertEquals("gear $it", DriveMode.UNKNOWN, gearMode(it, available = true))
        }
    }

    @Test fun unavailableStatusIsUnknownEvenForPark() {
        assertEquals(DriveMode.UNKNOWN, gearMode(4, available = false))
        assertEquals(DriveMode.UNKNOWN, gearMode(8, available = false))
    }
}
