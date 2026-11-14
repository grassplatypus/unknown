package com.break8t.unknown.ui.shop

import com.break8t.unknown.config.BalanceConfig
import com.break8t.unknown.config.Catalog
import com.break8t.unknown.data.DemoState
import com.break8t.unknown.data.Profile
import org.junit.Assert.assertEquals
import org.junit.Test

class ShopRulesTest {
    private val config = BalanceConfig(shopMaxQty = 3)
    private val rug = Catalog.item("it_rug")!! // 80
    private val firefly = Catalog.item("ch_firefly")!! // 200
    private val voice = Catalog.item("voice_sprout")!! // 200

    private fun state(coins: Int = 1000) = DemoState(Profile(coins = coins))

    @Test fun countedItemAddsQuantityAndSpendsTotal() {
        val s = state()
        val before = s.profile.value.items["it_rug"] ?: 0
        assertEquals(BuyResult.BOUGHT, buy(s, rug, 2, config))
        assertEquals(1000 - 160, s.profile.value.coins)
        assertEquals(before + 2, s.profile.value.items["it_rug"])
    }

    @Test fun quantityAboveCapIsRejectedAndChangesNothing() {
        val s = state()
        val before = s.profile.value
        assertEquals(BuyResult.BAD_QUANTITY, buy(s, rug, config.shopMaxQty + 1, config))
        assertEquals(BuyResult.BAD_QUANTITY, buy(s, rug, 0, config))
        assertEquals(before, s.profile.value)
    }

    @Test fun clampKeepsPickerInsideCap() {
        assertEquals(3, clampQty(9, config))
        assertEquals(1, clampQty(0, config))
    }

    @Test fun notEnoughCoinsChangesNothing() {
        val s = state(coins = 240)
        val before = s.profile.value
        assertEquals(BuyResult.NOT_ENOUGH_COINS, buy(s, rug, 4, BalanceConfig.DEFAULT)) // 320 > 240
        assertEquals(BuyResult.NOT_ENOUGH_COINS, buy(state(coins = 150), firefly, 1, config))
        assertEquals(before, s.profile.value)
    }

    @Test fun exactCoinsAreEnough() {
        val s = state(coins = 240)
        assertEquals(BuyResult.BOUGHT, buy(s, rug, 3, config))
        assertEquals(0, s.profile.value.coins)
    }

    @Test fun singleItemIsOwnedOnceAndIgnoresQuantity() {
        val s = state()
        assertEquals(BuyResult.BOUGHT, buy(s, firefly, 99, config))
        assertEquals(800, s.profile.value.coins)
        assertEquals(true, "ch_firefly" in s.profile.value.owned)
        assertEquals(BuyResult.ALREADY_OWNED, buy(s, firefly, 1, config))
        assertEquals(800, s.profile.value.coins)
    }

    @Test fun ownedOrUnpricedItemNeverSpends() {
        val s = state()
        assertEquals(BuyResult.NOT_FOR_SALE, buy(s, Catalog.item("ch_sprout")!!, 1, config))
        assertEquals(BuyResult.NOT_FOR_SALE, buy(s, Catalog.item("car_yellow")!!, 1, config))
        assertEquals(1000, s.profile.value.coins)
    }

    @Test fun switchingAfterPurchaseSetsTheRightField() {
        val s = state()
        buy(s, voice, 1, config)
        switchTo(s, voice)
        assertEquals("voice_sprout", s.profile.value.voicePackId)
        buy(s, firefly, 1, config)
        switchTo(s, firefly)
        assertEquals("ch_firefly", s.profile.value.selectedCharacterId)
    }

    @Test fun onlyVoicePacksAndCharactersAskToSwitch() {
        assertEquals(null, askAfterBuying(rug))
        assertEquals(null, askAfterBuying(Catalog.item("pose_jump")!!))
        assertEquals(voice.kind, askAfterBuying(voice))
        assertEquals(firefly.kind, askAfterBuying(firefly))
    }

    @Test fun tabsCountCatalogItems() {
        assertEquals(Catalog.items.count { it.kind == com.break8t.unknown.config.ItemKind.SEED }, shopItems(ShopTab.SEED).size)
        assertEquals(10, shopItems(ShopTab.CHARACTER).size) // 5 characters + 5 poses
    }

    @Test fun josaFollowsLastSyllable() {
        assertEquals("를", josa("새싹이 목소리", "을", "를"))
        assertEquals("을", josa("옆모습", "을", "를"))
        assertEquals("를", josa("", "을", "를"))
    }
}
