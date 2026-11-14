package com.break8t.unknown.ui.island

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class MoonPhaseTest {
    private val day = 86_400_000L
    private fun after(days: Double): Instant = REFERENCE_NEW_MOON.plusMillis((days * day).toLong())

    @Test fun referenceAndWholeMonthsAreNewMoon() {
        assertEquals(0.0, moonPhase(REFERENCE_NEW_MOON), 1e-9)
        val p = moonPhase(after(SYNODIC_DAYS * 300))
        assertTrue("$p", p < 1e-6 || p > 1 - 1e-6)
        assertEquals(0.5, moonPhase(after(SYNODIC_DAYS / 2)), 1e-6)
        assertEquals(0.25, moonPhase(after(SYNODIC_DAYS / 4)), 1e-6)
    }

    @Test fun datesBeforeReferenceStayInRange() {
        val p = moonPhase(Instant.parse("1999-07-01T00:00:00Z"))
        assertTrue("$p", p >= 0.0 && p < 1.0)
        assertEquals(0.75, moonPhase(after(-SYNODIC_DAYS / 4)), 1e-6)
    }

    /** Full moon of 2000-01-21 04:40 UTC and new moon of 2024-01-11 11:57 UTC, from published tables. */
    @Test fun matchesKnownMoons() {
        assertEquals(0.5, moonPhase(Instant.parse("2000-01-21T04:40:00Z")), 0.03)
        val p = moonPhase(Instant.parse("2024-01-11T11:57:00Z"))
        assertTrue("$p", p < 0.03 || p > 0.97)
    }

    @Test fun samePhaseAllDay() {
        val seoul = ZoneId.of("Asia/Seoul")
        val d = LocalDate.of(2026, 11, 15)
        assertEquals(moonPhase(d.atTime(12, 0).atZone(seoul).toInstant()), moonPhase(d, seoul), 0.0)
    }

    private fun litPixels(phase: Double, r: Int = 6) =
        (-r..r).flatMap { j -> (-r..r).map { i -> i to j } }.filter { (i, j) -> moonLit(i, j, r, phase) }

    private val disc = (-6..6).sumOf { j -> (-6..6).count { i -> i * i + j * j <= 36 } }

    @Test fun newMoonIsDarkAndFullMoonIsWhole() {
        assertTrue(litPixels(0.0).isEmpty())
        assertTrue(litPixels(0.999).isEmpty())
        assertEquals(disc, litPixels(0.5).size)
    }

    @Test fun waxingLightsTheRightAndWaningTheLeft() {
        assertTrue(litPixels(0.12).isNotEmpty() && litPixels(0.12).all { it.first > 0 })
        assertTrue(litPixels(0.25).all { it.first > 0 })
        assertTrue(litPixels(0.88).isNotEmpty() && litPixels(0.88).all { it.first < 0 })
        assertTrue(litPixels(0.75).all { it.first < 0 })
        // Gibbous is fuller than a quarter, and a quarter fuller than a crescent.
        assertTrue(litPixels(0.4).size > litPixels(0.25).size && litPixels(0.25).size > litPixels(0.12).size)
    }
}
