package com.break8t.unknown.ui.fielddrop

import com.break8t.unknown.config.BalanceConfig
import com.break8t.unknown.data.DemoState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.util.Locale
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

enum class MushroomKind { GOLD, BLUE }

/** WGS84 좌표. 지도 라이브러리 타입을 쓰지 않아서 Android 없이 테스트할 수 있다. */
data class GeoPoint(val lat: Double, val lng: Double)

/** 내 차 위치와 진행 방향(북 0도, 시계 방향). */
data class CarFix(val at: GeoPoint, val headingDeg: Float)

data class Mushroom(
    val id: String,
    val kind: MushroomKind,
    val place: String,
    val ad: Boolean,
    val at: GeoPoint,
    val rewardCoins: Int,
    val rewardSeedId: String?,
)

/** 필드 드랍 견본. 양천구 신정동, 안양천 건너 서부간선도로 옆에 차를 두었다. */
object FieldDropSamples {
    // FusedLocation이 들어오기 전까지 쓰는 값. 250도는 S-10-a의 차가 25도 돌아 있는 것과 같다.
    val CAR = CarFix(GeoPoint(37.5180, 126.8760), headingDeg = 250f)

    // 거리는 S-10 견본(1.2km, 800m, 1.6km)에 맞춰 골랐다. 금색 보상은 D-08 견본(코인 30, 당근 씨앗 1).
    val mushrooms = listOf(
        Mushroom("park_sisa", MushroomKind.BLUE, "시사 공영주차장", false, GeoPoint(37.52807, 126.87112), 10, null),
        Mushroom("shop_bossam", MushroomKind.GOLD, "항아리 보쌈", true, GeoPoint(37.52878, 126.87671), 30, "seed_carrot"),
        Mushroom("park_sinjeong", MushroomKind.BLUE, "신정 공영주차장", false, GeoPoint(37.51614, 126.86724), 10, null),
        Mushroom("shop_bunsik", MushroomKind.GOLD, "새싹 분식", true, GeoPoint(37.53233, 126.87758), 30, "seed_carrot"),
        // 시연용: 내 차 바로 옆이라 누르면 D-08이 열린다. 받아도 남아 있어서 오늘 한도까지 시연할 수 있다.
        Mushroom("shop_cafe", MushroomKind.GOLD, "골목 카페", true, GeoPoint(37.51806, 126.87605), 30, "seed_carrot"),
    )
}

/** 내 차 위치를 주는 곳. FusedLocation 의존성과 권한이 들어오면 그 구현으로 바꾼다. */
interface CarLocationSource {
    val fix: StateFlow<CarFix>
    fun start() {}
    fun stop() {}
}

class DemoCarLocation(fix: CarFix = FieldDropSamples.CAR) : CarLocationSource {
    override val fix: StateFlow<CarFix> = MutableStateFlow(fix)
}

/** 두 좌표 사이의 거리(m). 하버사인 공식, 지구 반지름은 평균값. */
fun distanceM(a: GeoPoint, b: GeoPoint): Double {
    val dLat = Math.toRadians(b.lat - a.lat)
    val dLng = Math.toRadians(b.lng - a.lng)
    val h = sin(dLat / 2).pow(2) + cos(Math.toRadians(a.lat)) * cos(Math.toRadians(b.lat)) * sin(dLng / 2).pow(2)
    return 2 * EARTH_RADIUS_M * asin(sqrt(h))
}

private const val EARTH_RADIUS_M = 6_371_008.8

// ponytail: 시내 평균 18km/h로 어림한다. 경로 시간이 필요하면 NaviHelper 경로 탐색으로 바꾼다.
private const val CITY_M_PER_MIN = 300.0

fun distanceRoundedM(car: GeoPoint, m: Mushroom): Int = distanceM(car, m.at).roundToInt()

fun minutesFor(meters: Int): Int = (meters / CITY_M_PER_MIN).roundToInt()

/** 처음 열 때와 현재 위치 버튼을 누를 때의 카메라 높이(m). 값은 BalanceConfig에 있다. */
val FIELD_DROP_ALTITUDE_M: Double get() = com.break8t.unknown.config.BalanceConfig.DEFAULT.fieldDropAltitudeM.toDouble()

/** MapLibre 기본 세로 시야각(0.6435 rad). 첫 화면과 현재 위치 버튼이 같은 값을 쓴다. */
const val MAP_DEFAULT_FOV_DEG = 36.8699

// MapLibre는 512dp 타일을 쓴다. 적도에서 줌 0일 때 1dp가 덮는 거리(m)다.
private const val M_PER_DP_AT_Z0 = 78_271.517

fun metersPerDp(latDeg: Double, zoom: Double): Double = M_PER_DP_AT_Z0 * cos(Math.toRadians(latDeg)) / 2.0.pow(zoom)

/** 위에서 똑바로 내려다볼 때 카메라 높이(m). 화면 세로 절반이 시야각 절반에 걸린다. */
fun altitudeM(latDeg: Double, zoom: Double, viewportHeightDp: Double, fovDeg: Double = MAP_DEFAULT_FOV_DEG): Double =
    viewportHeightDp / 2 / tan(Math.toRadians(fovDeg / 2)) * metersPerDp(latDeg, zoom)

