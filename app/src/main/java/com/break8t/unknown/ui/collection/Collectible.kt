package com.break8t.unknown.ui.collection

import androidx.annotation.DrawableRes
import com.break8t.unknown.R
import com.break8t.unknown.config.BalanceConfig
import com.break8t.unknown.config.Catalog
import com.break8t.unknown.config.ItemKind
import com.break8t.unknown.data.Profile
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** 내 정보(S-21)와 도감(S-24)이 함께 보이는 다섯 묶음. 탭 순서와 같다. */

/** 엽서 그림의 비율. 그림에 직각 액자가 그려져 있어서 이 비율 그대로, 모서리를 깎지 않고 보인다. */
const val POSTCARD_ASPECT = 960f / 645f

enum class CollectionKind { CARD, POSTCARD, CHARACTER, CAR, BADGE }

/** 엽서 판. 같은 장소에도 판이 여러 개 있고, 판마다 따로 모은다. 테두리와 배지 색은 CollectionUi.kt에 있다. */
enum class PostcardRarity { NORMAL, CHRISTMAS, GOLD }

/** 정체 카드의 구간을 한 번 지난 기록. [seconds]는 그 구간에 걸린 시간이다. */
data class Visit(val date: LocalDate, val seconds: Int)

/**
 * 모은 것 하나. [recency]는 같은 묶음 안에서만 비교하며 클수록 최근에 얻은 것이다.
 * [gotAt]은 얻은 시각(epoch ms)으로 NEW 뱃지에만 쓴다. 정체 카드, 엽서, 뱃지의 나머지 필드는 견본 데이터에만 있다.
 */
data class Collectible(
    val kind: CollectionKind,
    val id: String,
    val name: String,
    @DrawableRes val art: Int?,
    val owned: Boolean,
    val recency: Long = 0,
    val price: Int? = null,
    val meetCount: Int? = null,
    val route: String? = null,
    val usualTime: String? = null,
    val note: String? = null,
    val date: String? = null,
    val visits: List<Visit> = emptyList(),
    val rarity: PostcardRarity? = null,
    val gotAt: Long? = null,
)

/** 견본의 "최근에 얻은 것"은 이 시각을 기준으로 정한다. 그래야 언제 시연해도 NEW가 보인다. */
internal val demoLaunchedAt: Long = System.currentTimeMillis()
private val launchDay: LocalDate = Instant.ofEpochMilli(demoLaunchedAt).atZone(ZoneId.systemDefault()).toLocalDate()
private const val HOUR_MS = 3_600_000L

// Figma S-24 견본. D-24 자세히 값은 성산대교만 Figma에 있고 나머지는 같은 꼴로 지어 넣었다.
// 만난 횟수, 가장 빠른 기록과 느린 기록은 지난 기록에서 계산한다. 처음 지난 날이 얻은 날이다.
private val demoCards = listOf(
    card("card_yangjae", "양재 나들목", R.drawable.myinfo_card_ic,
        "경부고속도로 · 서초IC → 양재IC", "평일 저녁 6시쯤", listOf(Visit(launchDay, 9 * 60 + 20))),
    card("card_seongsan", "성산대교 구간", R.drawable.myinfo_card_seongsan,
        "서부간선도로 · 성산대교 → 목동교", "평일 오전 8시쯤", listOf(
            v("2026-11-13", 18, 40), v("2026-11-12", 22, 10), v("2026-11-11", 6, 10), v("2026-11-10", 15, 30),
            v("2026-11-06", 31, 20), v("2026-11-05", 12, 50), v("2026-11-04", 250), v("2026-11-02", 19),
            v("2026-10-30", 9, 40), v("2026-10-29", 27, 30), v("2026-10-28", 14, 20), v("2026-10-27", 11),
            v("2026-10-23", 24, 10), v("2026-10-21", 16, 40),
        )),
    card("card_hannam", "한남대교 북단", R.drawable.myinfo_card_hannam,
        "강변북로 · 한남대교 북단 → 반포대교 북단", "금요일 저녁 7시쯤", listOf(
            v("2026-11-13", 21, 10), v("2026-11-06", 38), v("2026-10-30", 12, 40), v("2026-10-24", 4, 30),
            v("2026-10-23", 26, 50), v("2026-10-17", 9, 20), v("2026-10-16", 17), v("2026-10-10", 7, 40),
            v("2026-10-08", 15, 30),
        )),
    card("card_singal", "신갈 분기점", R.drawable.myinfo_card_ic,
        "경부고속도로 · 신갈JC → 수원신갈IC", "주말 오후 5시쯤", listOf(
            v("2026-11-08", 33, 20), v("2026-10-25", 85), v("2026-10-18", 14, 10), v("2026-10-04", 7, 50),
            v("2026-09-27", 41), v("2026-09-25", 52, 30),
        )),
    card("card_tunnel", "남산1호터널", R.drawable.myinfo_card_tunnel,
        "남산1호터널 · 한남오거리 → 회현동", "평일 오전 9시쯤", listOf(
            v("2026-10-29", 8, 10), v("2026-10-14", 22), v("2026-09-16", 3, 20), v("2026-09-02", 11, 40),
        )),
    card("card_hangang", "한강대교 구간", R.drawable.myinfo_card_hangang,
        "한강대로 · 노들역 → 한강대교 북단", "평일 저녁 6시쯤", listOf(
            v("2026-10-07", 41), v("2026-09-10", 17, 20), v("2026-08-17", 5, 40),
        )),
    card("card_riverside", "올림픽대로 잠실", R.drawable.myinfo_card_riverside,
        "올림픽대로 · 잠실대교 → 청담대교", "평일 저녁 7시쯤", listOf(v("2026-09-03", 52), v("2026-07-30", 8, 10))),
    card("card_pangyo", "판교 나들목", R.drawable.myinfo_card_ic,
        "경부고속도로 · 판교IC → 서초IC", "주말 오전 11시쯤", listOf(v("2026-07-11", 12, 30))),
    Collectible(CollectionKind.CARD, "card_gayang", "강변북로 가양", R.drawable.myinfo_card_unknown, owned = false),
)

