package com.break8t.unknown.ui.settings

import com.break8t.unknown.config.Catalog
import com.break8t.unknown.config.CatalogItem
import com.break8t.unknown.config.ItemKind

// 화면 없이 테스트하는 설정 규칙. 길이는 입력란의 maxLength, 카운터와 같은 단위(String.length)로 센다.

/** 저장할 값. 앞뒤 공백을 지우고 [max]자로 자른 뒤 끝 공백을 다시 지운다. 비었는데 [allowBlank]가 아니면 null(저장 못 함). */
internal fun textToSave(draft: String, max: Int, allowBlank: Boolean): String? {
    val text = draft.trim().take(max).trimEnd()
    return if (text.isEmpty() && !allowBlank) null else text
}

internal fun nicknameToSave(draft: String, max: Int): String? = textToSave(draft, max, allowBlank = false)

// 상태 메시지는 비워 둘 수 있다. 비우기 버튼이 있는 이유다.
internal fun statusMessageToSave(draft: String, max: Int): String = textToSave(draft, max, allowBlank = true)!!

/** 섬 시간을 실제 시간에 맞추면 밤/낮은 고를 수 없다(S-25-b). 끄면 고른다(S-25-d). */
internal fun islandTimeChoiceEnabled(realTime: Boolean): Boolean = !realTime

/** 가진 음성팩. 순서와 이름은 Catalog를 따르고, Catalog에 없는 id는 이름이 없어서 뺀다. */
internal fun ownedVoicePacks(owned: Set<String>): List<CatalogItem> =
    Catalog.ofKind(ItemKind.VOICE_PACK).filter { it.id in owned }

// ponytail: Catalog id(voice_default)와 assets/voice/<폴더>(default)를 이어 주는 약속이 아직 없어서 접두어만 뗀다.
internal fun voiceAssetFolder(packId: String): String = packId.removePrefix("voice_")

/** 이름 뒤에 "로/으로"를 붙인다. 받침이 없거나 ㄹ 받침이면 "로"다. */
internal fun withRo(name: String): String {
    val last = name.lastOrNull() ?: return name
    val code = last.code - 0xAC00
    val jong = if (code in 0 until 11172) code % 28 else 0
    return name + if (jong == 0 || jong == 8) "로" else "으로"
}