/** [altitudeM]의 역. 스타일 최대 줌을 넘으면 최대 줌에서 멈춘다. */
fun zoomForAltitude(
    latDeg: Double,
    altitudeM: Double,
    viewportHeightDp: Double,
    maxZoom: Double,
    fovDeg: Double = MAP_DEFAULT_FOV_DEG,
): Double {
    val mPerDp = altitudeM * tan(Math.toRadians(fovDeg / 2)) / (viewportHeightDp / 2)
    return log2(M_PER_DP_AT_Z0 * cos(Math.toRadians(latDeg)) / mPerDp).coerceAtMost(maxZoom)
}

private val SCALE_STEPS_M = listOf(1, 2, 5, 10, 20, 50, 100, 200, 500, 1_000, 2_000, 5_000, 10_000)

/** 축척 막대에 적을 거리(m)와 막대 길이(dp). [maxDp]에 들어가는 가장 긴 눈금을 고른다. */
fun scaleBar(metersPerDp: Double, maxDp: Double): Pair<Int, Double> {
    val step = SCALE_STEPS_M.lastOrNull { it / metersPerDp <= maxDp } ?: SCALE_STEPS_M.first()
    return step to step / metersPerDp
}

fun scaleLabel(meters: Int): String = if (meters < 1000) "${meters}m" else "${meters / 1000}km"

fun reachable(car: GeoPoint, m: Mushroom, config: BalanceConfig): Boolean = distanceM(car, m.at) <= config.mushroomRadiusM

/** 버섯을 눌렀을 때 일어나는 일. */
enum class MarkerTap { NONE, CARD, REWARD }

/** 한도에 닿으면 버섯은 흐려지고 눌리지 않는다. 반경 안이면 열고(D-08), 밖이면 카드를 띄운다. */
fun markerTap(m: Mushroom, car: GeoPoint, state: FieldDropState, config: BalanceConfig): MarkerTap = when {
    state.atLimit(config) -> MarkerTap.NONE
    reachable(car, m, config) -> MarkerTap.REWARD
    else -> MarkerTap.CARD
}

/** 자동차 그림(TopCar.kt)의 앞쪽은 회전 0일 때 북(위)을 본다. 북 0도, 시계 방향. */
const val SPRITE_FRONT_DEG = 0f

/** 앞쪽이 heading을 보도록 그림을 돌릴 각도. [0, 360) */
fun carRotation(headingDeg: Float): Float = ((headingDeg - SPRITE_FRONT_DEG) % 360f + 360f) % 360f

/** 지도가 직접 그리는 버섯 하나. 그림 이름과 흐림을 속성으로 넘겨서 지도와 같은 프레임에 그려진다. */
data class MushroomFeature(val id: String, val at: GeoPoint, val icon: String, val dim: Boolean)

data class MapMarkers(val car: GeoPoint, val carIcon: String, val carRotate: Float, val mushrooms: List<MushroomFeature>)

fun mushroomIcon(kind: MushroomKind, ad: Boolean, selected: Boolean): String =
    "fd_mush_" + kind.name.lowercase() + (if (ad) "_ad" else "") + (if (selected) "_sel" else "")

fun carIconName(carId: String): String = if (carId == "car_blue" || carId == "car_yellow") "fd_$carId" else "fd_car_red"

/** 한도에 닿으면 모두 흐리고, 고른 버섯(S-10-b)만 금색 테두리 그림을 쓴다. */
fun mapMarkers(car: CarFix, carId: String, mushrooms: List<Mushroom>, state: FieldDropState, config: BalanceConfig) = MapMarkers(
    car.at,
    carIconName(carId),
    carRotation(car.headingDeg),
    mushrooms.map { MushroomFeature(it.id, it.at, mushroomIcon(it.kind, it.ad, state.selectedId == it.id), state.atLimit(config)) },
)

/** 누른 자리에서 지도가 찾은 표시 id 가운데 첫 버섯. 없으면 빈 곳을 누른 것이다. */
fun mushroomAt(hitIds: List<String?>, mushrooms: List<Mushroom>): Mushroom? =
    hitIds.firstNotNullOfOrNull { id -> mushrooms.firstOrNull { it.id == id } }

/** 정사각형 그림을 [deg]만큼 돌려 담을 정사각형의 한 변(px). 원래 변과 홀짝을 맞춰서 가운데가 같은 픽셀에 남는다. */
fun rotatedSidePx(sidePx: Int, deg: Float): Int {
    val r = Math.toRadians(deg.toDouble())
    val need = ceil(sidePx * (abs(cos(r)) + abs(sin(r))) - 1e-9).toInt()
    return need + (need - sidePx).mod(2)
}

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

    fun tap(m: Mushroom, car: GeoPoint, config: BalanceConfig) = mutable.update { s ->
        when (markerTap(m, car, s, config)) {
            MarkerTap.NONE -> s
            MarkerTap.REWARD -> s.copy(openId = m.id, selectedId = null)
            MarkerTap.CARD -> s.copy(selectedId = m.id)
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
    val car: CarLocationSource = DemoCarLocation()
}
