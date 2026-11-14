package com.break8t.unknown.ui.iso

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/** Voxels along one cell edge. Shapes are authored in voxels; the grid is in cells. */
const val VPC = 8

/** A solid block of voxels, z up. [emissive] replaces the color at night (windows, lamps). */
@Immutable
data class Cuboid(
    val x: Int, val y: Int, val z: Int,
    val w: Int, val d: Int, val h: Int,
    val color: Color,
    val emissive: Color? = null,
) {
    /** Turns the block by [r] quarter turns (x toward y) inside an [fw] x [fd] voxel footprint. */
    fun rotated(fw: Int, fd: Int, r: Int): Cuboid = when (r.mod(4)) {
        1 -> copy(x = fd - y - d, y = x, w = d, d = w)
        2 -> copy(x = fw - x - w, y = fd - y - d)
        3 -> copy(x = y, y = fw - x - w, w = d, d = w)
        else -> this
    }
}

/**
 * A voxel model on a [w] x [d] cell footprint, built from [parts] in order (later parts overwrite).
 * Wall-mounted shapes hang on local y = 0.
 */
@Immutable
data class IsoShape(val w: Int, val d: Int, val parts: List<Cuboid>, val wallMounted: Boolean = false) {
    /** Flat shapes (rug, path, pond) lie on the floor, so furniture may stand on them. */
    val flat: Boolean get() = model.sz <= 1
    val model: VoxelModel by lazy { VoxelModel.of(this) }
}

/** Filled voxels and their exposed faces. Faces are packed as x, y, z, direction, palette index. */
class VoxelModel(val sx: Int, val sy: Int, val sz: Int, val day: IntArray, val glow: IntArray, val faces: IntArray) {
    val faceCount: Int get() = faces.size / 5

    companion object {
        /** Direction vectors for +X, -X, +Y, -Y, +Z. The bottom is never visible, so it has no faces. */
        val DX = intArrayOf(1, -1, 0, 0, 0)
        val DY = intArrayOf(0, 0, 1, -1, 0)
        val DZ = intArrayOf(0, 0, 0, 0, 1)

        fun of(shape: IsoShape): VoxelModel {
            val sx = shape.w * VPC
            val sy = shape.d * VPC
            val sz = maxOf(1, shape.parts.maxOfOrNull { it.z + it.h } ?: 1)
            val keys = ArrayList<Pair<Color, Color?>>()
            val grid = ByteArray(sx * sy * sz)
            for (c in shape.parts) {
                // A transparent part carves voxels out, which is how corners get rounded.
                val v = if (c.color.alpha == 0f) 0 else {
                    val key = c.color to c.emissive
                    var k = keys.indexOf(key)
                    if (k < 0) { keys += key; k = keys.lastIndex }
                    k + 1
                }
                for (z in c.z until c.z + c.h) for (y in c.y until c.y + c.d) for (x in c.x until c.x + c.w) {
                    if (x in 0 until sx && y in 0 until sy && z in 0 until sz) grid[x + sx * (y + sy * z)] = v.toByte()
                }
            }
            fun at(x: Int, y: Int, z: Int) =
                if (x in 0 until sx && y in 0 until sy && z in 0 until sz) grid[x + sx * (y + sy * z)].toInt() else 0
            val faces = ArrayList<Int>()
            for (z in 0 until sz) for (y in 0 until sy) for (x in 0 until sx) {
                val v = at(x, y, z)
                if (v == 0) continue
                for (dir in 0 until 5) if (at(x + DX[dir], y + DY[dir], z + DZ[dir]) == 0) {
                    faces += x; faces += y; faces += z; faces += dir; faces += v - 1
                }
            }
            return VoxelModel(
                sx, sy, sz,
                IntArray(keys.size) { keys[it].first.toArgb() },
                IntArray(keys.size) { keys[it].second?.toArgb() ?: 0 },
                faces.toIntArray(),
            )
        }
    }
}

/** NORTH is the y = 0 edge, WEST the x = 0 edge. The ordinal is the rotation a wall object takes. */
enum class WallSide { NORTH, EAST, SOUTH, WEST }

enum class FloorType { NONE, GRASS, SAND, SOIL, WATER, WOOD, ROAD }

/** [depth] is how far this cell's slab reaches below its top; negative means the scene's floorDepth. */
@Immutable
data class Cell(val floor: FloorType = FloorType.GRASS, val height: Float = 0f, val depth: Float = -1f)

@Immutable
data class Placement(val id: String, val x: Int, val y: Int, val rotation: Int, val wall: WallSide? = null)

