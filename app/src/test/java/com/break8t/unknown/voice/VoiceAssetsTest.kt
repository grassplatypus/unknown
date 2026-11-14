package com.break8t.unknown.voice

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceAssetsTest {
    @Test fun oggWinsOverMp3AndWav() {
        val paths = setOf("voice/a/level_up.wav", "voice/a/level_up.mp3", "voice/a/level_up.ogg")
        assertEquals("voice/a/level_up.ogg", resolveVoiceFile("a", VoiceLine.LEVEL_UP, paths))
    }

    @Test fun mp3WinsOverWav() {
        val paths = setOf("voice/a/raid_enter.wav", "voice/a/raid_enter.mp3")
        assertEquals("voice/a/raid_enter.mp3", resolveVoiceFile("a", VoiceLine.RAID_ENTER, paths))
    }

    @Test fun wavAloneIsUsed() {
        assertEquals("voice/a/raid_exit.wav", resolveVoiceFile("a", VoiceLine.RAID_EXIT, setOf("voice/a/raid_exit.wav")))
    }

    @Test fun missingFileIsNull() {
        val paths = setOf("voice/a/pack.json", "voice/a/level_up.ogg", "voice/b/crops_ready.ogg", "voice/a/crops_ready.flac")
        assertNull(resolveVoiceFile("a", VoiceLine.CROPS_READY, paths))
        assertNull(resolveVoiceFile("missing", VoiceLine.LEVEL_UP, paths))
    }

    @Test fun packsAreFoldersUnderVoice() {
        val paths = listOf(
            "voice/kid/level_up.ogg", "voice/default/pack.json", "voice/default/README.md",
            "voice/kid/raid_enter.mp3", "voice/notes.txt", "images/x/y.png",
        )
        val names = mapOf("default" to "기본 목소리", "kid" to " ")
        assertEquals(
            listOf(VoicePack("default", "기본 목소리"), VoicePack("kid", "kid")),
            discoverPacks(paths) { names[it] },
        )
    }

    @Test fun noVoiceAssetsMeansNoPacks() = assertEquals(emptyList<VoicePack>(), discoverPacks(emptyList()) { null })

    @Test fun fileNamesAreUniqueSnakeCase() {
        val names = VoiceLine.entries.map { it.fileName }
        assertEquals(names.size, names.toSet().size)
        names.forEach { assertTrue(it, it.matches(Regex("[a-z][a-z0-9_]*"))) }
    }

    // Gradle은 모듈 폴더(app/)에서 테스트를 돌린다.
    @Test fun readmeListsExactlyTheEnum() {
        val readme = File("src/main/assets/voice/default/README.md").readText()
        val listed = Regex("^\\| `([a-z0-9_]+)` \\|", RegexOption.MULTILINE).findAll(readme).map { it.groupValues[1] }.toList()
        assertEquals(VoiceLine.entries.map { it.fileName }, listed)
    }
}
