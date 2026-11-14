package com.break8t.unknown.ui.photo

import com.break8t.unknown.ui.house.CameraPreset
import com.break8t.unknown.ui.house.SceneCamera
import com.break8t.unknown.ui.house.SceneCharacter
import com.break8t.unknown.ui.iso.IsoCamera
import com.break8t.unknown.ui.iso.IsoShapes
import com.break8t.unknown.ui.iso.ViewPreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoSceneTest {
    @Test fun presetsAndRotationPickTheEngineCamera() {
        for (p in CameraPreset.entries) for (r in 0..3) {
            assertEquals(IsoCamera.of(ViewPreset.valueOf(p.name), r), SceneCamera(p, r).iso)
        }
        assertEquals(IsoCamera(135f, 30f), SceneCamera(CameraPreset.OBLIQUE, 1).iso)
    }

    @Test fun characterStandsWhereItsFractionPointsAndScalesWithItsSize() {
        val o = characterObject(SceneCharacter("ch_cat", 0.5f, 0.25f, CharacterSizes[1], 0), 10)
        assertEquals(1f, o.scale, 1e-4f)
        assertEquals(5f, o.x + o.ox + o.scale / 2, 1e-4f)
        assertEquals(2.5f, o.y + o.oy + o.scale / 2, 1e-4f)
        assertEquals(IsoShapes.cat, o.shape)
        assertEquals(2f, characterObject(SceneCharacter("ch_cat", 0.5f, 0.5f, CharacterSizes[1] * 2, 0), 10).scale, 1e-4f)
    }

    @Test fun posesAreSmallModelChangesOrTurns() {
        fun of(id: String, turns: Int = 0) = characterObject(SceneCharacter(id, 0.5f, 0.5f, CharacterSizes[1], turns), 5)
        val stand = of("ch_sprout")
        assertEquals(0, stand.rotation)
        assertNotEquals(stand.shape, of("pose_wave").shape)
        assertTrue(of("pose_jump").z > 0f)
        assertEquals(1, of("pose_side").rotation)
        assertEquals(2, of("pose_back").rotation)
        assertEquals(1, of("ch_sprout", turns = 1).rotation)
        assertEquals(3, of("pose_back", turns = 1).rotation)
    }

    @Test fun aPoseIsPutOnTheChosenCharacter() {
        fun cat(id: String) = characterObject(SceneCharacter(id, 0.5f, 0.5f, CharacterSizes[1], 0), 5, characterId = "ch_cat")
        assertEquals(IsoShapes.cat, cat("ch_cat").shape)
        assertEquals(IsoShapes.posed(IsoShapes.cat, "pose_wave", com.break8t.unknown.ui.iso.IsoPalette.Orange), cat("pose_wave").shape)
        assertEquals(IsoShapes.cat, cat("pose_back").shape)
        assertEquals(2, cat("pose_back").rotation)
    }

    @Test fun aNewCharacterStandsAtTheSpawnAndKeepsItsSpotAfterwards() {
        val s = PhotoState().startCharacter(listOf("ch_sprout"), 0.27f to 0.36f)
        assertEquals(0.27f, s.x, 1e-4f)
        val moved = s.copy(x = 0.8f).startCharacter(listOf("ch_sprout"), 0.27f to 0.36f)
        assertEquals(0.8f, moved.x, 1e-4f)
    }

    @Test fun captureFrameFollowsFigmaAndStopsBeforeTheAnglePanel() {
        // Landscape content 1487 x 1171 dp: the Figma frame fits, and its right edge clears the panel by the gap.
        val wide = captureFrame(1487f, 1171f, 1487f - 35f - 243f, 26f)
        assertEquals(1487f * 150f / 1144f, wide.left, 0.01f)
        assertTrue(wide.left + wide.width <= 1487f - 35f - 243f - 26f + 0.01f)
        // Portrait: the frame narrows instead of running under the panel.
        val tall = captureFrame(700f, 1171f, 700f - 35f - 243f, 26f)
        assertEquals(700f - 35f - 243f - 26f, tall.left + tall.width, 0.01f)
    }

    @Test fun tappingACellCentresTheCharacterOnIt() {
        val s = PhotoState().placedAt(2, 4, 5)
        assertEquals(0.5f, s.x, 1e-4f)
        assertEquals(0.9f, s.y, 1e-4f)
    }
}
