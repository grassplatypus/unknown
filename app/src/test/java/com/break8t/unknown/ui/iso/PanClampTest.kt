package com.break8t.unknown.ui.iso

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import org.junit.Assert.assertEquals
import org.junit.Test

class PanClampTest {
    private val view = Size(1000f, 800f)

    @Test fun unzoomedSceneMovesOnlyBySlack() {
        near(Offset(150f, -120f), clampPan(Offset(900f, -900f), 1f, view))
    }

    @Test fun zoomedSceneMovesByItsOverflowPlusSlack() {
        // At zoom 2 the scene is 2000 wide: 500 of overflow each side, plus 150.
        near(Offset(-650f, 520f), clampPan(Offset(-5000f, 5000f), 2f, view))
    }

    private fun near(want: Offset, got: Offset) {
        assertEquals(want.x, got.x, 0.01f)
        assertEquals(want.y, got.y, 0.01f)
    }
}
