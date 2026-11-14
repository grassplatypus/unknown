package com.break8t.unknown.drive

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UxLimitsTest {
    @Test fun unknownBlocksEverything() {
        val u = UxLimits.UNKNOWN
        assertTrue(u.noSetup && u.noKeyboard && u.noDialpad && u.noVideo)
    }

    @Test fun noneBlocksNothing() {
        assertFalse(UxLimits.NONE.blocks(UxLimits.ALL))
    }

    @Test fun flagsOnlyApplyWhenDistractionOptimizationRequired() {
        assertFalse(UxLimits(false, UxLimits.NO_SETUP).noSetup)
        assertTrue(UxLimits(true, UxLimits.NO_SETUP).noSetup)
    }

    @Test fun onlyTheActiveFlagBlocks() {
        val l = UxLimits(true, UxLimits.NO_KEYBOARD or UxLimits.NO_VIDEO)
        assertTrue(l.noKeyboard)
        assertTrue(l.noVideo)
        assertFalse(l.noSetup)
        assertFalse(l.noDialpad)
    }

    @Test fun zeroMaskNeverBlocks() {
        assertFalse(UxLimits.UNKNOWN.blocks(0))
    }
}
