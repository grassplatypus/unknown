package com.break8t.unknown.ui.scene

import com.break8t.unknown.ui.island.friendStash
import com.break8t.unknown.ui.myinfo.Page
import com.break8t.unknown.ui.myinfo.backFrom
import com.break8t.unknown.ui.photo.CharacterSizes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class SceneStashTest {
    private val cat = StashItem("ch_cat", StashKind.CHARACTER)
    private val sofa = StashItem("it_sofa", StashKind.FURNITURE)
    private val plant = StashItem("it_plant", StashKind.OBJECT)
    private val bounds = StageBounds(grid = 12, baseScale = 1.6f)
    private val out = SceneStage(panelOpen = true).takeOut(cat, FriendVisitRule, 0.3f to 0.4f, bounds)

    @Test fun friendRuleLetsOnlyCharactersOut() {
        assertTrue(FriendVisitRule(cat))
        assertFalse(FriendVisitRule(sofa))
        assertFalse(FriendVisitRule(plant))
        val s = SceneStage()
        assertSame(s, s.takeOut(sofa, FriendVisitRule, 0.5f to 0.5f, bounds))
        assertSame(s, s.takeOut(plant, FriendVisitRule, 0.5f to 0.5f, bounds))
    }

    @Test fun friendStashHoldsOnlyTheOwnersCharacter() {
        assertEquals(listOf(StashItem("ch_mush", StashKind.CHARACTER)), friendStash("ch_mush"))
    }

    @Test fun takingOutStandsTheCharacterInTheDefaultPoseAndClosesThePanel() {
        val p = out.piece("ch_cat")!!
        assertEquals(0.3f, p.x, 1e-6f)
        assertEquals(0.4f, p.y, 1e-6f)
        assertNull(p.poseId)
        assertEquals("ch_cat", p.look)
        assertEquals(DEFAULT_SIZE, p.sizeIndex)
        assertEquals("ch_cat", out.selected)
        assertFalse(out.panelOpen)
        assertTrue(out.inStash(listOf(cat)).isEmpty())
    }

    @Test fun takingOutTwiceKeepsOne() {
        assertEquals(1, out.takeOut(cat, FriendVisitRule, 0.9f to 0.9f, bounds).pieces.size)
    }

    @Test fun puttingAwayReturnsItToTheStash() {
        val s = out.putAway("ch_cat")
        assertTrue(s.pieces.isEmpty())
        assertNull(s.selected)
        assertEquals(listOf(cat), s.inStash(listOf(cat)))
    }

    @Test fun sizeStaysWithinSteps() {
        var s = out
        repeat(10) { s = s.resize("ch_cat", -1, bounds) }
        assertEquals(0, s.piece("ch_cat")!!.sizeIndex)
        repeat(10) { s = s.resize("ch_cat", +1, bounds) }
        assertEquals(CharacterSizes.lastIndex, s.piece("ch_cat")!!.sizeIndex)
    }

    @Test fun sizeAndPoseApplyToCharactersOnly() {
        val s = SceneStage().takeOut(sofa, { true }, 0.5f to 0.5f, bounds)
        assertEquals(DEFAULT_SIZE, s.resize("it_sofa", 1, bounds).piece("it_sofa")!!.sizeIndex)
        assertNull(s.pose("it_sofa", "pose_wave").piece("it_sofa")!!.poseId)
    }

    @Test fun poseCanBeSetAndChosenBackToDefault() {
        val waved = out.pose("ch_cat", "pose_wave")
        assertEquals("pose_wave", waved.piece("ch_cat")!!.look)
        assertNull(waved.pose("ch_cat", "ch_cat").piece("ch_cat")!!.poseId)
    }

    @Test fun allSixPosesAreOffered() {
        assertEquals(listOf("ch_cat", "pose_wave", "pose_jump", "pose_sit", "pose_side", "pose_back"), allPoseOptions("ch_cat"))
    }

    @Test fun turningCyclesInQuarterTurns() {
        var s = out
        repeat(4) { s = s.turn("ch_cat") }
        assertEquals(0, s.piece("ch_cat")!!.turns)
        assertEquals(1, s.turn("ch_cat").piece("ch_cat")!!.turns)
    }

    @Test fun moveStopsAtTheEdgeOfTheScene() {
        val p = out.piece("ch_cat")!!
        val m = bounds.margin(p)
        assertTrue(m > 0f && m < 0.5f)
        val far = out.moveBy("ch_cat", 5f, -5f, bounds).piece("ch_cat")!!
        assertEquals(1f - m, far.x, 1e-6f)
        assertEquals(m, far.y, 1e-6f)
        val cell = out.placeAtCell("ch_cat", 0, 11, bounds).piece("ch_cat")!!
        assertEquals(m, cell.x, 1e-6f)
        assertEquals(1f - m, cell.y, 1e-6f)
    }

    @Test fun growingAtTheEdgePushesTheCharacterBackInside() {
        val atEdge = out.moveBy("ch_cat", 5f, 0f, bounds)
        val grown = atEdge.resize("ch_cat", 2, bounds).piece("ch_cat")!!
        assertEquals(1f - bounds.margin(grown), grown.x, 1e-6f)
        assertTrue(grown.x < atEdge.piece("ch_cat")!!.x)
    }

    @Test fun characterModelStaysOnTheFloorAtTheEdge() {
        val s = out.resize("ch_cat", 2, bounds).moveBy("ch_cat", 5f, 5f, bounds)
        val o = stageObjects(s, bounds).single()
        assertEquals("ch_cat", o.id)
        assertEquals(bounds.grid.toFloat(), o.ox + o.scale, 1e-4f)
        assertEquals(bounds.grid.toFloat(), o.oy + o.scale, 1e-4f)
    }

    @Test fun backClosesPanelThenCardThenLeaves() {
        assertFalse(SceneStage(panelOpen = true).back()!!.panelOpen)
        assertNull(out.back()!!.selected)
        assertNull(out.select(null).back())
    }

    @Test fun selectingOnlyPicksPiecesInTheScene() {
        assertNull(SceneStage().select("ch_cat").selected)
        assertEquals("ch_cat", out.select(null).select("ch_cat").selected)
    }

    @Test fun photoStartsWithTheCharacterAsItStands() {
        val s = out.pose("ch_cat", "pose_jump").resize("ch_cat", 1, bounds).turn("ch_cat")
        val p = s.piece("ch_cat")!!
        val photo = s.photoStart("ch_cat")
        assertEquals("pose_jump", photo.poseId)
        assertEquals(p.x, photo.x, 0f)
        assertEquals(p.y, photo.y, 0f)
        assertEquals(p.sizeIndex, photo.sizeIndex)
        assertEquals(1, photo.turns)
        assertFalse(photo.shot)
        assertNull(out.putAway("ch_cat").photoStart("ch_cat").poseId)
    }

    @Test fun stageSurvivesSaving() {
        val s = out.pose("ch_cat", "pose_sit").turn("ch_cat").copy(panelOpen = true)
        assertEquals(s, SceneStage.fromList(s.toList()))
        assertEquals(SceneStage(), SceneStage.fromList(SceneStage().toList()))
    }

    @Test fun friendPagesGoBackWhereTheyCameFrom() {
        assertEquals(Page.FRIEND_ISLAND, backFrom(Page.FRIEND_PHOTO))
        assertEquals(Page.FRIENDS, backFrom(Page.FRIEND_ISLAND))
        assertEquals(Page.FRIENDS, backFrom(Page.FRIEND_COLLECTION))
        assertEquals(Page.FRIENDS, backFrom(Page.ADD))
        assertEquals(Page.HOME, backFrom(Page.FRIENDS))
    }
}