// 같은 장소의 다른 판은 이름이 같고 id만 다르다. 새 그림 없이 같은 그림에 판 테두리를 얹어 구분한다.
private val demoPostcards = listOf(
    postcard("post_cheomseongdae", "경주 첨성대", R.drawable.myinfo_post_cheomseongdae, "방문", LocalDate.parse("2026-10-12")),
    // 크리스마스 판은 눈 덮인 전용 그림이다(노말 그림을 참고 그림으로 같은 방식으로 만듦).
    postcard("post_cheomseongdae_xmas", "경주 첨성대", R.drawable.myinfo_post_cheomseongdae_xmas, "방문",
        LocalDate.parse("2025-12-25"), PostcardRarity.CHRISTMAS),
    Collectible(CollectionKind.POSTCARD, "post_cheomseongdae_gold", "경주 첨성대", R.drawable.myinfo_post_cheomseongdae,
        owned = false, rarity = PostcardRarity.GOLD),
    postcard("post_coast_gold", "등대 해안도로", R.drawable.myinfo_post_coast, "통과", launchDay, PostcardRarity.GOLD),
    postcard("post_coast", "등대 해안도로", R.drawable.myinfo_post_coast, "통과", LocalDate.parse("2026-09-28")),
    postcard("post_blossom", "벚꽃길", R.drawable.myinfo_post_blossom, "통과", LocalDate.parse("2026-04-05")),
)

private val demoBadges = listOf(
    badge("badge_safe", "부드러운 통과", R.drawable.myinfo_badge_safe, "급제동 없이 10번 지남", LocalDate.parse("2026-10-20")),
    badge("badge_regular", "단골 10번", R.drawable.myinfo_badge_regular, "같은 정체를 10번 만남", LocalDate.parse("2026-09-14")),
    badge("badge_holiday", "명절 귀성길", R.drawable.myinfo_badge_holiday, "추석에 경부고속도로를 지남", LocalDate.parse("2026-09-25")),
    // Figma는 잠긴 자리였지만 NEW 뱃지 시연을 위해 방금 받은 뱃지로 둔다. 잠긴 예는 엽서 뱃지가 보인다.
    badge("badge_night", "밤길 운전", R.drawable.myinfo_badge_night, "밤 10시 넘어 1시간 운전함", launchDay),
)

// 캐릭터와 자동차는 보유 목록에 얻은 시각이 없다. 시연용으로 기본 보유 중 하나씩만 방금 얻은 것으로 본다.
private val demoGotAt = mapOf("ch_cloud" to demoLaunchedAt - 3 * HOUR_MS, "car_blue" to demoLaunchedAt - 20 * HOUR_MS)

/** 다섯 묶음 전체. 캐릭터와 자동차는 Catalog와 보유 목록에서, 나머지는 견본에서 온다. */
fun collections(profile: Profile, config: BalanceConfig = BalanceConfig.DEFAULT): Map<CollectionKind, List<Collectible>> = mapOf(
    CollectionKind.CARD to demoCards,
    CollectionKind.POSTCARD to demoPostcards,
    CollectionKind.CHARACTER to fromCatalog(ItemKind.CHARACTER, CollectionKind.CHARACTER, profile),
    CollectionKind.CAR to fromCatalog(ItemKind.CAR, CollectionKind.CAR, profile),
    // 받은 뱃지를 앞에 두고 잠긴 뱃지를 뒤로 보낸다. 받은 것끼리는 Figma 순서를 지킨다.
    CollectionKind.BADGE to (demoBadges + postcardBadges(demoPostcards, config.postcardBadgeSteps)).sortedBy { !it.owned },
)

private val POSTCARD_BADGE_NAMES = listOf("첫 엽서 꾸러미", "엽서 수집가", "전국 일주")
private val POSTCARD_BADGE_ARTS = listOf(R.drawable.myinfo_badge_post_bundle, R.drawable.myinfo_badge_post_album, R.drawable.myinfo_badge_post_tour)

