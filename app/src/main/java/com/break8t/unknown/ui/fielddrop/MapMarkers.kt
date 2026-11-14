package com.break8t.unknown.ui.fielddrop

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.RectF
import android.graphics.Typeface
import android.util.TypedValue
import android.view.ViewConfiguration
import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.toArgb
import com.break8t.unknown.R
import com.break8t.unknown.ui.theme.AppColors
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression.get
import org.maplibre.android.style.expressions.Expression.literal
import org.maplibre.android.style.expressions.Expression.switchCase
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory.iconAllowOverlap
import org.maplibre.android.style.layers.PropertyFactory.iconAnchor
import org.maplibre.android.style.layers.PropertyFactory.iconIgnorePlacement
import org.maplibre.android.style.layers.PropertyFactory.iconImage
import org.maplibre.android.style.layers.PropertyFactory.iconOffset
import org.maplibre.android.style.layers.PropertyFactory.iconOpacity
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

// Figma px x 1.3, same sizes the Compose markers used.
internal const val CAR_DP = 90f
private const val CARD_DP = 109f
private const val MUSH_DP = 83f
private const val CARD_RADIUS_DP = 18f
private const val BORDER_DP = 3f
private const val TAIL_W_DP = 30f
private const val TAIL_H_DP = 20f
internal const val DIM_ALPHA = 0.45f

private const val ID = "id"
private const val ICON = "icon"
private const val DIM = "dim"
private const val CAR_IMAGE = "fd_car"
private const val CAR_SOURCE = "fd-car"
private const val MUSH_SOURCE = "fd-mush"
private const val MUSH_LAYER = "fd-mush"

/** 표시 그림을 화면 픽셀 크기로 미리 그린다. 지도는 같은 배율이면 1:1로 찍어서 픽셀 그림이 뭉개지지 않는다. */
fun markerImages(context: Context): Map<String, Bitmap> {
    val res = context.resources
    val dm = res.displayMetrics
    fun px(dp: Float) = (dp * dm.density).roundToInt()
    val scaled = mutableMapOf<Pair<Int, Int>, Bitmap>()
    fun pixel(@DrawableRes id: Int, sizePx: Int): Bitmap = scaled.getOrPut(id to sizePx) {
        val src = BitmapFactory.decodeResource(res, id, BitmapFactory.Options().apply { inScaled = false })
        Bitmap.createScaledBitmap(src, sizePx, sizePx, false) // 보간 없이
    }
    // 말풍선: 카드 아래 꼬리 끝이 버섯 자리를 가리킨다(레이어가 그림 아래 끝을 그 자리에 둔다).
    // 광고 표시는 지도에서 빼고 버섯 카드(S-10-b)에만 둔다.
    fun mushroom(@DrawableRes art: Int, selected: Boolean): Bitmap {
        val card = px(CARD_DP)
        val tailW = px(TAIL_W_DP).toFloat()
        val tailH = px(TAIL_H_DP).toFloat()
        val b = Bitmap.createBitmap(card, card + tailH.toInt(), Bitmap.Config.ARGB_8888)
        Canvas(b).apply {
            val (edge, width) = if (selected) AppColors.Selected to px(BORDER_DP * 2) else AppColors.Line to px(BORDER_DP) // S-10-b
            box(0f, 0f, card.toFloat(), card.toFloat(), px(CARD_RADIUS_DP).toFloat(), AppColors.Card.toArgb(), edge.toArgb(), width.toFloat())
            val cx = card / 2f
            val top = card - width.toFloat()
            fun tri(half: Float, tip: Float, color: Int) = drawPath(
                Path().apply { moveTo(cx - half, top); lineTo(cx + half, top); lineTo(cx, tip); close() },
                Paint().apply { this.color = color }, // 보간 없이 계단 모양 가장자리
            )
            tri(tailW / 2 + width, card + tailH, edge.toArgb())
            tri(tailW / 2, card + tailH - width * 2f, AppColors.Card.toArgb())
            val m = pixel(art, px(MUSH_DP))
            drawBitmap(m, ((card - m.width) / 2).toFloat(), ((card - m.height) / 2).toFloat(), null)
        }
        return b
    }
    val images = buildMap {
        for (kind in MushroomKind.entries) {
            val art = if (kind == MushroomKind.GOLD) R.drawable.fielddrop_mush_gold else R.drawable.fielddrop_mush_blue
            for (sel in listOf(false, true)) {
                val bubble = mushroom(art, sel)
                for (ad in listOf(false, true)) put(mushroomIcon(kind, ad, sel), bubble)
            }
        }
        // 칸 하나를 정수 픽셀로 키워야 보간 없이 고르게 찍힌다.
        val cell = maxOf(1, (px(CAR_DP) / TOP_CAR_CELLS.toFloat()).roundToInt())
        for (icon in listOf("fd_car_red", "fd_car_blue", "fd_car_yellow")) {
            val small = Bitmap.createBitmap(topCarCells(topCarBody(icon)), TOP_CAR_CELLS, TOP_CAR_CELLS, Bitmap.Config.ARGB_8888)
            put(icon, Bitmap.createScaledBitmap(small, TOP_CAR_CELLS * cell, TOP_CAR_CELLS * cell, false))
        }
    }
    // MapLibre는 그림 배율을 bitmap.density로 읽는다. 지도 배율과 다르면 선형 보간으로 바뀐다.
    images.values.forEach { it.density = dm.densityDpi }
    return images
}

