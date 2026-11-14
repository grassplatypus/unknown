package com.break8t.unknown.ui.fielddrop

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
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
import org.maplibre.android.style.layers.PropertyFactory.iconRotate
import org.maplibre.android.style.layers.PropertyFactory.iconRotationAlignment
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

// Figma px x 1.3, same sizes the Compose markers used.
internal const val CAR_DP = 114f
internal const val CAR_TAG_TOP_DP = CAR_DP / 2 + 5f // 그림 아래 5dp
private const val CARD_DP = 109f
private const val MUSH_DP = 83f
private const val CARD_RADIUS_DP = 18f
private const val BORDER_DP = 3f
private const val AD_GAP_DP = 8f
internal const val DIM_ALPHA = 0.45f

internal const val MY_CAR_TAG = "fd_tag_my_car"
private const val ID = "id"
private const val ICON = "icon"
private const val ROTATE = "rotate"
private const val DIM = "dim"
// 차는 돌려 그려서 늘 선형 보간을 쓴다. 지도 엔진이 소스마다 그림 묶음을 따로 두니 버섯과 소스를 나눈다.
private const val CAR_SOURCE = "fd-car"
private const val TAG_SOURCE = "fd-car-tag"
private const val MUSH_SOURCE = "fd-mush"
private const val MUSH_LAYER = "fd-mush"

/** 표시 그림을 화면 픽셀 크기로 미리 그린다. 지도는 같은 배율이면 1:1로 찍어서 픽셀 그림이 뭉개지지 않는다. */
fun markerImages(context: Context): Map<String, Bitmap> {
    val res = context.resources
    val dm = res.displayMetrics
    fun px(dp: Float) = (dp * dm.density).roundToInt()
    fun sp(v: Float) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, v, dm)
    val scaled = mutableMapOf<Pair<Int, Int>, Bitmap>()
    fun pixel(@DrawableRes id: Int, sizePx: Int): Bitmap = scaled.getOrPut(id to sizePx) {
        val src = BitmapFactory.decodeResource(res, id, BitmapFactory.Options().apply { inScaled = false })
        Bitmap.createScaledBitmap(src, sizePx, sizePx, false) // 보간 없이
    }
    fun tag(text: String, heightDp: Float, textSp: Float, padDp: Float, radiusPx: Float, fill: Int): Bitmap {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = sp(textSp); typeface = Typeface.DEFAULT_BOLD; color = AppColors.Text.toArgb() }
        val h = px(heightDp)
        val b = Bitmap.createBitmap(ceil(paint.measureText(text)).toInt() + 2 * px(padDp), h, Bitmap.Config.ARGB_8888)
        Canvas(b).apply {
            box(0f, 0f, b.width.toFloat(), h.toFloat(), radiusPx, fill, AppColors.Line.toArgb(), px(BORDER_DP).toFloat())
            val fm = paint.fontMetrics
            drawText(text, px(padDp).toFloat(), h / 2f - (fm.ascent + fm.descent) / 2, paint)
        }
        return b
    }
    fun mushroom(@DrawableRes art: Int, ad: Boolean, selected: Boolean): Bitmap {
        val card = px(CARD_DP)
        val adTag = if (ad) tag(context.getString(R.string.fielddrop_ad), 42f, 23f, 13f, px(42f) / 2f, AppColors.Accent.toArgb()) else null
        val w = max(card, adTag?.width ?: 0)
        val b = Bitmap.createBitmap(w, card + (adTag?.let { px(AD_GAP_DP) + it.height } ?: 0), Bitmap.Config.ARGB_8888)
        Canvas(b).apply {
            val left = (w - card) / 2f
            val (edge, width) = if (selected) AppColors.Selected to px(BORDER_DP * 2) else AppColors.Line to px(BORDER_DP) // S-10-b
            box(left, 0f, left + card, card.toFloat(), px(CARD_RADIUS_DP).toFloat(), AppColors.Card.toArgb(), edge.toArgb(), width.toFloat())
            val m = pixel(art, px(MUSH_DP))
            drawBitmap(m, ((w - m.width) / 2).toFloat(), ((card - m.height) / 2).toFloat(), null)
            adTag?.let { drawBitmap(it, ((w - it.width) / 2).toFloat(), (card + px(AD_GAP_DP)).toFloat(), null) }
        }
        return b
    }
    val images = buildMap {
        for (kind in MushroomKind.entries) {
            val art = if (kind == MushroomKind.GOLD) R.drawable.fielddrop_mush_gold else R.drawable.fielddrop_mush_blue
            for (ad in listOf(false, true)) for (sel in listOf(false, true)) put(mushroomIcon(kind, ad, sel), mushroom(art, ad, sel))
        }
        put("fd_car_red", pixel(R.drawable.fielddrop_car_red, px(CAR_DP)))
        put("fd_car_blue", pixel(R.drawable.fielddrop_car_blue, px(CAR_DP)))
        put("fd_car_yellow", pixel(R.drawable.fielddrop_car_yellow, px(CAR_DP)))
        put(MY_CAR_TAG, tag(context.getString(R.string.fielddrop_my_car), 57f, 31f, 18f, px(16f).toFloat(), AppColors.Card.toArgb()))
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

/** 다시 칠한 뒤에 부른다. recolor는 모르는 레이어를 지운다. 차, 내 차 글자, 버섯 순서라 버섯이 맨 위다. */
fun addMarkerLayers(style: Style, images: Map<String, Bitmap>) {
    style.addImages(HashMap(images))
    listOf(CAR_SOURCE, TAG_SOURCE, MUSH_SOURCE).forEach { style.addSource(GeoJsonSource(it)) }
    style.addLayer(
        SymbolLayer(CAR_SOURCE, CAR_SOURCE).withProperties(
            iconImage(get(ICON)),
            iconRotate(get(ROTATE)),
            iconRotationAlignment(Property.ICON_ROTATION_ALIGNMENT_MAP),
            iconAllowOverlap(true),
            iconIgnorePlacement(true),
        ),
    )
    // 아래 두 레이어에는 크기와 회전을 두지 않는다. 멈춘 지도에서 보간 없이 찍히는 조건이다.
    style.addLayer(
        SymbolLayer(TAG_SOURCE, TAG_SOURCE).withProperties(
            iconImage(MY_CAR_TAG),
            iconAnchor(Property.ICON_ANCHOR_TOP),
            iconOffset(arrayOf(0f, CAR_TAG_TOP_DP)),
            iconAllowOverlap(true),
            iconIgnorePlacement(true),
        ),
    )
    style.addLayer(
        SymbolLayer(MUSH_LAYER, MUSH_SOURCE).withProperties(
            iconImage(get(ICON)),
            iconAnchor(Property.ICON_ANCHOR_BOTTOM), // 핀처럼 아래 끝이 그 자리를 가리킨다.
            iconOpacity(switchCase(get(DIM), literal(DIM_ALPHA), literal(1f))),
            iconAllowOverlap(true),
            iconIgnorePlacement(true),
        ),
    )
}

fun pushMarkers(style: Style, m: MapMarkers) {
    if (!style.isFullyLoaded) return
    val car = Point.fromLngLat(m.car.lng, m.car.lat)
    style.getSourceAs<GeoJsonSource>(CAR_SOURCE)?.setGeoJson(
        Feature.fromGeometry(car).apply { addStringProperty(ICON, m.carIcon); addNumberProperty(ROTATE, m.carRotate) },
    )
    style.getSourceAs<GeoJsonSource>(TAG_SOURCE)?.setGeoJson(Feature.fromGeometry(car))
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
