package com.break8t.unknown.ui.fielddrop

import androidx.compose.ui.graphics.toArgb
import com.break8t.unknown.ui.theme.AppColors
import org.json.JSONArray
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.BackgroundLayer
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.PropertyValue
import org.maplibre.android.style.layers.SymbolLayer

/** OpenFreeMap Liberty. 에뮬레이터에서 도로와 한글 지명이 뜨는 것을 확인한 스타일이다. */
const val MAP_STYLE_URL = "https://tiles.openfreemap.org/styles/liberty"

/** 지도 색. Figma 지도 그림(fielddrop_map_city)에서 뽑았고, 밤에는 같은 색을 어둡게 낮춘 값을 쓴다. */
object MapPalette {
    private fun pick(day: Long, dark: Long) = (if (AppColors.night) dark else day).toInt()
    val LAND get() = pick(0xFFBEAAA3, 0xFF3A302C)
    val EDGE get() = pick(0xFF806C68, 0xFF5E4E48)
    val ROAD get() = pick(0xFFFEEFD5, 0xFF6B5A48)
    val MAJOR_ROAD get() = pick(0xFFFEC28D, 0xFF9A6A3E)
    val WATER get() = pick(0xFF88B7DF, 0xFF2C4A66)
    val PARK get() = pick(0xFFB8C77C, 0xFF3E4A2A)
    val RAIL get() = pick(0xFFA6A2A1, 0xFF5A5654)
    val LABEL get() = AppColors.Text.toArgb()
}

enum class MapPaint { LAND, BUILDING, PARK, WATER, ROAD, MAJOR_ROAD, RAIL, LABEL, HIDE }

private val GREEN = setOf("landcover_wood", "landcover_grass", "landuse_pitch", "landuse_cemetery")

/** Liberty 레이어 하나를 어떤 색으로 칠할지. 입체 건물, 음영, 무늬, 아이콘, 경계선은 Figma 지도 그림에 없어서 뺀다. */
fun mapPaint(layerId: String, isSymbol: Boolean): MapPaint = with(layerId) {
    when {
        this in setOf("natural_earth", "building-3d", "landcover_wetland", "road_area_pattern", "airport") -> MapPaint.HIDE
        startsWith("poi") || startsWith("boundary") || contains("one_way") || contains("shield") -> MapPaint.HIDE
        isSymbol -> MapPaint.LABEL
        this == "building" -> MapPaint.BUILDING
        startsWith("park") || this in GREEN -> MapPaint.PARK
        this == "water" || startsWith("waterway") -> MapPaint.WATER
        contains("casing") -> MapPaint.LAND // Figma 그림의 길은 테두리가 없다.
        contains("rail") -> MapPaint.RAIL
        contains("motorway") || contains("trunk") -> MapPaint.MAJOR_ROAD
        startsWith("road") || startsWith("bridge") || startsWith("tunnel") || startsWith("aeroway_") && this != "aeroway_fill" -> MapPaint.ROAD
        else -> MapPaint.LAND
    }
}

private fun colorOf(paint: MapPaint): Int = when (paint) {
    MapPaint.PARK -> MapPalette.PARK
    MapPaint.WATER -> MapPalette.WATER
    MapPaint.ROAD, MapPaint.MAJOR_ROAD -> MapPalette.ROAD
    MapPaint.RAIL -> MapPalette.RAIL
    MapPaint.LABEL -> MapPalette.LABEL
    MapPaint.LAND, MapPaint.BUILDING, MapPaint.HIDE -> MapPalette.LAND
}

