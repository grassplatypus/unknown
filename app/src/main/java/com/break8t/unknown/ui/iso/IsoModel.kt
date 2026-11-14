package com.break8t.unknown.ui.iso

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/** A box in local cell units, z up. [emissive] replaces the color at night (windows, lamps). */
@Immutable
data class Cuboid(
    val x: Float, val y: Float, val z: Float,
    val w: Float, val d: Float, val h: Float,
    val color: Color,
    val emissive: Color? = null,
) {
    /** Turns the box by [r] quarter turns clockwise (seen from above) inside a [fw] x [fd] footprint. */
    fun rotated(fw: Int, fd: Int, r: Int): Cuboid = when (r.mod(4)) {
        1 -> copy(x = fd - y - d, y = x, w = d, d = w)
        2 -> copy(x = fw - x - w, y = fd - y - d)
        3 -> copy(x = y, y = fw - x - w, w = d, d = w)
        else -> this
    }
}

/** Parts of one object in its unrotated [w] x [d] footprint. Wall-mounted shapes hang on local y = 0. */
@Immutable
data class IsoShape(val w: Int, val d: Int, val parts: List<Cuboid>, val wallMounted: Boolean = false) {
    /** Flat shapes (rug, path, pond) lie on the floor, so furniture may stand on them. */
    val flat: Boolean = parts.all { it.z + it.h <= FLAT_HEIGHT }
}

/** NORTH is the y = 0 edge, WEST the x = 0 edge. The ordinal is the rotation a wall object takes. */
enum class WallSide { NORTH, EAST, SOUTH, WEST }

enum class FloorType { NONE, GRASS, SAND, SOIL, WATER, WOOD }

@Immutable
data class Cell(val floor: FloorType = FloorType.GRASS, val height: Float = 0f)

@Immutable
data class Placement(val id: String, val x: Int, val y: Int, val rotation: Int, val wall: WallSide? = null)

/** [id] is unique in the scene and is what taps report. [x], [y] is the footprint's minimum corner. */
@Immutable
data class IsoObject(
    val id: String,
    val shape: IsoShape,
    val x: Int,
    val y: Int,
    val rotation: Int = 0,
    val wall: WallSide? = null,
) {
    val rot: Int get() = wall?.ordinal ?: rotation.mod(4)
    val footW: Int get() = if (rot % 2 == 0) shape.w else shape.d
    val footD: Int get() = if (rot % 2 == 0) shape.d else shape.w
    val placement: Placement get() = Placement(id, x, y, rot, wall)
    fun moved(p: Placement) = copy(x = p.x, y = p.y, rotation = p.rotation, wall = p.wall)
}

/** Row-major [cells]. [wallHeight] > 0 puts walls on the sides facing away from the camera. */
@Immutable
data class IsoScene(
    val width: Int,
    val depth: Int,
    val cells: List<Cell> = List(width * depth) { Cell() },
    val objects: List<IsoObject> = emptyList(),
    val wallHeight: Float = 0f,
) {
    init {
        require(cells.size == width * depth) { "cells must be width * depth" }
    }

    fun inside(x: Int, y: Int) = x in 0 until width && y in 0 until depth
    fun cell(x: Int, y: Int): Cell = cells[y * width + x]
    fun find(id: String): IsoObject? = objects.firstOrNull { it.id == id }
    fun baseZ(o: IsoObject): Float = if (inside(o.x, o.y)) cell(o.x, o.y).height else 0f

    fun with(p: Placement): IsoScene = copy(objects = objects.map { if (it.id == p.id) it.moved(p) else it })

    /** A ready-made validator: inside the grid, on solid floor, against its wall, not on top of a same-layer object. */
    fun isFree(p: Placement): Boolean {
        val o = find(p.id)?.moved(p) ?: return false
        if (o.x < 0 || o.y < 0 || o.x + o.footW > width || o.y + o.footD > depth) return false
        if (o.wall != null && snapToWall(o.x, o.y, o) != (o.x to o.y)) return false
        for (cy in o.y until o.y + o.footD) for (cx in o.x until o.x + o.footW) {
            val f = cell(cx, cy).floor
            if (o.wall == null && (f == FloorType.NONE || f == FloorType.WATER)) return false
        }
        return objects.none { it.id != o.id && sameLayer(it, o) && overlaps(it, o) }
    }

    /** Forces a wall object onto its wall's edge row or column. */
    fun snapToWall(x: Int, y: Int, o: IsoObject): Pair<Int, Int> = when (o.wall) {
        WallSide.NORTH -> x to 0
        WallSide.SOUTH -> x to depth - o.footD
        WallSide.WEST -> 0 to y
        WallSide.EAST -> width - o.footW to y
        null -> x to y
    }

    /** The placement for dragging [o] when the finger is over cell ([cx], [cy]) and grabbed it [grabX], [grabY] cells from its origin. */
    fun snap(o: IsoObject, cx: Int, cy: Int, grabX: Int, grabY: Int): Placement {
        val (x, y) = snapToWall(cx - grabX, cy - grabY, o)
        return Placement(o.id, x, y, o.rot, o.wall)
    }

    /** The next quarter turn of [id], or null for wall objects, which keep facing out of their wall. */
    fun turned(id: String): Placement? {
        val o = find(id) ?: return null
        if (o.wall != null) return null
        return Placement(id, o.x, o.y, (o.rot + 1) % 4)
    }

    private fun sameLayer(a: IsoObject, b: IsoObject) =
        if (a.wall != null || b.wall != null) a.wall == b.wall else a.shape.flat == b.shape.flat

    private fun overlaps(a: IsoObject, b: IsoObject) =
        a.x < b.x + b.footW && b.x < a.x + a.footW && a.y < b.y + b.footD && b.y < a.y + a.footD
}

internal const val FLAT_HEIGHT = 0.2f