/** Compose의 clip + background + border와 같은 모양. 테두리는 안쪽에 그린다. */
private fun Canvas.box(l: Float, t: Float, r: Float, b: Float, radius: Float, fill: Int, edge: Int, width: Float) {
    val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = fill }
    drawRoundRect(RectF(l, t, r, b), radius, radius, p)
    p.apply { color = edge; style = Paint.Style.STROKE; strokeWidth = width }
    val h = width / 2
    drawRoundRect(RectF(l + h, t + h, r - h, b - h), radius - h, radius - h, p)
}

/** 다시 칠한 뒤에 부른다. recolor는 모르는 레이어를 지운다. 버섯, 차 순서라 차가 맨 위다. */
fun addMarkerLayers(style: Style, images: Map<String, Bitmap>) {
    style.addImages(HashMap(images))
    listOf(CAR_SOURCE, MUSH_SOURCE).forEach { style.addSource(GeoJsonSource(it)) }
    // 두 레이어 모두 크기와 회전을 두지 않는다. 멈춘 지도에서 보간 없이 찍히는 조건이다(차는 pushCar가 미리 돌린다).
    style.addLayer(
        SymbolLayer(MUSH_LAYER, MUSH_SOURCE).withProperties(
            iconImage(get(ICON)),
            iconAnchor(Property.ICON_ANCHOR_BOTTOM), // 핀처럼 아래 끝이 그 자리를 가리킨다.
            iconOpacity(switchCase(get(DIM), literal(DIM_ALPHA), literal(1f))),
            iconAllowOverlap(true),
            iconIgnorePlacement(true),
        ),
    )
    // 차는 버섯보다 나중에 올려서 언제나 맨 위에 보인다.
    style.addLayer(
        SymbolLayer(CAR_SOURCE, CAR_SOURCE).withProperties(
            iconImage(CAR_IMAGE),
            iconAllowOverlap(true),
            iconIgnorePlacement(true),
        ),
    )
}

/** [rotate]는 화면 기준 각도다(지도가 돈 만큼 뺀 값). 지도에 회전을 맡기면 선형 보간이 걸려서 미리 돌린다. */
fun pushCar(style: Style, images: Map<String, Bitmap>, carIcon: String, rotate: Float) {
    if (style.isFullyLoaded) images[carIcon]?.let { style.addImage(CAR_IMAGE, rotatedCar(it, rotate)) }
}

/** 보간 없이(가장 가까운 픽셀) 돌린다. Compose의 FilterQuality.None + rotate와 같은 결과다. */
fun rotatedCar(base: Bitmap, deg: Float): Bitmap {
    val side = rotatedSidePx(base.width, deg)
    val out = Bitmap.createBitmap(side, side, Bitmap.Config.ARGB_8888)
    val nearest = Paint().apply { isFilterBitmap = false; isAntiAlias = false }
    Canvas(out).apply {
        rotate(deg, side / 2f, side / 2f)
        drawBitmap(base, (side - base.width) / 2f, (side - base.height) / 2f, nearest)
    }
    out.density = base.density
    return out
}

fun pushMarkers(style: Style, m: MapMarkers) {
    if (!style.isFullyLoaded) return
    val car = Point.fromLngLat(m.car.lng, m.car.lat)
    style.getSourceAs<GeoJsonSource>(CAR_SOURCE)?.setGeoJson(
        Feature.fromGeometry(car),
    )
    style.getSourceAs<GeoJsonSource>(MUSH_SOURCE)?.setGeoJson(
        FeatureCollection.fromFeatures(
            m.mushrooms.map { f ->
                Feature.fromGeometry(Point.fromLngLat(f.at.lng, f.at.lat)).apply {
                    addStringProperty(ID, f.id)
                    addStringProperty(ICON, f.icon)
                    addBooleanProperty(DIM, f.dim)
                }
            },
        ),
    )
}

/** 누른 점 둘레(터치 허용 거리)에 그려진 버섯 id. 맨 위에 그려진 것이 먼저 온다. */
fun mushroomIdsAt(map: MapLibreMap, context: Context, at: PointF): List<String?> {
    val slop = ViewConfiguration.get(context).scaledTouchSlop.toFloat()
    return map.queryRenderedFeatures(RectF(at.x - slop, at.y - slop, at.x + slop, at.y + slop), MUSH_LAYER)
        .map { if (it.hasProperty(ID)) it.getStringProperty(ID) else null }
}
