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
        val o = characterObject(SceneCharacter("ch_cat", 0.5f, 0.25f, CharacterSizes[1], false), 10)
        assertEquals(1f, o.scale, 1e-4f)
        assertEquals(5f, o.x + o.ox + o.scale / 2, 1e-4f)
        assertEquals(2.5f, o.y + o.oy + o.scale / 2, 1e-4f)
        assertEquals(IsoShapes.cat, o.shape)
        assertEquals(2f, characterObject(SceneCharacter("ch_cat", 0.5f, 0.5f, CharacterSizes[1] * 2, false), 10).scale, 1e-4f)
    }

    @Test fun posesAreSmallModelChangesOrTurns() {
        fun of(id: String, flipped: Boolean = false) = characterObject(SceneCharacter(id, 0.5f, 0.5f, CharacterSizes[1], flipped), 5)
        val stand = of("ch_sprout")
        assertEquals(0, stand.rotation)
        assertNotEquals(stand.shape, of("pose_wave").shape)
        assertTrue(of("pose_jump").z > 0f)
        assertEquals(1, of("pose_side").rotation)
        assertEquals(2, of("pose_back").rotation)
        assertEquals(3, of("ch_sprout", flipped = true).rotation)
    }

    @Test fun tappingACellCentresTheCharacterOnIt() {
        val s = PhotoState().placedAt(2, 4, 5)
        assertEquals(0.5f, s.x, 1e-4f)
        assertEquals(0.9f, s.y, 1e-4f)
    }
}
