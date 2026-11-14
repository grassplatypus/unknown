package com.break8t.unknown.ui.fielddrop

import androidx.compose.ui.graphics.toArgb
import com.break8t.unknown.ui.theme.AppColors
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.BackgroundLayer
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer

/** OpenFreeMap Liberty. 에뮬레이터에서 도로와 한글 지명이 뜨는 것을 확인한 스타일이다. */
const val MAP_STYLE_URL = "https://tiles.openfreemap.org/styles/liberty"

/** 지도 색. Figma 지도 그림(fielddrop_map_city)에서 뽑았고, 글자는 AppColors를 쓴다. */
object MapPalette {
    const val LAND = 0xFFBEAAA3.toInt()
    const val EDGE = 0xFF806C68.toInt()
    const val ROAD = 0xFFFEEFD5.toInt()
    const val MAJOR_ROAD = 0xFFFEC28D.toInt()
    const val WATER = 0xFF88B7DF.toInt()
    const val PARK = 0xFFB8C77C.toInt()
    const val RAIL = 0xFFA6A2A1.toInt()
    val LABEL = AppColors.Text.toArgb()
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
            layer is SymbolLayer -> layer.setProperties(
                PropertyFactory.textField(Expression.coalesce(Expression.get("name:ko"), Expression.get("name"))),
                PropertyFactory.textColor(color),
                PropertyFactory.textHaloColor(MapPalette.ROAD),
                PropertyFactory.textHaloBlur(0f),
            )
            else -> style.removeLayer(layer)
        }
    }
}

private fun majorRoadColor(): Expression = Expression.match(
    Expression.get("class"),
    Expression.color(MapPalette.ROAD),
    Expression.stop("motorway", Expression.color(MapPalette.MAJOR_ROAD)),
    Expression.stop("trunk", Expression.color(MapPalette.MAJOR_ROAD)),
)

/** 타일을 못 받을 때 쓰는 스타일. 땅 색만 있어서 버섯과 내 차 표시는 그대로 놓인다. */
val OFFLINE_STYLE_JSON = """
    {"version":8,"sources":{},"layers":[{"id":"background","type":"background",
    "paint":{"background-color":"#%06X"}}]}
""".trimIndent().format(MapPalette.LAND and 0xFFFFFF)
