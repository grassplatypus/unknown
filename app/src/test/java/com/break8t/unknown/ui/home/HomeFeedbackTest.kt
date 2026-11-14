package com.break8t.unknown.ui.home

import com.break8t.unknown.config.BalanceConfig
import com.break8t.unknown.voice.VoiceLine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeFeedbackTest {
    private val jam = HomeDemo.JAM
    private val solo = RaidState.Solo(jam)
    private val group = RaidState.Group(jam, HomeDemo.PARTICIPANTS)
    private val passed = RaidState.Passed(520, 10, emptyList(), jam)

    @Test
    fun frameStepsFollowConfigBoundaries() {
        val tiers = listOf(1, 4, 5, 9, 10, 19, 20, 99).map { avatarFrame(it) }
        val f = AvatarFrame.entries
        assertEquals(listOf(f[0], f[0], f[1], f[1], f[2], f[2], f[3], f[3]), tiers)
        assertEquals(AvatarFrame.GOLD, avatarFrame(3, BalanceConfig(avatarFrameLevels = listOf(2, 3, 50))))
    }

    @Test
    fun demoShowsDifferentFramesOnVisibleAvatars() {
        val visible = HomeDemo.PARTICIPANTS.take(BalanceConfig.DEFAULT.raidVisibleAvatars).map { avatarFrame(it.level) }
        assertEquals(visible.size, visible.toSet().size)
    }

    @Test
    fun voiceOnlyOnEnterAndPass() {
        assertEquals(VoiceLine.RAID_ENTER, raidVoiceLine(RaidState.None, solo))
        assertEquals(VoiceLine.RAID_ENTER, raidVoiceLine(RaidState.Approaching(jam, 500.0), solo))
        assertEquals(VoiceLine.RAID_ENTER, raidVoiceLine(passed, group))
        assertNull(raidVoiceLine(solo, RaidState.Solo(jam.copy(remainingM = 900.0))))
        assertNull(raidVoiceLine(solo, group))
        assertEquals(VoiceLine.RAID_EXIT, raidVoiceLine(group, passed))
        assertEquals(VoiceLine.RAID_EXIT, raidVoiceLine(solo, passed))
        assertNull(raidVoiceLine(passed, RaidState.None))
        // 종료 버튼이나 P로 끝난 것은 지난 것이 아니다.
        assertNull(raidVoiceLine(solo, RaidState.None))
        assertNull(raidVoiceLine(RaidState.None, passed))
    }

    @Test
    fun fakeRaidSpeaksOncePerJam() = runTest {
        val src = FakeRaidSource(backgroundScope)
        val lines = mutableListOf<VoiceLine>()
        backgroundScope.launch { announceRaid(src.state, lines::add) }
        src.setDriving(true)
        val cycle = HomeDemo.RAID_NONE + HomeDemo.RAID_SOLO + HomeDemo.RAID_GROUP + HomeDemo.RAID_PASSED
        advanceTimeBy(cycle * 2)
        runCurrent()
        val once = listOf(VoiceLine.RAID_ENTER, VoiceLine.RAID_EXIT)
        assertEquals(once + once, lines)
    }

    @Test
    fun confettiFallsThenStops() {
        val pieces = confettiPieces(colorCount = 8)
        assertTrue(pieces.any { it.at(0.5f) != null })
        assertTrue(pieces.all { it.at(1f) == null })
        assertTrue(pieces.all { it.color in 0 until 8 })
        pieces.forEach { p ->
            for (i in 0..100) p.at(i / 100f)?.let { (_, y) -> assertTrue(y in -0.05f..1.05f) }
        }
    }
}
