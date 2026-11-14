package com.break8t.unknown.ui.home

import com.break8t.unknown.config.BalanceConfig
import org.junit.Assert.assertEquals
import org.junit.Test

class ProfileFrameTest {
    private val cfg = BalanceConfig(avatarFrameLevels = listOf(5, 10, 20))

    @Test fun framesOpenOneByOneAtTheConfiguredLevels() {
        assertEquals(listOf(AvatarFrame.BASIC), unlockedFrames(4, cfg))
        assertEquals(listOf(AvatarFrame.BASIC, AvatarFrame.SILVER), unlockedFrames(5, cfg))
        assertEquals(AvatarFrame.entries, unlockedFrames(20, cfg))
        assertEquals(listOf(1, 5, 10, 20), AvatarFrame.entries.map { frameUnlockLevel(it, cfg) })
    }

    @Test fun profileUsesTheChoiceOnlyWhileItIsOpen() {
        assertEquals(AvatarFrame.SILVER, profileFrame(null, 7, cfg))
        assertEquals(AvatarFrame.BASIC, profileFrame("BASIC", 7, cfg))
        // A frame above the level, or an unknown name, falls back to the best open one.
        assertEquals(AvatarFrame.SILVER, profileFrame("JEWEL", 7, cfg))
        assertEquals(AvatarFrame.SILVER, profileFrame("PLATINUM", 7, cfg))
        assertEquals(AvatarFrame.JEWEL, profileFrame("JEWEL", 25, cfg))
    }
}
