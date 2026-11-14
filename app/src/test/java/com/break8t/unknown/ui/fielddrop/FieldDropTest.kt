package com.break8t.unknown.ui.fielddrop

import com.break8t.unknown.config.BalanceConfig
import com.break8t.unknown.data.DemoState
import com.break8t.unknown.data.Profile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FieldDropTest {
    private val config = BalanceConfig(mushroomDailyLimit = 4, mushroomRadiusM = 20)
    private val far = Mushroom("far", MushroomKind.GOLD, "항아리 보쌈", true, 1200, 4, 0.5f, 0.5f, 30, "seed_carrot")
    private val other = Mushroom("other", MushroomKind.BLUE, "주차장", false, 800, 3, 0.3f, 0.3f, 10, null)
    private val near = Mushroom("near", MushroomKind.GOLD, "카페", true, 20, 0, 0.4f, 0.6f, 30, "seed_carrot")
    private val sink = FakeWaypointSink()

    private fun model(count: Int = 0) = FieldDrop(listOf(far, other, near), sink, FieldDropState(todayCount = count))

    @Test fun rotationFacesHeading() {
        assertEquals(0f, carRotation(225f), 0f)
        assertEquals(25f, carRotation(250f), 0f) // S-10-a
        assertEquals(135f, carRotation(0f), 0f) // north
        assertEquals(315f, carRotation(180f), 0f) // wraps below zero
        assertEquals(134f, carRotation(359f), 0.001f)
        assertEquals(carRotation(10f), carRotation(370f), 0.001f)
    }

    @Test fun panClampsToMapEdges() {
        assertEquals(0f, clampPan(50f, 2000f, 1000f), 0f)
        assertEquals(-1000f, clampPan(-1500f, 2000f, 1000f), 0f)
        assertEquals(-300f, clampPan(-300f, 2000f, 1000f), 0f)
        assertEquals(100f, clampPan(-999f, 800f, 1000f), 0f) // smaller map is centred
    }

    @Test fun distanceLabel() {
        assertEquals("15m", distanceText(15))
        assertEquals("1.2km", distanceText(1200))
    }

    @Test fun farMushroomOpensCardNearOneOpensReward() {
        val m = model()
        m.tap(far, config)
        assertEquals("far", m.state.value.selectedId)
        m.tap(near, config)
        assertEquals("near", m.state.value.openId)
        assertNull(m.state.value.selectedId)
    }

    @Test fun guideTogglesAndReplaces() {
        val m = model()
        assertTrue(m.toggleGuide(far))
        assertEquals("far", m.state.value.guidingId)
        assertEquals(listOf("far"), sink.waypoints)

        assertTrue(m.toggleGuide(other))
        assertEquals("other", m.state.value.guidingId)
        assertEquals(listOf("other"), sink.waypoints)

        assertFalse(m.toggleGuide(other))
        assertNull(m.state.value.guidingId)
        assertTrue(sink.waypoints.isEmpty())
    }

    @Test fun receiveGrantsOncePerOpen() {
        val demo = DemoState(Profile(coins = 100, items = mapOf("seed_carrot" to 0)))
        val m = model(count = 1)
        m.tap(near, config)
        assertTrue(m.receive(config, demo))
        assertFalse(m.receive(config, demo)) // second tap on 받기 does nothing
        assertEquals(130, demo.profile.value.coins)
        assertEquals(1, demo.profile.value.items["seed_carrot"])
        assertEquals(2, m.state.value.todayCount)
        assertNull(m.state.value.openId)
    }

    @Test fun limitStopsTapsAndRewards() {
        val demo = DemoState(Profile(coins = 100))
        val m = model(count = 3)
        m.tap(near, config)
        assertTrue(m.receive(config, demo))
        assertTrue(m.state.value.atLimit(config))

        m.tap(near, config)
        m.tap(far, config)
        assertNull(m.state.value.openId)
        assertNull(m.state.value.selectedId)
        assertEquals(130, demo.profile.value.coins)
    }

    @Test fun rewardOpenedBeforeLimitIsNotPaidPastIt() {
        val demo = DemoState(Profile(coins = 100))
        val m = FieldDrop(listOf(near), sink, FieldDropState(todayCount = 4, openId = "near"))
        assertFalse(m.receive(config, demo))
        assertEquals(100, demo.profile.value.coins)
        assertEquals(4, m.state.value.todayCount)
    }
}
