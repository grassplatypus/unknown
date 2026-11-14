package com.break8t.unknown.ui.fielddrop

import androidx.compose.ui.graphics.toArgb
import com.break8t.unknown.ui.iso.IsoPalette
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TopCarTest {
    @Test fun topCarIsSquareCentredAndHeadlightsAreAtTheTop() {
        val n = TOP_CAR_CELLS
        val px = topCarCells(IsoPalette.Red)
        assertEquals(n * n, px.size)
        val light = IsoPalette.Light.toArgb()
        val lightRows = (0 until n).filter { y -> (0 until n).any { x -> px[y * n + x] == light } }
        assertTrue(lightRows.isNotEmpty() && lightRows.all { it < n / 4 }) // the front is up, so heading 0 needs no turn
        val cols = (0 until n).filter { x -> (0 until n).any { y -> px[y * n + x] != 0 } }
        assertEquals(n - 1, cols.first() + cols.last()) // centred across
    }
}
