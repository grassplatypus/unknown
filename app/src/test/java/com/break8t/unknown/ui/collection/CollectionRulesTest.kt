package com.break8t.unknown.ui.collection

import com.break8t.unknown.R
import com.break8t.unknown.config.BalanceConfig
import com.break8t.unknown.data.Profile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CollectionRulesTest {
    private fun item(id: String, owned: Boolean, recency: Long) =
        Collectible(CollectionKind.CARD, id, id, null, owned, recency)

    @Test
    fun thumbnailIsNewestOwnedItem() {
        val items = listOf(item("old", true, 1), item("new", true, 5), item("mid", true, 3))
        assertEquals("new", newestOwned(items)?.id)
    }

    @Test
    fun thumbnailIgnoresLockedItemsEvenIfNewer() {
        val items = listOf(item("owned", true, 1), item("locked", false, 99))
        assertEquals("owned", newestOwned(items)?.id)
        assertNull(newestOwned(listOf(item("locked", false, 1))))
    }

    @Test
    fun characterThumbnailFollowsOwnedOrder() {
        // 캐릭터는 날짜가 없어서 보유 목록에 나중에 들어간 것이 최근이다.
        val bought = Profile(owned = setOf("ch_sprout", "car_red", "ch_firefly", "ch_cat"))
        assertEquals("ch_cat", newestOwned(collections(bought).getValue(CollectionKind.CHARACTER))?.id)
    }

    @Test
    fun demoThumbnailsShowTheRecentSample() {
        // 묶음마다 방금 얻은 견본(NEW 시연용)이 가장 최근 것이라 내 정보 썸네일이 된다.
        val all = collections(Profile())
        assertEquals("card_yangjae", newestOwned(all.getValue(CollectionKind.CARD))?.id)
        assertEquals("post_coast_gold", newestOwned(all.getValue(CollectionKind.POSTCARD))?.id)
        assertEquals("badge_night", newestOwned(all.getValue(CollectionKind.BADGE))?.id)
    }

    @Test
    fun postcardBadgeOpensOnTheNthPostcard() {
        fun post(id: String, owned: Boolean, day: Long) = Collectible(CollectionKind.POSTCARD, id, id, null, owned, day)
        val posts = listOf(post("c", true, 30), post("a", true, 10), post("x", false, 99), post("b", true, 20))
        val badges = postcardBadges(posts, listOf(2, 3, 4))
        assertEquals(listOf(true, true, false), badges.map { it.owned })
        // 2번째로 얻은 엽서(b, 20일)가 첫 뱃지를 연다. 잠긴 엽서는 세지 않는다.
        assertEquals(20L, badges[0].recency)
        assertEquals(30L, badges[1].recency)
        assertEquals("4곳의 엽서를 모으면 열려요. 지금 3곳이에요.", badges[2].note)
    }

    @Test
    fun otherRaritiesOfTheSamePlaceCountOnce() {
        fun post(id: String, place: String, day: Long, rarity: PostcardRarity) =
            Collectible(CollectionKind.POSTCARD, id, place, null, true, day, rarity = rarity)
        val posts = listOf(
            post("a", "첨성대", 50, PostcardRarity.NORMAL),
            post("a_xmas", "첨성대", 5, PostcardRarity.CHRISTMAS),
            post("a_gold", "첨성대", 60, PostcardRarity.GOLD),
            post("b", "해안도로", 20, PostcardRarity.NORMAL),
        )
        val badges = postcardBadges(posts, listOf(2, 3))
        // 엽서는 네 장이지만 장소는 두 곳이다. 첨성대는 처음 얻은 판(5일)으로 줄 선다.
        assertEquals(listOf(true, false), badges.map { it.owned })
        assertEquals(20L, badges[0].recency)
        assertEquals("3곳의 엽서를 모으면 열려요. 지금 2곳이에요.", badges[1].note)
    }

    @Test
    fun demoPostcardBadgesCountPlacesNotRarities() {
        val all = collections(Profile())
        val owned = all.getValue(CollectionKind.POSTCARD).filter { it.owned }
        assertEquals(5, owned.size)
        assertEquals(3, owned.map { it.name }.distinct().size)
        // 첨성대 노말과 크리스마스 판이 같은 장소라서 열린 엽서 뱃지는 3곳 하나뿐이다.
        val opened = all.getValue(CollectionKind.BADGE).filter { it.id.startsWith("badge_postcard_") && it.owned }
        assertEquals(listOf("badge_postcard_3"), opened.map { it.id })
        assertEquals("2026.09.28", opened.single().date)
    }

    @Test
    fun demoCardStatsComeFromVisits() {
        val cards = collections(Profile()).getValue(CollectionKind.CARD).filter { it.owned }
        for (card in cards) {
            assertEquals(card.id, card.visits.size, card.meetCount)
            assertEquals(card.id, card.visits.sortedByDescending { it.date }, card.visits)
            assertEquals(card.id, card.visits.last().date.toEpochDay(), card.recency)
        }
        // Figma D-24 견본: 성산대교 14번, 가장 빨리 6분 10초, 가장 느리게 4시간 10분.
        val seongsan = cards.first { it.id == "card_seongsan" }
        assertEquals(14, seongsan.meetCount)
        assertEquals(6 * 60 + 10, seongsan.visits.minOf { it.seconds })
        assertEquals(4 * 3600 + 10 * 60, seongsan.visits.maxOf { it.seconds })
    }

    @Test
    fun recentFirstSortsNewestOnTop() {
        val d = java.time.LocalDate.parse("2026-11-01")
        val visits = listOf(Visit(d, 1), Visit(d.plusDays(3), 2), Visit(d.plusDays(1), 3))
        assertEquals(listOf(2, 3, 1), recentFirst(visits).map { it.seconds })
    }

    @Test
    fun durationUsesTwoLargestUnits() {
        assertEquals(R.string.collection_dur_hm to listOf(4, 10), durationText(4 * 3600 + 10 * 60 + 5))
        assertEquals(R.string.collection_dur_h to listOf(1), durationText(3600))
        assertEquals(R.string.collection_dur_ms to listOf(6, 10), durationText(6 * 60 + 10))
        assertEquals(R.string.collection_dur_m to listOf(38), durationText(38 * 60))
        assertEquals(R.string.collection_dur_s to listOf(45), durationText(45))
    }

    @Test
    fun newOnlyWithinWindowAndUnseen() {
        val config = BalanceConfig(newBadgeHours = 36)
        val hour = 3_600_000L
        val now = 1_000 * hour
        fun got(hoursAgo: Long, owned: Boolean = true) =
            Collectible(CollectionKind.CARD, "x", "x", null, owned, gotAt = now - hoursAgo * hour)
        assertTrue(isNew(got(0), emptySet(), now, config))
        assertTrue(isNew(got(35), emptySet(), now, config))
        assertFalse(isNew(got(36), emptySet(), now, config))
        assertFalse(isNew(got(1), setOf("x"), now, config))
        assertFalse(isNew(got(1, owned = false), emptySet(), now, config))
        assertFalse(isNew(got(-1), emptySet(), now, config))
        assertFalse(isNew(Collectible(CollectionKind.CAR, "c", "c", null, true), emptySet(), now, config))
    }

    @Test
    fun demoHasOneNewItemPerKindAtLaunch() {
        val all = collections(Profile())
        for (kind in CollectionKind.entries) {
            val fresh = all.getValue(kind).filter { isNew(it, emptySet(), demoLaunchedAt, BalanceConfig.DEFAULT) }
            assertEquals(kind.name, 1, fresh.size)
        }
        val seen = all.values.flatten().map { it.id }.toSet()
        assertTrue(all.values.flatten().none { isNew(it, seen, demoLaunchedAt, BalanceConfig.DEFAULT) })
    }

    @Test
    fun emptyOnlyAfterLoaded() {
        assertEquals(ListPhase.LOADING, listPhase(loaded = false, count = 0))
        assertEquals(ListPhase.LOADING, listPhase(loaded = false, count = 3))
        assertEquals(ListPhase.EMPTY, listPhase(loaded = true, count = 0))
        assertEquals(ListPhase.CONTENT, listPhase(loaded = true, count = 3))
    }

    @Test
    fun particlesFollowFinalConsonant() {
        assertEquals("냥이로", Josa.euroRo("냥이"))
        assertEquals("반딧불로", Josa.euroRo("반딧불"))
        assertEquals("새싹으로", Josa.euroRo("새싹"))
        assertEquals("차7로", Josa.euroRo("차7"))
        assertEquals("차3으로", Josa.euroRo("차3"))
        assertEquals("별빛운전과", Josa.waGwa("별빛운전"))
        assertEquals("도토리길을", Josa.eulReul("도토리길"))
        assertEquals("새벽출근을", Josa.eulReul("새벽출근"))
        assertEquals("퇴근요정을", Josa.eulReul("퇴근요정"))
        assertEquals("느린거북을", Josa.eulReul("느린거북"))
        assertEquals("버섯이를", Josa.eulReul("버섯이"))
        assertEquals("주말캠퍼와", Josa.waGwa("주말캠퍼"))
    }

    @Test
    fun visitBadgeShowsReachedStepOnly() {
        assertEquals(listOf(null, null, 5, 5, 10, 10, 15, 20, 20), listOf(0, 4, 5, 9, 10, 14, 19, 20, 99).map { visitBadge(it) })
    }
}