/**
 * 엽서를 모은 장소 수로 열리는 뱃지. 같은 장소의 다른 판(이름이 같은 엽서)은 한 곳으로 센다.
 * 장소마다 처음 얻은 날로 줄 세워 [steps]번째 장소를 얻은 날이 받은 날이다. 잠긴 뱃지는 [Collectible.note]에 남은 수를 적는다.
 */
internal fun postcardBadges(postcards: List<Collectible>, steps: List<Int>): List<Collectible> {
    val places = postcards.filter { it.owned }.groupBy { it.name }.values.map { same -> same.minOf { it.recency } }.sorted()
    return steps.mapIndexed { i, n ->
        val name = POSTCARD_BADGE_NAMES.getOrNull(i) ?: "엽서 ${n}곳"
        val opened = places.getOrNull(n - 1)
        val art = POSTCARD_BADGE_ARTS.getOrNull(i) ?: R.drawable.myinfo_badge_safe
        if (opened == null) {
            Collectible(CollectionKind.BADGE, "badge_postcard_$n", name, art, owned = false,
                note = "${n}곳의 엽서를 모으면 열려요. 지금 ${places.size}곳이에요.")
        } else {
            val on = LocalDate.ofEpochDay(opened)
            Collectible(CollectionKind.BADGE, "badge_postcard_$n", name, art, true, opened,
                note = "${n}곳의 엽서를 모음", date = dotted(on), gotAt = startMs(on))
        }
    }
}

// 캐릭터와 자동차에는 얻은 날짜가 없어서, 보유 목록에 들어간 순서(뒤가 최근)를 쓴다.
private fun fromCatalog(itemKind: ItemKind, kind: CollectionKind, profile: Profile): List<Collectible> {
    val order = profile.owned.toList()
    return Catalog.ofKind(itemKind).map {
        val owned = it.id in profile.owned
        Collectible(kind, it.id, it.name, artFor(it.id), owned, order.indexOf(it.id).toLong(), it.price,
            gotAt = if (owned) demoGotAt[it.id] else null)
    }
}

/** Catalog id에 맞는 그림. 그림이 없는 새 항목은 null이고 화면은 단색 원과 글자로 대신한다. */
@DrawableRes
fun artFor(id: String): Int? = when (id) {
    "ch_sprout" -> R.drawable.myinfo_ch_sprout
    "ch_cat" -> R.drawable.myinfo_ch_cat
    "ch_mush" -> R.drawable.myinfo_ch_mush
    "ch_cloud" -> R.drawable.myinfo_ch_cloud
    "ch_firefly" -> R.drawable.myinfo_ch_firefly
    "car_red" -> R.drawable.myinfo_car_red
    "car_blue" -> R.drawable.myinfo_car_blue
    "car_yellow" -> R.drawable.myinfo_car_yellow
    else -> null
}

private fun startMs(day: LocalDate) = day.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

private fun dotted(day: LocalDate) = day.toString().replace('-', '.')

private fun v(iso: String, min: Int, sec: Int = 0) = Visit(LocalDate.parse(iso), min * 60 + sec)

private fun card(id: String, name: String, art: Int, route: String, usualTime: String, visits: List<Visit>): Collectible {
    val first = visits.minOf { it.date }
    return Collectible(CollectionKind.CARD, id, name, art, true, first.toEpochDay(), meetCount = visits.size,
        route = route, usualTime = usualTime, visits = recentFirst(visits), gotAt = startMs(first))
}

private fun postcard(id: String, name: String, art: Int, how: String, on: LocalDate, rarity: PostcardRarity = PostcardRarity.NORMAL) =
    Collectible(CollectionKind.POSTCARD, id, name, art, true, on.toEpochDay(), note = "${dotted(on)} $how",
        rarity = rarity, gotAt = startMs(on))

private fun badge(id: String, name: String, art: Int, condition: String, on: LocalDate) =
    Collectible(CollectionKind.BADGE, id, name, art, true, on.toEpochDay(), note = condition, date = dotted(on), gotAt = startMs(on))

/**
 * 친구의 컬렉션(엽서와 뱃지 두 묶음). 도감 견본에서 친구가 가진 id만 골라 가진 것으로 보인다.
 * 친구 것이라 NEW 뱃지는 붙이지 않는다(얻은 시각을 지운다).
 */
fun friendCollections(postcardIds: Set<String>, badgeIds: Set<String>): Map<CollectionKind, List<Collectible>> {
    val mine = collections(Profile())
    fun pick(kind: CollectionKind, ids: Set<String>) = mine[kind].orEmpty().filter { it.id in ids }.map { it.copy(owned = true, gotAt = null) }
    return mapOf(CollectionKind.POSTCARD to pick(CollectionKind.POSTCARD, postcardIds), CollectionKind.BADGE to pick(CollectionKind.BADGE, badgeIds))
}
