package com.break8t.unknown.ui.home

import com.break8t.unknown.data.DemoState
import com.break8t.unknown.data.Profile
import com.break8t.unknown.drive.DriveMode
import com.break8t.unknown.reward.RewardResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ParkedRewardsTest {
    private val figma = RewardResult(driveCoin = 28, raidCoin = 16, safeCoin = 10, xp = 77)

    @Test
    fun noLevelUpSkipsLevelDialog() {
        assertEquals(
            listOf(HomePopup.REWARD, HomePopup.POSTCARD, HomePopup.JAM_CARD),
            popupsFor(levelBefore = 7, levelAfter = 7),
        )
    }

    @Test
    fun levelUpComesRightAfterReward() {
        assertEquals(
            listOf(HomePopup.REWARD, HomePopup.LEVEL_UP, HomePopup.POSTCARD, HomePopup.JAM_CARD),
            popupsFor(levelBefore = 7, levelAfter = 8),
        )
    }

    @Test
    fun rewardAddsCoinsAndXp() {
        val p = applyReward(Profile(coins = 240, level = 7, xp = 465, xpToNext = 600), figma)
        assertEquals(294, p.coins)
        assertEquals(7, p.level)
        assertEquals(542, p.xp)
    }

    @Test
    fun xpOverflowCarriesIntoNextLevel() {
        val p = applyReward(Profile(level = 7, xp = 542, xpToNext = 600), figma)
        assertEquals(8, p.level)
        assertEquals(19, p.xp)
    }

    @Test
    fun rewardEqualToRemainderLevelsUpWithZeroXp() {
        val p = applyReward(Profile(level = 7, xp = 523, xpToNext = 600), figma)
        assertEquals(8, p.level)
        assertEquals(0, p.xp)
    }

    @Test
    fun onlyParkingAfterDrivingGrants() = runTest {
        val demo = DemoState(Profile(coins = 240, level = 7, xp = 465, xpToNext = 600))
        val home = HomeController(backgroundScope)

        // 앱을 켜거나 화면을 나누면 UNKNOWN에서 PARKED로 간다. 이때는 주지 않는다.
        home.onMode(DriveMode.UNKNOWN, demo)
        home.onMode(DriveMode.PARKED, demo)
        assertEquals(emptyList<HomePopup>(), home.popups.value)
        assertEquals(240, demo.profile.value.coins)

        home.onMode(DriveMode.DRIVING, demo)
        home.onMode(DriveMode.PARKED, demo)
        assertEquals(listOf(HomePopup.REWARD, HomePopup.POSTCARD, HomePopup.JAM_CARD), home.popups.value)
        assertEquals(294, demo.profile.value.coins)

        // 같은 P를 다시 받아도(Activity 재생성) 두 번 주지 않는다.
        home.onMode(DriveMode.UNKNOWN, demo)
        home.onMode(DriveMode.PARKED, demo)
        assertEquals(294, demo.profile.value.coins)
        assertEquals(emptyList<HomePopup>(), home.popups.value)
    }

    @Test
    fun secondDriveLevelsUpAndShowsLevelDialog() = runTest {
        val demo = DemoState(Profile(level = 7, xp = 465, xpToNext = 600))
        val home = HomeController(backgroundScope)
        repeat(2) {
            home.onMode(DriveMode.DRIVING, demo)
            home.onMode(DriveMode.PARKED, demo)
        }
        assertEquals(
            listOf(HomePopup.REWARD, HomePopup.LEVEL_UP, HomePopup.POSTCARD, HomePopup.JAM_CARD),
            home.popups.value,
        )
        assertEquals(8, demo.profile.value.level)
    }

    @Test
    fun leavingParkDropsQueueAndDismissAdvances() = runTest {
        val demo = DemoState()
        val home = HomeController(backgroundScope)
        home.onMode(DriveMode.DRIVING, demo)
        home.onMode(DriveMode.PARKED, demo)
        home.dismiss()
        assertEquals(HomePopup.POSTCARD, home.popups.value.first())
        home.onMode(DriveMode.UNKNOWN, demo)
        assertEquals(emptyList<HomePopup>(), home.popups.value)
    }
}
