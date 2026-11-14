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
    private val car = GeoPoint(37.5180, 126.8760)
    private val far = Mushroom("far", MushroomKind.GOLD, "항아리 보쌈", true, GeoPoint(37.5288, 126.8767), 30, "seed_carrot")
    private val other = Mushroom("other", MushroomKind.BLUE, "주차장", false, GeoPoint(37.5161, 126.8672), 10, null)
    private val near = Mushroom("near", MushroomKind.GOLD, "카페", true, GeoPoint(37.51815, 126.8760), 30, "seed_carrot")
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

    @Test fun haversineMatchesKnownDistances() {
        assertEquals(0.0, distanceM(car, car), 1e-9)
        // 1/1000 degree of latitude is about 111.2 m anywhere.
        assertEquals(111.2, distanceM(GeoPoint(37.0, 127.0), GeoPoint(37.001, 127.0)), 0.1)
        // Seoul City Hall to Gangnam station, about 8.8 km.
        assertEquals(8_800.0, distanceM(GeoPoint(37.5663, 126.9779), GeoPoint(37.4979, 127.0276)), 100.0)
        assertEquals(distanceM(car, far.at), distanceM(far.at, car), 1e-9)
    }

    @Test fun demoSamplesKeepFigmaDistances() {
        val labels = FieldDropSamples.mushrooms.associate { it.id to distanceText(distanceRoundedM(FieldDropSamples.CAR.at, it)) }
        assertEquals(
            mapOf("park_sisa" to "1.2km", "shop_bossam" to "1.2km", "park_sinjeong" to "800m", "shop_bunsik" to "1.6km", "shop_cafe" to "8m"),
            labels,
        )
        assertEquals(4, minutesFor(1200))
        assertEquals(3, minutesFor(800))
        assertEquals(5, minutesFor(1600))
    }

    @Test fun altitudeAndZoomRoundTrip() {
        assertEquals(78_271.517, metersPerDp(0.0, 0.0), 1e-6) // 512 dp tiles
        // One zoom step halves the distance per dp.
        assertEquals(metersPerDp(car.lat, 18.0) / 2, metersPerDp(car.lat, 19.0), 1e-9)
        val zoom = zoomForAltitude(car.lat, 50.0, 1210.0, maxZoom = 25.5)
        assertEquals(50.0, altitudeM(car.lat, zoom, 1210.0), 1e-6)
        // tan(18.43 deg) = 1/3, so 50 m over a 1210 dp tall map is 0.02755 m per dp.
        assertEquals(50.0 / 3 / 605, metersPerDp(car.lat, zoom), 1e-6)
        assertEquals(21.10, zoom, 0.01)
        // A taller viewport sees more ground from the same height, so it needs a higher zoom.
        assertTrue(zoomForAltitude(car.lat, 50.0, 2420.0, 25.5) > zoom)
    }

    @Test fun altitudeZoomStopsAtMaxZoom() {
        assertEquals(20.0, zoomForAltitude(car.lat, 50.0, 1210.0, maxZoom = 20.0), 0.0)
        assertEquals(22.0, zoomForAltitude(car.lat, 5.0, 1210.0, maxZoom = 22.0), 0.0)
    }

    @Test fun scaleBarPicksLongestStepThatFits() {
        assertEquals(100 to 100.0, scaleBar(1.0, 160.0)) // default view
        assertEquals(200 to 100.0, scaleBar(2.0, 160.0)) // zoomed out one step
        assertEquals(50 to 100.0, scaleBar(0.5, 160.0))
        assertEquals(5 to 5 / 0.02755, scaleBar(0.02755, 200.0)) // 50 m altitude
        assertEquals(1 to 1000.0, scaleBar(0.001, 160.0)) // nothing fits, smallest step
        assertEquals("100m", scaleLabel(100))
        assertEquals("2km", scaleLabel(2000))
    }

    @Test fun reachableIsWithinRadius() {
        assertTrue(reachable(car, near, config)) // about 17 m
        assertFalse(reachable(car, far, config))
        val edge = near.copy(at = GeoPoint(car.lat + 21.0 / 111_195.0, car.lng))
        assertFalse(reachable(car, edge, config)) // 21 m against a 20 m radius
        val demo = FieldDropSamples.mushrooms.filter { reachable(FieldDropSamples.CAR.at, it, BalanceConfig.DEFAULT) }
        assertEquals(listOf("shop_cafe"), demo.map { it.id })
    }

    @Test fun markerTapRules() {
        val open = FieldDropState(todayCount = 0)
        assertEquals(MarkerTap.REWARD, markerTap(near, car, open, config))
        assertEquals(MarkerTap.CARD, markerTap(far, car, open, config))
        val full = FieldDropState(todayCount = 4)
        assertEquals(MarkerTap.NONE, markerTap(near, car, full, config))
        assertEquals(MarkerTap.NONE, markerTap(far, car, full, config))
        // The same mushroom becomes a card once the car drives away.
        assertEquals(MarkerTap.CARD, markerTap(near, far.at, open, config))
    }

    @Test fun mapLayersGetPalettePaint() {
        assertEquals(MapPaint.LAND, mapPaint("background", false))
        assertEquals(MapPaint.WATER, mapPaint("water", false))
        assertEquals(MapPaint.WATER, mapPaint("waterway_river", false))
        assertEquals(MapPaint.PARK, mapPaint("park", false))
        assertEquals(MapPaint.PARK, mapPaint("landcover_grass", false))
        assertEquals(MapPaint.ROAD, mapPaint("road_minor", false))
        assertEquals(MapPaint.MAJOR_ROAD, mapPaint("road_trunk_primary", false))
        assertEquals(MapPaint.MAJOR_ROAD, mapPaint("bridge_motorway", false))
        assertEquals(MapPaint.LAND, mapPaint("road_trunk_primary_casing", false))
        assertEquals(MapPaint.RAIL, mapPaint("road_major_rail", false))
        assertEquals(MapPaint.BUILDING, mapPaint("building", false))
        assertEquals(MapPaint.LABEL, mapPaint("label_city", true))
        assertEquals(MapPaint.LABEL, mapPaint("highway-name-major", true))
        // 3D buildings, relief, patterns and sprite icons are dropped.
        listOf("building-3d", "natural_earth", "landcover_wetland", "road_area_pattern", "poi_r1", "highway-shield-non-us", "road_one_way_arrow", "boundary_2")
            .forEach { assertEquals(it, MapPaint.HIDE, mapPaint(it, it.startsWith("poi") || it.contains("shield") || it.contains("arrow"))) }
    }

    @Test fun distanceLabel() {
        assertEquals("15m", distanceText(15))
        assertEquals("1.2km", distanceText(1200))
    }

    @Test fun farMushroomOpensCardNearOneOpensReward() {
        val m = model()
        m.tap(far, car, config)
        assertEquals("far", m.state.value.selectedId)
        m.tap(near, car, config)
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
        m.tap(near, car, config)
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
        m.tap(near, car, config)
        assertTrue(m.receive(config, demo))
        assertTrue(m.state.value.atLimit(config))

        m.tap(near, car, config)
        m.tap(far, car, config)
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

    @Test fun markerPropertiesFollowState() {
        val car = CarFix(car, 250f)
        val plain = mapMarkers(car, "car_blue", listOf(far, other), FieldDropState(todayCount = 0), config)
        assertEquals("fd_car_blue", plain.carIcon)
        assertEquals(25f, plain.carRotate, 0f)
        assertEquals(listOf("fd_mush_gold_ad", "fd_mush_blue"), plain.mushrooms.map { it.icon })
        assertEquals(listOf(far.at, other.at), plain.mushrooms.map { it.at })
        assertFalse(plain.mushrooms.any { it.dim })

        val picked = mapMarkers(car, "car_red", listOf(far, other), FieldDropState(todayCount = 0, selectedId = "other"), config)
        assertEquals(listOf("fd_mush_gold_ad", "fd_mush_blue_sel"), picked.mushrooms.map { it.icon }) // S-10-b
        assertTrue(mapMarkers(car, "car_red", listOf(far), FieldDropState(todayCount = 4), config).mushrooms.all { it.dim })
        assertEquals("fd_car_red", mapMarkers(car, "unknown", emptyList(), FieldDropState(), config).carIcon)
    }

    @Test fun everyIconNameIsDistinct() {
        val names = MushroomKind.entries.flatMap { k -> listOf(false, true).flatMap { ad -> listOf(false, true).map { mushroomIcon(k, ad, it) } } }
        assertEquals(8, names.toSet().size)
    }

    @Test fun tapResolvesFirstMushroomHit() {
        val list = listOf(far, other, near)
        assertEquals(other, mushroomAt(listOf(null, "other", "far"), list))
        assertEquals(far, mushroomAt(listOf("gone", "far"), list))
        assertNull(mushroomAt(emptyList(), list)) // empty map closes the card
        assertNull(mushroomAt(listOf("gone", null), list))
    }

    @Test fun rotatedCarCanvasFitsAndKeepsCentre() {
        assertEquals(114, rotatedSidePx(114, 0f))
        assertEquals(114, rotatedSidePx(114, 90f))
        assertEquals(114, rotatedSidePx(114, 180f))
        assertEquals(162, rotatedSidePx(114, 45f)) // 114 x 1.4142 = 161.2, rounded up to the same parity
        assertEquals(152, rotatedSidePx(114, carRotation(250f))) // S-10-a, 25 degrees: 151.5
        assertEquals(161, rotatedSidePx(113, 45f)) // odd side stays odd
        for (deg in 0 until 360 step 5) {
            val side = rotatedSidePx(114, deg.toFloat())
            assertEquals(0, (side - 114) % 2)
            assertTrue(side >= 114 && side <= 162)
        }
    }
}
