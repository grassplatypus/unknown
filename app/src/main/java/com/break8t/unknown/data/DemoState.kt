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
    val drivingStatusPublic: Boolean = true,
)

class DemoState(initial: Profile = Profile()) {
    private val state = MutableStateFlow(initial)
    val profile: StateFlow<Profile> = state

    fun update(change: (Profile) -> Profile) = state.update(change)

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
