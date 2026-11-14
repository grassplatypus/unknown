package com.break8t.unknown.ui.collection

import androidx.annotation.DrawableRes
import com.break8t.unknown.R
import com.break8t.unknown.config.BalanceConfig
import com.break8t.unknown.config.Catalog
import com.break8t.unknown.config.ItemKind
import com.break8t.unknown.data.Profile
import java.time.LocalDate

/** 내 정보(S-21)와 도감(S-24)이 함께 보이는 다섯 묶음. 탭 순서와 같다. */

/** 엽서 그림의 비율. 그림에 직각 액자가 그려져 있어서 이 비율 그대로, 모서리를 깎지 않고 보인다. */
const val POSTCARD_ASPECT = 960f / 645f

enum class CollectionKind { CARD, POSTCARD, CHARACTER, CAR, BADGE }

/**
 * 모은 것 하나. [recency]는 같은 묶음 안에서만 비교하며 클수록 최근에 얻은 것이다.
 * 정체 카드, 엽서, 뱃지의 나머지 필드는 견본 데이터에만 있다.
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
    val fastest: String? = null,
    val slowest: String? = null,
    val usualTime: String? = null,
    val note: String? = null,
    val date: String? = null,
)

// Figma S-24 견본. D-24 자세히 값은 성산대교만 Figma에 있고 나머지는 같은 꼴로 지어 넣었다.
// 날짜는 최근 순서를 정하는 데만 쓴다(화면에 안 보임). 한 번 만난 카드는 가장 빠른 기록과 느린 기록이 같다.
private val demoCards = listOf(
    card("card_seongsan", "성산대교 구간", R.drawable.myinfo_card_seongsan, 14, "2026-10-21",
        route = "서부간선도로 · 성산대교 → 목동교", fastest = "6분 10초", slowest = "4시간 10분", usualTime = "평일 오전 8시쯤"),
    card("card_hannam", "한남대교 북단", R.drawable.myinfo_card_hannam, 9, "2026-10-08",
        route = "강변북로 · 한남대교 북단 → 반포대교 북단", fastest = "4분 30초", slowest = "38분", usualTime = "금요일 저녁 7시쯤"),
    card("card_singal", "신갈 분기점", R.drawable.myinfo_card_ic, 6, "2026-09-25",
        route = "경부고속도로 · 신갈JC → 수원신갈IC", fastest = "7분 50초", slowest = "1시간 25분", usualTime = "주말 오후 5시쯤"),
    card("card_tunnel", "남산1호터널", R.drawable.myinfo_card_tunnel, 4, "2026-09-02",
        route = "남산1호터널 · 한남오거리 → 회현동", fastest = "3분 20초", slowest = "22분", usualTime = "평일 오전 9시쯤"),
    card("card_hangang", "한강대교 구간", R.drawable.myinfo_card_hangang, 3, "2026-08-17",
        route = "한강대로 · 노들역 → 한강대교 북단", fastest = "5분 40초", slowest = "41분", usualTime = "평일 저녁 6시쯤"),
    card("card_riverside", "올림픽대로 잠실", R.drawable.myinfo_card_riverside, 2, "2026-07-30",
        route = "올림픽대로 · 잠실대교 → 청담대교", fastest = "8분 10초", slowest = "52분", usualTime = "평일 저녁 7시쯤"),
    card("card_pangyo", "판교 나들목", R.drawable.myinfo_card_ic, 1, "2026-07-11",
        route = "경부고속도로 · 판교IC → 서초IC", fastest = "12분 30초", slowest = "12분 30초", usualTime = "주말 오전 11시쯤"),
    Collectible(CollectionKind.CARD, "card_gayang", "강변북로 가양", R.drawable.myinfo_card_unknown, owned = false),
)

private val demoPostcards = listOf(
    postcard("post_cheomseongdae", "경주 첨성대", R.drawable.myinfo_post_cheomseongdae, "2026.10.12 방문", "2026-10-12"),
    postcard("post_coast", "등대 해안도로", R.drawable.myinfo_post_coast, "2026.09.28 통과", "2026-09-28"),
    postcard("post_blossom", "벚꽃길", R.drawable.myinfo_post_blossom, "2026.04.05 통과", "2026-04-05"),
)

private val demoBadges = listOf(
    badge("badge_safe", "부드러운 통과", R.drawable.myinfo_badge_safe, "급제동 없이 10번 지남", "2026-10-20"),
    badge("badge_regular", "단골 10번", R.drawable.myinfo_badge_regular, "같은 정체를 10번 만남", "2026-09-14"),
    badge("badge_holiday", "명절 귀성길", R.drawable.myinfo_badge_holiday, "추석에 경부고속도로를 지남", "2026-09-25"),
    // Figma는 이 자리에 badge_safe를 흐리게 썼다. 명절 귀성길 뱃지를 참고 그림으로 같은 방식으로 만든 밤길 그림을 쓴다.
    Collectible(CollectionKind.BADGE, "badge_night", "밤길 운전", R.drawable.myinfo_badge_night, owned = false),
)

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

/**
 * 모은 엽서 수로 열리는 뱃지. [steps]번째 엽서를 얻은 날이 받은 날이고, 그 엽서 그림을 뱃지 그림으로 쓴다.
 * 아직 못 연 뱃지는 다른 잠긴 뱃지처럼 badge_safe를 흐리게 보이고, [Collectible.note]에 남은 수를 적는다.
 */
internal fun postcardBadges(postcards: List<Collectible>, steps: List<Int>): List<Collectible> {
    val got = postcards.filter { it.owned }.sortedBy { it.recency }
    return steps.mapIndexed { i, n ->
        val name = POSTCARD_BADGE_NAMES.getOrNull(i) ?: "엽서 ${n}장"
        val opener = got.getOrNull(n - 1)
        if (opener == null) {
            Collectible(CollectionKind.BADGE, "badge_postcard_$n", name, R.drawable.myinfo_badge_safe, owned = false,
                note = "엽서를 ${n}장 모으면 열려요. 지금 ${got.size}장이에요.")
        } else {
            val on = LocalDate.ofEpochDay(opener.recency).toString()
            Collectible(CollectionKind.BADGE, "badge_postcard_$n", name, opener.art, true, opener.recency,
                note = "엽서 ${n}장을 모음", date = on.replace('-', '.'))
        }
    }
}

// 캐릭터와 자동차에는 얻은 날짜가 없어서, 보유 목록에 들어간 순서(뒤가 최근)를 쓴다.
private fun fromCatalog(itemKind: ItemKind, kind: CollectionKind, profile: Profile): List<Collectible> {
    val order = profile.owned.toList()
    return Catalog.ofKind(itemKind).map {
        Collectible(kind, it.id, it.name, artFor(it.id), it.id in profile.owned, order.indexOf(it.id).toLong(), it.price)
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

private fun day(iso: String) = LocalDate.parse(iso).toEpochDay()

private fun card(
    id: String, name: String, art: Int, meets: Int, firstMet: String,
    route: String? = null, fastest: String? = null, slowest: String? = null, usualTime: String? = null,
) = Collectible(CollectionKind.CARD, id, name, art, true, day(firstMet), null, meets, route, fastest, slowest, usualTime)

private fun postcard(id: String, name: String, art: Int, note: String, on: String) =
    Collectible(CollectionKind.POSTCARD, id, name, art, true, day(on), note = note)

private fun badge(id: String, name: String, art: Int, condition: String, on: String) =
    Collectible(CollectionKind.BADGE, id, name, art, true, day(on), note = condition, date = on.replace('-', '.'))