/**
 * [id] is unique in the scene and is what taps report. [x], [y] is the footprint's minimum corner.
 * [ox], [oy], [z] shift the drawing (cells) without moving the footprint; [scale] resizes it.
 * Objects that are not [pickable] (scenery) are drawn but taps fall through to the floor.
 */
@Immutable
data class IsoObject(
    val id: String,
    val shape: IsoShape,
    val x: Int,
    val y: Int,
    val rotation: Int = 0,
    val wall: WallSide? = null,
    val ox: Float = 0f,
    val oy: Float = 0f,
    val z: Float = 0f,
    val scale: Float = 1f,
    val pickable: Boolean = true,
) {
    val rot: Int get() = wall?.ordinal ?: rotation.mod(4)
    val footW: Int get() = if (rot % 2 == 0) shape.w else shape.d
    val footD: Int get() = if (rot % 2 == 0) shape.d else shape.w
    val placement: Placement get() = Placement(id, x, y, rot, wall)
    fun moved(p: Placement) = copy(x = p.x, y = p.y, rotation = p.rotation, wall = p.wall)
}

/**
 * Row-major [cells], slabs [floorDepth] deep; [wallHeight] > 0 puts walls on the sides facing away from the camera.
 * [frame], when set, is the world box the camera fits instead of the whole grid.
 * Floor outlines go round [tile] x [tile] blocks, for scenes built finer than the tiles they show.
 */
@Immutable
data class IsoScene(
    val width: Int,
    val depth: Int,
    val cells: List<Cell> = List(width * depth) { Cell() },
    val objects: List<IsoObject> = emptyList(),
    val wallHeight: Float = 0f,
    val floorDepth: Float = 0.5f,
    val frame: FrameBox? = null,
    val roads: List<RoadRay> = emptyList(),
    val tile: Int = 1,
) {
    init {
        require(cells.size == width * depth) { "cells must be width * depth" }
    }

    fun inside(x: Int, y: Int) = x in 0 until width && y in 0 until depth
    fun cell(x: Int, y: Int): Cell = cells[y * width + x]
    fun find(id: String): IsoObject? = objects.firstOrNull { it.id == id }
    fun baseZ(o: IsoObject): Float = (if (inside(o.x, o.y)) cell(o.x, o.y).height else 0f) + o.z

    /** Bottom of the slab under cell ([x], [y]). */
    fun bottom(x: Int, y: Int): Float = cell(x, y).let { if (it.depth < 0f) -floorDepth else it.height - it.depth }

    fun with(p: Placement): IsoScene = copy(objects = objects.map { if (it.id == p.id) it.moved(p) else it })

    /** A ready-made validator: inside the grid, on solid floor, against its wall, not on a same-layer object. */
    fun isFree(p: Placement): Boolean {
        val o = find(p.id)?.moved(p) ?: return false
        if (o.x < 0 || o.y < 0 || o.x + o.footW > width || o.y + o.footD > depth) return false
        if (o.wall != null && snapToWall(o.x, o.y, o) != (o.x to o.y)) return false
        for (cy in o.y until o.y + o.footD) for (cx in o.x until o.x + o.footW) {
            val f = cell(cx, cy).floor
            if (o.wall == null && (f == FloorType.NONE || f == FloorType.WATER)) return false
        }
        return objects.none { it.id != o.id && it.pickable && sameLayer(it, o) && overlaps(it, o) }
    }

    /** Forces a wall object onto its wall's edge row or column. */
    fun snapToWall(x: Int, y: Int, o: IsoObject): Pair<Int, Int> = when (o.wall) {
        WallSide.NORTH -> x to 0
        WallSide.SOUTH -> x to depth - o.footD
        WallSide.WEST -> 0 to y
        WallSide.EAST -> width - o.footW to y
        null -> x to y
    }

    /** Placement for dragging [o] with the finger over cell ([cx], [cy]), grabbed [grabX], [grabY] cells from its origin. */
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

/**
 * A road that starts at cell ([x], [y]), usually just outside the grid, and runs on in direction ([dx], [dy])
 * for as long as the frame being drawn can see it, whatever its size, camera or pan. [paint] is drawn on each cell.
 */
@Immutable
data class RoadRay(val x: Int, val y: Int, val dx: Int, val dy: Int, val depth: Float, val paint: IsoShape? = null)

/** A world box in cells, x east, y south, z up. */
@Immutable
data class FrameBox(val x0: Float, val y0: Float, val x1: Float, val y1: Float, val z0: Float, val z1: Float)