/** 불러온 Liberty 스타일을 앱 팔레트의 단색으로 다시 칠한다. 스타일 서버를 따로 두지 않으려고 실행 중에 바꾼다. */
fun recolor(style: Style) {
    style.layers.forEach { layer ->
        val paint = mapPaint(layer.id, layer is SymbolLayer)
        val color = colorOf(paint)
        when {
            paint == MapPaint.HIDE -> style.removeLayer(layer)
            layer is BackgroundLayer -> layer.setProperties(PropertyFactory.backgroundColor(color))
            layer is FillLayer -> layer.setProperties(
                PropertyFactory.fillColor(color),
                PropertyFactory.fillOutlineColor(if (paint == MapPaint.BUILDING) MapPalette.EDGE else color),
                PropertyFactory.fillOpacity(1f),
            )
            // 간선 레이어에 일반 도로도 섞여 있어서 도시고속도로와 고속도로만 Figma의 주황 길로 칠한다.
            layer is LineLayer && paint == MapPaint.MAJOR_ROAD -> layer.setProperties(
                PropertyFactory.lineColor(majorRoadColor()),
                PropertyFactory.lineOpacity(1f),
                PropertyFactory.lineBlur(0f),
            )
            layer is LineLayer -> layer.setProperties(
                PropertyFactory.lineColor(color),
                PropertyFactory.lineOpacity(1f),
                PropertyFactory.lineBlur(0f),
            )
            // Liberty는 로마자와 한글을 함께 적는다. 한글 이름만 남긴다.
            // 지도가 진행 방향으로 돌아가므로 도로 이름도 길을 따라 눕히지 않고 늘 화면 기준 가로로 쓴다.
            layer is SymbolLayer -> layer.setProperties(
                PropertyFactory.textField(Expression.coalesce(Expression.get("name:ko"), Expression.get("name"))),
                scaledTextSize(layer),
                // 도로는 구간과 차로마다 선이 따로라서, 선마다 반복해 찍으면 같은 이름이 줄줄이 나온다.
                // 선마다 가운데 한 번만 찍고, 글자 둘레를 넓게 비워 가까운 같은 이름은 하나만 남긴다.
                PropertyFactory.symbolPlacement(
                    if (layer.symbolPlacement.value == Property.SYMBOL_PLACEMENT_POINT) Property.SYMBOL_PLACEMENT_POINT
                    else Property.SYMBOL_PLACEMENT_LINE_CENTER,
                ),
                PropertyFactory.textPadding(LABEL_PADDING),
                PropertyFactory.textRotationAlignment(Property.TEXT_ROTATION_ALIGNMENT_VIEWPORT),
                PropertyFactory.textKeepUpright(true),
                PropertyFactory.textColor(color),
                PropertyFactory.textHaloColor(MapPalette.ROAD),
                PropertyFactory.textHaloBlur(0f),
            )
            else -> style.removeLayer(layer)
        }
    }
}

/** 차 화면에서 멀리 읽히도록 Liberty 글자를 키우는 배율. */
private const val LABEL_SCALE = 3f

/** 라벨 둘레에 비울 거리(dp). 이 안에 다른 라벨이 오면 하나만 찍는다. */
private const val LABEL_PADDING = 80f

/** Liberty가 정한 글자 크기의 [LABEL_SCALE]배. 줌 식이면 식 안의 크기 값만 바꾼다(줌 식은 맨 바깥에 있어야 한다). */
private fun scaledTextSize(layer: SymbolLayer): PropertyValue<*> {
    val size = layer.textSize
    size.expression?.let { return PropertyFactory.textSize(Expression.raw(scaleOutputs(JSONArray(it.toString()), LABEL_SCALE).toString())) }
    return PropertyFactory.textSize((size.value ?: DEFAULT_TEXT_SIZE) * LABEL_SCALE)
}

// interpolate는 [op, 방식, 입력, 기준, 값, ...]이라 4번째부터, step은 [op, 입력, 값, 기준, 값, ...]이라 2번째부터 한 칸 걸러 값이다.
private fun scaleOutputs(a: JSONArray, k: Float): JSONArray {
    var i = when (a.optString(0)) { "interpolate" -> 4; "step" -> 2; else -> return a }
    while (i < a.length()) {
        (a.opt(i) as? Number)?.let { a.put(i, it.toDouble() * k) }
        i += 2
    }
    return a
}

private const val DEFAULT_TEXT_SIZE = 16f // MapLibre의 text-size 기본값

private fun majorRoadColor(): Expression = Expression.match(
    Expression.get("class"),
    Expression.color(MapPalette.ROAD),
    Expression.stop("motorway", Expression.color(MapPalette.MAJOR_ROAD)),
    Expression.stop("trunk", Expression.color(MapPalette.MAJOR_ROAD)),
)

/** 타일을 못 받을 때 쓰는 스타일. 땅 색만 있어서 버섯과 내 차 표시는 그대로 놓인다. */
val OFFLINE_STYLE_JSON get() = """
    {"version":8,"sources":{},"layers":[{"id":"background","type":"background",
    "paint":{"background-color":"#%06X"}}]}
""".trimIndent().format(MapPalette.LAND and 0xFFFFFF)
