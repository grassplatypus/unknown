package com.break8t.unknown.ui.fielddrop

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.delay
import kotlin.math.cos
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style

private const val STYLE_TIMEOUT_MS = 10_000L

enum class MapStatus { LOADING, READY, OFFLINE }

/** 지도와 그 위에 놓을 표시 사이의 연결. 카메라가 움직일 때마다 [moves]가 바뀌어 표시가 다시 놓인다. */
@Stable
class MapHandle {
    var map by mutableStateOf<MapLibreMap?>(null)
    var status by mutableStateOf(MapStatus.LOADING)
    var moves by mutableIntStateOf(0)

    /** 화면 위 위치(px). 지도가 아직 없으면 null. */
    fun project(p: GeoPoint): Offset? {
        moves // 카메라가 움직이면 다시 읽도록 구독한다.
        val point = map?.projection?.toScreenLocation(LatLng(p.lat, p.lng)) ?: return null
        return Offset(point.x, point.y)
    }

    /** 지금 화면 가운데의 1dp가 덮는 거리(m). 투영에서 직접 재서 줌을 바꿔도 맞는다. */
    fun metersPerDp(density: Float): Double? {
        moves
        val m = map ?: return null
        val at = m.cameraPosition.target ?: return null
        val east = GeoPoint(at.latitude, at.longitude + PROBE_M / (METERS_PER_DEG_LAT * cos(Math.toRadians(at.latitude))))
        val a = project(GeoPoint(at.latitude, at.longitude)) ?: return null
        val b = project(east) ?: return null
        return PROBE_M / ((b.x - a.x) / density)
    }

    internal var onRecenter: () -> Unit = {}

    /** 내 차를 가운데에 두고 기본 높이로 돌아간다. 화면 높이(dp)에 따라 줌이 달라진다. */
    fun recenter(p: GeoPoint, viewportHeightDp: Double) {
        val m = map ?: return
        onRecenter()
        val zoom = defaultZoom(p, viewportHeightDp, m.maxZoomLevel)
        m.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(p.lat, p.lng), zoom))
    }
}

private const val PROBE_M = 1_000.0
private const val METERS_PER_DEG_LAT = 111_320.0

// MapLibre 기본 최대 줌. 지도가 뜨기 전 첫 카메라에만 쓰고, 뜬 뒤에는 maxZoomLevel을 읽는다.
private const val MAP_MAX_ZOOM = 25.5

fun defaultZoom(at: GeoPoint, viewportHeightDp: Double, maxZoom: Double = MAP_MAX_ZOOM): Double =
    zoomForAltitude(at.lat, FIELD_DROP_ALTITUDE_M, viewportHeightDp, maxZoom)

/** MapLibre 지도. 화면을 나누면 Activity가 다시 만들어진다. 사용자가 옮긴 카메라는 이어 가고, 아니면 새 창 높이로 기본 높이를 다시 잡는다. */
@Composable
fun LiveMap(handle: MapHandle, start: GeoPoint, viewportHeightDp: Double, onMapTap: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val tap by rememberUpdatedState(onMapTap)
    var lat by rememberSaveable { mutableDoubleStateOf(start.lat) }
    var lng by rememberSaveable { mutableDoubleStateOf(start.lng) }
    var zoom by rememberSaveable { mutableDoubleStateOf(defaultZoom(start, viewportHeightDp)) }
    var userMoved by rememberSaveable { mutableStateOf(false) }
    handle.onRecenter = { userMoved = false }
    val mapView = remember(context) {
        val camera = if (userMoved) {
            CameraPosition.Builder().target(LatLng(lat, lng)).zoom(zoom).build()
        } else {
            CameraPosition.Builder().target(LatLng(start.lat, start.lng)).zoom(defaultZoom(start, viewportHeightDp)).build()
        }
        createMapView(context, handle, camera, { tap() }, { userMoved = true }) {
            lat = it.target?.latitude ?: lat
            lng = it.target?.longitude ?: lng
            zoom = it.zoom
        }
    }
    MapViewLifecycle(mapView)
    // 창 크기만 바뀌고 Activity가 남는 경우에도 기본 시점이면 높이를 다시 맞춘다.
    LaunchedEffect(handle.map, viewportHeightDp) {
        val map = handle.map ?: return@LaunchedEffect
        if (!userMoved) {
            val z = defaultZoom(start, viewportHeightDp, map.maxZoomLevel)
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(start.lat, start.lng), z))
        }
    }
    // 실패 알림이 오지 않는 경우(응답 없는 연결)도 있어서 시간이 지나면 땅 색만 있는 스타일로 바꾼다.
    LaunchedEffect(mapView) {
        delay(STYLE_TIMEOUT_MS)
        if (handle.status == MapStatus.LOADING) handle.map?.let { goOffline(it, handle) }
    }
    AndroidView({ mapView }, modifier)
}

private fun createMapView(
    context: Context,
    handle: MapHandle,
    camera: CameraPosition,
    onMapTap: () -> Unit,
    onGesture: () -> Unit,
    onIdle: (CameraPosition) -> Unit,
): MapView {
    MapLibre.getInstance(context.applicationContext)
    handle.status = MapStatus.LOADING
    return MapView(context).apply {
        onCreate(null)
        addOnDidFailLoadingMapListener { handle.map?.let { goOffline(it, handle) } }
        getMapAsync { map ->
            map.uiSettings.apply {
                // 북쪽을 위로 고정해야 자동차 그림의 회전이 진행 방향과 맞는다.
                isRotateGesturesEnabled = false
                isTiltGesturesEnabled = false
                isCompassEnabled = false
                isLogoEnabled = false
                // 기본 출처 버튼은 카드 밑에 가리고 창 대화상자를 띄워서, 화면에 글자로 따로 둔다.
                isAttributionEnabled = false
            }
            map.cameraPosition = camera
            map.addOnCameraMoveListener { handle.moves++ }
            map.addOnCameraMoveStartedListener { if (it == MapLibreMap.OnCameraMoveStartedListener.REASON_API_GESTURE) onGesture() }
            map.addOnCameraIdleListener { handle.moves++; onIdle(map.cameraPosition) }
            map.addOnMapClickListener { onMapTap(); false }
            handle.map = map
            map.setStyle(Style.Builder().fromUri(MAP_STYLE_URL)) { style ->
                recolor(style)
                handle.status = MapStatus.READY
            }
        }
    }
}

/** 다시 칠한 지도가 이미 떠 있으면 바꾸지 않는다. 나중의 실패로 멀쩡한 지도를 지우지 않으려는 것이다. */
private fun goOffline(map: MapLibreMap, handle: MapHandle) {
    if (handle.status != MapStatus.LOADING) return
    handle.status = MapStatus.OFFLINE
    map.setStyle(Style.Builder().fromJson(OFFLINE_STYLE_JSON))
}

/** 화면을 떠날 때(메뉴 이동, Activity 재생성) 한 번만 내려 보낸다. 참고 코드는 여기서 MapView가 샜다. */
@Composable
private fun MapViewLifecycle(mapView: MapView) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, mapView) {
        var started = false
        var resumed = false
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> if (!started) { mapView.onStart(); started = true }
                Lifecycle.Event.ON_RESUME -> if (!resumed) { mapView.onResume(); resumed = true }
                Lifecycle.Event.ON_PAUSE -> if (resumed) { mapView.onPause(); resumed = false }
                Lifecycle.Event.ON_STOP -> if (started) { mapView.onStop(); started = false }
                else -> {}
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            if (resumed) mapView.onPause()
            if (started) mapView.onStop()
            mapView.onDestroy()
        }
    }
}
