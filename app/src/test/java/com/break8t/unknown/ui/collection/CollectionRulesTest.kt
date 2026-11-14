package com.break8t.unknown.ui.collection

import com.break8t.unknown.data.Profile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
    fun demoPostcardAndBadgeThumbnailsMatchFigma() {
        val all = collections(Profile())
        assertEquals("post_cheomseongdae", newestOwned(all.getValue(CollectionKind.POSTCARD))?.id)
        assertEquals("badge_safe", newestOwned(all.getValue(CollectionKind.BADGE))?.id)
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
        assertEquals("엽서를 4장 모으면 열려요. 지금 3장이에요.", badges[2].note)
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
}
