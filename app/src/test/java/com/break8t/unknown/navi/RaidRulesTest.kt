package com.break8t.unknown.navi

import ai.pleos.playground.navi.constants.NaviMode
import com.break8t.unknown.ui.home.RaidState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RaidRulesTest {
    private val rules = RaidRules()
    private val late = rules.earlyWindowMs + 1

    private fun item(offset: Double, length: Double, road: String = "올림픽대로", jammed: Boolean = true, start: String = "", end: String = "") =
        JamItem(offset, length, jammed, avgKmh = 20.0, road, start, end)

    private fun decide(items: List<JamItem>, mode: NaviMode = NaviMode.ROUTE_GUIDING, since: Long = late, stale: List<JamItem>? = null, insideRoad: String? = null) =
        decideRaid(mode, items, since, stale, insideRoad, rules)

    @Test
    fun insideWhenTheJamStartsAtOrBehindUs() {
        val fix = decide(listOf(item(0.0, 1200.0, start = "동작대교분기점"))) as RaidFix.Inside
        assertEquals(1200.0, fix.remainingM, 0.0)
        assertEquals("동작대교분기점", fix.jam.startCross)
        assertEquals(900.0, (decide(listOf(item(-300.0, 1200.0))) as RaidFix.Inside).remainingM, 0.0)
    }

    @Test
    fun approachingTheNearestLongJamWithinRange() {
        val fix = decide(listOf(item(1800.0, 900.0, "남부순환로"), item(1500.0, 800.0))) as RaidFix.Approaching
        assertEquals(1500.0, fix.aheadM, 0.0)
        assertEquals("올림픽대로", fix.jam.road)
    }

    @Test
    fun noneWhenFarShortOrNotJammed() {
        assertEquals(RaidFix.None, decide(listOf(item(rules.approachM + 1, 2000.0))))
        assertEquals(RaidFix.None, decide(listOf(item(0.0, rules.minJamM - 1))))
        assertEquals(RaidFix.None, decide(listOf(item(0.0, 3000.0, jammed = false))))
        assertEquals(RaidFix.None, decide(emptyList()))
    }

    @Test
    fun adjacentPiecesOnOneRoadMergeIntoOneJam() {
        // 실제 응답처럼 300 m 조각 둘이 이어져 있다. 따로 보면 둘 다 기준보다 짧다.
        val items = listOf(item(0.0, 300.0, start = "삼호가든사거리"), item(300.0, 300.0, end = "반포IC"), item(650.0, 100.0, "사평대로"))
        val fix = decide(items) as RaidFix.Inside
        assertEquals(600.0, fix.remainingM, 0.0)
        assertEquals("삼호가든사거리" to "반포IC", fix.jam.startCross to fix.jam.endCross)
    }

    @Test
    fun ignoredUnlessRouteGuiding() {
        val items = listOf(item(0.0, 1200.0))
        for (mode in listOf(NaviMode.SAFE_GUIDING, NaviMode.NOT_WORKING, NaviMode.SIMULATION_GUIDING)) {
            assertEquals(RaidFix.None, decide(items, mode))
        }
    }

    @Test
    fun ignoredInTheEarlyWindowAfterRouteStart() {
        val items = listOf(item(0.0, 1200.0))
        assertEquals(RaidFix.Unknown, decide(items, since = 26))
        assertEquals(RaidFix.Unknown, decide(items, since = rules.earlyWindowMs - 1))
        assertTrue(decide(items, since = rules.earlyWindowMs) is RaidFix.Inside)
    }

    @Test
    fun theCancelledRoutesListIsStaleEvenAfterTheWindow() {
        val old = listOf(item(0.0, 1200.0))
        assertEquals(RaidFix.Unknown, decide(old, stale = old))
        assertTrue(decide(listOf(item(0.0, 1100.0)), stale = old) is RaidFix.Inside)
    }

    @Test
    fun aJamWeAreInsideIsKeptWhenItsRemainderIsShort() {
        val tail = listOf(item(0.0, 100.0))
        assertEquals(RaidFix.None, decide(tail))
        assertEquals(RaidFix.None, decide(tail, insideRoad = "남부순환로"))
        assertEquals(100.0, (decide(tail, insideRoad = "올림픽대로") as RaidFix.Inside).remainingM, 0.0)
    }

    private fun inside(remaining: Double, road: String = "올림픽대로") =
        RaidFix.Inside(Jam(0.0, remaining, road, "", ""), remaining)

    @Test
    fun remainingAndProgressFollowTheJamWhenItShrinksAndGrows() {
        val t = JamTracker(rules = rules)
        val shown = listOf(1000.0, 800.0, 900.0, 1200.0, 600.0).mapIndexed { i, m ->
            (t.update(inside(m), i * rules.pollMs) as RaidState.Solo).jam
        }
        assertEquals(listOf(1000.0, 800.0, 900.0, 1200.0, 600.0), shown.map { it.remainingM })
        // 지금까지 본 가장 긴 남은 길이에 대한 비율이다. 정체가 앞으로 늘면 막대가 줄어든다.
        val expected = listOf(0f, 0.2f, 0.1f, 0f, 0.5f)
        expected.zip(shown).forEach { (e, j) -> assertEquals(e, j.progress, 1e-6f) }
    }

    @Test
    fun jamTimeIsCountedPerSegmentAndEndsWithPassed() {
        val t = JamTracker(rules = rules)
        t.update(inside(1000.0), 0)
        t.update(inside(900.0), 5_000)
        t.update(inside(700.0), 10_000)
        val passed = t.update(RaidFix.None, 15_000) as RaidState.Passed
        assertEquals(10L, passed.tookSec)
        assertEquals(1f, passed.jam.progress)
        assertEquals(RaidState.None, t.update(RaidFix.None, 20_000))
        t.update(inside(800.0, "남부순환로"), 30_000)
        t.update(inside(500.0, "남부순환로"), 37_500)
        assertEquals(listOf(10L, 7L), t.segmentsSec())
        t.update(RaidFix.None, 40_000)
        assertEquals(listOf(10L, 7L), t.segmentsSec())
    }

    @Test
    fun aRoadChangeClosesTheSegmentWithoutAPassedScreen() {
        val t = JamTracker(rules = rules)
        t.update(inside(1000.0), 0)
        t.update(inside(500.0), 6_000)
        assertTrue(t.update(inside(900.0, "사평대로"), 12_000) is RaidState.Solo)
        t.update(inside(400.0, "사평대로"), 17_000)
        assertEquals(listOf(6L, 5L), t.segmentsSec())
    }

    @Test
    fun aGapLongerThanTheLimitIsNotCounted() {
        val t = JamTracker(rules = rules)
        t.update(inside(1000.0), 0)
        t.update(inside(900.0), 70_000)
        t.update(inside(800.0), 70_000 + rules.maxCountedGapMs + 1)
        assertEquals(listOf(70L), t.segmentsSec())
    }

    @Test
    fun unreliableResponsesDoNotEndTheJam() {
        val t = JamTracker(rules = rules)
        t.update(inside(1000.0), 0)
        val held = t.update(RaidFix.Unknown, 5_000) as RaidState.Solo
        assertEquals(1000.0, held.jam.remainingM, 0.0)
        t.update(inside(800.0), 10_000)
        assertEquals(listOf(10L), t.segmentsSec())
        assertEquals(RaidState.None, RaidFix.Unknown.let { JamTracker(rules = rules).update(it, 0) })
    }

    @Test
    fun aJamSeenInOnlyOnePollLeavesNoSegmentAndNoPassedScreen() {
        val t = JamTracker(rules = rules)
        t.update(inside(1000.0), 0)
        assertEquals(emptyList<Long>(), t.segmentsSec())
        assertEquals(RaidState.None, t.update(RaidFix.None, rules.pollMs))
        assertEquals(emptyList<Long>(), t.segmentsSec())
    }
}
