package com.break8t.unknown.ui.fielddrop

import com.break8t.unknown.config.BalanceConfig
import com.break8t.unknown.data.DemoState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.util.Locale

enum class MushroomKind { GOLD, BLUE }

/** 지도 위 버섯. x, y는 지도 그림 안에서의 비율(0..1)이라 창 크기와 상관없다. */
data class Mushroom(
    val id: String,
    val kind: MushroomKind,
    val place: String,
    val ad: Boolean,
    val distanceM: Int,
    val minutes: Int,
    val x: Float,
    val y: Float,
    val rewardCoins: Int,
    val rewardSeedId: String?,
)

/** 필드 드랍 견본. 위치는 S-10-a에서 잰 값을 지도 그림 비율로 옮긴 것이다. */
object FieldDropSamples {
    const val CAR_X = 0.402f
    const val CAR_Y = 0.728f

    // FusedLocation heading이 들어오기 전까지 쓰는 값. S-10-a의 차가 25도 돌아 있는 것과 같다.
    const val DEMO_HEADING_DEG = 250f

    // 금색 보상은 D-08 견본(코인 30, 당근 씨앗 1). 파란 버섯 보상은 임시.
    val mushrooms = listOf(
        Mushroom("park_sisa", MushroomKind.BLUE, "시사 공영주차장", false, 1200, 4, 0.279f, 0.235f, 10, null),
        Mushroom("shop_bossam", MushroomKind.GOLD, "항아리 보쌈", true, 1200, 4, 0.493f, 0.413f, 30, "seed_carrot"),
        Mushroom("park_sinjeong", MushroomKind.BLUE, "신정 공영주차장", false, 800, 3, 0.331f, 0.579f, 10, null),
        Mushroom("shop_bunsik", MushroomKind.GOLD, "새싹 분식", true, 1600, 5, 0.522f, 0.213f, 30, "seed_carrot"),
        // 시연용: 내 차 바로 옆이라 누르면 D-08이 열린다. 받아도 남아 있어서 오늘 한도까지 시연할 수 있다.
        Mushroom("shop_cafe", MushroomKind.GOLD, "골목 카페", true, 15, 0, 0.47f, 0.66f, 30, "seed_carrot"),
    )
}

/** 자동차 그림의 앞쪽은 회전 0일 때 남서(225도)를 본다. 북 0도, 시계 방향. */
const val SPRITE_FRONT_DEG = 225f

/** 앞쪽이 heading을 보도록 그림을 돌릴 각도. [0, 360) */
fun carRotation(headingDeg: Float): Float = ((headingDeg - SPRITE_FRONT_DEG) % 360f + 360f) % 360f

/** 지도 왼쪽 위의 위치를 화면 안으로 묶는다. 지도가 화면보다 작으면 가운데에 둔다. */
fun clampPan(offset: Float, content: Float, viewport: Float): Float =
    if (content <= viewport) (viewport - content) / 2f else offset.coerceIn(viewport - content, 0f)

fun distanceText(meters: Int): String =
    if (meters < 1000) "${meters}m" else String.format(Locale.ROOT, "%.1fkm", meters / 1000.0)

/** 경유지 등록. 지금은 가짜이고, 나중에 NaviHelper 호출로 바꾼다. */
interface WaypointSink {
    fun add(mushroom: Mushroom)
    fun remove(mushroom: Mushroom)
}

class FakeWaypointSink : WaypointSink {
    val waypoints = mutableListOf<String>()
    override fun add(mushroom: Mushroom) { waypoints += mushroom.id }
    override fun remove(mushroom: Mushroom) { waypoints -= mushroom.id }
}

data class FieldDropState(
    val todayCount: Int = 3, // Figma 견본 "오늘 3 / 10"
    val selectedId: String? = null,
    val openId: String? = null,
    val guidingId: String? = null,
) {
    fun atLimit(config: BalanceConfig) = todayCount >= config.mushroomDailyLimit
}

class FieldDrop(
    val mushrooms: List<Mushroom>,
    private val waypoints: WaypointSink,
    initial: FieldDropState = FieldDropState(),
) {
    private val mutable = MutableStateFlow(initial)
    val state: StateFlow<FieldDropState> = mutable

    fun mushroom(id: String?): Mushroom? = mushrooms.firstOrNull { it.id == id }

    /** 가까운 버섯은 열고(D-08), 먼 버섯은 카드를 띄운다. 한도에 닿으면 아무것도 하지 않는다. */
    fun tap(m: Mushroom, config: BalanceConfig) = mutable.update { s ->
        when {
            s.atLimit(config) -> s
            m.distanceM <= config.mushroomRadiusM -> s.copy(openId = m.id, selectedId = null)
            else -> s.copy(selectedId = m.id)
        }
    }

    fun dismissCard() = mutable.update { it.copy(selectedId = null) }

    fun closeReward() = mutable.update { it.copy(openId = null) }

    /** 안내 중인 버섯이면 취소하고, 아니면 안내를 이 버섯으로 바꾼다. 안내 중이 되면 true. */
    fun toggleGuide(m: Mushroom): Boolean {
        val before = mushroom(mutable.value.guidingId)
        val guiding = before?.id != m.id
        mutable.update { it.copy(guidingId = if (guiding) m.id else null) }
        before?.let(waypoints::remove)
        if (guiding) waypoints.add(m)
        return guiding
    }

    /** D-08 받기. 열린 버섯 하나에 한 번만 주고, 오늘 받은 수에 더한다. */
    fun receive(config: BalanceConfig, demo: DemoState): Boolean {
        var granted: Mushroom? = null
        mutable.update { s ->
            granted = mushroom(s.openId)?.takeIf { !s.atLimit(config) }
            if (granted == null) s.copy(openId = null) else s.copy(openId = null, todayCount = s.todayCount + 1)
        }
        val m = granted ?: return false
        demo.update { it.copy(coins = it.coins + m.rewardCoins) }
        m.rewardSeedId?.let { demo.addItem(it, 1) }
        return true
    }
}

/** 화면을 나누거나 메뉴를 옮겨도 오늘 받은 수가 남도록 프로세스에 하나만 둔다. */
object FieldDropStore {
    val model = FieldDrop(FieldDropSamples.mushrooms, FakeWaypointSink())
}
