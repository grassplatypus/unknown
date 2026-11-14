package com.break8t.unknown.ui.settings

import com.break8t.unknown.config.BalanceConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsRulesTest {
    private val cfg = BalanceConfig(nicknameMax = 4, statusMessageMax = 6)

    @Test fun nicknameIsTrimmed() = assertEquals("구름", nicknameToSave("  구름 ", cfg.nicknameMax))

    @Test fun nicknameBlankCannotBeSaved() {
        assertNull(nicknameToSave("", cfg.nicknameMax))
        assertNull(nicknameToSave("   ", cfg.nicknameMax))
    }

    @Test fun nicknameIsCappedAfterTrimming() {
        // 앞 공백은 길이에 들어가지 않는다.
        assertEquals("구름농부", nicknameToSave("  구름농부 ", cfg.nicknameMax))
        assertEquals("구름농부", nicknameToSave("구름농부님", cfg.nicknameMax))
    }

    @Test fun statusMessageMayBeEmpty() {
        assertEquals("", statusMessageToSave("", cfg.statusMessageMax))
        assertEquals("", statusMessageToSave("   ", cfg.statusMessageMax))
    }

    @Test fun statusMessageCapLeavesNoTrailingSpace() =
        assertEquals("강릉 가요", statusMessageToSave(" 강릉 가요 가요", cfg.statusMessageMax))

    @Test fun exactlyMaxIsKept() = assertEquals("가나다라", nicknameToSave("가나다라", cfg.nicknameMax))

    @Test fun timeChoiceFollowsRealTimeSwitch() {
        assertFalse(islandTimeChoiceEnabled(realTime = true))
        assertTrue(islandTimeChoiceEnabled(realTime = false))
    }

    @Test fun ownedVoicePacksUseCatalogNamesAndOrder() {
        val packs = ownedVoicePacks(setOf("voice_cat", "ch_cat", "voice_default", "voice_unknown"))
        assertEquals(listOf("voice_default", "voice_cat"), packs.map { it.id })
        assertEquals(listOf("기본 목소리", "냥이 목소리"), packs.map { it.name })
    }

    @Test fun voiceFolderDropsPrefix() = assertEquals("default", voiceAssetFolder("voice_default"))

    @Test fun particleRo() {
        assertEquals("기본 목소리로", withRo("기본 목소리"))
        assertEquals("하늘로", withRo("하늘"))
        assertEquals("구름빵으로", withRo("구름빵"))
        assertEquals("", withRo(""))
    }
}
