package com.break8t.unknown.ui.photo

import com.break8t.unknown.ui.house.CameraPreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoRulesTest {
    @Test fun rotationCyclesInQuarterTurns() {
        var s = PhotoState()
        repeat(3) { s = s.rotated() }
        assertEquals(3, s.rotation)
        assertEquals(0, s.rotated().rotation)
    }

    @Test fun sizeStaysWithinSteps() {
        assertEquals(0, PhotoState(sizeIndex = 0).resized(-1).sizeIndex)
        assertEquals(CharacterSizes.lastIndex, PhotoState(sizeIndex = CharacterSizes.lastIndex).resized(+1).sizeIndex)
    }

    @Test fun characterStaysInsideScene() {
        val s = PhotoState(x = 0.9f, y = 0.1f).moved(0.5f, -0.5f)
        assertEquals(1f, s.x)
        assertEquals(0f, s.y)
    }

    @Test fun posesOnlyOwnedAndOnlyForSprout() {
        val owned = setOf("pose_wave", "pose_back")
        assertEquals(listOf("ch_sprout", "pose_wave", "pose_back"), poseOptions("ch_sprout", owned))
        assertEquals(listOf("ch_cat"), poseOptions("ch_cat", owned))
    }

    @Test fun startCharacterUsesFirstOptionOnce() {
        val s = PhotoState().startCharacter(listOf("ch_sprout", "pose_wave"))
        assertEquals("ch_sprout", s.poseId)
        assertTrue(s.editing)
        assertEquals("pose_wave", s.copy(poseId = "pose_wave").startCharacter(listOf("ch_sprout")).poseId)
    }

    @Test fun stateSurvivesSaveRoundTrip() {
        val s = PhotoState(CameraPreset.SIDE, 2, "pose_sit", 0.3f, 0.7f, 3, 3, true, false)
        assertEquals(s, PhotoState.fromList(s.toList()))
        assertEquals(PhotoState(), PhotoState.fromList(PhotoState().toList()))
    }

    @Test fun nightFollowsClockOrSetting() {
        assertTrue(isNight(realTime = true, nightFlag = false, hour = 22))
        assertFalse(isNight(realTime = true, nightFlag = true, hour = 12))
        assertTrue(isNight(realTime = false, nightFlag = true, hour = 12))
    }

    @Test fun countdownRoundsUpAndStopsAtZero() {
        assertEquals(SHARE_TTL_SEC, remainingSec(SHARE_TTL_SEC * 1000L, 0))
        assertEquals(1, remainingSec(1000, 1))
        assertEquals(0, remainingSec(1000, 5000))
    }

    @Test fun qrHasFinderCorners() {
        val q = qrModules(42)
        assertTrue(q[0][0] && q[0][6] && q[6][0] && q[3][3])
        assertFalse(q[1][1] || q[7][7])
    }
}
