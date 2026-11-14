package com.break8t.unknown.data

import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/** 여러 화면이 함께 읽는 사용자 상태. 백엔드가 없으므로 앱 안 메모리에만 있다(시연용). */
data class Profile(
    val nickname: String = "구름농부",
    val statusMessage: String = "서부간선 단골",
    val coins: Int = 240,
    val level: Int = 7,
    val xp: Int = 465,
    val xpToNext: Int = 600,
    val selectedCarId: String = "car_red",
    val selectedCharacterId: String = "ch_sprout",
    val voiceGuideOn: Boolean = true,
    val voicePackId: String = "voice_default",
    val drivingStatusPublic: Boolean = true,
    // 섬 시간: 실제 시간을 따를지, 따르지 않으면 밤인지
    val islandRealTime: Boolean = true,
    val islandNight: Boolean = false,
    // 개수가 있는 아이템(씨앗, 가구). 키는 Figma 이름(seed_carrot, it_sofa 등)
    val items: Map<String, Int> = mapOf(
        "seed_carrot" to 3, "seed_potato" to 1, "seed_wheat" to 0,
        "it_sofa" to 1, "it_plant" to 2, "it_picture" to 1, "it_rug" to 3, "it_lamp" to 1,
    ),
    // 하나만 가지는 것(캐릭터, 자동차, 음성팩, 사진 포즈)
    val owned: Set<String> = setOf(
        "ch_sprout", "ch_cat", "ch_mush", "ch_cloud", "car_red", "car_blue",
        "voice_default", "pose_wave", "pose_sit", "pose_back",
    ),
    // 도감과 내 정보에서 눌러 본 항목 id. 여기 있으면 NEW 뱃지를 붙이지 않는다.
    val seenIds: Set<String> = emptySet(),
)

class DemoState(initial: Profile = Profile()) {
    private val state = MutableStateFlow(initial)
    val profile: StateFlow<Profile> = state

    fun update(change: (Profile) -> Profile) = state.update(change)

    /** 개수를 더하거나 뺀다. 0 아래로 내려가면 바꾸지 않고 false를 돌려준다. */
    fun addItem(id: String, delta: Int): Boolean {
        var ok = false
        state.update { p ->
            val next = (p.items[id] ?: 0) + delta
            if (next < 0) p else p.copy(items = p.items + (id to next)).also { ok = true }
        }
        return ok
    }

    /** 코인이 모자라면 아무것도 바꾸지 않고 false를 돌려준다. */
    fun spend(coins: Int): Boolean {
        var ok = false
        state.update { p -> if (p.coins >= coins) p.copy(coins = p.coins - coins).also { ok = true } else p }
        return ok
    }
}

/** 화면을 나누면 Activity가 다시 만들어지므로 상태는 프로세스에 하나만 둔다. */
object DemoStore {
    val state = DemoState()
}

val LocalDemoState = staticCompositionLocalOf { DemoStore.state }
