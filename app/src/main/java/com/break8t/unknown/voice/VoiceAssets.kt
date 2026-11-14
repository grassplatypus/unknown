package com.break8t.unknown.voice

// android 스텁 없이 테스트하도록 여기에는 android 호출을 두지 않는다. 경로는 모두 "voice/<pack>/<file>" 꼴이다.

internal const val VOICE_DIR = "voice"

// 같은 이름의 파일이 여럿이면 앞에 있는 확장자를 쓴다.
internal val VOICE_EXTENSIONS = listOf("ogg", "mp3", "wav")

/** [packId] 묶음에서 [line]의 녹음 경로를 돌려준다. 파일이 없으면 null이다. */
internal fun resolveVoiceFile(packId: String, line: VoiceLine, paths: Set<String>): String? =
    VOICE_EXTENSIONS.map { "$VOICE_DIR/$packId/${line.fileName}.$it" }.firstOrNull { it in paths }

/** voice/ 바로 아래 폴더마다 묶음 하나다. 이름은 [nameOf]가 주고, 없거나 비면 id를 쓴다. */
internal fun discoverPacks(paths: Collection<String>, nameOf: (String) -> String?): List<VoicePack> =
    paths.mapNotNull { p -> p.split('/').takeIf { it.size == 3 && it[0] == VOICE_DIR }?.get(1) }
        .distinct()
        .sorted()
        .map { id -> VoicePack(id, nameOf(id)?.takeIf { it.isNotBlank() } ?: id) }
